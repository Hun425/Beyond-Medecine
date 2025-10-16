package com.beyondmedicine.domain.entity

import com.beyondmedicine.domain.model.PrescriptionStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("Prescription Entity 테스트")
class PrescriptionTest {

    @Test
    @DisplayName("처방 생성 시 PENDING 상태")
    fun create_NewPrescription_StatusIsPending() {
        // given
        val createdAt = LocalDateTime.of(2025, 10, 1, 10, 0)

        // when
        val prescription = Prescription(
            code = "ABCD1234",
            createdAt = createdAt,
            activatedAt = null
        )

        // then
        assertEquals(PrescriptionStatus.PENDING, prescription.getStatus(createdAt))
    }

    @Test
    @DisplayName("처방 활성화 후 ACTIVE 상태")
    fun activate_Prescription_StatusIsActive() {
        // given
        val createdAt = LocalDateTime.of(2025, 10, 1, 10, 0)
        val activatedAt = LocalDateTime.of(2025, 10, 5, 9, 0)

        val prescription = Prescription(
            code = "ABCD1234",
            createdAt = createdAt,
            activatedAt = null
        )

        // when
        prescription.activate(activatedAt)

        // then
        assertEquals(activatedAt, prescription.activatedAt)
        assertEquals(PrescriptionStatus.ACTIVE, prescription.getStatus(activatedAt))
    }

    @Test
    @DisplayName("주차 번호 계산 - 활성화 후 가능")
    fun calculateWeekNumber_AfterActivation_ReturnsWeekNumber() {
        // given
        val prescription = Prescription(
            code = "ABCD1234",
            createdAt = LocalDateTime.of(2025, 9, 25, 10, 0),
            activatedAt = LocalDateTime.of(2025, 10, 1, 9, 0)
        )

        // when
        val weekNumber = prescription.calculateWeekNumber(LocalDate.of(2025, 10, 15))

        // then
        assertEquals(3, weekNumber)  // (14일 / 7) + 1 = 3주차
    }

    @Test
    @DisplayName("주차 번호 계산 - 활성화 전이면 null")
    fun calculateWeekNumber_BeforeActivation_ReturnsNull() {
        // given
        val prescription = Prescription(
            code = "ABCD1234",
            createdAt = LocalDateTime.of(2025, 10, 1, 10, 0),
            activatedAt = null
        )

        // when
        val weekNumber = prescription.calculateWeekNumber(LocalDate.of(2025, 10, 15))

        // then
        assertNull(weekNumber)
    }

    @Test
    @DisplayName("검사 수행 가능 여부 - ACTIVE 상태만 true")
    fun canPerformAssessment_OnlyActiveStatus_ReturnsTrue() {
        // given: PENDING 상태
        val pendingPrescription = Prescription(
            code = "PEND1234",
            createdAt = LocalDateTime.of(2025, 10, 1, 10, 0),
            activatedAt = null
        )

        // when & then
        assertFalse(pendingPrescription.canPerformAssessment(LocalDateTime.of(2025, 10, 2, 10, 0)))

        // given: ACTIVE 상태
        val activePrescription = Prescription(
            code = "ACTV1234",
            createdAt = LocalDateTime.of(2025, 9, 25, 10, 0),
            activatedAt = LocalDateTime.of(2025, 10, 1, 9, 0)
        )

        // when & then
        assertTrue(activePrescription.canPerformAssessment(LocalDateTime.of(2025, 10, 15, 10, 0)))
    }
}
