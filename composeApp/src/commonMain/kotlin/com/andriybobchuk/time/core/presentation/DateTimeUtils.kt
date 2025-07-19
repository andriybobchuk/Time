package com.andriybobchuk.time.core.presentation

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.plus

object DateTimeUtils {
    
    fun formatDate(date: LocalDate): String {
        val monthName = when (date.month) {
            Month.JANUARY -> "Jan"
            Month.FEBRUARY -> "Feb"
            Month.MARCH -> "Mar"
            Month.APRIL -> "Apr"
            Month.MAY -> "May"
            Month.JUNE -> "Jun"
            Month.JULY -> "Jul"
            Month.AUGUST -> "Aug"
            Month.SEPTEMBER -> "Sep"
            Month.OCTOBER -> "Oct"
            Month.NOVEMBER -> "Nov"
            Month.DECEMBER -> "Dec"
            else -> TODO()
        }
        
        return "$monthName ${date.dayOfMonth}"
    }
    
    fun formatDateWithYear(date: LocalDate): String {
        val monthName = when (date.month) {
            Month.JANUARY -> "January"
            Month.FEBRUARY -> "February"
            Month.MARCH -> "March"
            Month.APRIL -> "April"
            Month.MAY -> "May"
            Month.JUNE -> "June"
            Month.JULY -> "July"
            Month.AUGUST -> "August"
            Month.SEPTEMBER -> "September"
            Month.OCTOBER -> "October"
            Month.NOVEMBER -> "November"
            Month.DECEMBER -> "December"
            else -> TODO()
        }
        
        return "$monthName ${date.dayOfMonth}, ${date.year}"
    }
    
    fun formatTime(dateTime: LocalDateTime): String {
        val hour = dateTime.hour
        val minute = dateTime.minute
        
        val hourStr = if (hour < 10) "0$hour" else "$hour"
        val minuteStr = if (minute < 10) "0$minute" else "$minute"
        
        return "$hourStr:$minuteStr"
    }
    
    fun formatTime12Hour(dateTime: LocalDateTime): String {
        val hour = dateTime.hour
        val minute = dateTime.minute
        
        val period = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        
        val hourStr = if (displayHour < 10) "0$displayHour" else "$displayHour"
        val minuteStr = if (minute < 10) "0$minute" else "$minute"
        
        return "$hourStr:$minuteStr $period"
    }
    
    fun formatDuration(hours: Double): String {
        return when {
            hours < 1 -> "${(hours * 60).toInt()}m"
            hours == hours.toInt().toDouble() -> "${hours.toInt()}h"
            else -> {
                val wholeHours = hours.toInt()
                val minutes = ((hours - wholeHours) * 60).toInt()
                if (minutes == 0) "${wholeHours}h" else "${wholeHours}h ${minutes}m"
            }
        }
    }
    
    fun formatWeekRange(weekStart: LocalDate): String {
        val weekEnd = weekStart.plus(kotlinx.datetime.DatePeriod(days = 6))
        return "${formatDate(weekStart)} - ${formatDate(weekEnd)}"
    }
} 