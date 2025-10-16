package com.beyondmedicine.application.dto

import java.time.LocalDate

/**
 * 일일 검사 생성 결과
 * - Service 반환 값
 */
data class DailyAssessmentResult(
    val assessmentId: Long,
    val prescriptionCode: String,
    val weekNumber: Int,
    val assessmentDate: LocalDate
)
