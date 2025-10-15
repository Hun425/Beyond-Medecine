package com.beyondmedicine.domain.calculator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import java.time.LocalDate

@DisplayName("WeekCalculator 테스트")
class WeekCalculatorTest {

    @Test
    @DisplayName("활성화 당일은 1주차다 (D+0)")
    fun calculateWeekNumber_SameDay_Returns1() {
        // given
        val activationDate = LocalDate.of(2025, 10, 1)
        val assessmentDate = LocalDate.of(2025, 10, 1)

        // when
        val weekNumber = WeekCalculator.calculateWeekNumber(activationDate, assessmentDate)

        // then
        assertEquals(1, weekNumber)
    }

    @Test
    @DisplayName("활성화 후 6일은 1주차다 (D+6)")
    fun calculateWeekNumber_SixDaysLater_Returns1() {
        // given
        val activationDate = LocalDate.of(2025, 10, 1)
        val assessmentDate = LocalDate.of(2025, 10, 7)  // D+6

        // when
        val weekNumber = WeekCalculator.calculateWeekNumber(activationDate, assessmentDate)

        // then
        assertEquals(1, weekNumber)
    }

    @Test
    @DisplayName("활성화 후 7일은 2주차다 (D+7)")
    fun calculateWeekNumber_SevenDaysLater_Returns2() {
        // given
        val activationDate = LocalDate.of(2025, 10, 1)
        val assessmentDate = LocalDate.of(2025, 10, 8)  // D+7

        // when
        val weekNumber = WeekCalculator.calculateWeekNumber(activationDate, assessmentDate)

        // then
        assertEquals(2, weekNumber)
    }

    @Test
    @DisplayName("활성화 후 14일은 3주차다 (과제 예시)")
    fun calculateWeekNumber_FourteenDaysLater_Returns3() {
        // given: 과제 PDF 예시
        val activationDate = LocalDate.of(2025, 9, 1)
        val assessmentDate = LocalDate.of(2025, 9, 15)  // 14일 후

        // when
        val weekNumber = WeekCalculator.calculateWeekNumber(activationDate, assessmentDate)

        // then
        assertEquals(3, weekNumber)  // (14 / 7) + 1 = 3
    }

    @Test
    @DisplayName("활성화 후 41일은 6주차다 (D+41, 마지막 주차)")
    fun calculateWeekNumber_FortyOneDaysLater_Returns6() {
        // given
        val activationDate = LocalDate.of(2025, 10, 1)
        val assessmentDate = LocalDate.of(2025, 11, 11)  // D+41

        // when
        val weekNumber = WeekCalculator.calculateWeekNumber(activationDate, assessmentDate)

        // then
        assertEquals(6, weekNumber)
    }

    @Test
    @DisplayName("평가일이 활성화일보다 이전이면 null을 반환한다")
    fun calculateWeekNumber_AssessmentBeforeActivation_ReturnsNull() {
        // given
        val activationDate = LocalDate.of(2025, 10, 1)
        val assessmentDate = LocalDate.of(2025, 9, 30)  // 활성화 1일 전

        // when
        val weekNumber = WeekCalculator.calculateWeekNumber(activationDate, assessmentDate)

        // then
        assertEquals(null, weekNumber)
    }

    @Test
    @DisplayName("활성화 후 42일(D+42)은 유효 기간 종료로 null을 반환한다")
    fun calculateWeekNumber_AfterActiveWeeks_ReturnsNull() {
        // given
        val activationDate = LocalDate.of(2025, 10, 1)
        val assessmentDate = LocalDate.of(2025, 11, 12)  // D+42

        // when
        val weekNumber = WeekCalculator.calculateWeekNumber(activationDate, assessmentDate)

        // then
        assertEquals(null, weekNumber)
    }
}
