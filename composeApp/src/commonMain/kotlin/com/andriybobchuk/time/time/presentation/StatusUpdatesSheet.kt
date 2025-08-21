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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andriybobchuk.time.core.presentation.DateTimeUtils
import com.andriybobchuk.time.core.presentation.bottomSheetBackground
import com.andriybobchuk.time.core.presentation.buttonBackground
import com.andriybobchuk.time.core.presentation.buttonTextColor
import com.andriybobchuk.time.core.presentation.textColor
import com.andriybobchuk.time.time.domain.Job
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusUpdatesSheet(
    selectedDate: LocalDate,
    jobs: List<Job>,
    statusTexts: Map<String, String>,
    onDismiss: () -> Unit,
    onStatusTextChange: (String, String) -> Unit,
    onSave: () -> Unit
) {
    // Use local state for text inputs to prevent cursor jumping
    val localStatusTexts = remember(statusTexts) {
        mutableStateOf(statusTexts.toMutableMap())
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.bottomSheetBackground(),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Status Updates",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.textColor(),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Update status for ${DateTimeUtils.formatDateWithYear(selectedDate)}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.textColor().copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Status fields for each job
            jobs.forEach { job ->
                val jobColor = remember(job.id) { Color(job.color) }
                val currentText = localStatusTexts.value[job.id] ?: ""
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    // Job name with color indicator
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .height(16.dp)
                                .fillMaxWidth(0.05f)
                                .background(
                                    color = jobColor,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        )
                        Text(
                            text = job.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.textColor(),
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }

                    // Status text field
                    OutlinedTextField(
                        value = currentText,
                        onValueChange = { newText ->
                            localStatusTexts.value = localStatusTexts.value.toMutableMap().apply {
                                this[job.id] = newText
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { 
                            Text(
                                "What did you work on for ${job.name} today?",
                                color = MaterialTheme.colorScheme.textColor().copy(alpha = 0.5f)
                            )
                        },
                        minLines = 3,
                        maxLines = 5,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "Cancel",
                        color = MaterialTheme.colorScheme.textColor()
                    )
                }

                Button(
                    onClick = {
                        // Update the main state with all local changes
                        localStatusTexts.value.forEach { (jobId, text) ->
                            onStatusTextChange(jobId, text)
                        }
                        onSave()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.buttonBackground()
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Save",
                        color = MaterialTheme.colorScheme.buttonTextColor(),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}