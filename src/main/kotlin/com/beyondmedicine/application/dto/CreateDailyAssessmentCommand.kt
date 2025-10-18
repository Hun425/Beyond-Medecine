package com.beyondmedicine.application.dto

import java.time.LocalDate

/**
 * 일일 검사 생성 커맨드
 * - Application 계층 진입점
 * - Presentation 계층과 분리된 도메인 중심 DTO
 * - 참고: docs/api-design.md, docs/fp-hybrid-design.md
 */
data class CreateDailyAssessmentCommand(
    val prescriptionCode: String,
    val assessmentDate: LocalDate,
    val painScore: Int,
    val stressScore: Int,
    val jawFunctionScore: Int,
    val painAreas: List<PainAreaCommand>
)
