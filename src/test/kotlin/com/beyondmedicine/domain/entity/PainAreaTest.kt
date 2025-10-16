package com.beyondmedicine.domain.entity

import com.beyondmedicine.domain.model.PainLocation
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("PainArea Entity 테스트")
class PainAreaTest {

    @Test
    @DisplayName("통증 부위 생성 - 모든 필드 설정")
    fun create_PainArea_AllFieldsSet() {
        // given
        val assessment = createTestAssessment()

        // when
        val painArea = PainArea(
            dailyAssessment = assessment,
            location = PainLocation.LEFT_JAW,
            intensity = 8,
            description = "씹을 때 통증"
        )

        // then
        assertEquals(assessment, painArea.dailyAssessment)
        assertEquals(PainLocation.LEFT_JAW, painArea.location)
        assertEquals(8, painArea.intensity)
        assertEquals("씹을 때 통증", painArea.description)
    }

    @Test
    @DisplayName("통증 부위 생성 - 부연 설명 null 가능")
    fun create_PainArea_DescriptionCanBeNull() {
        // given
        val assessment = createTestAssessment()

        // when
        val painArea = PainArea(
            dailyAssessment = assessment,
            location = PainLocation.NECK,
            intensity = 5,
            description = null
        )

        // then
        assertNull(painArea.description)
    }

    @Test
    @DisplayName("통증 강도 검증 - 0~10")
    fun init_InvalidIntensity_ThrowsException() {
        // given
        val assessment = createTestAssessment()

        // when & then: -1
        assertThrows(IllegalArgumentException::class.java) {
            PainArea(
                dailyAssessment = assessment,
                location = PainLocation.CHIN,
                intensity = -1,
                description = null
            )
        }

        // when & then: 11
        assertThrows(IllegalArgumentException::class.java) {
            PainArea(
                dailyAssessment = assessment,
                location = PainLocation.CHIN,
                intensity = 11,
                description = null
            )
        }
    }

    @Test
    @DisplayName("부연 설명 길이 검증 - 최대 500자")
    fun init_DescriptionTooLong_ThrowsException() {
        // given
        val assessment = createTestAssessment()
        val longDescription = "a".repeat(501)

        // when & then
        assertThrows(IllegalArgumentException::class.java) {
            PainArea(
                dailyAssessment = assessment,
                location = PainLocation.LEFT_TEMPLE,
                intensity = 7,
                description = longDescription
            )
        }
    }

    private fun createTestAssessment(): DailyAssessment {
        val prescription = Prescription(
            code = "TEST1234",
            createdAt = LocalDateTime.of(2025, 9, 25, 10, 0),
            activatedAt = LocalDateTime.of(2025, 10, 1, 9, 0)
        )

        return DailyAssessment(
            prescription = prescription,
            assessmentDate = LocalDate.of(2025, 10, 15),
            weekNumber = 3,
            painScore = 7,
            stressScore = 5,
            jawFunctionScore = 6
        )
    }
}
