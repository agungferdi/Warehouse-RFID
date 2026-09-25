package com.warehouse.rfid.edge.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.warehouse.rfid.edge.ActivityType
import com.warehouse.rfid.edge.LookupState
import com.warehouse.rfid.edge.ScanState
import com.warehouse.rfid.edge.TagRow
import com.warehouse.rfid.edge.ui.components.AppTopBar
import com.warehouse.rfid.edge.ui.components.ChipTone
import com.warehouse.rfid.edge.ui.components.PrimaryButton
import com.warehouse.rfid.edge.ui.components.SecondaryButton
import com.warehouse.rfid.edge.ui.components.StatusChip
import com.warehouse.rfid.edge.ui.components.TertiaryButton
import com.warehouse.rfid.edge.ui.theme.Spacing
import com.warehouse.rfid.edge.ui.theme.TabularNumsStyle
import com.warehouse.rfid.edge.ui.theme.WarehouseAppTheme

data class SendNoticeUi(val message: String, val tone: ChipTone)

data class ScanScreenState(
    val activityType: ActivityType,
    val scanState: ScanState,
    val currentPowerDb: Int,
    val powerMin: Int,
    val powerMax: Int,
    val locationLabel: String,
    val statusText: String,
    val totalTagsText: String,
    val sendNotice: SendNoticeUi?,
    val tags: List<TagRow>,
    val isRegistrationMode: Boolean,
    val selectionModeEnabled: Boolean,
    val selectedCount: Int,
    val bulkRegisterEnabled: Boolean,
)

class ScanScreenCallbacks(
    val onBack: () -> Unit,
    val onPowerChange: (Int) -> Unit,
    val onPowerChangeFinished: () -> Unit,
    val onLocationClick: () -> Unit,
    val onStartPause: () -> Unit,
    val onSend: () -> Unit,
    val onClear: () -> Unit,
    val onSelectAllToggle: (Boolean) -> Unit,
    val onBulkRegisterClick: () -> Unit,
    val onTagClick: (TagRow) -> Unit,
    val onTagLongPress: (TagRow) -> Unit,
    val onTagSelectToggle: (TagRow, Boolean) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ScanScreen(state: ScanScreenState, callbacks: ScanScreenCallbacks) {
    val canScan = state.scanState != ScanState.SCANNING &&
        state.scanState != ScanState.SENDING &&
        state.scanState != ScanState.INITIALIZING

    Scaffold(
        topBar = {
            AppTopBar(
                title = state.activityType.label,
                subtitle = "RFID power ${state.currentPowerDb} dB",
                onBack = callbacks.onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Low", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Slider(
                    value = state.currentPowerDb.toFloat(),
                    onValueChange = { callbacks.onPowerChange(it.toInt()) },
                    onValueChangeFinished = callbacks.onPowerChangeFinished,
                    valueRange = state.powerMin.toFloat()..state.powerMax.toFloat(),
                    enabled = canScan,
                    modifier = Modifier.weight(1f).padding(horizontal = Spacing.sm),
                )
                Text("High", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            SecondaryButton(
                text = state.locationLabel,
                onClick = callbacks.onLocationClick,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.sm))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                PrimaryButton(
                    text = if (state.scanState == ScanState.SCANNING) "Pause" else "Start",
                    onClick = callbacks.onStartPause,
                    enabled = state.scanState != ScanState.INITIALIZING && state.scanState != ScanState.SENDING,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = "Send",
                    onClick = callbacks.onSend,
                    enabled = (state.scanState == ScanState.PAUSED || state.scanState == ScanState.ERROR) && state.tags.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                )
                if (state.scanState == ScanState.PAUSED || state.scanState == ScanState.ERROR || state.scanState == ScanState.SUCCESS) {
                    TertiaryButton(
                        text = if (state.scanState == ScanState.SUCCESS) "New" else "Clear",
                        onClick = callbacks.onClear,
                    )
                }
            }
            Spacer(Modifier.height(Spacing.sm))

            Text(state.statusText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                state.totalTagsText,
                style = MaterialTheme.typography.labelMedium.merge(TabularNumsStyle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.sendNotice?.let { notice ->
                Spacer(Modifier.height(Spacing.sm))
                StatusChip(text = notice.message, tone = notice.tone, modifier = Modifier.fillMaxWidth())
            }

            if (state.isRegistrationMode) {
                Spacer(Modifier.height(Spacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.selectionModeEnabled && state.selectedCount > 0 && state.selectedCount == state.tags.count { it.status.isNullOrEmpty() },
                        onCheckedChange = callbacks.onSelectAllToggle,
                    )
                    Text("Select all", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    SecondaryButton(
                        text = if (state.selectedCount > 0) "Bulk register (${state.selectedCount})" else "Bulk register",
                        onClick = callbacks.onBulkRegisterClick,
                        enabled = state.bulkRegisterEnabled,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.sm))

            if (state.tags.isEmpty()) {
                Text(
                    "No tags scanned yet. Tap Start and bring a tag into range.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Spacing.xl),
                )
            } else {
                TableHeaderRow(showCheckboxSpacer = state.isRegistrationMode)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = Spacing.xl),
                ) {
                    items(state.tags, key = { it.epc }) { tag ->
                        TagRowItem(
                            tag = tag,
                            isRegistrationMode = state.isRegistrationMode,
                            selectionModeEnabled = state.selectionModeEnabled,
                            onClick = { callbacks.onTagClick(tag) },
                            onLongPress = { callbacks.onTagLongPress(tag) },
                            onSelectToggle = { checked -> callbacks.onTagSelectToggle(tag, checked) },
                        )
                    }
                }
            }
        }
    }
}

private const val SkuColumnWeight = 0.9f
private const val ProductColumnWeight = 1.6f
private const val QtyColumnWeight = 0.5f

@Composable
private fun TableHeaderRow(showCheckboxSpacer: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Spacing.xs, bottom = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showCheckboxSpacer) Spacer(Modifier.width(40.dp))
        TableHeaderCell("SKU", Modifier.weight(SkuColumnWeight))
        TableHeaderCell("PRODUCT NAME", Modifier.weight(ProductColumnWeight))
        TableHeaderCell("QTY", Modifier.weight(QtyColumnWeight))
    }
}

