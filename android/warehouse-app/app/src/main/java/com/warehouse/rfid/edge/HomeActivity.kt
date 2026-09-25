package com.warehouse.rfid.edge

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.warehouse.rfid.edge.ui.screens.HomeScreen
import com.warehouse.rfid.edge.ui.screens.HomeUiState
import com.warehouse.rfid.edge.ui.screens.RecordActivitySheet
import com.warehouse.rfid.edge.ui.theme.WarehouseAppTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeActivity : ComponentActivity() {

    private val repository = SupabaseRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            WarehouseAppTheme {
                var uiState by remember { mutableStateOf<HomeUiState>(HomeUiState.Loading) }
                var showRecordActivitySheet by remember { mutableStateOf(false) }

                fun refresh() {
                    uiState = HomeUiState.Loading
                    lifecycleScope.launch {
                        uiState = when (val result = repository.fetchStats()) {
                            is ApiResult.Success -> HomeUiState.Success(
                                stats = result.value,
                                updatedAt = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()),
                            )
                            is ApiResult.Failure -> HomeUiState.Error(result.message)
                        }
                    }
                }

                LaunchedEffect(Unit) { refresh() }

                HomeScreen(
                    uiState = uiState,
                    onRefresh = { refresh() },
                    onTagRegistration = { launchScan(ActivityType.INBOUND) },
                    onRecordActivity = { showRecordActivitySheet = true },
                )

                if (showRecordActivitySheet) {
                    RecordActivitySheet(
                        options = RECORD_ACTIVITY_TYPES,
                        onSelect = { type ->
                            showRecordActivitySheet = false
                            launchScan(type)
                        },
                        onDismiss = { showRecordActivitySheet = false },
                    )
                }
            }
        }
    }

    private fun launchScan(type: ActivityType) {
        val intent = Intent(this, ScanActivity::class.java)
        intent.putExtra(ScanActivity.EXTRA_ACTIVITY_TYPE, type.name)
        startActivity(intent)
    }
}
