package com.andriybobchuk.time.time.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.time.core.presentation.Toolbars
import com.andriybobchuk.time.core.presentation.cardBackground
import com.andriybobchuk.time.time.domain.Job

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSettingsScreen(
    viewModel: TimeSettingsViewModel,
    bottomNavbar: @Composable () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Toolbars.Primary(
                title = "Jobs",
                scrollBehavior = scrollBehavior
            )
        },
        bottomBar = { bottomNavbar() },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.onAction(TimeSettingsAction.ShowAddJobSheet) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Job")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            
            items(state.jobs) { job ->
                JobCard(
                    job = job,
                    onEdit = { viewModel.onAction(TimeSettingsAction.ShowEditJobSheet(job)) },
                    onDelete = { viewModel.onAction(TimeSettingsAction.DeleteJob(job)) },
                    onToggleVisibility = { viewModel.onAction(TimeSettingsAction.ToggleJobVisibility(job)) },
                    onSetMain = { viewModel.onAction(TimeSettingsAction.SetMainJob(job.id)) },
                    onClick = { viewModel.onAction(TimeSettingsAction.ShowJobDetails(job)) }
                )
            }
            
            if (state.jobs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No jobs yet. Add your first job!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        
        // Add/Edit Job Sheet
        if (state.showAddEditSheet) {
            AddEditJobSheet(
                job = state.editingJob,
                onSave = { id, name, color, isMain, hourlyRate, isHidden ->
                    viewModel.onAction(
                        TimeSettingsAction.SaveJob(id, name, color, isMain, hourlyRate, isHidden)
                    )
                },
                onDismiss = { viewModel.onAction(TimeSettingsAction.HideSheets) }
            )
        }
        
        // Delete Confirmation Dialog
        state.showDeleteConfirmation?.let { job ->
            AlertDialog(
                onDismissRequest = { viewModel.onAction(TimeSettingsAction.CancelDelete) },
                title = { Text("Delete Job") },
                text = { Text("Are you sure you want to delete \"${job.name}\"? This will not delete existing time blocks.") },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.onAction(TimeSettingsAction.ConfirmDeleteJob(job)) }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.onAction(TimeSettingsAction.CancelDelete) }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
        
        // Job Details Bottom Sheet
        state.selectedJobForDetails?.let { job ->
            JobDetailsSheet(
                job = job,
                onDismiss = { viewModel.onAction(TimeSettingsAction.HideJobDetails) }
            )
        }
        
        // Error Snackbar
        state.error?.let { error ->
            LaunchedEffect(error) {
                // Clear error after showing it
                // In a real app, you'd show a Snackbar here
                viewModel.onAction(TimeSettingsAction.ClearError)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun JobCard(
    job: Job,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleVisibility: () -> Unit,
    onSetMain: () -> Unit,
    onClick: () -> Unit
) {
    var showContextMenu by remember { mutableStateOf(false) }
    
    Box {
        Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!job.isHidden) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else Modifier
            )
            .combinedClickable(
                onClick = { onClick() },
                onLongClick = { showContextMenu = true }
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.cardBackground()
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Job color indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(job.color))
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Job details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = job.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (job.isHidden) 
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        else 
                            MaterialTheme.colorScheme.onSurface
                    )
                    
                    if (job.isMain) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = Color(0xFF9C27B0),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "J1",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    
                    if (job.isHidden) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Hidden",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                job.hourlyRatePLN?.let { rate ->
                    Text(
                        text = "${rate.toInt()} PLN/h",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
            },
            leadingIcon = {
                Icon(Icons.Default.Edit, contentDescription = null)
            }
        )
        DropdownMenuItem(
            text = { Text("Delete") },
            onClick = {
                onDelete()
                showContextMenu = false
            },
            leadingIcon = {
                Icon(Icons.Default.Delete, contentDescription = null)
            }
        )
        DropdownMenuItem(
            text = { Text(if (job.isHidden) "Show" else "Hide") },
            onClick = {
                onToggleVisibility()
                showContextMenu = false
            },
            leadingIcon = {
                Icon(
                    if (job.isHidden) Icons.Default.Settings else Icons.Default.Close,
                    contentDescription = null
                )
            }
        )
        if (!job.isMain) {
            DropdownMenuItem(
                text = { Text("Set as Main Job") },
                onClick = {
                    onSetMain()
                    showContextMenu = false
                },
                leadingIcon = {
                    Icon(Icons.Default.Star, contentDescription = null)
                }
            )
        }
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditJobSheet(
    job: Job?,
    onSave: (String?, String, Long, Boolean, Double?, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(job) { mutableStateOf(job?.name ?: "") }
    var isMain by remember(job) { mutableStateOf(job?.isMain ?: false) }
    var isHidden by remember(job) { mutableStateOf(job?.isHidden ?: false) }
    var hourlyRate by remember(job) { mutableStateOf(job?.hourlyRatePLN?.toString() ?: "") }
    var selectedColor by remember(job) { mutableStateOf(job?.color ?: 0xFFFFC747) }
    
    val colors = listOf(
        0xFFFFC747, // Yellow
        0xFF41D0FF, // Blue
        0xFF4CAF50, // Green
        0xFFFF5722, // Deep Orange
        0xFF9C27B0, // Purple
        0xFFE91E63, // Pink
        0xFF00BCD4, // Cyan
        0xFFFFEB3B  // Amber
    )
    
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = if (job == null) "Add Job" else "Edit Job",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Client Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = hourlyRate,
                onValueChange = { hourlyRate = it },
                label = { Text("Hourly Rate (PLN)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Color",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                colors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(color))
                            .clickable { selectedColor = color }
                            .then(
                                if (selectedColor == color)
                                    Modifier.background(
                                        Color.Black.copy(alpha = 0.3f),
                                        CircleShape
                                    )
                                else Modifier
                            )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = isMain,
                    onCheckedChange = { isMain = it }
                )
                Text(
                    text = "Main Job (J1)",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = !isHidden,
                    onCheckedChange = { isHidden = !it }
                )
                Text(
                    text = "Show in app",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onSave(
                            job?.id,
                            name,
                            selectedColor,
                            isMain,
                            hourlyRate.toDoubleOrNull(),
                            isHidden
                        )
                    },
                    enabled = name.isNotBlank()
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
fun JobDetailsSheet(
    job: Job,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(job.color))
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = job.name,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (job.isMain) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color(0xFF9C27B0),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "J1",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    
                    job.hourlyRatePLN?.let { rate ->
                        Text(
                            text = "${rate.toInt()} PLN/hour",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            HorizontalDivider()
            
            // Total analysis placeholder
            Text(
                text = "Total Analysis",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            // Placeholder stats
            StatRow("Total Hours", "Coming soon")
            StatRow("Total Earnings", "Coming soon")
            StatRow("Average Daily Hours", "Coming soon")
            StatRow("Effectiveness Rate", "Coming soon")
            StatRow("Most Productive Time", "Coming soon")
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}