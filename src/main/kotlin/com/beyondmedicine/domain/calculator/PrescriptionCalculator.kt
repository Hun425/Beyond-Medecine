package com.beyondmedicine.domain.calculator

import com.beyondmedicine.domain.model.PrescriptionStatus
import java.time.LocalDateTime

object PrescriptionCalculator {

    fun calculateStatus(
        createdAt: LocalDateTime,
        activatedAt: LocalDateTime?,
        currentTime: LocalDateTime
    ): PrescriptionStatus {
        // activatedAt이 있으면 ACTIVE, 없으면 PENDING
        return if (activatedAt != null) {
            PrescriptionStatus.ACTIVE
        } else {
            PrescriptionStatus.PENDING
        }
    }
}