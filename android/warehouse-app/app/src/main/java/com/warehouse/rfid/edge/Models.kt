package com.warehouse.rfid.edge

enum class ActivityType(val apiValue: String, val label: String) {
    INBOUND("inbound", "Tag Registration"),
    STOCK_OPNAME("stock_opname", "Stock Opname"),
    TRANSFER("transfer", "Transfer"),
    OUTBOUND("outbound", "Outbound")
}

val RECORD_ACTIVITY_TYPES = listOf(ActivityType.STOCK_OPNAME, ActivityType.TRANSFER, ActivityType.OUTBOUND)

enum class ScanState { INITIALIZING, READY, SCANNING, PAUSED, SENDING, SUCCESS, ERROR }

private const val POWER_DB_REGISTRATION = 5
private const val POWER_DB_RECORD_ACTIVITY = 30

const val POWER_DB_MIN = 1
const val POWER_DB_MAX = 30

val ActivityType.powerDb: Int
    get() = if (this == ActivityType.INBOUND) POWER_DB_REGISTRATION else POWER_DB_RECORD_ACTIVITY

enum class LookupState { PENDING, FOUND, NOT_FOUND }

data class TagRow(
    val epc: String,
    var readCount: Int,
    var latestRssi: Int,
    var strongestRssi: Int,
    var sku: String? = null,
    var productName: String? = null,
    var quantity: Int? = null,
    var location: String? = null,
    var lookupState: LookupState? = null,
    var status: String? = null,
    var reason: String? = null,
    var isSelected: Boolean = false
)

data class ProductLookup(
    val sku: String,
    val productName: String,
    val quantity: Int,
    val location: String?,
    val status: String
)

data class ScanSession(
    val activityType: ActivityType,
    val tagsMap: MutableMap<String, TagRow> = mutableMapOf(),
    val tagsList: MutableList<TagRow> = mutableListOf(),
    var totalRawReads: Int = 0
)

data class DashboardStats(
    val available: Int,
    val sold: Int,
    val inTransit: Int,
    val totalProducts: Int,
    val activityDays: List<ActivityDayCount>
)

data class ActivityDayCount(
    val date: String,
    val inbound: Int,
    val stockOpname: Int,
    val transfer: Int,
    val outbound: Int
)
