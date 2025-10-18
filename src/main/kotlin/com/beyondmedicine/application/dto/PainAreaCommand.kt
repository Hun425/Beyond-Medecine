package com.beyondmedicine.application.dto

import com.beyondmedicine.domain.model.PainLocation

/**
 * 통증 부위 커맨드
 * - Application 계층 DTO
 */
data class PainAreaCommand(
    val location: PainLocation,
    val intensity: Int,
    val description: String?
)
