package com.example.alarm.feature.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.alarm.core.ui.theme.Corners
import com.example.alarm.core.ui.theme.Spacing
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LineChartCard(
    title: String,
    subtitle: String = "",
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Corners.cornerLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.spacing5)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.spacing1)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.spacing4))

            if (data.isNotEmpty()) {
                val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
                val gridColor = MaterialTheme.colorScheme.outlineVariant.toArgb()
                AndroidView(
                    factory = { context ->
                        LineChart(context).apply {
                            val entries = data.mapIndexed { index, (_, value) ->
                                Entry(index.toFloat(), value)
                            }
                            val dataSet = LineDataSet(entries, "").apply {
                                color = android.graphics.Color.parseColor("#6200EE")
                                setCircleColor(android.graphics.Color.parseColor("#6200EE"))
                                lineWidth = 2f
                                circleRadius = 4f
                                setDrawCircleHole(false)
                                setDrawValues(false)
                                mode = LineDataSet.Mode.CUBIC_BEZIER
                            }
                            this.data = LineData(dataSet)
                            xAxis.apply {
                                valueFormatter = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String {
                                        val index = value.toInt()
                                        return if (index >= 0 && index < data.size) {
                                            data[index].first
                                        } else ""
                                    }
                                }
                                granularity = 1f
                                setDrawGridLines(false)
                                this.textColor = textColor
                            }
                            axisLeft.apply {
                                setDrawGridLines(true)
                                this.gridColor = gridColor
                                this.textColor = textColor
                            }
                            axisRight.isEnabled = false
                            legend.isEnabled = false
                            description.isEnabled = false
                            setTouchEnabled(true)
                            isDragEnabled = true
                            setScaleEnabled(true)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(Corners.cornerMedium)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BarChartCard(
    title: String,
    subtitle: String = "",
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Corners.cornerLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.spacing5)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.spacing1)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.spacing4))

            if (data.isNotEmpty()) {
                val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
                val gridColor = MaterialTheme.colorScheme.outlineVariant.toArgb()
                AndroidView(
                    factory = { context ->
                        BarChart(context).apply {
                            val entries = data.mapIndexed { index, (_, value) ->
                                BarEntry(index.toFloat(), value)
                            }
                            val dataSet = BarDataSet(entries, "").apply {
                                color = android.graphics.Color.parseColor("#03DAC6")
                                setDrawValues(false)
                            }
                            this.data = BarData(dataSet)
                            xAxis.apply {
                                valueFormatter = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String {
                                        val index = value.toInt()
                                        return if (index >= 0 && index < data.size) {
                                            data[index].first
                                        } else ""
                                    }
                                }
                                granularity = 1f
                                setDrawGridLines(false)
                                this.textColor = textColor
                            }
                            axisLeft.apply {
                                setDrawGridLines(true)
                                this.gridColor = gridColor
                                this.textColor = textColor
                            }
                            axisRight.isEnabled = false
                            legend.isEnabled = false
                            description.isEnabled = false
                            setTouchEnabled(true)
                            isDragEnabled = true
                            setScaleEnabled(true)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(Corners.cornerMedium)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PieChartCard(
    title: String,
    subtitle: String = "",
    data: List<Pair<String, Float>>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Corners.cornerLarge),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.spacing5)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = Spacing.spacing1)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.spacing4))

            if (data.isNotEmpty()) {
                val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
                AndroidView(
                    factory = { context ->
                        PieChart(context).apply {
                            val entries = data.map { (label, value) ->
                                PieEntry(value, label)
                            }
                            val colors = listOf(
                                android.graphics.Color.parseColor("#6200EE"),
                                android.graphics.Color.parseColor("#03DAC6"),
                                android.graphics.Color.parseColor("#FF6D00"),
                                android.graphics.Color.parseColor("#BB86FC"),
                                android.graphics.Color.parseColor("#018786")
                            )
                            val dataSet = PieDataSet(entries, "").apply {
                                this.colors = colors.take(entries.size)
                                setDrawValues(true)
                                valueTextSize = 10f
                                valueTextColor = textColor
                            }
                            this.data = PieData(dataSet)
                            legend.apply {
                                isEnabled = true
                                this.textColor = textColor
                            }
                            description.isEnabled = false
                            setTouchEnabled(true)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(Corners.cornerMedium)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    label: String,
    value: String,
    subtext: String = "",
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(Corners.cornerLarge),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.spacing4),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (highlight) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (highlight) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                if (subtext.isNotEmpty()) {
                    Text(
                        text = subtext,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (highlight) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}
