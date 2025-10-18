package com.beyondmedicine.presentation.controller

import com.beyondmedicine.application.dto.WeeklyTrendQuery
import com.beyondmedicine.application.service.AssessmentAnalysisService
import com.beyondmedicine.application.service.AssessmentService
import com.beyondmedicine.presentation.dto.CreateDailyAssessmentRequest
import com.beyondmedicine.presentation.dto.CreateDailyAssessmentResponse
import com.beyondmedicine.presentation.dto.WeeklyTrendResponse
import com.beyondmedicine.presentation.mapper.AssessmentDtoMapper
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 일일 검사 및 추이 분석 API Controller
 * - 과제 PDF 5-9페이지
 * - Presentation 계층: DTO 변환 및 HTTP 처리
 */
@Tag(name = "일일 검사 API", description = "턱관절 질환 디지털 치료제 일일 검사 및 추이 분석 API")
@RestController
@RequestMapping("/api/v1/assessments")
class AssessmentController(
    private val assessmentService: AssessmentService,
    private val assessmentAnalysisService: AssessmentAnalysisService,
    private val dtoMapper: AssessmentDtoMapper
) {

    /**
     * 과제 2: 일일 검사 결과 등록 API
     * POST /api/v1/assessments/daily
     */
    @Operation(
        summary = "일일 검사 등록",
        description = "특정 처방의 일일 검사 결과를 등록합니다. 하루 1회만 등록 가능하며, ACTIVE 상태의 처방만 가능합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "201", description = "검사 등록 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청 (검증 실패, 유효하지 않은 처방 상태 등)"),
            ApiResponse(responseCode = "404", description = "처방을 찾을 수 없음"),
            ApiResponse(responseCode = "409", description = "중복 검사 (하루 1회 제한)")
        ]
    )
    @PostMapping("/daily")
    fun createDailyAssessment(
        @Valid @RequestBody request: CreateDailyAssessmentRequest
    ): ResponseEntity<CreateDailyAssessmentResponse> {
        // 1. Request → Command 변환
        val command = dtoMapper.toCommand(request)

        // 2. Service 호출
        val result = assessmentService.createDailyAssessment(command)

        // 3. Result → Response 변환
        val response = dtoMapper.toResponse(result)

        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /**
     * 과제 3: 주차별 검사 추이 분석 API
     * GET /api/v1/assessments/weekly-trend
     */
    @Operation(
        summary = "주차별 추이 분석",
        description = "특정 처방의 주차별 검사 추이를 분석합니다. 주차별 평균 점수와 전주 대비 변화율을 제공합니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "추이 분석 성공"),
            ApiResponse(responseCode = "400", description = "잘못된 요청 (주차 범위 오류 등)"),
            ApiResponse(responseCode = "404", description = "처방을 찾을 수 없음")
        ]
    )
    @GetMapping("/weekly-trend")
    fun getWeeklyTrend(
        @Parameter(description = "처방 코드 (영대문자 4자 + 숫자 4자)", example = "ABCD1234")
        @RequestParam(name = "prescriptionCode") prescriptionCode: String,

        @Parameter(description = "시작 주차 (1~6, 기본값: 1)", example = "1")
        @RequestParam(name = "startWeek", required = false, defaultValue = "1") startWeek: Int,

        @Parameter(description = "종료 주차 (1~6, 미입력 시 현재 주차)", example = "6")
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
