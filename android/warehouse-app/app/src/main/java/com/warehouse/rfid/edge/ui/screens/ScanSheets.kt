package com.warehouse.rfid.edge.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.warehouse.rfid.edge.TagRow
import com.warehouse.rfid.edge.ui.components.PrimaryButton
import com.warehouse.rfid.edge.ui.components.WarehouseListItem
import com.warehouse.rfid.edge.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerSheet(
    locationWord: String,
    knownLocations: List<String>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newLocation by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = Spacing.lg),
        ) {
            Text("Select $locationWord", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(Spacing.md))

            Row {
                OutlinedTextField(
                    value = newLocation,
                    onValueChange = { newLocation = it },
                    label = { Text("Add new $locationWord") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(Spacing.sm))
                PrimaryButton(
                    text = "Add",
                    enabled = newLocation.isNotBlank(),
                    onClick = {
                        onSelect(newLocation.trim())
                        newLocation = ""
                    },
                )
            }
            Spacer(Modifier.height(Spacing.md))

            for (location in knownLocations) {
                WarehouseListItem(
                    title = location,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(location) },
                )
            }
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkRegisterSheet(
    selectedCount: Int,
    onApply: (sku: String?, name: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var sku by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = Spacing.lg),
        ) {
            Text("Bulk register $selectedCount tags", style = MaterialTheme.typography.titleLarge)
            Text(
                "Applying to $selectedCount selected tag(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.md))

            OutlinedTextField(
                value = sku,
                onValueChange = { sku = it },
                label = { Text("SKU (applied to all)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.sm))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text("Product name (applied to all)") },
                singleLine = true,
                isError = nameError,
                supportingText = if (nameError) { { Text("Product name is required") } } else null,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                "Quantity isn't entered here — each tag counts as 1 unit, and a SKU's total is however many tags share it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.lg))

            PrimaryButton(
                text = "Apply",
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                    } else {
                        onApply(sku.trim().ifBlank { null }, name.trim())
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagEditSheet(
    tag: TagRow,
    onSave: (sku: String?, name: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var sku by remember { mutableStateOf(tag.sku.orEmpty()) }
    var name by remember { mutableStateOf(tag.productName.orEmpty()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = Spacing.lg),
        ) {
            Text("Register tag", style = MaterialTheme.typography.titleLarge)
            Text(
                tag.epc,
                style = MaterialTheme.typography.bodySmall.merge(com.warehouse.rfid.edge.ui.theme.TabularNumsStyle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.md))

            OutlinedTextField(
                value = sku,
                onValueChange = { sku = it },
                label = { Text("SKU") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.sm))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Product name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.lg))

            PrimaryButton(
                text = "Save",
                onClick = {
                    onSave(sku.trim().ifBlank { null }, name.trim().ifBlank { null })
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}
