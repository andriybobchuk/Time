package com.andriybobchuk.time.time.presentation

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus

/**
 * Formats hours as "1h 30m" format
 */
fun formatHoursAndMinutes(hours: Double): String {
    val wholeHours = hours.toInt()
    val minutes = ((hours - wholeHours) * 60).toInt()
    
    return when {
        wholeHours > 0 && minutes > 0 -> "${wholeHours}h ${minutes}m"
        wholeHours > 0 -> "${wholeHours}h"
        minutes > 0 -> "${minutes}m"
        else -> "0m"
    }
}

/**
 * Formats hours as whole hours only (rounded) for analytics
 */
fun formatHoursOnly(hours: Double): String {
    val roundedHours = kotlin.math.round(hours).toInt()
    return if (roundedHours > 0) "${roundedHours}h" else "0h"
}

/**
 * Calculates weekdays (Monday-Friday) in a date range
 */
fun getWeekdaysInRange(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
    val weekdays = mutableListOf<LocalDate>()
    var currentDate = startDate
    
    while (currentDate <= endDate) {
        if (currentDate.dayOfWeek != DayOfWeek.SATURDAY && currentDate.dayOfWeek != DayOfWeek.SUNDAY) {
            weekdays.add(currentDate)
        }
        currentDate = currentDate.plus(DatePeriod(days = 1))
    }
    
    return weekdays
}

/**
 * Counts weekdays (Monday-Friday) in a date range
 */
fun countWeekdaysInRange(startDate: LocalDate, endDate: LocalDate): Int {
    return getWeekdaysInRange(startDate, endDate).size
}