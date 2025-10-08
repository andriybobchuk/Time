package com.andriybobchuk.time.time.domain.usecase

import com.andriybobchuk.time.time.domain.ContextSwitchAnalysis
import com.andriybobchuk.time.time.domain.TimeBlock
import com.andriybobchuk.time.time.domain.TimeRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.plus
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

class CalculateContextSwitchTimeUseCase(
    private val repository: TimeRepository
) {
    suspend operator fun invoke(startDate: LocalDate, endDate: LocalDate): ContextSwitchAnalysis {
        // Get all time blocks in the date range
        val allBlocks = mutableListOf<TimeBlock>()
        var currentDate = startDate
        
        while (currentDate <= endDate) {
            val dayBlocks = repository.getTimeBlocksByDate(currentDate).first()
            allBlocks.addAll(dayBlocks)
            currentDate = currentDate.plus(DatePeriod(days = 1))
        }
        
        // Sort blocks by start time
        val sortedBlocks = allBlocks.sortedBy { it.startTime }
        
        // Calculate context switch time (gaps between blocks < 1 hour)
        var contextSwitchTime = 0L
        var effectiveTime = 0L
        
        for (i in 0 until sortedBlocks.size) {
            val block = sortedBlocks[i]
            effectiveTime += block.calculateDuration()
            
            // Check gap to next block
            if (i < sortedBlocks.size - 1) {
                val nextBlock = sortedBlocks[i + 1]
                val gap = nextBlock.startTime.toInstant(TimeZone.currentSystemDefault()) - 
                          (block.endTime?.toInstant(TimeZone.currentSystemDefault()) 
                           ?: block.startTime.toInstant(TimeZone.currentSystemDefault()))
                
                // If gap is less than 1 hour, count it as context switch time
                if (gap < 1.hours && gap > 0.milliseconds) {
                    contextSwitchTime += gap.inWholeMilliseconds
                }
            }
        }
        
        val contextSwitchHours = contextSwitchTime / (1000.0 * 60 * 60)
        val effectiveHours = effectiveTime / (1000.0 * 60 * 60)
        val totalTrackedHours = contextSwitchHours + effectiveHours
        
        return ContextSwitchAnalysis(
            totalContextSwitchTime = contextSwitchHours,
            totalEffectiveTime = effectiveHours,
            totalTrackedTime = totalTrackedHours,
            contextSwitchPercentage = if (totalTrackedHours > 0) (contextSwitchHours / totalTrackedHours) * 100 else 0.0
        )
    }
}