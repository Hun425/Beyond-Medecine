package com.beyondmedicine.application.service

import com.beyondmedicine.application.dto.*
import com.beyondmedicine.application.exception.PrescriptionNotFoundException
import com.beyondmedicine.application.repository.DailyAssessmentRepository
import com.beyondmedicine.application.repository.PrescriptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 검사 분석 서비스
 * - 참고: docs/service-layer-design.md
 * - 과제 PDF 7-8페이지
 */
@Service
@Transactional(readOnly = true)
class AssessmentAnalysisService(
    private val prescriptionRepository: PrescriptionRepository,
    private val dailyAssessmentRepository: DailyAssessmentRepository
) {

    /**
     * 주차별 추이 분석
     */
    fun analyzeWeeklyTrend(query: WeeklyTrendQuery): WeeklyTrendData {
        // 1. 처방 조회
        val prescription = prescriptionRepository.findByCode(query.prescriptionCode)
            ?: throw PrescriptionNotFoundException(
                "처방을 찾을 수 없습니다. 처방 코드: ${query.prescriptionCode}"
            )

        // 2. 주차 범위 검증
        validateWeekRange(query.startWeek, query.endWeek)

        // 3. 주차별 집계 데이터 조회
        val weeklyAggregations = dailyAssessmentRepository.findWeeklyAggregations(
            prescription = prescription,
            startWeek = query.startWeek,
            endWeek = query.endWeek
        )

        // 4. 데이터 없는 경우 빈 응답 반환
        if (weeklyAggregations.isEmpty()) {
            return WeeklyTrendData(
                prescriptionCode = prescription.code,
                startWeek = query.startWeek,
                endWeek = query.endWeek,
                weeklyTrends = emptyList(),
                topPainAreas = emptyList()
            )
        }

        // 5. 주차별 데이터 변환
        val weeklyTrends = weeklyAggregations.map { agg ->
            WeeklyData(
                weekNumber = agg.weekNumber,
                averagePainScore = agg.averagePainScore,
                averageStressScore = agg.averageStressScore,
                averageJawFunctionScore = agg.averageJawFunctionScore,
                assessmentCount = agg.assessmentCount
            )
        }

        // 6. Top 3 통증 부위 집계
        val topPainAreasAgg = dailyAssessmentRepository.findTopPainAreas(
            prescription = prescription,
            startWeek = query.startWeek,
            endWeek = query.endWeek
        )

        val topPainAreas = topPainAreasAgg.take(3).map { agg ->
            TopPainAreaData(
                location = agg.location,
                occurrenceCount = agg.count,
                averageIntensity = agg.averageIntensity
            )
        }

        return WeeklyTrendData(
            prescriptionCode = prescription.code,
            startWeek = query.startWeek,
            endWeek = query.endWeek,
            weeklyTrends = weeklyTrends,
            topPainAreas = topPainAreas
        )
    }

    /**
     * 주차 범위 검증
     */
    private fun validateWeekRange(startWeek: Int, endWeek: Int) {
        require(startWeek in 1..6) {
            "시작 주차는 1~6 사이여야 합니다. 입력값: $startWeek"
        }
        require(endWeek in 1..6) {
            "종료 주차는 1~6 사이여야 합니다. 입력값: $endWeek"
        }
        require(startWeek <= endWeek) {
            "시작 주차는 종료 주차보다 작거나 같아야 합니다. " +
            "시작: $startWeek, 종료: $endWeek"
        }
    }
}
