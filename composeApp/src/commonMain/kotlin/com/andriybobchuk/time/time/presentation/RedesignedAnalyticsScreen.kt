package com.andriybobchuk.time.time.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.time.core.presentation.Toolbars
import com.andriybobchuk.time.core.presentation.cardBackground
import com.andriybobchuk.time.time.domain.WeekData
import com.andriybobchuk.time.time.domain.WeekDetails
import com.andriybobchuk.time.time.domain.Job
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RedesignedAnalyticsScreen(
    viewModel: RedesignedAnalyticsViewModel,
    bottomNavbar: @Composable () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    
    Scaffold(
        topBar = {
            Toolbars.Primary(
                title = "Analytics",
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = { bottomNavbar() }
    ) { paddingValues ->
        when {
            state.isLoading && state.eightWeekTrend == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Error loading analytics",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = state.error ?: "Unknown error",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Button(
                            onClick = { viewModel.onAction(RedesignedAnalyticsAction.Refresh) },
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text("Retry")
                        }
                    }
                }
            }
            state.eightWeekTrend != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Week-over-week trend panel
                    WeekTrendPanel(
                        trend = state.eightWeekTrend!!,
                        selectedWeekIndex = state.selectedWeekIndex,
                        onWeekSelected = { weekIndex ->
                            viewModel.onAction(RedesignedAnalyticsAction.SelectWeek(weekIndex))
                        }
                    )
                    
                    // Week details panel
                    state.weekDetails?.let { details ->
                        WeekDetailsPanel(
                            details = details,
                            jobs = state.eightWeekTrend?.jobs ?: emptyList()
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No analytics data available",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun WeekTrendPanel(
    trend: com.andriybobchuk.time.time.domain.EightWeekTrend,
    selectedWeekIndex: Int,
    onWeekSelected: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.cardBackground()
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "8-Week Trend",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Trend chart with hour labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                // Hour labels column
                val maxHours = trend.weeks.maxOfOrNull { it.totalHours } ?: 1.0
                val gridLines = 5
                Column(
                    modifier = Modifier
                        .width(40.dp)
                        .fillMaxHeight()
                        .padding(top = 20.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    for (i in 0..gridLines) {
                        val hours = maxHours * (gridLines - i) / gridLines
                        Text(
                            text = if (hours > 0) formatHoursOnly(hours) else "",
                            fontSize = 9.sp,
                            color = Color.Gray.copy(alpha = 0.7f),
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Chart
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    TrendChart(
                        weeks = trend.weeks,
                        jobs = trend.jobs,
                        selectedWeekIndex = selectedWeekIndex,
                        onWeekSelected = onWeekSelected
                    )
                }
            }
            
            // Week labels with pill highlighting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                trend.weeks.forEachIndexed { index, week ->
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (index == selectedWeekIndex) 
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                else 
                                    Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { onWeekSelected(index) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${week.weekNumber}",
                            fontSize = 11.sp,
                            color = if (index == selectedWeekIndex) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (index == selectedWeekIndex) 
                                FontWeight.Bold 
                            else 
                                FontWeight.Normal
                        )
                    }
                }
            }
            
            // Legend
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                trend.jobs.forEach { job ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(Color(job.color))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = job.name,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (job.isMain) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "J1",
                                fontSize = 10.sp,
                                color = Color(0xFF9C27B0),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrendChart(
    weeks: List<WeekData>,
    jobs: List<Job>,
    selectedWeekIndex: Int,
    onWeekSelected: (Int) -> Unit
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val chartWidth = size.width - 40.dp.toPx()
                    val padding = 20.dp.toPx()
                    val clickX = offset.x - padding
                    
                    if (clickX >= 0 && clickX <= chartWidth) {
                        val weekIndex = (clickX / (chartWidth / (weeks.size - 1))).toInt()
                            .coerceIn(0, weeks.size - 1)
                        onWeekSelected(weekIndex)
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height
        val padding = 20.dp.toPx()
        
        val chartWidth = width - 2 * padding
        val chartHeight = height - 2 * padding
        
        // Find max hours for scaling
        val maxHours = weeks.maxOfOrNull { it.totalHours } ?: 1.0
        
        // Draw grid lines
        val gridLines = 5
        for (i in 0..gridLines) {
            val y = padding + (chartHeight * i / gridLines)
            drawLine(
                color = Color.Gray.copy(alpha = 0.2f),
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 1.dp.toPx()
            )
        }
        
        // Draw trend lines for each job
        jobs.forEach { job ->
            val jobColor = Color(job.color)
            val points = weeks.mapIndexed { index, week ->
                val x = padding + (chartWidth * index / (weeks.size - 1))
                val hours = week.jobHours[job.id] ?: 0.0
                val y = padding + chartHeight - (chartHeight * hours / maxHours).toFloat()
                Offset(x, y)
            }
            
            // Draw line
            if (points.size > 1) {
                val path = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        lineTo(points[i].x, points[i].y)
                    }
                }
                drawPath(
                    path = path,
                    color = jobColor,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
            
            // Draw points
            points.forEachIndexed { index, point ->
                drawCircle(
                    color = jobColor,
                    radius = if (index == selectedWeekIndex) 6.dp.toPx() else 4.dp.toPx(),
                    center = point
                )
                if (index == selectedWeekIndex) {
                    drawCircle(
                        color = Color.White,
                        radius = 2.dp.toPx(),
                        center = point
                    )
                }
            }
        }
        
    }
}

@Composable
fun WeekDetailsPanel(details: WeekDetails, jobs: List<Job>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        // Pie charts row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Job distribution pie chart
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.cardBackground()
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Hours by Job",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    )
                    
                    Text(
                        text = formatHoursOnly(details.weekData.totalHours),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        SimplePieChart(
                            data = details.weekData.jobHours,
                            jobs = jobs.associateBy({ it.id }, { it.color }),
                            modifier = Modifier.size(100.dp)
                        )
                    }
                    
                    // Legend
                    details.weekData.jobHours.forEach { (jobId, hours) ->
                        if (hours > 0) {
                            val job = jobs.find { it.id == jobId }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(job?.color ?: 0xFF808080), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${job?.name ?: jobId}: ${formatHoursOnly(hours)}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            // Context switch pie chart
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.cardBackground()
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Time Utilization",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    )
                    
                    Text(
                        text = formatHoursOnly(details.totalTrackedTime),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        UtilizationPieChart(
                            productiveTime = details.effectiveHours.values.sum(),
                            meetingTime = details.weekData.totalHours - details.effectiveHours.values.sum(),
                            contextSwitchTime = details.contextSwitchHours,
                            modifier = Modifier.size(100.dp)
                        )
                    }
                    
                    // Legend
                    val productiveTime = details.effectiveHours.values.sum()
                    val meetingTime = details.weekData.totalHours - productiveTime
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 1.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF4CAF50), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Productive: ${formatHoursOnly(productiveTime)}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 1.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFFF9800), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Meetings: ${formatHoursOnly(meetingTime)}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 1.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.Gray.copy(alpha = 0.5f), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Context: ${formatHoursOnly(details.contextSwitchHours)}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // Stats cards
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.cardBackground()
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Week ${details.weekData.weekNumber} Details",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // Job stats
                details.averageDailyHours.forEach { (jobId, avgHours) ->
                    val effectiveness = details.effectivenessRate[jobId] ?: 0.0
                    val job = jobs.find { it.id == jobId }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = job?.name ?: jobId,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                text = "${formatHoursOnly(avgHours)}/day",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${effectiveness.toInt()}% eff",
                                fontSize = 14.sp,
                                color = if (effectiveness >= 70) Color(0xFF4CAF50) else Color(0xFFFF9800)
                            )
                        }
                    }
                    
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
                
                // Context switch stats
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Avg Context Switch/Day",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = formatHoursOnly(details.contextSwitchHours / 7.0),
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun SimplePieChart(
    data: Map<String, Double>,
    jobs: Map<String, Long> = emptyMap(),
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val total = data.values.sum()
        if (total == 0.0) return@Canvas
        
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = minOf(centerX, centerY) * 0.8f
        
        var startAngle = -90f
        val colors = listOf(
            Color(0xFFFFC747), // Yellow
            Color(0xFF41D0FF), // Blue
            Color(0xFF4CAF50), // Green
            Color(0xFFFF5722), // Orange
            Color(0xFF9C27B0), // Purple
            Color(0xFFE91E63)  // Pink
        )
        
        data.entries.forEachIndexed { index, (jobId, value) ->
            val sweepAngle = (value / total * 360).toFloat()
            val color = if (jobs.containsKey(jobId)) {
                Color(jobs[jobId]!!)
            } else {
                colors.getOrElse(index) { Color.Gray }
            }
            
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = true,
                topLeft = Offset(centerX - radius, centerY - radius),
                size = Size(radius * 2, radius * 2)
            )
            
            startAngle += sweepAngle
        }
    }
}

