package com.beyondmedicine.domain.calculator

import com.beyondmedicine.domain.model.PrescriptionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import java.time.LocalDateTime

@DisplayName("PrescriptionCalculator 테스트")
class PrescriptionCalculatorTest {

    @Test
    @DisplayName("생성 후 활성화하지 않으면 PENDING 상태다")
    fun calculateStatus_NotActivated_ReturnsPending() {
        // given
        val createdAt = LocalDateTime.of(2025, 10, 1, 10, 0)
        val activatedAt = null
        val currentTime = LocalDateTime.of(2025, 10, 2, 10, 0)

        // when
        val status = PrescriptionCalculator.calculateStatus(
            createdAt, activatedAt, currentTime
        )

        // then
        assertEquals(PrescriptionStatus.PENDING, status)
    }
}