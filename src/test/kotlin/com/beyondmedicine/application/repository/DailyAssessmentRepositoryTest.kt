package com.beyondmedicine.application.repository

import com.beyondmedicine.domain.entity.DailyAssessment
import com.beyondmedicine.domain.entity.PainArea
import com.beyondmedicine.domain.entity.Prescription
import com.beyondmedicine.domain.model.PainLocation
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
    @DisplayName("주차별 집계 - 여러 주차 데이터")
    fun findWeeklyAggregations_MultipleWeeks_ReturnsAggregatedData() {
        // given: 1주차 2개, 2주차 2개, 3주차 1개
        dailyAssessmentRepository.save(createAssessment(testPrescription, 1, LocalDate.of(2025, 10, 1), 8, 7, 5))
        dailyAssessmentRepository.save(createAssessment(testPrescription, 1, LocalDate.of(2025, 10, 3), 6, 5, 7))
        dailyAssessmentRepository.save(createAssessment(testPrescription, 2, LocalDate.of(2025, 10, 8), 7, 6, 6))
        dailyAssessmentRepository.save(createAssessment(testPrescription, 2, LocalDate.of(2025, 10, 10), 5, 4, 8))
        dailyAssessmentRepository.save(createAssessment(testPrescription, 3, LocalDate.of(2025, 10, 15), 4, 3, 9))

        // when
        val aggregations = dailyAssessmentRepository.findWeeklyAggregations(
            prescription = testPrescription,
            startWeek = 1,
            endWeek = 3
        )

        // then
        assertEquals(3, aggregations.size)

        // 1주차: (8+6)/2=7.0, (7+5)/2=6.0, (5+7)/2=6.0
        assertEquals(1, aggregations[0].weekNumber)
        assertEquals(7.0, aggregations[0].averagePainScore, 0.01)
        assertEquals(6.0, aggregations[0].averageStressScore, 0.01)
        assertEquals(6.0, aggregations[0].averageJawFunctionScore, 0.01)
        assertEquals(2, aggregations[0].assessmentCount)

        // 2주차: (7+5)/2=6.0, (6+4)/2=5.0, (6+8)/2=7.0
        assertEquals(2, aggregations[1].weekNumber)
        assertEquals(6.0, aggregations[1].averagePainScore, 0.01)
        assertEquals(5.0, aggregations[1].averageStressScore, 0.01)
        assertEquals(7.0, aggregations[1].averageJawFunctionScore, 0.01)
        assertEquals(2, aggregations[1].assessmentCount)

        // 3주차: 4.0, 3.0, 9.0
        assertEquals(3, aggregations[2].weekNumber)
        assertEquals(4.0, aggregations[2].averagePainScore, 0.01)
        assertEquals(3.0, aggregations[2].averageStressScore, 0.01)
        assertEquals(9.0, aggregations[2].averageJawFunctionScore, 0.01)
        assertEquals(1, aggregations[2].assessmentCount)
    }

    @Test
    @DisplayName("주차별 집계 - 데이터 없는 주차는 제외")
    fun findWeeklyAggregations_MissingWeeks_OnlyReturnsExistingWeeks() {
        // given: 1주차와 3주차만 데이터 (2주차 없음)
        dailyAssessmentRepository.save(createAssessment(testPrescription, 1, LocalDate.of(2025, 10, 1), 8, 7, 5))
        dailyAssessmentRepository.save(createAssessment(testPrescription, 3, LocalDate.of(2025, 10, 15), 4, 3, 9))

        // when
        val aggregations = dailyAssessmentRepository.findWeeklyAggregations(
            prescription = testPrescription,
            startWeek = 1,
            endWeek = 6
        )

        // then
        assertEquals(2, aggregations.size)  // 2주차는 제외
        assertEquals(1, aggregations[0].weekNumber)
        assertEquals(3, aggregations[1].weekNumber)
    }

    @Test
    @DisplayName("Top 통증 부위 집계 - 횟수 기준 내림차순")
    fun findTopPainAreas_OrderedByCount_ReturnsTopAreas() {
        // given
        val assessment1 = createAssessment(testPrescription, 1, LocalDate.of(2025, 10, 1), 8, 7, 5)
        val assessment2 = createAssessment(testPrescription, 1, LocalDate.of(2025, 10, 2), 7, 6, 6)
        val assessment3 = createAssessment(testPrescription, 2, LocalDate.of(2025, 10, 8), 6, 5, 7)

        // LEFT_JAW: 3회 (평균 강도: (8+7+6)/3 = 7.0)
        addPainArea(assessment1, PainLocation.LEFT_JAW, 8)
        addPainArea(assessment2, PainLocation.LEFT_JAW, 7)
        addPainArea(assessment3, PainLocation.LEFT_JAW, 6)

        // RIGHT_TEMPLE: 2회 (평균 강도: (6+5)/2 = 5.5)
        addPainArea(assessment1, PainLocation.RIGHT_TEMPLE, 6)
        addPainArea(assessment2, PainLocation.RIGHT_TEMPLE, 5)

        // NECK: 1회 (평균 강도: 9.0)
        addPainArea(assessment3, PainLocation.NECK, 9)

        dailyAssessmentRepository.saveAll(listOf(assessment1, assessment2, assessment3))

        // when
        val topPainAreas = dailyAssessmentRepository.findTopPainAreas(
            prescription = testPrescription,
            startWeek = 1,
            endWeek = 6
        )

        // then
        assertEquals(3, topPainAreas.size)

        // 1위: LEFT_JAW (3회)
        assertEquals(PainLocation.LEFT_JAW, topPainAreas[0].location)
        assertEquals(3, topPainAreas[0].count)
        assertEquals(7.0, topPainAreas[0].averageIntensity, 0.1)

        // 2위: RIGHT_TEMPLE (2회)
        assertEquals(PainLocation.RIGHT_TEMPLE, topPainAreas[1].location)
        assertEquals(2, topPainAreas[1].count)
        assertEquals(5.5, topPainAreas[1].averageIntensity, 0.1)

        // 3위: NECK (1회)
        assertEquals(PainLocation.NECK, topPainAreas[2].location)
        assertEquals(1, topPainAreas[2].count)
        assertEquals(9.0, topPainAreas[2].averageIntensity, 0.1)
    }

    // 헬퍼 메서드
    private fun createAssessment(
        prescription: Prescription,
        weekNumber: Int,
        assessmentDate: LocalDate,
        painScore: Int,
        stressScore: Int,
        jawFunctionScore: Int
    ): DailyAssessment {
        return DailyAssessment(
            prescription = prescription,
            assessmentDate = assessmentDate,
            weekNumber = weekNumber,
            painScore = painScore,
            stressScore = stressScore,
            jawFunctionScore = jawFunctionScore
        )
    }

    private fun addPainArea(
        assessment: DailyAssessment,
        location: PainLocation,
        intensity: Int
    ) {
        val painArea = PainArea(
            dailyAssessment = assessment,
            location = location,
            intensity = intensity,
            description = null
        )
        assessment.addPainArea(painArea)
    }
}
