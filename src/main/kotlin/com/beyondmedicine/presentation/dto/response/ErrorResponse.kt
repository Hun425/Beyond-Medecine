package com.beyondmedicine.presentation.dto.response

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

/**
 * 표준 에러 응답
 */
data class ErrorResponse(
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val timestamp: LocalDateTime,
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val validationErrors: List<ValidationError>? = null
)

data class ValidationError(
    val field: String,
    val rejectedValue: Any?,
    val message: String
)
