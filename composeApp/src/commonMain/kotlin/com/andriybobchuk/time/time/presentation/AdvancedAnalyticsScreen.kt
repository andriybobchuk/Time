package com.andriybobchuk.time.time.presentation

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.time.core.presentation.AdvancedPieChart
import com.andriybobchuk.time.core.presentation.TrendLineChart
import com.andriybobchuk.time.core.presentation.TrendLineData
import com.andriybobchuk.time.core.presentation.TrendPoint
import com.andriybobchuk.time.core.presentation.PieChartData
import com.andriybobchuk.time.core.presentation.DateTimeUtils
import com.andriybobchuk.time.core.presentation.Toolbars
import com.andriybobchuk.time.core.presentation.cardBackground
import com.andriybobchuk.time.time.data.TimeDataSource
import com.andriybobchuk.time.time.domain.AnalyticsPeriod

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedAnalyticsScreen(
    viewModel: AdvancedAnalyticsViewModel,
    bottomNavbar: @Composable () -> Unit
) {
    val state by viewModel.state.collectAsState()
    
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    
    Scaffold(
        topBar = {
            Toolbars.Primary(
                title = "Advanced Analytics",
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = { bottomNavbar() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab selector
            AnalyticsTabSelector(
                selectedPeriod = state.selectedPeriod,
                onPeriodSelected = { period ->
                    viewModel.onAction(AnalyticsAction.SelectPeriod(period))
                }
            )
            
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        when (state.selectedPeriod) {
                            AnalyticsPeriod.WEEK -> {
                                state.weekAnalytics?.let { analytics ->
                                    WeekAnalyticsContent(
                                        analytics = analytics,
                                        onDateClick = { date ->
                                            viewModel.onAction(AnalyticsAction.SelectDateForDetails(date))
                                        }
                                    )
                                }
                            }
                            AnalyticsPeriod.SIX_WEEKS -> {
                                state.sixWeekAnalytics?.let { analytics ->
                                    SixWeekAnalyticsContent(
                                        analytics = analytics,
                                        onWeekClick = { weekNumber ->
                                            viewModel.onAction(AnalyticsAction.SelectWeekForDetails(weekNumber))
                                        }
                                    )
                                }
                            }
                            AnalyticsPeriod.SIX_MONTHS -> {
                                state.sixMonthAnalytics?.let { analytics ->
                                    SixMonthAnalyticsContent(
                                        analytics = analytics,
                                        onMonthClick = { monthName ->
                                            viewModel.onAction(AnalyticsAction.SelectMonthForDetails(monthName))
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    // Work Pattern Insights
                    item {
                        state.workPatternInsights?.let { insights ->
                            WorkPatternInsightsCard(insights = insights)
                        }
                    }
                    
                    // Selected Period Details
                    item {
                        state.selectedPeriodDetails?.let { details ->
                            PeriodDetailsCard(
                                details = details,
                                onDismiss = {
                                    viewModel.onAction(AnalyticsAction.ClearSelection)
                                }
                            )
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
fun AnalyticsTabSelector(
    selectedPeriod: AnalyticsPeriod,
    onPeriodSelected: (AnalyticsPeriod) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AnalyticsPeriod.entries.forEach { period ->
            val isSelected = selectedPeriod == period
            val text = when (period) {
                AnalyticsPeriod.WEEK -> "Week"
                AnalyticsPeriod.SIX_WEEKS -> "6 Weeks"
                AnalyticsPeriod.SIX_MONTHS -> "6 Months"
            }
            
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onPeriodSelected(period) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.cardBackground()
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (isSelected) 4.dp else 2.dp
                )
            ) {
                Text(
                    text = text,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimary 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun WeekAnalyticsContent(
    analytics: com.andriybobchuk.time.time.domain.WeekAnalytics,
    onDateClick: (kotlinx.datetime.LocalDate) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Pie Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.cardBackground()
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Job Distribution - Week",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                val pieChartData = analytics.jobBreakdown.values.map { jobAnalytics ->
                    val jobColor = TimeDataSource.jobs.find { it.id == jobAnalytics.jobId }
                        ?.color?.let { Color(it) } ?: Color.Gray
                    PieChartData(
                        label = jobAnalytics.jobName,
                        value = jobAnalytics.totalHours,
                        color = jobColor,
                        percentage = jobAnalytics.percentage
                    )
                }
                
                AdvancedPieChart(
                    data = pieChartData,
                    totalHours = analytics.totalWorkingDayHours,
                    dailyAverageHours = analytics.averageDailyHours,
                    effectivenessRate = analytics.effectivenessRate
                )
            }
        }
        
        // Trend Line Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.cardBackground()
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Daily Trends",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                val trendData = createWeekTrendData(analytics)
                val labels = analytics.dailyTrends.map { it.dayName }
                
                TrendLineChart(
                    data = trendData,
                    labels = labels,
                    onPointClick = { dayName ->
                        val selectedDay = analytics.dailyTrends.find { it.dayName == dayName }
                        selectedDay?.let { onDateClick(it.date) }
                    }
                )
            }
        }
    }
}

@Composable
fun SixWeekAnalyticsContent(
    analytics: com.andriybobchuk.time.time.domain.SixWeekAnalytics,
    onWeekClick: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Pie Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.cardBackground()
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Job Distribution - 6 Weeks",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                val pieChartData = analytics.jobBreakdown.values.map { jobAnalytics ->
                    val jobColor = TimeDataSource.jobs.find { it.id == jobAnalytics.jobId }
                        ?.color?.let { Color(it) } ?: Color.Gray
                    PieChartData(
                        label = jobAnalytics.jobName,
                        value = jobAnalytics.totalHours,
                        color = jobColor,
                        percentage = jobAnalytics.percentage
                    )
                }
                
                AdvancedPieChart(
                    data = pieChartData,
                    totalHours = analytics.totalWorkingDayHours,
                    dailyAverageHours = analytics.averageDailyHours,
                    weeklyAverageHours = analytics.averageWeeklyHours,
                    effectivenessRate = analytics.effectivenessRate
                )
            }
        }
        
        // Trend Line Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.cardBackground()
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Weekly Trends",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                val trendData = createSixWeekTrendData(analytics)
                val labels = analytics.weeklyTrends.map { "Week ${it.weekNumber}" }
                
                TrendLineChart(
                    data = trendData,
                    labels = labels,
                    onPointClick = { label ->
                        val weekNumber = label.substringAfter("Week ").toIntOrNull()
                        weekNumber?.let { onWeekClick(it) }
                    }
                )
            }
        }
    }
}

@Composable
fun SixMonthAnalyticsContent(
    analytics: com.andriybobchuk.time.time.domain.SixMonthAnalytics,
    onMonthClick: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Pie Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.cardBackground()
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Job Distribution - 6 Months",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                val pieChartData = analytics.jobBreakdown.values.map { jobAnalytics ->
                    val jobColor = TimeDataSource.jobs.find { it.id == jobAnalytics.jobId }
                        ?.color?.let { Color(it) } ?: Color.Gray
                    PieChartData(
                        label = jobAnalytics.jobName,
                        value = jobAnalytics.totalHours,
                        color = jobColor,
                        percentage = jobAnalytics.percentage
                    )
                }
                
                AdvancedPieChart(
                    data = pieChartData,
                    totalHours = analytics.totalWorkingDayHours,
                    dailyAverageHours = analytics.averageDailyHours,
                    weeklyAverageHours = analytics.averageWeeklyHours,
                    monthlyAverageHours = analytics.averageMonthlyHours,
                    effectivenessRate = analytics.effectivenessRate
                )
            }
        }
        
        // Trend Line Chart
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.cardBackground()
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Monthly Trends",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                val trendData = createSixMonthTrendData(analytics)
                val labels = analytics.monthlyTrends.map { it.monthName }
                
                TrendLineChart(
                    data = trendData,
                    labels = labels,
                    onPointClick = { monthName ->
                        onMonthClick(monthName)
                    }
                )
            }
        }
    }
}

