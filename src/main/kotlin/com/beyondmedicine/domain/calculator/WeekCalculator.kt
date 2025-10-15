package com.beyondmedicine.domain.calculator

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object WeekCalculator {

    private const val DAYS_PER_WEEK = 7

    fun calculateWeekNumber(
        activationDate: LocalDate,
        assessmentDate: LocalDate
    ): Int? {
        val daysSinceActivation = ChronoUnit.DAYS.between(activationDate, assessmentDate)
        return (daysSinceActivation / DAYS_PER_WEEK).toInt() + 1
    }
}
