package com.beyondmedicine.presentation.dto

/**
 * 일일 검사 등록 응답 DTO
 * - 과제 PDF 6페이지
 */
data class CreateDailyAssessmentResponse(
    val assessmentId: String,
    val prescriptionCode: String,
    val weekNumber: Int
)
