package com.andriybobchuk.time.time.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.time.core.presentation.BarChart
import com.andriybobchuk.time.core.presentation.DateTimeUtils
import com.andriybobchuk.time.core.presentation.PieChart
import com.andriybobchuk.time.core.presentation.PieChartData
import com.andriybobchuk.time.core.presentation.BarChartData
import com.andriybobchuk.time.core.presentation.DailyBarData
import com.andriybobchuk.time.core.presentation.Toolbars
import com.andriybobchuk.time.core.presentation.cardBackground
import com.andriybobchuk.time.time.domain.Job


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    bottomNavbar: @Composable () -> Unit
) {
    val state by viewModel.state.collectAsState()

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        topBar = {
            Toolbars.Primary(
                title = "Analytics - Last 7 Days",
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
            if (state.isLoading) {
                // Loading state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.error != null) {
                // Error state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
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
                    }
                }
            } else if (state.weeklyAnalytics != null) {
                // Content state
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        JobBreakdownCard(weeklyAnalytics = state.weeklyAnalytics!!, jobs = state.jobs)
                    }
                    
                    item {
                        DailyBreakdownCard(weeklyAnalytics = state.weeklyAnalytics!!, jobs = state.jobs)
                    }
                }
            } else {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
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
fun JobBreakdownCard(weeklyAnalytics: com.andriybobchuk.time.time.domain.WeeklyAnalytics, jobs: List<Job>) {
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
                val jobColor = jobs.find { it.id == jobAnalytics.jobId }?.color?.let { Color(it) } ?: Color.Gray
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
fun DailyBreakdownCard(weeklyAnalytics: com.andriybobchuk.time.time.domain.WeeklyAnalytics, jobs: List<Job>) {
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
                    val jobColor = jobs.find { it.id == jobSummary.jobId }?.color?.let { Color(it) } ?: Color.Gray
                    BarChartData(
                        label = jobSummary.jobName,
                        value = jobSummary.totalHours,
                        color = jobColor,
                        percentage = jobSummary.percentage
                    )
                }
                
                DailyBarData(
                    date = DateTimeUtils.formatDayName(dailySummary.date),
                    totalHours = dailySummary.totalHours,
                    jobData = jobData
                )
            }
            
            BarChart(data = barChartData)
        }
    }
}
