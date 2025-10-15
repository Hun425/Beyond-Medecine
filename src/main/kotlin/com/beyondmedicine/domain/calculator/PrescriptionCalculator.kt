package com.beyondmedicine.domain.calculator

import com.beyondmedicine.domain.model.PrescriptionStatus
import java.time.LocalDateTime

object PrescriptionCalculator {

    private const val ACTIVE_PERIOD_DAYS = 42L
    private const val EXPIRATION_WEEKS = 6L

    fun calculateStatus(
        createdAt: LocalDateTime,
        activatedAt: LocalDateTime?,
        currentTime: LocalDateTime
    ): PrescriptionStatus = when {
        activatedAt != null -> calculateActivatedStatus(activatedAt, currentTime)
        else -> calculateNotActivatedStatus(createdAt, currentTime)
    }

    private fun calculateActivatedStatus(
        activatedAt: LocalDateTime,
        currentTime: LocalDateTime
    ): PrescriptionStatus {
        val endOfActiveWeek = calculateEndOfActiveWeek(activatedAt)

        return if (currentTime.isAfter(endOfActiveWeek)) {
            PrescriptionStatus.COMPLETED
        } else {
            PrescriptionStatus.ACTIVE
        }
    }

    private fun calculateEndOfActiveWeek(activatedAt: LocalDateTime): LocalDateTime {
        return activatedAt
            .plusDays(ACTIVE_PERIOD_DAYS - 1)
            .withHour(23)
            .withMinute(59)
            .withSecond(59)
            .withNano(999_999_999)
    }

    private fun calculateNotActivatedStatus(
        createdAt: LocalDateTime,
        currentTime: LocalDateTime
    ): PrescriptionStatus {
        val expirationTime = createdAt.plusWeeks(EXPIRATION_WEEKS)
        return if (currentTime.isAfter(expirationTime)) {
            PrescriptionStatus.EXPIRED
        } else {
            PrescriptionStatus.PENDING
        }
    }
}