@Composable
private fun TableHeaderCell(text: String, modifier: Modifier) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
    )
}

/** True when this tag has no product info to show yet: unregistered (registration mode) or
 * not found / rejected as unknown by the backend (record-activity mode). */
private fun isUnknownTag(tag: TagRow, isRegistrationMode: Boolean): Boolean =
    if (isRegistrationMode) {
        tag.productName.isNullOrEmpty()
    } else {
        tag.lookupState == LookupState.NOT_FOUND || tag.status == "UNKNOWN_EPC"
    }

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TagRowItem(
    tag: TagRow,
    isRegistrationMode: Boolean,
    selectionModeEnabled: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onSelectToggle: (Boolean) -> Unit,
) {
    val canSelect = isRegistrationMode && tag.status.isNullOrEmpty()
    val isPending = !isRegistrationMode && tag.lookupState == LookupState.PENDING
    val isUnknown = isUnknownTag(tag, isRegistrationMode)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = { if (canSelect) onLongPress() })
            .padding(vertical = Spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                tag.epc,
                style = MaterialTheme.typography.bodySmall.merge(TabularNumsStyle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            tag.status?.let { status ->
                val tone = when (status) {
                    "ACCEPTED" -> ChipTone.Success
                    "UNKNOWN_EPC" -> ChipTone.Neutral
                    else -> ChipTone.Error
                }
                StatusChip(text = status, tone = tone)
            }
        }
        Spacer(Modifier.height(Spacing.xs))

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (selectionModeEnabled && canSelect) {
                Checkbox(
                    checked = tag.isSelected,
                    onCheckedChange = onSelectToggle,
                    modifier = Modifier.width(40.dp),
                )
            }
            when {
                isPending -> Text(
                    "Looking up…",
                    modifier = Modifier.weight(SkuColumnWeight + ProductColumnWeight + QtyColumnWeight),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                isUnknown -> {
                    TableCell("—", Modifier.weight(SkuColumnWeight))
                    Text(
                        "Unknown",
                        modifier = Modifier.weight(ProductColumnWeight),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (canSelect) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    TableCell("—", Modifier.weight(QtyColumnWeight))
                }
                else -> {
                    TableCell(tag.sku ?: "—", Modifier.weight(SkuColumnWeight))
                    Text(
                        tag.productName.orEmpty(),
                        modifier = Modifier.weight(ProductColumnWeight),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    TableCell((tag.quantity ?: 1).toString(), Modifier.weight(QtyColumnWeight))
                }
            }
        }

        if (!tag.reason.isNullOrEmpty()) {
            Text(
                tag.reason.orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TableCell(text: String, modifier: Modifier) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium.merge(TabularNumsStyle),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Preview(showBackground = true)
@Composable
private fun ScanScreenPreview() {
    WarehouseAppTheme {
        ScanScreen(
            state = ScanScreenState(
                activityType = ActivityType.INBOUND,
                scanState = ScanState.PAUSED,
                currentPowerDb = 5,
                powerMin = 1,
                powerMax = 30,
                locationLabel = "Location: Rack A1",
                statusText = "Status: PAUSED",
                totalTagsText = "Total Tags: 2",
                sendNotice = null,
                tags = listOf(
                    TagRow(epc = "E28011606000...", readCount = 4, latestRssi = -40, strongestRssi = -35, productName = "Blue Widget", sku = "SKU-1", quantity = 1),
                    TagRow(epc = "E28011606111...", readCount = 2, latestRssi = -44, strongestRssi = -38),
                ),
                isRegistrationMode = true,
                selectionModeEnabled = false,
                selectedCount = 0,
                bulkRegisterEnabled = false,
            ),
            callbacks = ScanScreenCallbacks(
                onBack = {}, onPowerChange = {}, onPowerChangeFinished = {}, onLocationClick = {},
                onStartPause = {}, onSend = {}, onClear = {}, onSelectAllToggle = {},
                onBulkRegisterClick = {}, onTagClick = {}, onTagLongPress = {}, onTagSelectToggle = { _, _ -> },
            ),
        )
    }
}
