package com.warehouse.rfid.edge.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.warehouse.rfid.edge.ActivityDayCount
import com.warehouse.rfid.edge.DashboardStats
import com.warehouse.rfid.edge.ui.components.ActivityColumnChart
import com.warehouse.rfid.edge.ui.components.AppTopBar
import com.warehouse.rfid.edge.ui.components.ChartSeries
import com.warehouse.rfid.edge.ui.components.ErrorState
import com.warehouse.rfid.edge.ui.components.PrimaryButton
import com.warehouse.rfid.edge.ui.components.ProportionSegment
import com.warehouse.rfid.edge.ui.components.ProportionSummary
import com.warehouse.rfid.edge.ui.components.SecondaryButton
import com.warehouse.rfid.edge.ui.components.ShimmerBlock
import com.warehouse.rfid.edge.ui.components.StatCard
import com.warehouse.rfid.edge.ui.components.TopBarAction
import com.warehouse.rfid.edge.ui.components.WarehouseCard
import com.warehouse.rfid.edge.ui.theme.Spacing
import com.warehouse.rfid.edge.ui.theme.WarehouseAppTheme
import com.warehouse.rfid.edge.ui.theme.WarehouseTheme

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val stats: DashboardStats, val updatedAt: String) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onRefresh: () -> Unit,
    onTagRegistration: () -> Unit,
    onRecordActivity: () -> Unit,
) {
    Scaffold(
        topBar = {
            AppTopBar(
                title = "RFID Warehouse",
                subtitle = when (uiState) {
                    is HomeUiState.Success -> "Updated ${uiState.updatedAt}"
                    is HomeUiState.Error -> "Unable to load stats"
                    HomeUiState.Loading -> "Loading…"
                },
                actions = listOf(
                    TopBarAction(Icons.Rounded.Refresh, "Refresh", onRefresh),
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.xl),
        ) {
            when (uiState) {
                HomeUiState.Loading -> HomeLoadingContent()
                is HomeUiState.Error -> ErrorState(message = uiState.message)
                is HomeUiState.Success -> HomeContent(uiState.stats)
            }

            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                PrimaryButton(text = "Tag Registration", onClick = onTagRegistration, modifier = Modifier.fillMaxWidth())
                SecondaryButton(text = "Record Activity", onClick = onRecordActivity, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun HomeLoadingContent() {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        repeat(3) {
            ShimmerBlock(modifier = Modifier.weight(1f), height = 64.dp)
        }
    }
    ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 220.dp)
    Spacer(Modifier.height(Spacing.sm))
    ShimmerBlock(modifier = Modifier.fillMaxWidth(), height = 220.dp)
}

@Composable
private fun HomeContent(stats: DashboardStats) {
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        StatCard(
            label = "Available",
            value = stats.available.toString(),
            valueColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = "Sold",
            value = stats.sold.toString(),
            modifier = Modifier.weight(1f),
        )
        StatCard(
            label = "In Transit",
            value = stats.inTransit.toString(),
            valueColor = WarehouseTheme.extendedColors.warning,
            modifier = Modifier.weight(1f),
        )
    }

    WarehouseCard {
        Text("Stock by status", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(Spacing.sm))
        if (stats.available == 0 && stats.sold == 0 && stats.inTransit == 0) {
            ErrorState(title = "No stock data yet", message = "Register tags to see status breakdown here.")
        } else {
            ProportionSummary(
                segments = listOf(
                    ProportionSegment("Available", stats.available, MaterialTheme.colorScheme.primary),
                    ProportionSegment("Sold", stats.sold, MaterialTheme.colorScheme.onSurfaceVariant),
                    ProportionSegment("In Transit", stats.inTransit, WarehouseTheme.extendedColors.warning),
                ),
            )
        }
    }

    WarehouseCard {
        if (stats.activityDays.isEmpty() || stats.activityDays.all { it.inbound + it.stockOpname + it.transfer + it.outbound == 0 }) {
            ErrorState(title = "No activity yet", message = "Activity from the last 7 days will appear here.")
        } else {
            ActivityColumnChart(
                headline = "Activity — last 7 days",
                xLabels = stats.activityDays.map { it.date.takeLast(5) },
                series = listOf(
                    ChartSeries("Inbound", MaterialTheme.colorScheme.primary, stats.activityDays.map { it.inbound.toFloat() }),
                    ChartSeries("Stock opname", MaterialTheme.colorScheme.secondary, stats.activityDays.map { it.stockOpname.toFloat() }),
                    ChartSeries("Transfer", WarehouseTheme.extendedColors.info, stats.activityDays.map { it.transfer.toFloat() }),
                    ChartSeries("Outbound", MaterialTheme.colorScheme.outline, stats.activityDays.map { it.outbound.toFloat() }),
                ),
            )
        }
    }
}

@Preview(name = "Home — light", showBackground = true)
@Composable
private fun HomeScreenSuccessPreview() {
    WarehouseAppTheme(darkTheme = false) {
        HomeScreen(
            uiState = HomeUiState.Success(fakeStats(), "14:32:10"),
            onRefresh = {},
            onTagRegistration = {},
            onRecordActivity = {},
        )
    }
}

@Preview(name = "Home — dark", showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HomeScreenDarkPreview() {
    WarehouseAppTheme(darkTheme = true) {
        HomeScreen(
            uiState = HomeUiState.Success(fakeStats(), "14:32:10"),
            onRefresh = {},
            onTagRegistration = {},
            onRecordActivity = {},
        )
    }
}

@Preview(name = "Home — loading", showBackground = true)
@Composable
private fun HomeScreenLoadingPreview() {
    WarehouseAppTheme {
        HomeScreen(uiState = HomeUiState.Loading, onRefresh = {}, onTagRegistration = {}, onRecordActivity = {})
    }
}

@Preview(name = "Home — error", showBackground = true)
@Composable
private fun HomeScreenErrorPreview() {
    WarehouseAppTheme {
        HomeScreen(
            uiState = HomeUiState.Error("Network error"),
            onRefresh = {},
            onTagRegistration = {},
            onRecordActivity = {},
        )
    }
}

private fun fakeStats() = DashboardStats(
    available = 128,
    sold = 42,
    inTransit = 7,
    totalProducts = 177,
    activityDays = listOf(
        ActivityDayCount("09-18", 4, 1, 0, 2),
        ActivityDayCount("09-19", 2, 0, 1, 0),
        ActivityDayCount("09-20", 6, 2, 0, 3),
        ActivityDayCount("09-21", 1, 0, 0, 0),
        ActivityDayCount("09-22", 3, 1, 2, 1),
        ActivityDayCount("09-23", 5, 0, 1, 2),
        ActivityDayCount("09-24", 8, 3, 0, 4),
    ),
)
