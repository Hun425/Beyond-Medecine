package com.beyondmedicine.presentation.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.*
import java.time.LocalDate

/**
 * 일일 검사 등록 요청 DTO
 * - 과제 PDF 5-6페이지
 */
data class CreateDailyAssessmentRequest(
    @field:NotBlank(message = "처방 코드는 필수입니다")
    @field:Pattern(
        regexp = "^([A-Z]{4}[0-9]{4}|[0-9]{4}[A-Z]{4})$",
        message = "처방 코드는 영대문자 4글자 + 숫자 4글자 조합이어야 합니다"
    )
    val prescriptionCode: String,

    @field:NotNull(message = "검사 일자는 필수입니다")
    val assessmentDate: LocalDate,

    @field:NotNull(message = "통증 점수는 필수입니다")
    @field:Min(value = 0, message = "통증 점수는 0 이상이어야 합니다")
    @field:Max(value = 10, message = "통증 점수는 10 이하여야 합니다")
    val painScore: Int,

    @field:NotNull(message = "스트레스 점수는 필수입니다")
    @field:Min(value = 0, message = "스트레스 점수는 0 이상이어야 합니다")
    @field:Max(value = 10, message = "스트레스 점수는 10 이하여야 합니다")
    val stressScore: Int,

    @field:NotNull(message = "턱 기능 점수는 필수입니다")
    @field:Min(value = 0, message = "턱 기능 점수는 0 이상이어야 합니다")
    @field:Max(value = 10, message = "턱 기능 점수는 10 이하여야 합니다")
    val jawFunctionScore: Int,

    @field:Valid
    @field:Size(max = 6, message = "통증 부위는 최대 6개까지 등록 가능합니다")
    val painAreas: List<PainAreaRequest> = emptyList()
)

data class PainAreaRequest(
    @field:NotBlank(message = "통증 부위는 필수입니다")
    @field:Pattern(
        regexp = "^(LEFT_JAW|RIGHT_JAW|LEFT_TEMPLE|RIGHT_TEMPLE|NECK|CHIN)$",
        message = "유효하지 않은 통증 부위입니다"
    )
    val location: String,  // "LEFT_JAW", "RIGHT_TEMPLE", etc.

    @field:NotNull(message = "통증 강도는 필수입니다")
    @field:Min(value = 0, message = "통증 강도는 0 이상이어야 합니다")
    @field:Max(value = 10, message = "통증 강도는 10 이하여야 합니다")
    val intensity: Int,

    val description: String? = null
)
