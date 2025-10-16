package com.beyondmedicine.domain.entity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("DailyAssessment Entity 테스트")
class DailyAssessmentTest {

    @Test
    @DisplayName("일일 검사 생성 - 모든 필드 설정")
    fun create_DailyAssessment_AllFieldsSet() {
        // given
        val prescription = createTestPrescription()
        val assessmentDate = LocalDate.of(2025, 10, 15)

        // when
        val assessment = DailyAssessment(
            prescription = prescription,
            assessmentDate = assessmentDate,
            weekNumber = 3,
            painScore = 7,
            stressScore = 5,
            jawFunctionScore = 6
        )

        // then
        assertEquals(prescription, assessment.prescription)
        assertEquals(assessmentDate, assessment.assessmentDate)
        assertEquals(3, assessment.weekNumber)
        assertEquals(7, assessment.painScore)
        assertEquals(5, assessment.stressScore)
        assertEquals(6, assessment.jawFunctionScore)
        assertTrue(assessment.painAreas.isEmpty())
    }

    @Test
    @DisplayName("점수 범위 검증 - painScore 0~10")
    fun init_InvalidPainScore_ThrowsException() {
        // given
        val prescription = createTestPrescription()

        // when & then: -1 (범위 밖)
        assertThrows(IllegalArgumentException::class.java) {
            DailyAssessment(
                prescription = prescription,
                assessmentDate = LocalDate.now(),
                weekNumber = 1,
                painScore = -1,
                stressScore = 5,
                jawFunctionScore = 6
            )
        }

        // when & then: 11 (범위 밖)
        assertThrows(IllegalArgumentException::class.java) {
            DailyAssessment(
                prescription = prescription,
                assessmentDate = LocalDate.now(),
                weekNumber = 1,
                painScore = 11,
                stressScore = 5,
                jawFunctionScore = 6
            )
        }
    }

    @Test
    @DisplayName("점수 범위 검증 - stressScore 0~10")
    fun init_InvalidStressScore_ThrowsException() {
        // given
        val prescription = createTestPrescription()

        // when & then
        assertThrows(IllegalArgumentException::class.java) {
            DailyAssessment(
                prescription = prescription,
                assessmentDate = LocalDate.now(),
                weekNumber = 1,
                painScore = 5,
                stressScore = 15,  // 범위 밖
                jawFunctionScore = 6
            )
        }
    }

    @Test
    @DisplayName("점수 범위 검증 - jawFunctionScore 0~10")
    fun init_InvalidJawFunctionScore_ThrowsException() {
        // given
        val prescription = createTestPrescription()

        // when & then
        assertThrows(IllegalArgumentException::class.java) {
            DailyAssessment(
                prescription = prescription,
                assessmentDate = LocalDate.now(),
                weekNumber = 1,
                painScore = 5,
                stressScore = 5,
                jawFunctionScore = -5  // 범위 밖
            )
        }
    }

    @Test
    @DisplayName("주차 번호 검증 - 1~6")
    fun init_InvalidWeekNumber_ThrowsException() {
        // given
        val prescription = createTestPrescription()

        // when & then: 0 (범위 밖)
        assertThrows(IllegalArgumentException::class.java) {
            DailyAssessment(
                prescription = prescription,
                assessmentDate = LocalDate.now(),
                weekNumber = 0,
                painScore = 5,
                stressScore = 5,
                jawFunctionScore = 6
            )
        }

        // when & then: 7 (범위 밖)
        assertThrows(IllegalArgumentException::class.java) {
            DailyAssessment(
                prescription = prescription,
                assessmentDate = LocalDate.now(),
                weekNumber = 7,
                painScore = 5,
                stressScore = 5,
                jawFunctionScore = 6
            )
        }
    }

    private fun createTestPrescription(): Prescription {
        return Prescription(
            code = "TEST1234",
            createdAt = LocalDateTime.of(2025, 9, 25, 10, 0),
            activatedAt = LocalDateTime.of(2025, 10, 1, 9, 0)
        )
    }
}
