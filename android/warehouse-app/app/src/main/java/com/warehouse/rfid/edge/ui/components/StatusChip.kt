package com.warehouse.rfid.edge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.warehouse.rfid.edge.ui.theme.Spacing
import com.warehouse.rfid.edge.ui.theme.WarehouseTheme

enum class ChipTone { Success, Warning, Error, Info, Neutral }

/** Small semantic badge, e.g. tag status (Accepted/Rejected/Unknown) or stock status. */
@Composable
fun StatusChip(
    text: String,
    tone: ChipTone,
    modifier: Modifier = Modifier,
) {
    val extended = WarehouseTheme.extendedColors
    val (container, onContainer) = when (tone) {
        ChipTone.Success -> extended.successContainer to extended.onSuccessContainer
        ChipTone.Warning -> extended.warningContainer to extended.onWarningContainer
        ChipTone.Error -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        ChipTone.Info -> extended.infoContainer to extended.onInfoContainer
        ChipTone.Neutral -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = onContainer,
        modifier = modifier
            .background(color = container, shape = MaterialTheme.shapes.extraSmall)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xs),
    )
}
