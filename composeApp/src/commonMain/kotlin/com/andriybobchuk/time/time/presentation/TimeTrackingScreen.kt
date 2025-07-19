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
import com.andriybobchuk.time.time.domain.TimeBlock
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import com.andriybobchuk.time.core.presentation.DateTimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeTrackingScreen(
    viewModel: TimeTrackingViewModel,
    bottomNavbar: @Composable () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Time Tracking") }
            )
        },
        bottomBar = { bottomNavbar() }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Date selector
            DateSelector(
                selectedDate = state.selectedDate,
                onDateSelected = { date ->
                    viewModel.onAction(TimeTrackingAction.SelectDate(date))
                }
            )

            // Time blocks list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.timeBlocks) { timeBlock ->
                    TimeBlockCard(
                        timeBlock = timeBlock,
                        onDelete = {
                            viewModel.onAction(TimeTrackingAction.DeleteTimeBlock(timeBlock.id))
                        }
                    )
                }

                // Total summary card
                if (state.dailySummary != null) {
                    item {
                        TotalSummaryCard(summary = state.dailySummary!!)
                    }
                }
            }

            // Job buttons
            JobButtons(
                jobs = state.jobs,
                activeTimeBlock = state.activeTimeBlock,
                onStartTracking = { jobId ->
                    viewModel.onAction(TimeTrackingAction.StartTracking(jobId))
                },
                onStopTracking = {
                    viewModel.onAction(TimeTrackingAction.StopTracking)
                }
            )
        }
    }
}

@Composable
fun DateSelector(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    // Simple date formatting without DateTimeFormat
    
    // Generate last 7 days for dropdown
    val dateOptions = remember {
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        (0..6).map { daysAgo ->
            currentDate.minus(DatePeriod(days = daysAgo))
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
            Text("Selected: ${DateTimeUtils.formatDateWithYear(selectedDate)}")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            dateOptions.forEach { date ->
                            DropdownMenuItem(
                text = { Text(DateTimeUtils.formatDateWithYear(date)) },
                onClick = {
                    onDateSelected(date)
                    expanded = false
                }
            )
            }
        }
    }
}

@Composable
fun TimeBlockCard(
    timeBlock: TimeBlock,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(timeBlock.jobColor).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                Row {
                    Text(
                        text = timeBlock.jobName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = DateTimeUtils.formatDuration(timeBlock.getDurationInHours()),
                        fontSize = 14.sp
                    )
                }
                Spacer(Modifier.weight(1f))
                val endTime = timeBlock.endTime?.let { endTime ->
                    DateTimeUtils.formatTime(endTime)
                }
                Text(
                    text = "${DateTimeUtils.formatTime(timeBlock.startTime)} - ${endTime?:"In Progress"}",
                    fontSize = 12.sp
                )
            }

            // Job color indicator
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color(timeBlock.jobColor))
            )
        }
    }
}

@Composable
fun TotalSummaryCard(summary: com.andriybobchuk.time.time.domain.DailySummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Total: ${DateTimeUtils.formatDuration(summary.totalHours)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            summary.jobBreakdown.values.toList().forEach { jobSummary ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${jobSummary.jobName}: ${DateTimeUtils.formatDuration(jobSummary.totalHours)}",
                        fontSize = 14.sp
                    )
                    Text(
                        text = "${jobSummary.percentage}%",
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun JobButtons(
    jobs: List<com.andriybobchuk.time.time.domain.Job>,
    activeTimeBlock: TimeBlock?,
    onStartTracking: (String) -> Unit,
    onStopTracking: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        if (activeTimeBlock != null) {
            // Stop button
            Button(
                onClick = onStopTracking,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red
                )
            ) {
                Text("Stop Tracking (${activeTimeBlock.jobName})")
            }
        } else {
            // Job buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                jobs.forEach { job ->
                    Button(
                        onClick = { onStartTracking(job.id) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(job.color)
                        )
                    ) {
                        Text(job.name)
                    }
                }
            }
        }
    }
} 