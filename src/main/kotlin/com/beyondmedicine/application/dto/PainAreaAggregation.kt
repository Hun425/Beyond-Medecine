package com.beyondmedicine.application.dto

import com.beyondmedicine.domain.model.PainLocation

/**
 * 통증 부위 집계 결과
 * - JPQL 생성자 표현식용 DTO
 */
data class PainAreaAggregation(
    val location: PainLocation,
    val count: Long,
    val averageIntensity: Double
)
