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
    val assessmentCount: Long,
    val changeRates: ChangeRates? = null  // 첫 주차는 null
)

/**
 * 전주 대비 변화율 (%)
 * - 통증/스트레스: 낮을수록 좋음 (증가=악화=음수, 감소=호전=양수)
 * - 턱 기능: 높을수록 좋음 (증가=호전=양수, 감소=악화=음수)
 */
data class ChangeRates(
    val pain: Double,       // 통증 변화율
    val stress: Double,     // 스트레스 변화율
    val jawFunction: Double // 턱 기능 변화율
)

/**
 * 빈발 통증 부위 데이터
 */
data class TopPainAreaData(
    val location: PainLocation,
    val occurrenceCount: Long,
    val averageIntensity: Double
)
