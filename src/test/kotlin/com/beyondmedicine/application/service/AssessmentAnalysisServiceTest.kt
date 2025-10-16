package com.beyondmedicine.application.service

import com.beyondmedicine.application.dto.*
import com.beyondmedicine.application.exception.PrescriptionNotFoundException
import com.beyondmedicine.application.repository.DailyAssessmentRepository
import com.beyondmedicine.application.repository.PrescriptionRepository
import com.beyondmedicine.domain.entity.Prescription
import com.beyondmedicine.domain.model.PainLocation
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("AssessmentAnalysisService 단위 테스트")
class AssessmentAnalysisServiceTest {

    @Mock
    private lateinit var prescriptionRepository: PrescriptionRepository

    @Mock
    private lateinit var dailyAssessmentRepository: DailyAssessmentRepository

    @InjectMocks
    private lateinit var assessmentAnalysisService: AssessmentAnalysisService

    @Test
    @DisplayName("주차별 추이 분석 - 정상 케이스")
    fun analyzeWeeklyTrend_ValidQuery_ReturnsData() {
        // given
        val prescription = createTestPrescription()
        val query = WeeklyTrendQuery(
            prescriptionCode = "ABCD1234",
            startWeek = 1,
            endWeek = 3
        )

        val weeklyAggregations = listOf(
            WeeklyAggregation(1, 7.0, 6.0, 5.0, 2),
            WeeklyAggregation(2, 6.0, 5.0, 6.0, 2),
            WeeklyAggregation(3, 5.0, 4.0, 7.0, 1)
        )

        val topPainAreas = listOf(
            PainAreaAggregation(PainLocation.LEFT_JAW, 3, 7.0),
            PainAreaAggregation(PainLocation.RIGHT_TEMPLE, 2, 6.0)
        )

        whenever(prescriptionRepository.findByCode(query.prescriptionCode))
            .thenReturn(prescription)
        whenever(dailyAssessmentRepository.findWeeklyAggregations(any(), any(), any()))
            .thenReturn(weeklyAggregations)
        whenever(dailyAssessmentRepository.findTopPainAreas(any(), any(), any()))
            .thenReturn(topPainAreas)

        // when
        val result = assessmentAnalysisService.analyzeWeeklyTrend(query)

        // then
        assertNotNull(result)
        assertEquals("ABCD1234", result.prescriptionCode)
        assertEquals(1, result.startWeek)
        assertEquals(3, result.endWeek)
        assertEquals(3, result.weeklyTrends.size)
        assertEquals(2, result.topPainAreas.size)

        // 주차별 데이터 검증
        assertEquals(1, result.weeklyTrends[0].weekNumber)
        assertEquals(7.0, result.weeklyTrends[0].averagePainScore)

        // Top 통증 부위 검증
        assertEquals(PainLocation.LEFT_JAW, result.topPainAreas[0].location)
        assertEquals(3L, result.topPainAreas[0].occurrenceCount)
    }

    @Test
    @DisplayName("처방이 없으면 PrescriptionNotFoundException")
    fun analyzeWeeklyTrend_PrescriptionNotFound_ThrowsException() {
        // given
        val query = WeeklyTrendQuery(
            prescriptionCode = "NOTEXIST",
            startWeek = 1,
            endWeek = 6
        )

        whenever(prescriptionRepository.findByCode(query.prescriptionCode))
            .thenReturn(null)

        // when & then
        assertThrows(PrescriptionNotFoundException::class.java) {
            assessmentAnalysisService.analyzeWeeklyTrend(query)
        }
    }

    @Test
    @DisplayName("데이터가 없으면 빈 응답 반환")
    fun analyzeWeeklyTrend_NoData_ReturnsEmptyData() {
        // given
        val prescription = createTestPrescription()
        val query = WeeklyTrendQuery(
            prescriptionCode = "ABCD1234",
            startWeek = 1,
            endWeek = 6
        )

        whenever(prescriptionRepository.findByCode(query.prescriptionCode))
            .thenReturn(prescription)
        whenever(dailyAssessmentRepository.findWeeklyAggregations(any(), any(), any()))
            .thenReturn(emptyList())

        // when
        val result = assessmentAnalysisService.analyzeWeeklyTrend(query)

        // then
        assertTrue(result.weeklyTrends.isEmpty())
        assertTrue(result.topPainAreas.isEmpty())
    }

    @Test
    @DisplayName("주차 범위 검증 - startWeek > endWeek")
    fun analyzeWeeklyTrend_InvalidWeekRange_ThrowsException() {
        // given
        val prescription = createTestPrescription()
        val query = WeeklyTrendQuery(
            prescriptionCode = "ABCD1234",
            startWeek = 5,
            endWeek = 2  // 잘못된 범위
        )

        whenever(prescriptionRepository.findByCode(query.prescriptionCode))
            .thenReturn(prescription)

        // when & then
        assertThrows(IllegalArgumentException::class.java) {
            assessmentAnalysisService.analyzeWeeklyTrend(query)
        }
    }

    private fun createTestPrescription(): Prescription {
        return Prescription(
            code = "ABCD1234",
            createdAt = LocalDateTime.of(2025, 9, 25, 10, 0),
            activatedAt = LocalDateTime.of(2025, 10, 1, 9, 0)
        )
    }
}
