package com.warehouse.rfid.edge.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.warehouse.rfid.edge.ActivityType
import com.warehouse.rfid.edge.ui.components.WarehouseListItem
import com.warehouse.rfid.edge.ui.theme.Spacing
import com.warehouse.rfid.edge.ui.theme.WarehouseAppTheme

/** Replaces the old AlertDialog list picker with a bottom sheet, per the design spec. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordActivitySheet(
    options: List<ActivityType>,
    onSelect: (ActivityType) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.navigationBarsPadding()) {
            Text(
                "Record activity",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            )
            for (option in options) {
                WarehouseListItem(
                    title = option.label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(option) }
                        .padding(horizontal = Spacing.lg),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RecordActivitySheetPreview() {
    WarehouseAppTheme {
        RecordActivitySheet(
            options = listOf(ActivityType.STOCK_OPNAME, ActivityType.TRANSFER, ActivityType.OUTBOUND),
            onSelect = {},
            onDismiss = {},
        )
    }
}
