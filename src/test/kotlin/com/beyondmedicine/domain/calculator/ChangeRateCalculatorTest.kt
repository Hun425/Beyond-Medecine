package com.beyondmedicine.domain.calculator

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

@DisplayName("ChangeRateCalculator 테스트")
class ChangeRateCalculatorTest {

    @Test
    @DisplayName("통증 점수 감소 시 양수 변화율 반환 (호전)")
    fun calculate_PainDecreased_ReturnsPositive() {
        // given: 통증 7.0 → 6.0 (14.3% 감소 = 호전)
        val previous = 7.0
        val current = 6.0

        // when
        val changeRate = ChangeRateCalculator.calculate(
            previous = previous,
            current = current,
            isHigherBetter = false
        )

        // then
        assertEquals(14.3, changeRate, 0.01)
    }

    @Test
    @DisplayName("통증 점수 증가 시 음수 변화율 반환 (악화)")
    fun calculate_PainIncreased_ReturnsNegative() {
        // given: 통증 6.0 → 8.5 (41.7% 증가 = 악화)
        val previous = 6.0
        val current = 8.5

        // when
        val changeRate = ChangeRateCalculator.calculate(
            previous = previous,
            current = current,
            isHigherBetter = false
        )

        // then
        assertEquals(-41.7, changeRate, 0.01)
    }

    @Test
    @DisplayName("턱 기능 점수 증가 시 양수 변화율 반환 (호전)")
    fun calculate_JawFunctionIncreased_ReturnsPositive() {
        // given: 턱 기능 6.0 → 7.0 (16.7% 증가 = 호전)
        val previous = 6.0
        val current = 7.0

        // when
        val changeRate = ChangeRateCalculator.calculate(
            previous = previous,
            current = current,
            isHigherBetter = true
        )

        // then
        assertEquals(16.7, changeRate, 0.01)
    }

    @Test
    @DisplayName("턱 기능 점수 감소 시 음수 변화율 반환 (악화)")
    fun calculate_JawFunctionDecreased_ReturnsNegative() {
        // given: 턱 기능 6.0 → 5.5 (-8.3% 감소 = 악화)
        val previous = 6.0
        val current = 5.5

        // when
        val changeRate = ChangeRateCalculator.calculate(
            previous = previous,
            current = current,
            isHigherBetter = true
        )

        // then
        assertEquals(-8.3, changeRate, 0.01)
    }

    @Test
    @DisplayName("이전 값이 0이면 0.0을 반환")
    fun calculate_PreviousIsZero_ReturnsZero() {
        // given
        val previous = 0.0
        val current = 5.0

        // when
        val changeRate = ChangeRateCalculator.calculate(
            previous = previous,
            current = current,
            isHigherBetter = false
        )

        // then
        assertEquals(0.0, changeRate)
    }

    @Test
    @DisplayName("스트레스 점수 감소 시 양수 변화율 (과제 예시)")
    fun calculate_StressDecreased_ExampleFromPdf() {
        // given: 과제 PDF 9페이지 예시
        // 1주차: 5.0 → 2주차: 4.0 (20% 감소 = 호전)
        val previous = 5.0
        val current = 4.0

        // when
        val changeRate = ChangeRateCalculator.calculate(
            previous = previous,
            current = current,
            isHigherBetter = false
        )

        // then
        assertEquals(20.0, changeRate, 0.01)
    }
}
