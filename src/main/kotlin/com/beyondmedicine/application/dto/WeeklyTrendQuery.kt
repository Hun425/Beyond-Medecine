package com.beyondmedicine.application.dto

/**
 * 주차별 추이 분석 쿼리
 */
data class WeeklyTrendQuery(
    val prescriptionCode: String,
    val startWeek: Int,
    val endWeek: Int
)
