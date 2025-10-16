package com.beyondmedicine.presentation.controller

import com.beyondmedicine.application.dto.WeeklyTrendQuery
import com.beyondmedicine.application.service.AssessmentAnalysisService
import com.beyondmedicine.application.service.AssessmentService
import com.beyondmedicine.presentation.dto.CreateDailyAssessmentRequest
import com.beyondmedicine.presentation.dto.CreateDailyAssessmentResponse
import com.beyondmedicine.presentation.dto.WeeklyTrendResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 일일 검사 및 추이 분석 API Controller
 * - 과제 PDF 5-9페이지
 */
@RestController
@RequestMapping("/api/v1/assessments")
class AssessmentController(
    private val assessmentService: AssessmentService,
    private val assessmentAnalysisService: AssessmentAnalysisService
) {

    /**
     * 과제 2: 일일 검사 결과 등록 API
     * POST /api/v1/assessments/daily
     */
    @PostMapping("/daily")
    fun createDailyAssessment(
        @Valid @RequestBody request: CreateDailyAssessmentRequest
    ): ResponseEntity<CreateDailyAssessmentResponse> {
        val response = assessmentService.createDailyAssessment(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /**
     * 과제 3: 주차별 검사 추이 분석 API
     * GET /api/v1/assessments/weekly-trend
     */
    @GetMapping("/weekly-trend")
    fun getWeeklyTrend(
        @RequestParam(name = "prescriptionCode") prescriptionCode: String,
        @RequestParam(name = "startWeek", required = false, defaultValue = "1") startWeek: Int,
        @RequestParam(name = "endWeek", required = false) endWeek: Int?
    ): ResponseEntity<WeeklyTrendResponse> {
        // endWeek가 null이면 처방의 현재 주차를 계산
        val finalEndWeek = endWeek ?: run {
            // 처방의 현재 주차 계산 (최대 6주차)
            assessmentService.getCurrentWeek(prescriptionCode) ?: 6
        }

        val query = WeeklyTrendQuery(
            prescriptionCode = prescriptionCode,
            startWeek = startWeek,
            endWeek = finalEndWeek
        )

        val trendData = assessmentAnalysisService.analyzeWeeklyTrend(query)

        // Application DTO를 Presentation DTO로 변환
        val response = WeeklyTrendResponse.from(trendData)

        return ResponseEntity.ok(response)
    }
}
