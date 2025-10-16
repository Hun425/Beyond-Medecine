package com.beyondmedicine.application.dto

import com.beyondmedicine.domain.model.PainLocation

/**
 * 주차별 추이 분석 결과
 */
data class WeeklyTrendData(
    val prescriptionCode: String,
    val startWeek: Int,
    val endWeek: Int,
    val weeklyTrends: List<WeeklyData>,
    val topPainAreas: List<TopPainAreaData>
)

/**
 * 주차별 데이터
 */
data class WeeklyData(
    val weekNumber: Int,
    val averagePainScore: Double,
    val averageStressScore: Double,
    val averageJawFunctionScore: Double,
    val assessmentCount: Long
)

/**
 * 빈발 통증 부위 데이터
 */
data class TopPainAreaData(
    val location: PainLocation,
    val occurrenceCount: Long,
    val averageIntensity: Double
)
