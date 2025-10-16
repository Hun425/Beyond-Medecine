package com.beyondmedicine.presentation.dto

import com.beyondmedicine.application.dto.WeeklyTrendData

/**
 * 주차별 검사 추이 분석 응답 DTO
 * - 과제 PDF 8-9페이지
 */
data class WeeklyTrendResponse(
    val prescriptionCode: String,
    val period: PeriodDto,
    val weeklyTrends: List<WeeklyTrendDto>,
    val topPainAreas: List<TopPainAreaDto>
) {
    companion object {
        /**
         * Application DTO를 Presentation DTO로 변환
         */
        fun from(data: WeeklyTrendData): WeeklyTrendResponse {
            return WeeklyTrendResponse(
                prescriptionCode = data.prescriptionCode,
                period = PeriodDto(
                    startWeek = data.startWeek,
                    endWeek = data.endWeek
                ),
                weeklyTrends = data.weeklyTrends.map { weeklyData ->
                    WeeklyTrendDto(
                        weekNumber = weeklyData.weekNumber,
                        averagePainScore = weeklyData.averagePainScore,
                        averageStressScore = weeklyData.averageStressScore,
                        averageJawFunctionScore = weeklyData.averageJawFunctionScore,
                        changeRates = weeklyData.changeRates?.let { rates ->
                            ChangeRatesDto(
                                pain = rates.pain,
                                stress = rates.stress,
                                jawFunction = rates.jawFunction
                            )
                        }
                    )
                },
                topPainAreas = data.topPainAreas.map { painArea ->
                    TopPainAreaDto(
                        location = painArea.location.name,
                        count = painArea.occurrenceCount,
                        averageIntensity = painArea.averageIntensity
                    )
                }
            )
        }
    }
}

data class PeriodDto(
    val startWeek: Int,
    val endWeek: Int
)

data class WeeklyTrendDto(
    val weekNumber: Int,
    val averagePainScore: Double,
    val averageStressScore: Double,
    val averageJawFunctionScore: Double,
    val changeRates: ChangeRatesDto?
)

data class ChangeRatesDto(
    val pain: Double,
    val stress: Double,
    val jawFunction: Double
)

data class TopPainAreaDto(
    val location: String,
    val count: Long,
    val averageIntensity: Double
)
