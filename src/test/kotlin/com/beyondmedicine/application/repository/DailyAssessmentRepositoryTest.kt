package com.beyondmedicine.application.repository

import com.beyondmedicine.domain.entity.DailyAssessment
import com.beyondmedicine.domain.entity.Prescription
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("DailyAssessmentRepository 테스트")
class DailyAssessmentRepositoryTest {

    @Autowired
    private lateinit var prescriptionRepository: PrescriptionRepository

    @Autowired
    private lateinit var dailyAssessmentRepository: DailyAssessmentRepository

    private lateinit var testPrescription: Prescription

    @BeforeEach
    fun setUp() {
        testPrescription = Prescription(
            code = "TEST1234",
            createdAt = LocalDateTime.of(2025, 9, 25, 10, 0),
            activatedAt = LocalDateTime.of(2025, 10, 1, 9, 0)
        )
        prescriptionRepository.save(testPrescription)
    }

    @Test
    @DisplayName("중복 검사 확인 - 같은 날짜 존재")
    fun existsByPrescriptionAndAssessmentDate_SameDate_ReturnsTrue() {
        // given
        val assessmentDate = LocalDate.of(2025, 10, 15)
        val assessment = DailyAssessment(
            prescription = testPrescription,
            assessmentDate = assessmentDate,
            weekNumber = 3,
            painScore = 7,
            stressScore = 5,
            jawFunctionScore = 6
        )
        dailyAssessmentRepository.save(assessment)

        // when
        val exists = dailyAssessmentRepository.existsByPrescriptionAndAssessmentDate(
            testPrescription,
            assessmentDate
        )

        // then
        assertTrue(exists)
    }

    @Test
    @DisplayName("중복 검사 확인 - 다른 날짜는 false")
    fun existsByPrescriptionAndAssessmentDate_DifferentDate_ReturnsFalse() {
        // given
        val assessment = DailyAssessment(
            prescription = testPrescription,
            assessmentDate = LocalDate.of(2025, 10, 15),
            weekNumber = 3,
            painScore = 7,
            stressScore = 5,
            jawFunctionScore = 6
        )
        dailyAssessmentRepository.save(assessment)

        // when
        val exists = dailyAssessmentRepository.existsByPrescriptionAndAssessmentDate(
            testPrescription,
            LocalDate.of(2025, 10, 16)  // 다른 날짜
        )

        // then
        assertFalse(exists)
    }

    @Test
    @DisplayName("주차별 집계 - 기본 기능 검증")
    fun findWeeklyAggregations_MultipleWeeks_ReturnsAggregatedData() {
        // given: 1주차 2개, 2주차 1개
        createAssessment(testPrescription, 1, LocalDate.of(2025, 10, 1), 8, 7, 5)
        createAssessment(testPrescription, 1, LocalDate.of(2025, 10, 3), 6, 5, 7)
        createAssessment(testPrescription, 2, LocalDate.of(2025, 10, 8), 7, 6, 6)

        // when
        val aggregations = dailyAssessmentRepository.findWeeklyAggregations(
            prescription = testPrescription,
            startWeek = 1,
            endWeek = 2
        )

        // then
        assertEquals(2, aggregations.size)

        // 1주차: (8+6)/2=7.0, (7+5)/2=6.0, (5+7)/2=6.0
        assertEquals(1, aggregations[0].weekNumber)
        assertEquals(7.0, aggregations[0].averagePainScore, 0.01)
        assertEquals(2, aggregations[0].assessmentCount)

        // 2주차
        assertEquals(2, aggregations[1].weekNumber)
        assertEquals(1, aggregations[1].assessmentCount)
    }

    private fun createAssessment(
        prescription: Prescription,
        weekNumber: Int,
        assessmentDate: LocalDate,
        painScore: Int,
        stressScore: Int,
        jawFunctionScore: Int
    ): DailyAssessment {
        val assessment = DailyAssessment(
            prescription = prescription,
            assessmentDate = assessmentDate,
            weekNumber = weekNumber,
            painScore = painScore,
            stressScore = stressScore,
            jawFunctionScore = jawFunctionScore
        )
        return dailyAssessmentRepository.save(assessment)
    }
}
