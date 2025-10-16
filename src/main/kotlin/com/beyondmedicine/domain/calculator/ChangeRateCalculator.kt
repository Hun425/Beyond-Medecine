package com.beyondmedicine.domain.calculator

/**
 * 변화율 계산 순수 함수
 * - 참고: docs/fp-hybrid-design.md
 * - 과제 PDF 8페이지 변화율 계산 로직
 */
object ChangeRateCalculator {

    /**
     * 변화율 계산
     *
     * @param previous 이전 값
     * @param current 현재 값
     * @param isHigherBetter true: 높을수록 좋음(턱기능), false: 낮을수록 좋음(통증/스트레스)
     * @return 변화율(%), 소수점 첫째 자리 반올림
     */
    fun calculate(
        previous: Double,
        current: Double,
        isHigherBetter: Boolean
    ): Double {
        // 0으로 나눌 수 없음
        if (previous == 0.0) return 0.0

        // 기본 변화율: ((현재 - 이전) / 이전) * 100
        val rawChangeRate = ((current - previous) / previous) * 100

        // 낮을수록 좋은 지표는 부호 반전
        // 예: 통증 7→6 = -14.3% → 14.3% (호전)
        val adjustedChangeRate = if (isHigherBetter) {
            rawChangeRate
        } else {
            -rawChangeRate
        }

        // 소수점 첫째 자리까지 반올림
        return String.format("%.1f", adjustedChangeRate).toDouble()
    }
}
