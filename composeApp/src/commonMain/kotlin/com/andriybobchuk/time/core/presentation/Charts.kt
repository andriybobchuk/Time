package com.andriybobchuk.time.core.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

data class PieChartData(
    val label: String,
    val value: Double,
    val color: Color,
    val percentage: Double
)

@Composable
fun PieChart(
    data: List<PieChartData>,
    modifier: Modifier = Modifier,
    totalValue: Double,
    averageValue: Double
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(8.dp)
    ) {
        // Pie chart area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Canvas(
                modifier = Modifier
                    .size(200.dp)
                    .align(Alignment.Center)
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = minOf(size.width, size.height) / 2 * 0.8f

                var startAngle = 0f

                data.forEach { item ->
                    val sweepAngle = (item.value / totalValue * 360).toFloat()

                    // Draw pie slice
                    drawArc(
                        color = item.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2)
                    )

                    // Draw border
                    drawArc(
                        color = Color.Black,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = 2f)
                    )

                    startAngle += sweepAngle
                }
            }

            // Center text with total and average
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                            Text(
                text = DateTimeUtils.formatDuration(totalValue),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.textColor()
            )
            Text(
                text = "Total Hours",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.textColor()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = DateTimeUtils.formatDuration(averageValue),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.textColor()
            )
            Text(
                text = "Daily Average",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.textColor()
            )
            }
        }

        // Legend below the pie chart
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            data.forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(item.color)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${item.label}: ${DateTimeUtils.formatDuration(item.value)} (${item.percentage.toInt()}%)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.textColor()
                    )
                }
            }
        }
    }
}

data class BarChartData(
    val label: String,
    val value: Double,
    val color: Color,
    val percentage: Double
)

data class DailyBarData(
    val date: String,
    val totalHours: Double,
    val jobData: List<BarChartData>
)

@Composable
fun BarChart(
    data: List<DailyBarData>,
    modifier: Modifier = Modifier
) {
    val maxValue = data.maxOfOrNull { it.totalHours } ?: 0.0

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(8.dp)
    ) {
        // Y-axis labels
        Column(
            modifier = Modifier
                .width(50.dp)
                .height(250.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Generate Y-axis labels (0, 25%, 50%, 75% of max value - remove top label)
            (3 downTo 0).forEach { i ->
                val value = (maxValue * i / 3.0)
                Text(
                    text = if (value > 0) DateTimeUtils.formatFullHours(value) else "0",
                    fontSize = 8.sp,
                    modifier = Modifier.padding(end = 4.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    color = MaterialTheme.colorScheme.textColor()
                )
            }
        }

        // Chart area
        Column(
            modifier = Modifier
                .weight(1f)
                .height(250.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {
                val barWidth = size.width / 7
                val barHeight = size.height * 0.8f

                data.forEachIndexed { index, dayData ->
                    val x = index * barWidth + barWidth * 0.1f
                    val barWidthActual = barWidth * 0.8f

                    // Draw background bar
                    drawRect(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        topLeft = Offset(x, size.height - barHeight),
                        size = Size(barWidthActual, barHeight)
                    )

                    // Draw job bars from bottom up
                    var currentHeight = 0f
                    dayData.jobData.forEach { job ->
                        val jobHeight = (job.value / maxValue * barHeight).toFloat()

                        drawRect(
                            color = job.color,
                            topLeft = Offset(x, size.height - currentHeight - jobHeight),
                            size = Size(barWidthActual, jobHeight)
                        )

                        currentHeight += jobHeight
                    }

                    // Draw border
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(x, size.height - barHeight),
                        size = Size(barWidthActual, barHeight),
                        style = Stroke(width = 1f)
                    )
                }
            }
        }
    }

    // X-axis labels (dates only) - positioned to match bar centers exactly
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .padding(horizontal = 0.dp) // No padding needed as we'll use the same logic as bars
    ) {
        data.forEachIndexed { index, dayData ->
            // Each label takes the same width as each bar section (1/7 of total width)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayData.date,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.textColor(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }

    // Legend with hours
//    Column(
//        modifier = Modifier
//            .fillMaxWidth()
//    ) {
//        data.firstOrNull()?.jobData?.forEach { job ->
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                Row(
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Box(
//                        modifier = Modifier
//                            .size(4.dp)
//                            .background(job.color)
//                    )
//                    Text(
//                        text = job.label,
//                        fontSize = 10.sp
//                    )
//                }
//                Text(
//                    text = DateTimeUtils.formatDuration(job.value),
//                    fontSize = 10.sp,
//                    fontWeight = FontWeight.Bold
//                )
//            }
//        }
//    }
}
