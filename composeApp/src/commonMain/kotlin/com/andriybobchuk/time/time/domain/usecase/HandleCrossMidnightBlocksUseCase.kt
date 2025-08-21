package com.andriybobchuk.time.time.domain.usecase

import com.andriybobchuk.time.time.domain.TimeBlock
import com.andriybobchuk.time.time.domain.TimeRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toInstant

class HandleCrossMidnightBlocksUseCase(
    private val repository: TimeRepository
) {
    suspend operator fun invoke() {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val currentDate = now.date
        
        // Check for active blocks that started before today
        val activeBlock = repository.getActiveTimeBlockSync()
        
        if (activeBlock != null && activeBlock.startTime.date < currentDate) {
            // Split the cross-midnight block
            val splitBlocks = splitCrossMidnightBlock(activeBlock)
            
            if (splitBlocks.size == 2) {
                val completedBlock = splitBlocks[0]
                val continuationBlock = splitBlocks[1]
                
                // Update the original block to end at 23:59
                repository.upsertTimeBlock(completedBlock)
                
                // Create the continuation block for today
                repository.upsertTimeBlock(continuationBlock)
            }
        }
    }
    
    private suspend fun splitCrossMidnightBlock(timeBlock: TimeBlock): List<TimeBlock> {
        val startDate = timeBlock.startTime.date
        val currentTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        
        // If block is still active and started before today
        if (timeBlock.endTime == null && startDate < currentTime.date) {
            val endOfStartDay = LocalDateTime(startDate, LocalTime(23, 59, 59))
            val startOfNextDay = LocalDateTime(currentTime.date, LocalTime(0, 0, 0))
            
            // First block: ends at 23:59 of the start day
            val firstBlock = timeBlock.copy(
                endTime = endOfStartDay,
                duration = timeBlock.startTime.let { start ->
                    val startInstant = start.toInstant(TimeZone.currentSystemDefault())
                    val endInstant = endOfStartDay.toInstant(TimeZone.currentSystemDefault())
                    (endInstant.epochSeconds - startInstant.epochSeconds) * 1000L
                }
            )
            
            // Second block: starts at 00:00 of today, still active
            val secondBlock = timeBlock.copy(
                id = 0, // New block will get auto-generated ID
                startTime = startOfNextDay,
                endTime = null,
                duration = 0
            )
            
            return listOf(firstBlock, secondBlock)
        }
        
        return listOf(timeBlock)
    }
}