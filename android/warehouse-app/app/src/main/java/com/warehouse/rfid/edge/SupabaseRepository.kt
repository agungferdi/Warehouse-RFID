package com.warehouse.rfid.edge

import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.temporal.ChronoUnit

sealed class ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>()
    data class Failure(val message: String) : ApiResult<Nothing>()
}

data class BatchItemResult(
    val epc: String,
    val status: String,
    val reason: String?,
    val productName: String?
)

data class BatchSubmitResult(
    val acceptedCount: Int,
    val rejectedCount: Int,
    val unknownCount: Int,
    val items: List<BatchItemResult>
)

@Serializable
private data class ProductRow(
    val epc: String,
    val sku: String,
    @SerialName("product_name") val productName: String,
    val quantity: Int,
    val location: String? = null,
    val status: String
)

@Serializable
private data class LocationRow(val location: String? = null)

@Serializable
private data class StatusRow(val status: String, val location: String? = null)

@Serializable
private data class ActivityRow(
    @SerialName("activity_type") val activityType: String,
    @SerialName("activity_time") val activityTime: String
)

@Serializable
private data class RpcResult(
    val epc: String,
    val status: String,
    val reason: String? = null,
    val sku: String? = null,
    val productName: String? = null,
    val quantity: Int? = null,
    val location: String? = null
)

@Serializable
private data class RegisterParams(
    @SerialName("p_epc") val epc: String,
    @SerialName("p_sku") val sku: String,
    @SerialName("p_product_name") val productName: String,
    @SerialName("p_quantity") val quantity: Int,
    @SerialName("p_location") val location: String?
)

// Deliberately no p_quantity: every EPC is 1 physical unit, so record-activity RPCs use the
// 2-arg overload, which records the product's own already-registered quantity (always 1)
// rather than letting the client overwrite it. See register_product_tag / RegisterParams for
// where quantity is actually set — once, at registration.
@Serializable
private data class LocationParams(
    @SerialName("p_epc") val epc: String,
    @SerialName("p_location") val location: String?
)

@Serializable
private data class DestinationParams(
    @SerialName("p_epc") val epc: String,
    @SerialName("p_destination") val destination: String
)

class SupabaseRepository {
    private val postgrest get() = SupabaseModule.client.postgrest

    suspend fun fetchStats(): ApiResult<DashboardStats> = withContext(Dispatchers.IO) {
        try {
            val statusRows = postgrest.from("products").select(columns = Columns.list("status", "location")).decodeList<StatusRow>()
            val available = statusRows.count { it.status == "available" }
            val sold = statusRows.count { it.status == "sold" }
            val inTransit = statusRows.count { it.status == "in_transit" }

            val locationStats = statusRows
                .groupBy { it.location?.trim()?.ifEmpty { null } ?: "Unassigned" }
                .map { (location, rows) ->
                    LocationStockStats(
                        location = location,
                        available = rows.count { it.status == "available" },
                        sold = rows.count { it.status == "sold" },
                        inTransit = rows.count { it.status == "in_transit" },
                    )
                }
                .sortedBy { it.location }

            val cutoff = java.time.Instant.now().minus(7, ChronoUnit.DAYS).toString()
            val activityRows = postgrest.from("activities")
                .select(columns = Columns.list("activity_type", "activity_time")) {
                    filter { gte("activity_time", cutoff) }
                }.decodeList<ActivityRow>()

            val byDate = activityRows.groupBy { it.activityTime.substring(0, 10) }
            val today = LocalDate.now()
            val activityDays = (6 downTo 0).map { offset ->
                val date = today.minusDays(offset.toLong())
                val key = date.toString()
                val dayRows = byDate[key] ?: emptyList()
                ActivityDayCount(
                    date = key,
                    inbound = dayRows.count { it.activityType == "inbound" },
                    stockOpname = dayRows.count { it.activityType == "stock_opname" },
                    transfer = dayRows.count { it.activityType == "transfer" },
                    outbound = dayRows.count { it.activityType == "outbound" }
                )
            }

            ApiResult.Success(
                DashboardStats(
                    available = available,
                    sold = sold,
                    inTransit = inTransit,
                    totalProducts = statusRows.size,
                    activityDays = activityDays,
                    locationStats = locationStats,
                )
            )
        } catch (e: Exception) {
            ApiResult.Failure(e.message ?: "Network error")
        }
    }

