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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
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
import com.andriybobchuk.time.core.presentation.Icons
import com.andriybobchuk.time.time.data.TimeDataSource
import com.andriybobchuk.time.time.domain.Job

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
                title = { Text("Time Tracking") },
                actions = {
                    // Date selector in top bar
                    DateSelectorInTopBar(
                        selectedDate = state.selectedDate,
                        onDateSelected = { date ->
                            viewModel.onAction(TimeTrackingAction.SelectDate(date))
                        }
                    )
                }
            )
        },
        bottomBar = { bottomNavbar() },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.onAction(TimeTrackingAction.ShowAddSheet)
                }
            ) {
                Icons.AddIcon()
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

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
                        },
                        onEdit = {
                            viewModel.onAction(TimeTrackingAction.EditTimeBlock(timeBlock))
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
    
    // Edit Time Block Bottom Sheet
    if (state.showEditSheet && state.editingTimeBlock != null) {
        EditTimeBlockSheet(
            timeBlock = state.editingTimeBlock!!,
            jobs = state.jobs,
            onDismiss = {
                viewModel.onAction(TimeTrackingAction.HideEditSheet)
            },
            onSave = { updatedTimeBlock ->
                viewModel.onAction(TimeTrackingAction.UpdateTimeBlock(updatedTimeBlock))
            }
        )
    }
    
    // Add Time Block Bottom Sheet
    if (state.showAddSheet) {
        AddTimeBlockSheet(
            jobs = state.jobs,
            onDismiss = {
                viewModel.onAction(TimeTrackingAction.HideAddSheet)
            },
            onSave = { jobId, startTime, endTime ->
                viewModel.onAction(TimeTrackingAction.AddTimeBlock(jobId, startTime, endTime))
            }
        )
    }
}

@Composable
fun DateSelectorInTopBar(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    // Generate last 7 days for dropdown
    val dateOptions = remember {
        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        (0..6).map { daysAgo ->
            currentDate.minus(DatePeriod(days = daysAgo))
        }.reversed()
    }

    Box {
        Button(
            onClick = { expanded = true },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        ) {
            Text(
                text = DateTimeUtils.formatDate(selectedDate),
                color = Color.Black
            )
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
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    // Get job color from TimeDataSource
    val jobColor = remember(timeBlock.jobId) {
        val job = TimeDataSource.jobs.find { it.id == timeBlock.jobId }
        job?.color?.let { Color(it) }
    }?:Color(0xFF808080)
    
    var showContextMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { showContextMenu = true }
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = jobColor
        )
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
                    text = timeBlock.jobName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Duration: ${DateTimeUtils.formatDuration(timeBlock.getDurationInHours())}",
                    fontSize = 14.sp
                )
                Text(
                    text = "Start: ${DateTimeUtils.formatTime(timeBlock.startTime)}",
                    fontSize = 12.sp
                )
                timeBlock.endTime?.let { endTime ->
                    Text(
                        text = "End: ${DateTimeUtils.formatTime(endTime)}",
                        fontSize = 12.sp
                    )
                }
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
    
    // Context menu
    DropdownMenu(
        expanded = showContextMenu,
        onDismissRequest = { showContextMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("Edit") },
            onClick = {
                onEdit()
                showContextMenu = false
            }
        )
        DropdownMenuItem(
            text = { Text("Delete") },
            onClick = {
                onDelete()
                showContextMenu = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTimeBlockSheet(
    timeBlock: TimeBlock,
    jobs: List<Job>,
    onDismiss: () -> Unit,
    onSave: (TimeBlock) -> Unit
) {
    var selectedJobId by remember { mutableStateOf(timeBlock.jobId) }
    var startTimeText by remember { mutableStateOf(DateTimeUtils.formatTime(timeBlock.startTime)) }
    var endTimeText by remember { mutableStateOf(timeBlock.endTime?.let { DateTimeUtils.formatTime(it) } ?: "") }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Job selection
            var jobExpanded by remember { mutableStateOf(false) }
            val selectedJob = jobs.find { it.id == selectedJobId }

            Text("Edit Time Block")
            Text("Project", fontWeight = FontWeight.Bold)
            Box {
                Button(
                    onClick = { jobExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
                ) {
                    Text(
                        text = selectedJob?.name ?: "Select Project",
                        color = Color.Black
                    )
                }
                
                DropdownMenu(
                    expanded = jobExpanded,
                    onDismissRequest = { jobExpanded = false }
                ) {
                    jobs.forEach { job ->
                        DropdownMenuItem(
                            text = { Text(job.name) },
                            onClick = {
                                selectedJobId = job.id
                                jobExpanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Start time
            Text("Start Time", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = startTimeText,
                onValueChange = { startTimeText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("HH:MM") }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // End time
            Text("End Time", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = endTimeText,
                onValueChange = { endTimeText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("HH:MM") }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = {
                        // Parse time and create updated time block
                        val startTime = parseTimeString(startTimeText, timeBlock.startTime.date)
                        val endTime = if (endTimeText.isNotEmpty()) parseTimeString(endTimeText, timeBlock.startTime.date) else null
                        
                        if (startTime != null) {
                            val updatedTimeBlock = timeBlock.copy(
                                jobId = selectedJobId,
                                jobName = selectedJob?.name ?: timeBlock.jobName,
                                startTime = startTime,
                                endTime = endTime
                            )
                            onSave(updatedTimeBlock)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTimeBlockSheet(
    jobs: List<Job>,
    onDismiss: () -> Unit,
    onSave: (String, kotlinx.datetime.LocalDateTime, kotlinx.datetime.LocalDateTime) -> Unit
) {
    var selectedJobId by remember { mutableStateOf("") }
    var startTimeText by remember { mutableStateOf("") }
    var endTimeText by remember { mutableStateOf("") }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Job selection
            var jobExpanded by remember { mutableStateOf(false) }
            val selectedJob = jobs.find { it.id == selectedJobId }

            Text("Add Time Block")
            Text("Project", fontWeight = FontWeight.Bold)
            Box {
                Button(
                    onClick = { jobExpanded = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
                ) {
                    Text(
                        text = selectedJob?.name ?: "Select Project",
                        color = Color.Black
                    )
                }
                
                DropdownMenu(
                    expanded = jobExpanded,
                    onDismissRequest = { jobExpanded = false }
                ) {
                    jobs.forEach { job ->
                        DropdownMenuItem(
                            text = { Text(job.name) },
                            onClick = {
                                selectedJobId = job.id
                                jobExpanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Start time
            Text("Start Time", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = startTimeText,
                onValueChange = { startTimeText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("HH:MM") }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // End time
            Text("End Time", fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = endTimeText,
                onValueChange = { endTimeText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("HH:MM") }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = {
                        val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                        val startTime = parseTimeString(startTimeText, currentDate)
                        val endTime = parseTimeString(endTimeText, currentDate)
                        
                        if (startTime != null && endTime != null && selectedJobId.isNotEmpty()) {
                            onSave(selectedJobId, startTime, endTime)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun parseTimeString(timeString: String, date: kotlinx.datetime.LocalDate): kotlinx.datetime.LocalDateTime? {
    return try {
        val parts = timeString.split(":")
        if (parts.size == 2) {
            val hour = parts[0].toInt()
            val minute = parts[1].toInt()
            if (hour in 0..23 && minute in 0..59) {
                kotlinx.datetime.LocalDateTime(date, kotlinx.datetime.LocalTime(hour, minute))
            } else null
        } else null
    } catch (e: Exception) {
        null
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
                        Text(
                            text = job.name,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
} 