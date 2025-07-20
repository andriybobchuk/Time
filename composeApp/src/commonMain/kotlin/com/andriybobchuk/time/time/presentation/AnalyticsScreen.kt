package com.andriybobchuk.time.time.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.time.core.presentation.BarChart
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import com.andriybobchuk.time.core.presentation.DateTimeUtils
import com.andriybobchuk.time.core.presentation.PieChart
import com.andriybobchuk.time.core.presentation.PieChartData
import com.andriybobchuk.time.core.presentation.BarChartData
import com.andriybobchuk.time.core.presentation.DailyBarData
import com.andriybobchuk.time.core.presentation.Toolbars
import com.andriybobchuk.time.core.presentation.buttonBackground
import com.andriybobchuk.time.core.presentation.buttonTextColor
import com.andriybobchuk.time.core.presentation.cardBackground
import com.andriybobchuk.time.time.data.TimeDataSource

private fun getWeekStart(date: LocalDate): LocalDate {
    val dayOfWeek = date.dayOfWeek.isoDayNumber
    val daysToSubtract = if (dayOfWeek == 1) 0 else dayOfWeek - 1
    return date.minus(DatePeriod(days = daysToSubtract))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    bottomNavbar: @Composable () -> Unit
) {
    val state by viewModel.state.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        topBar = {
            Toolbars.Primary(
                title = "Analytics",
                customContent = {
                    WeekSelectorInTopBar(
                        selectedWeekStart = state.selectedWeekStart,
                        onWeekSelected = { weekStart ->
                            viewModel.onAction(AnalyticsAction.SelectWeek(weekStart))
                        }
                    )
                },
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

            // Analytics content
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.weeklyAnalytics != null) {
                    item {
                        JobBreakdownCard(weeklyAnalytics = state.weeklyAnalytics!!)
                    }
                    
                    item {
                        DailyBreakdownCard(weeklyAnalytics = state.weeklyAnalytics!!)
                    }
                }
            }
        }
    }
}

@Composable
fun WeekSelectorInTopBar(
    selectedWeekStart: LocalDate,
    onWeekSelected: (LocalDate) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    // Generate last 4 weeks for dropdown
    val weekOptions = remember {
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val currentWeekStart = getWeekStart(currentDate)
        (0..3).map { weeksAgo ->
            currentWeekStart.minus(DatePeriod(days = weeksAgo * 7))
        }.reversed()
    }

    Box {
        Button(
            onClick = { expanded = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.buttonBackground()
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        ) {
            Text(
                text = DateTimeUtils.formatWeekRange(selectedWeekStart),
                color = MaterialTheme.colorScheme.buttonTextColor()
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            weekOptions.forEach { weekStart ->
                DropdownMenuItem(
                    text = { Text("Week of: ${DateTimeUtils.formatWeekRange(weekStart)}") },
                    onClick = {
                        onWeekSelected(weekStart)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun JobBreakdownCard(weeklyAnalytics: com.andriybobchuk.time.time.domain.WeeklyAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.cardBackground()
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Job Breakdown",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Convert job analytics to pie chart data
            val pieChartData = weeklyAnalytics.jobBreakdown.values.map { jobAnalytics ->
                val jobColor = TimeDataSource.jobs.find { it.id == jobAnalytics.jobId }?.color?.let { Color(it) } ?: Color.Gray
                PieChartData(
                    label = jobAnalytics.jobName,
                    value = jobAnalytics.totalHours,
                    color = jobColor,
                    percentage = jobAnalytics.percentage
                )
            }
            
            PieChart(
                data = pieChartData,
                totalValue = weeklyAnalytics.totalHours,
                averageValue = weeklyAnalytics.averageDailyHours
            )
        }
    }
}

@Composable
fun DailyBreakdownCard(weeklyAnalytics: com.andriybobchuk.time.time.domain.WeeklyAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.cardBackground()
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Daily Breakdown",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Convert daily summaries to bar chart data
            val barChartData = weeklyAnalytics.dailySummaries.map { dailySummary ->
                val jobData = dailySummary.jobBreakdown.values.map { jobSummary ->
                    val jobColor = TimeDataSource.jobs.find { it.id == jobSummary.jobId }?.color?.let { Color(it) } ?: Color.Gray
                    BarChartData(
                        label = jobSummary.jobName,
                        value = jobSummary.totalHours,
                        color = jobColor,
                        percentage = jobSummary.percentage
                    )
                }
                
                DailyBarData(
                    date = DateTimeUtils.formatDate(dailySummary.date),
                    totalHours = dailySummary.totalHours,
                    jobData = jobData
                )
            }
            
            BarChart(data = barChartData)
        }
    }
}
