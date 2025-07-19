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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import com.andriybobchuk.time.core.presentation.DateTimeUtils
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics") }
            )
        },
        bottomBar = { bottomNavbar() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Week selector
            WeekSelector(
                selectedWeekStart = state.selectedWeekStart,
                onWeekSelected = { weekStart ->
                    viewModel.onAction(AnalyticsAction.SelectWeek(weekStart))
                }
            )

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
                        WeeklySummaryCard(analytics = state.weeklyAnalytics!!)
                    }
                    
                    item {
                        Text(
                            text = "Job Breakdown",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    items(state.weeklyAnalytics!!.jobBreakdown.values.toList()) { jobAnalytics ->
                        JobAnalyticsCard(jobAnalytics = jobAnalytics)
                    }
                    
                    item {
                        Text(
                            text = "Daily Breakdown",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    
                    item {
                        androidx.compose.foundation.lazy.LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
                        ) {
                            items(state.weeklyAnalytics!!.dailySummaries) { dailySummary ->
                                DailySummaryCardHorizontal(summary = dailySummary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeekSelector(
    selectedWeekStart: LocalDate,
    onWeekSelected: (LocalDate) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    // Simple date formatting without DateTimeFormat
    
    // Generate last 4 weeks for dropdown
    val weekOptions = remember {
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val currentWeekStart = getWeekStart(currentDate)
        (0..3).map { weeksAgo ->
            currentWeekStart.minus(DatePeriod(days = weeksAgo * 7))
        }.reversed()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Button(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Week of: ${DateTimeUtils.formatWeekRange(selectedWeekStart)}")
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
fun WeeklySummaryCard(analytics: com.andriybobchuk.time.time.domain.WeeklyAnalytics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Blue.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Weekly Summary",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Total Hours: ${DateTimeUtils.formatDuration(analytics.totalHours)}",
                fontSize = 16.sp
            )
            
            Text(
                text = "Average Daily: ${DateTimeUtils.formatDuration(analytics.averageDailyHours)}",
                fontSize = 16.sp
            )
            
            Text(
                text = "Period: ${DateTimeUtils.formatDateWithYear(analytics.weekStart)} - ${DateTimeUtils.formatDateWithYear(analytics.weekEnd)}",
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun JobAnalyticsCard(jobAnalytics: com.andriybobchuk.time.time.domain.JobAnalytics) {
    // Get job color from TimeDataSource
    val jobColor = remember(jobAnalytics.jobId) {
        val job = TimeDataSource.jobs.find { it.id == jobAnalytics.jobId }
        job?.color?.let { Color(it) }
    }?:Color(0x6CFB95)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(jobColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = jobAnalytics.jobName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total: ${DateTimeUtils.formatDuration(jobAnalytics.totalHours)}",
                    fontSize = 14.sp
                )
                Text(
                    text = "Daily Avg: ${DateTimeUtils.formatDuration(jobAnalytics.averageDailyHours)}",
                    fontSize = 14.sp
                )
                Text(
                    text = "${jobAnalytics.percentage}% of total",
                    fontSize = 14.sp
                )
            }

            // Job color indicator
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(jobColor)
            )
        }
    }
}

@Composable
fun DailySummaryCard(summary: com.andriybobchuk.time.time.domain.DailySummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Gray.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "${DateTimeUtils.formatDate(summary.date)} - ${DateTimeUtils.formatDuration(summary.totalHours)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            summary.jobBreakdown.values.toList().forEach { jobSummary ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${jobSummary.jobName}: ${DateTimeUtils.formatDuration(jobSummary.totalHours)}",
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${jobSummary.percentage}%",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun DailySummaryCardHorizontal(summary: com.andriybobchuk.time.time.domain.DailySummary) {
    Card(
        modifier = Modifier.width(140.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Gray.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = DateTimeUtils.formatDate(summary.date),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = DateTimeUtils.formatDuration(summary.totalHours),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Blue
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            summary.jobBreakdown.values.toList().forEach { jobSummary ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = jobSummary.jobName,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "${jobSummary.percentage}%",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
} 