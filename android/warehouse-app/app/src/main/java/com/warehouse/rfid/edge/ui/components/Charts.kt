package com.warehouse.rfid.edge.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.compose.m3.style.m3ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import com.patrykandpatrick.vico.core.scroll.InitialScroll
import com.warehouse.rfid.edge.ui.theme.Spacing
import com.warehouse.rfid.edge.ui.theme.TabularNumsStyle

data class ChartSeries(val name: String, val color: Color, val values: List<Float>)

/** Header stat + Vico grouped column chart, one accent color per series, minimal gridlines. */
@Composable
fun ActivityColumnChart(
    headline: String,
    xLabels: List<String>,
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            headline,
            style = MaterialTheme.typography.titleMedium.merge(TabularNumsStyle),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.sm))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            for (s in series) {
                LegendDot(color = s.color, label = s.name)
            }
        }
        Spacer(Modifier.height(Spacing.sm))

        val producer = remember(series) {
            ChartEntryModelProducer(
                series.map { s -> s.values.mapIndexed { i, v -> entryOf(i.toFloat(), v) } },
            )
        }

        val columns = series.map { s ->
            lineComponent(color = s.color, thickness = 8.dp, shape = Shapes.roundedCornerShape(allPercent = 30))
        }

        ProvideChartStyle(m3ChartStyle()) {
            Chart(
                chart = columnChart(columns = columns),
                chartModelProducer = producer,
                // Opens already scrolled to the most recent day (today) instead of the oldest
                // day in the 7-day window — otherwise freshly recorded activity is off-screen to
                // the right and looks like it never showed up at all.
                chartScrollSpec = rememberChartScrollSpec(initialScroll = InitialScroll.End),
                startAxis = rememberStartAxis(
                    valueFormatter = { value, _ -> value.toInt().toString() },
                ),
                bottomAxis = rememberBottomAxis(
                    valueFormatter = { value, _ -> xLabels.getOrElse(value.toInt()) { "" } },
                ),
                // TODO tap-to-inspect: Vico 1.14.0's exact marker API (`rememberMarker` /
                // `Chart(marker = ...)`) didn't resolve on the first two guesses without a
                // docs lookup — dropped rather than keep guessing at an unverified API surface.
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            )
        }
    }
}

data class ProportionSegment(val label: String, val value: Int, val color: Color)

/**
 * A horizontal proportion bar + labeled counts — the Stripe/Linear-style substitute for a pie
 * chart (Vico has no pie chart type; see the Phase 3 note on why this exists).
 */
@Composable
fun ProportionSummary(
    segments: List<ProportionSegment>,
    modifier: Modifier = Modifier,
) {
    val total = segments.sumOf { it.value }.coerceAtLeast(1)
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(Spacing.sm)
                .background(MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape),
        ) {
            for (segment in segments) {
                if (segment.value <= 0) continue
                val fraction = segment.value.toFloat() / total.toFloat()
                Column(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .fillMaxHeight()
                        .background(segment.color),
                ) {}
            }
        }
        Spacer(Modifier.height(Spacing.md))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            for (segment in segments) {
                Column {
                    LegendDot(color = segment.color, label = segment.label)
                    Text(
                        segment.value.toString(),
                        style = MaterialTheme.typography.titleMedium.merge(TabularNumsStyle),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/** A ring chart — the circular counterpart to ProportionSummary (Vico has no pie/donut type). */
@Composable
fun DonutChart(
    segments: List<ProportionSegment>,
    centerLabel: String,
    modifier: Modifier = Modifier,
) {
    val total = segments.sumOf { it.value }.coerceAtLeast(1)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = size.minDimension * 0.22f
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth),
            )
            var startAngle = -90f
            for (segment in segments) {
                if (segment.value <= 0) continue
                val sweep = 360f * segment.value / total
                drawArc(
                    color = segment.color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                )
                startAngle += sweep
            }
        }
        Text(
            centerLabel,
            style = MaterialTheme.typography.titleMedium.merge(TabularNumsStyle),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
    }
}

/** One donut per location: name, ring chart sized to its total, and a compact status legend. */
@Composable
fun LocationDonutCard(
    location: String,
    segments: List<ProportionSegment>,
    total: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            location,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(Spacing.sm))
        DonutChart(
            segments = segments,
            centerLabel = total.toString(),
            modifier = Modifier.size(96.dp),
        )
        Spacer(Modifier.height(Spacing.sm))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            for (segment in segments) {
                if (segment.value <= 0) continue
                LegendDot(color = segment.color, label = "${segment.label} ${segment.value}")
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier
                .size(Spacing.sm)
                .background(color = color, shape = CircleShape),
        ) {}
        Spacer(Modifier.width(Spacing.xs))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}
