package com.warehouse.rfid.edge.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.warehouse.rfid.edge.POWER_DB_MAX
import com.warehouse.rfid.edge.POWER_DB_MIN
import com.warehouse.rfid.edge.ui.components.AppTopBar
import com.warehouse.rfid.edge.ui.components.WarehouseCard
import com.warehouse.rfid.edge.ui.theme.Spacing
import com.warehouse.rfid.edge.ui.theme.TabularNumsStyle
import com.warehouse.rfid.edge.ui.theme.WarehouseAppTheme

data class SettingsScreenState(
    val registrationPower: Int,
    val recordActivityPower: Int,
)

class SettingsScreenCallbacks(
    val onBack: () -> Unit,
    val onRegistrationPowerChange: (Int) -> Unit,
    val onRecordActivityPowerChange: (Int) -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(state: SettingsScreenState, callbacks: SettingsScreenCallbacks) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "RFID Power",
                subtitle = "Applied the next time each scan type starts",
                onBack = callbacks.onBack,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            PowerSettingCard(
                title = "Tag Registration",
                description = "Short range keeps registration to the one tag in hand.",
                power = state.registrationPower,
                onPowerChange = callbacks.onRegistrationPowerChange,
            )
            PowerSettingCard(
                title = "Record Activity",
                description = "Long range for bulk sweeps — stock opname, transfer, outbound.",
                power = state.recordActivityPower,
                onPowerChange = callbacks.onRecordActivityPowerChange,
            )
        }
    }
}

@Composable
private fun PowerSettingCard(
    title: String,
    description: String,
    power: Int,
    onPowerChange: (Int) -> Unit,
) {
    WarehouseCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                "$power dB",
                style = MaterialTheme.typography.titleMedium.merge(TabularNumsStyle),
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(Modifier.height(Spacing.xs))
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(Spacing.sm))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Low", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = power.toFloat(),
                onValueChange = { onPowerChange(it.toInt()) },
                valueRange = POWER_DB_MIN.toFloat()..POWER_DB_MAX.toFloat(),
                modifier = Modifier.weight(1f).padding(horizontal = Spacing.sm),
            )
            Text("High", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    WarehouseAppTheme {
        SettingsScreen(
            state = SettingsScreenState(registrationPower = 5, recordActivityPower = 30),
            callbacks = SettingsScreenCallbacks(onBack = {}, onRegistrationPowerChange = {}, onRecordActivityPowerChange = {}),
        )
    }
}