@Composable
fun UtilizationPieChart(
    productiveTime: Double,
    meetingTime: Double,
    contextSwitchTime: Double,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val total = productiveTime + meetingTime + contextSwitchTime
        if (total == 0.0) return@Canvas
        
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = minOf(centerX, centerY) * 0.8f
        
        val productiveAngle = (productiveTime / total * 360).toFloat()
        val meetingAngle = (meetingTime / total * 360).toFloat()
        val contextAngle = (contextSwitchTime / total * 360).toFloat()
        
        // Draw productive time (green)
        drawArc(
            color = Color(0xFF4CAF50),
            startAngle = -90f,
            sweepAngle = productiveAngle,
            useCenter = true,
            topLeft = Offset(centerX - radius, centerY - radius),
            size = Size(radius * 2, radius * 2)
        )
        
        // Draw meeting time (orange)
        drawArc(
            color = Color(0xFFFF9800),
            startAngle = -90f + productiveAngle,
            sweepAngle = meetingAngle,
            useCenter = true,
            topLeft = Offset(centerX - radius, centerY - radius),
            size = Size(radius * 2, radius * 2)
        )
        
        // Draw context switch time (gray)
        drawArc(
            color = Color.Gray.copy(alpha = 0.5f),
            startAngle = -90f + productiveAngle + meetingAngle,
            sweepAngle = contextAngle,
            useCenter = true,
            topLeft = Offset(centerX - radius, centerY - radius),
            size = Size(radius * 2, radius * 2)
        )
    }
}

// Helper function to get job color
fun getJobColor(jobId: String): Long {
    return when (jobId) {
        "rivian" -> 4294950727L
        "plato" -> 4279168511L
        else -> 0xFFFFC747L // Default yellow
    }
}