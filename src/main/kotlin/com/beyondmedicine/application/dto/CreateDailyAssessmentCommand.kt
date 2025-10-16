package com.beyondmedicine.application.dto

import com.beyondmedicine.domain.model.PainLocation
import java.time.LocalDate

/**
 * 일일 검사 생성 커맨드
 * - Application 계층 진입점
 * - 참고: docs/api-design.md
 */
data class CreateDailyAssessmentCommand(
    val prescriptionCode: String,
    val assessmentDate: LocalDate,
    val painScore: Int,
    val stressScore: Int,
    val jawFunctionScore: Int,
    val painAreas: List<PainAreaCommand>
)

/**
 * 통증 부위 커맨드
 */
data class PainAreaCommand(
    val location: PainLocation,
    val intensity: Int,
    val description: String?
)