    suspend fun fetchLocations(): ApiResult<List<String>> = withContext(Dispatchers.IO) {
        try {
            val rows = postgrest.from("products").select(columns = Columns.list("location")).decodeList<LocationRow>()
            val locations = rows.mapNotNull { it.location?.trim()?.ifEmpty { null } }.distinct().sorted()
            ApiResult.Success(locations)
        } catch (e: Exception) {
            ApiResult.Failure(e.message ?: "Network error")
        }
    }

    suspend fun lookupProduct(epc: String): ApiResult<ProductLookup?> = withContext(Dispatchers.IO) {
        try {
            val row = postgrest.from("products").select {
                filter { eq("epc", epc.uppercase()) }
            }.decodeSingleOrNull<ProductRow>()
            ApiResult.Success(
                row?.let { ProductLookup(it.sku, it.productName, it.quantity, it.location, it.status) }
            )
        } catch (e: Exception) {
            ApiResult.Failure(e.message ?: "Network error")
        }
    }

    /**
     * One request for every registered product, keyed by EPC. A record-activity scan can bring
     * dozens of distinct tags into range within a second or two; querying each EPC individually
     * means dozens of concurrent round trips competing for the HTTP client's connection pool,
     * which is what made the SKU/Product Name/Qty columns lag minutes behind the scan. Loading
     * the whole table once up front turns every subsequent tag match into an in-memory lookup.
     */
    suspend fun fetchProductsByEpc(): ApiResult<Map<String, ProductLookup>> = withContext(Dispatchers.IO) {
        try {
            val rows = postgrest.from("products").select().decodeList<ProductRow>()
            ApiResult.Success(
                rows.associate { it.epc.uppercase() to ProductLookup(it.sku, it.productName, it.quantity, it.location, it.status) }
            )
        } catch (e: Exception) {
            ApiResult.Failure(e.message ?: "Network error")
        }
    }

    suspend fun submitActivityBatch(
        activityType: ActivityType,
        location: String?,
        items: List<TagRow>
    ): ApiResult<BatchSubmitResult> = withContext(Dispatchers.IO) {
        try {
            val results = items.map { tag ->
                val rpcResult: RpcResult = when (activityType) {
                    ActivityType.INBOUND -> postgrest.rpc(
                        "register_product_tag",
                        RegisterParams(
                            epc = tag.epc,
                            sku = tag.sku ?: "",
                            productName = tag.productName ?: "",
                            quantity = tag.quantity ?: 1,
                            location = location
                        )
                    ).decodeAs()
                    ActivityType.STOCK_OPNAME -> postgrest.rpc(
                        "record_stock_opname",
                        LocationParams(epc = tag.epc, location = location)
                    ).decodeAs()
                    ActivityType.TRANSFER -> postgrest.rpc(
                        "record_transfer",
                        DestinationParams(epc = tag.epc, destination = location ?: "")
                    ).decodeAs()
                    ActivityType.OUTBOUND -> postgrest.rpc(
                        "record_outbound",
                        LocationParams(epc = tag.epc, location = location)
                    ).decodeAs()
                }
                BatchItemResult(rpcResult.epc, rpcResult.status, rpcResult.reason, rpcResult.productName)
            }

            val accepted = results.count { it.status == "ACCEPTED" }
            val unknown = results.count { it.status == "UNKNOWN_EPC" }
            val rejected = results.size - accepted - unknown

            ApiResult.Success(BatchSubmitResult(accepted, rejected, unknown, results))
        } catch (e: Exception) {
            ApiResult.Failure(e.message ?: "Network error")
        }
    }
}
