package com.beyondmedicine.application.dto

/**
 * 주차별 집계 결과
 * - JPQL 생성자 표현식용 DTO
 */
data class WeeklyAggregation(
    val weekNumber: Int,
    val averagePainScore: Double,
    val averageStressScore: Double,
    val averageJawFunctionScore: Double,
    val assessmentCount: Long
)
