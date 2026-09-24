package com.warehouse.rfid.edge

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private lateinit var repository: SupabaseRepository

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvKpiAvailable: TextView
    private lateinit var tvKpiSold: TextView
    private lateinit var tvKpiInTransit: TextView
    private lateinit var tvHomeStatus: TextView
    private lateinit var pieChartStatus: PieChart
    private lateinit var barChartActivity: BarChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        repository = SupabaseRepository()

        swipeRefresh = findViewById(R.id.swipeRefresh)
        tvKpiAvailable = findViewById(R.id.tvKpiAvailable)
        tvKpiSold = findViewById(R.id.tvKpiSold)
        tvKpiInTransit = findViewById(R.id.tvKpiInTransit)
        tvHomeStatus = findViewById(R.id.tvHomeStatus)
        pieChartStatus = findViewById(R.id.pieChartStatus)
        barChartActivity = findViewById(R.id.barChartActivity)

        findViewById<Button>(R.id.btnTagRegistration).setOnClickListener { launchScan(ActivityType.INBOUND) }
        findViewById<Button>(R.id.btnRecordActivity).setOnClickListener { showRecordActivityMenu() }

        swipeRefresh.setOnRefreshListener { loadStats() }

        setupPieChart()
        setupBarChart()
    }

    override fun onResume() {
        super.onResume()
        loadStats()
    }

    private fun launchScan(type: ActivityType) {
        val intent = Intent(this, ScanActivity::class.java)
        intent.putExtra(ScanActivity.EXTRA_ACTIVITY_TYPE, type.name)
        startActivity(intent)
    }

    private fun showRecordActivityMenu() {
        val labels = RECORD_ACTIVITY_TYPES.map { it.label }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Record Activity")
            .setItems(labels) { _, index -> launchScan(RECORD_ACTIVITY_TYPES[index]) }
            .show()
    }

    private fun setupPieChart() {
        pieChartStatus.description.isEnabled = false
        pieChartStatus.setUsePercentValues(false)
        pieChartStatus.setDrawEntryLabels(false)
        pieChartStatus.holeRadius = 55f
        pieChartStatus.transparentCircleRadius = 58f
        pieChartStatus.legend.isEnabled = true
    }

    private fun setupBarChart() {
        barChartActivity.description.isEnabled = false
        barChartActivity.legend.isEnabled = true
        barChartActivity.axisRight.isEnabled = false
        barChartActivity.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChartActivity.xAxis.granularity = 1f
        barChartActivity.setFitBars(true)
    }

    private fun loadStats() {
        tvHomeStatus.text = "Loading..."
        lifecycleScope.launch {
            when (val result = repository.fetchStats()) {
                is ApiResult.Success -> {
                    val stats = result.value
                    tvKpiAvailable.text = stats.available.toString()
                    tvKpiSold.text = stats.sold.toString()
                    tvKpiInTransit.text = stats.inTransit.toString()
                    tvHomeStatus.text = "Updated: ${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())}"
                    renderPieChart(stats)
                    renderBarChart(stats)
                }
                is ApiResult.Failure -> {
                    tvHomeStatus.text = "Unable to load stats: ${result.message}"
                }
            }
            swipeRefresh.isRefreshing = false
        }
    }

    private fun renderPieChart(stats: DashboardStats) {
        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()
        if (stats.available > 0) { entries.add(PieEntry(stats.available.toFloat(), "Available")); colors.add(getColor(R.color.status_available)) }
        if (stats.sold > 0) { entries.add(PieEntry(stats.sold.toFloat(), "Sold")); colors.add(getColor(R.color.status_sold)) }
        if (stats.inTransit > 0) { entries.add(PieEntry(stats.inTransit.toFloat(), "In Transit")); colors.add(getColor(R.color.status_in_transit)) }

        if (entries.isEmpty()) {
            pieChartStatus.clear()
            pieChartStatus.invalidate()
            return
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.valueTextSize = 12f
        pieChartStatus.data = PieData(dataSet)
        pieChartStatus.invalidate()
    }

    private fun renderBarChart(stats: DashboardStats) {
        if (stats.activityDays.isEmpty()) {
            barChartActivity.clear()
            barChartActivity.invalidate()
            return
        }

        val labels = stats.activityDays.map { it.date.takeLast(5) }
        val inboundEntries = stats.activityDays.mapIndexed { i, d -> BarEntry(i.toFloat(), d.inbound.toFloat()) }
        val stockOpnameEntries = stats.activityDays.mapIndexed { i, d -> BarEntry(i.toFloat(), d.stockOpname.toFloat()) }
        val transferEntries = stats.activityDays.mapIndexed { i, d -> BarEntry(i.toFloat(), d.transfer.toFloat()) }
        val outboundEntries = stats.activityDays.mapIndexed { i, d -> BarEntry(i.toFloat(), d.outbound.toFloat()) }

        val inboundSet = BarDataSet(inboundEntries, "Inbound")
        inboundSet.setColor(getColor(R.color.brand_primary))
        val stockOpnameSet = BarDataSet(stockOpnameEntries, "Stock Opname")
        stockOpnameSet.setColor(getColor(R.color.brand_primary_light))
        val transferSet = BarDataSet(transferEntries, "Transfer")
        transferSet.setColor(getColor(R.color.chart_tone_3))
        val outboundSet = BarDataSet(outboundEntries, "Outbound")
        outboundSet.setColor(getColor(R.color.chart_tone_4))

        val groupWidth = 0.8f
        val barWidth = groupWidth / 4f

        val data = BarData(inboundSet, stockOpnameSet, transferSet, outboundSet)
        data.barWidth = barWidth

        barChartActivity.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChartActivity.xAxis.axisMinimum = 0f
        barChartActivity.xAxis.setCenterAxisLabels(true)
        barChartActivity.data = data
        barChartActivity.groupBars(0f, groupWidth, 0f)
        barChartActivity.xAxis.axisMaximum = 0f + barChartActivity.barData.getGroupWidth(groupWidth, 0f) * labels.size
        barChartActivity.invalidate()
    }
}
