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

    @Test
    @DisplayName("활성화 후 42일 이내면 ACTIVE 상태다")
    fun calculateStatus_WithinActiveWeeks_ReturnsActive() {
        // given
        val createdAt = LocalDateTime.of(2025, 9, 25, 10, 0)
        val activatedAt = LocalDateTime.of(2025, 10, 1, 9, 0)
        val currentTime = LocalDateTime.of(2025, 10, 15, 10, 0)  // 14일 후

        // when
        val status = PrescriptionCalculator.calculateStatus(
            createdAt, activatedAt, currentTime
        )

        // then
        assertEquals(PrescriptionStatus.ACTIVE, status)
    }

    @Test
    @DisplayName("활성화 후 D+41 23:59:59까지는 ACTIVE 상태다 (경계값)")
    fun calculateStatus_EndOfActiveWeek_ReturnsActive() {
        // given
        val createdAt = LocalDateTime.of(2025, 9, 1, 10, 0)
        val activatedAt = LocalDateTime.of(2025, 10, 1, 9, 0)
        val currentTime = LocalDateTime.of(2025, 11, 11, 23, 59, 59, 999_999_999)  // D+41 끝

        // when
        val status = PrescriptionCalculator.calculateStatus(
            createdAt, activatedAt, currentTime
        )

        // then
        assertEquals(PrescriptionStatus.ACTIVE, status)
    }

    @Test
    @DisplayName("활성화 후 D+42 00:00:00부터는 COMPLETED 상태다 (경계값)")
    fun calculateStatus_StartOfCompletedWeek_ReturnsCompleted() {
        // given
        val createdAt = LocalDateTime.of(2025, 9, 1, 10, 0)
        val activatedAt = LocalDateTime.of(2025, 10, 1, 9, 0)
        val currentTime = LocalDateTime.of(2025, 11, 12, 0, 0, 0)  // D+42 시작

        // when
        val status = PrescriptionCalculator.calculateStatus(
            createdAt, activatedAt, currentTime
        )

        // then
        assertEquals(PrescriptionStatus.COMPLETED, status)
    }
}