@Composable
fun WorkPatternInsightsCard(
    insights: com.andriybobchuk.time.time.domain.WorkPatternInsights
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.cardBackground()
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Work Pattern Insights",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Peak productivity hours
            if (insights.peakProductivityHours.isNotEmpty()) {
                InsightItem(
                    title = "Peak Productivity Hours",
                    value = insights.peakProductivityHours.joinToString(", ") { "${it}:00" }
                )
            }
            
            // Scores
            InsightItem(
                title = "Work-Life Balance Score",
                value = "${insights.workLifeBalanceScore.toInt()}/100",
                color = getScoreColor(insights.workLifeBalanceScore)
            )
            
            InsightItem(
                title = "Consistency Score",
                value = "${insights.consistencyScore.toInt()}/100",
                color = getScoreColor(insights.consistencyScore)
            )
            
            InsightItem(
                title = "Burnout Risk",
                value = "${insights.burnoutRiskScore.toInt()}/100",
                color = getInverseScoreColor(insights.burnoutRiskScore)
            )
            
            // Recommendations
            if (insights.recommendedImprovements.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Recommendations:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                insights.recommendedImprovements.forEach { recommendation ->
                    Text(
                        text = "• $recommendation",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InsightItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon?.let {
                Icon(
                    it,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = color
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = title,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
fun PeriodDetailsCard(
    details: com.andriybobchuk.time.time.domain.PeriodDetails,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Period Details",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                TextButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }
            
            val periodText = when (details.period) {
                AnalyticsPeriod.WEEK -> details.selectedDate?.toString() ?: ""
                AnalyticsPeriod.SIX_WEEKS -> "Week ${details.selectedWeek}"
                AnalyticsPeriod.SIX_MONTHS -> details.selectedMonth ?: ""
            }
            
            Text(
                text = periodText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // Summary stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Total Hours",
                    value = DateTimeUtils.formatDuration(details.totalHours)
                )
                StatItem(
                    label = "Daily Average",
                    value = DateTimeUtils.formatDuration(details.averageDailyHours)
                )
                StatItem(
                    label = "Effectiveness",
                    value = "${details.effectivenessBreakdown.productivePercentage.toInt()}%"
                )
            }
            
            // Job breakdowns
            details.jobBreakdowns.forEach { job ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = job.jobName,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${DateTimeUtils.formatDuration(job.totalHours)} (${job.percentage.toInt()}%)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

private fun getScoreColor(score: Double): Color {
    return when {
        score >= 80 -> Color(0xFF4CAF50)
        score >= 60 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}

private fun getInverseScoreColor(score: Double): Color {
    return when {
        score >= 80 -> Color(0xFFF44336)
        score >= 60 -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }
}

// Helper functions to create trend data
private fun createWeekTrendData(analytics: com.andriybobchuk.time.time.domain.WeekAnalytics): List<TrendLineData> {
    val maxValue = analytics.dailyTrends.maxOfOrNull { it.totalHours } ?: 1.0
    
    return analytics.jobBreakdown.map { (jobId, jobAnalytics) ->
        val jobColor = TimeDataSource.jobs.find { it.id == jobId }?.color?.let { Color(it) } ?: Color.Gray
        
        val points = analytics.dailyTrends.mapIndexed { index, dayTrend ->
            val jobHours = dayTrend.jobHours[jobId] ?: 0.0
            TrendPoint(
                x = index.toFloat() / (analytics.dailyTrends.size - 1).toFloat(),
                y = (jobHours / maxValue).toFloat(),
                value = jobHours,
                label = dayTrend.dayName
            )
        }
        
        TrendLineData(
            jobId = jobId,
            jobName = jobAnalytics.jobName,
            color = jobColor,
            points = points
        )
    }
}

private fun createSixWeekTrendData(analytics: com.andriybobchuk.time.time.domain.SixWeekAnalytics): List<TrendLineData> {
    val maxValue = analytics.weeklyTrends.maxOfOrNull { it.totalHours } ?: 1.0
    
    return analytics.jobBreakdown.map { (jobId, jobAnalytics) ->
        val jobColor = TimeDataSource.jobs.find { it.id == jobId }?.color?.let { Color(it) } ?: Color.Gray
        
        val points = analytics.weeklyTrends.mapIndexed { index, weekTrend ->
            val jobHours = weekTrend.jobHours[jobId] ?: 0.0
            TrendPoint(
                x = index.toFloat() / (analytics.weeklyTrends.size - 1).toFloat(),
                y = (jobHours / maxValue).toFloat(),
                value = jobHours,
                label = "Week ${weekTrend.weekNumber}"
            )
        }
        
        TrendLineData(
            jobId = jobId,
            jobName = jobAnalytics.jobName,
            color = jobColor,
            points = points
        )
    }
}

private fun createSixMonthTrendData(analytics: com.andriybobchuk.time.time.domain.SixMonthAnalytics): List<TrendLineData> {
    val maxValue = analytics.monthlyTrends.maxOfOrNull { it.totalHours } ?: 1.0
    
    return analytics.jobBreakdown.map { (jobId, jobAnalytics) ->
        val jobColor = TimeDataSource.jobs.find { it.id == jobId }?.color?.let { Color(it) } ?: Color.Gray
        
        val points = analytics.monthlyTrends.mapIndexed { index, monthTrend ->
            val jobHours = monthTrend.jobHours[jobId] ?: 0.0
            TrendPoint(
                x = index.toFloat() / (analytics.monthlyTrends.size - 1).toFloat(),
                y = (jobHours / maxValue).toFloat(),
                value = jobHours,
                label = monthTrend.monthName
            )
        }
        
        TrendLineData(
            jobId = jobId,
            jobName = jobAnalytics.jobName,
            color = jobColor,
            points = points
        )
    }
}