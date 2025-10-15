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
}
