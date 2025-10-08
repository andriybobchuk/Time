package com.andriybobchuk.time.core.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

// Enhanced Pie Chart with working day averages
@Composable
fun AdvancedPieChart(
    data: List<PieChartData>,
    modifier: Modifier = Modifier,
    totalHours: Double,
    dailyAverageHours: Double,
    weeklyAverageHours: Double? = null,
    monthlyAverageHours: Double? = null,
    effectivenessRate: Double? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(16.dp)
    ) {
        // Pie chart area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Canvas(
                modifier = Modifier
                    .size(220.dp)
                    .align(Alignment.Center)
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = minOf(size.width, size.height) / 2 * 0.8f

                var startAngle = 0f

                data.forEach { item ->
                    val sweepAngle = (item.value / totalHours * 360).toFloat()

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
                        color = Color.Gray,
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

            // Center text with multiple averages
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = DateTimeUtils.formatDuration(totalHours),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Total Hours",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = DateTimeUtils.formatDuration(dailyAverageHours),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Daily Avg",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                weeklyAverageHours?.let { weeklyAvg ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = DateTimeUtils.formatDuration(weeklyAvg),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Weekly Avg",
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                monthlyAverageHours?.let { monthlyAvg ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = DateTimeUtils.formatDuration(monthlyAvg),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "Monthly Avg",
                        fontSize = 8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Effectiveness indicator
        effectivenessRate?.let { rate ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        rate >= 80 -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                        rate >= 60 -> Color(0xFFFF9800).copy(alpha = 0.1f)
                        else -> Color(0xFFF44336).copy(alpha = 0.1f)
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Effectiveness Rate",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${rate.toInt()}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            rate >= 80 -> Color(0xFF4CAF50)
                            rate >= 60 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )
                }
            }
        }

        // Legend with enhanced data
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            data.forEach { item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(item.color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = item.label,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = DateTimeUtils.formatDuration(item.value),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${item.percentage.toInt()}%",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// Line chart data structures
data class TrendLineData(
    val jobId: String,
    val jobName: String,
    val color: Color,
    val points: List<TrendPoint>
)

data class TrendPoint(
    val x: Float, // 0-1 normalized position
    val y: Float, // 0-1 normalized position
    val value: Double, // actual value for tooltips
    val label: String // label for this point (day name, week number, month name)
)

// Trend Line Chart with clickable points
@Composable
fun TrendLineChart(
    data: List<TrendLineData>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    onPointClick: (String) -> Unit = {},
    maxValue: Double? = null
) {
    val actualMaxValue = maxValue ?: data.flatMap { it.points }.maxOfOrNull { it.value } ?: 1.0
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .padding(16.dp)
    ) {
        // Chart area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { /* Handle chart click */ }
            ) {
                val chartWidth = size.width * 0.85f
                val chartHeight = size.height * 0.8f
                val chartLeft = size.width * 0.1f
                val chartTop = size.height * 0.1f
                
                // Draw grid lines
                drawGridLines(
                    chartLeft = chartLeft,
                    chartTop = chartTop,
                    chartWidth = chartWidth,
                    chartHeight = chartHeight,
                    labels = labels,
                    maxValue = actualMaxValue
                )
                
                // Draw trend lines
                data.forEach { lineData ->
                    drawTrendLine(
                        lineData = lineData,
                        chartLeft = chartLeft,
                        chartTop = chartTop,
                        chartWidth = chartWidth,
                        chartHeight = chartHeight
                    )
                }
                
                // Draw data points
                data.forEach { lineData ->
                    lineData.points.forEach { point ->
                        val x = chartLeft + point.x * chartWidth
                        val y = chartTop + (1 - point.y) * chartHeight
                        
                        // Draw point
                        drawCircle(
                            color = lineData.color,
                            radius = 6f,
                            center = Offset(x, y)
                        )
                        
                        // Draw border
                        drawCircle(
                            color = Color.White,
                            radius = 6f,
                            center = Offset(x, y),
                            style = Stroke(width = 2f)
                        )
                    }
                }
            }
        }
        
        // X-axis labels (clickable)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clickable { onPointClick(label) }
                        .padding(4.dp)
                )
            }
        }
        
        // Legend
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(data.size) { index ->
                val lineData = data[index]
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp, 3.dp)
                            .background(
                                color = lineData.color,
                                shape = RoundedCornerShape(1.5.dp)
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = lineData.jobName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// Helper functions for drawing
private fun DrawScope.drawGridLines(
    chartLeft: Float,
    chartTop: Float,
    chartWidth: Float,
    chartHeight: Float,
    labels: List<String>,
    maxValue: Double
) {
    val gridColor = Color.LightGray.copy(alpha = 0.3f)
    
    // Horizontal grid lines
    for (i in 0..4) {
        val y = chartTop + (i / 4f) * chartHeight
        drawLine(
            color = gridColor,
            start = Offset(chartLeft, y),
            end = Offset(chartLeft + chartWidth, y),
            strokeWidth = 1f
        )
    }
    
    // Vertical grid lines
    for (i in 0 until labels.size) {
        val x = chartLeft + (i.toFloat() / (labels.size - 1)) * chartWidth
        drawLine(
            color = gridColor,
            start = Offset(x, chartTop),
            end = Offset(x, chartTop + chartHeight),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawTrendLine(
    lineData: TrendLineData,
    chartLeft: Float,
    chartTop: Float,
    chartWidth: Float,
    chartHeight: Float
) {
    if (lineData.points.size < 2) return
    
    val path = Path()
    val firstPoint = lineData.points.first()
    val startX = chartLeft + firstPoint.x * chartWidth
    val startY = chartTop + (1 - firstPoint.y) * chartHeight
    path.moveTo(startX, startY)
    
    lineData.points.drop(1).forEach { point ->
        val x = chartLeft + point.x * chartWidth
        val y = chartTop + (1 - point.y) * chartHeight
        path.lineTo(x, y)
    }
    
    drawPath(
        path = path,
        color = lineData.color,
        style = Stroke(
            width = 3f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round
        )
    )
}

