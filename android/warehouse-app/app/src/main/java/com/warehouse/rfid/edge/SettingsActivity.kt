package com.warehouse.rfid.edge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.warehouse.rfid.edge.ui.screens.SettingsScreen
import com.warehouse.rfid.edge.ui.screens.SettingsScreenCallbacks
import com.warehouse.rfid.edge.ui.screens.SettingsScreenState
import com.warehouse.rfid.edge.ui.theme.WarehouseAppTheme

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WarehouseAppTheme {
                var registrationPower by remember { mutableStateOf(PowerSettings.getRegistrationPower(this)) }
                var recordActivityPower by remember { mutableStateOf(PowerSettings.getRecordActivityPower(this)) }

                SettingsScreen(
                    state = SettingsScreenState(
                        registrationPower = registrationPower,
                        recordActivityPower = recordActivityPower,
                    ),
                    callbacks = SettingsScreenCallbacks(
                        onBack = { finish() },
                        onRegistrationPowerChange = { value ->
                            registrationPower = value
                            PowerSettings.setRegistrationPower(this, value)
                        },
                        onRecordActivityPowerChange = { value ->
                            recordActivityPower = value
                            PowerSettings.setRecordActivityPower(this, value)
                        },
                    ),
                )
            }
        }
    }
}
