package com.beyondmedicine.domain.calculator

import com.beyondmedicine.domain.model.PrescriptionStatus
import java.time.LocalDateTime

object PrescriptionCalculator {

    fun calculateStatus(
        createdAt: LocalDateTime,
        activatedAt: LocalDateTime?,
        currentTime: LocalDateTime
    ): PrescriptionStatus {
        // 최소 구현: 항상 PENDING 반환
        return PrescriptionStatus.PENDING
    }
}