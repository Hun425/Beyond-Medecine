package com.beyondmedicine.domain.calculator

import com.beyondmedicine.domain.model.PrescriptionStatus
import java.time.LocalDateTime

object PrescriptionCalculator {

    private const val ACTIVE_PERIOD_DAYS = 42L

    fun calculateStatus(
        createdAt: LocalDateTime,
        activatedAt: LocalDateTime?,
        currentTime: LocalDateTime
    ): PrescriptionStatus {
        if (activatedAt != null) {
            val endOfActiveWeek = activatedAt
                .plusDays(ACTIVE_PERIOD_DAYS - 1)
                .withHour(23)
                .withMinute(59)
                .withSecond(59)
                .withNano(999_999_999)

            return if (currentTime.isAfter(endOfActiveWeek)) {
                PrescriptionStatus.COMPLETED
            } else {
                PrescriptionStatus.ACTIVE
            }
        }

        return PrescriptionStatus.PENDING
    }
}