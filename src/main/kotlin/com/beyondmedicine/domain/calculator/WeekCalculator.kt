package com.beyondmedicine.domain.calculator

import java.time.LocalDate
import java.time.temporal.ChronoUnit

object WeekCalculator {

    private const val DAYS_PER_WEEK = 7
    private const val ACTIVE_PERIOD_DAYS = 42L

    fun calculateWeekNumber(
        activationDate: LocalDate,
        assessmentDate: LocalDate
    ): Int? {
        val daysSinceActivation = ChronoUnit.DAYS.between(activationDate, assessmentDate)

        return when {
            daysSinceActivation < 0 -> null
            daysSinceActivation >= ACTIVE_PERIOD_DAYS -> null
            else -> (daysSinceActivation / DAYS_PER_WEEK).toInt() + 1
        }
    }
}
