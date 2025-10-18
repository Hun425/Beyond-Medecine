package com.beyondmedicine.presentation.mapper

import com.beyondmedicine.application.dto.DailyAssessmentResult
import com.beyondmedicine.domain.model.PainLocation
import com.beyondmedicine.presentation.dto.CreateDailyAssessmentRequest
import com.beyondmedicine.presentation.dto.PainAreaRequest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import java.time.LocalDate

@DisplayName("AssessmentDtoMapper 테스트")
class AssessmentDtoMapperTest {

    private lateinit var mapper: AssessmentDtoMapper

    @BeforeEach
    fun setUp() {
        mapper = AssessmentDtoMapper()
    }

    @Test
    @DisplayName("Request를 Command로 변환 - 정상 케이스")
    fun toCommand_ValidRequest_ReturnsCommand() {
        // given
        val request = CreateDailyAssessmentRequest(
            prescriptionCode = "ABCD1234",
            assessmentDate = LocalDate.of(2025, 10, 15),
            painScore = 7,
            stressScore = 5,
            jawFunctionScore = 6,
            painAreas = listOf(
                PainAreaRequest("LEFT_JAW", 8, "씹을 때 통증"),
                PainAreaRequest("RIGHT_TEMPLE", 6, null)
            )
        )

        // when
        val command = mapper.toCommand(request)

        // then
        assertEquals("ABCD1234", command.prescriptionCode)
        assertEquals(LocalDate.of(2025, 10, 15), command.assessmentDate)
        assertEquals(7, command.painScore)
        assertEquals(5, command.stressScore)
        assertEquals(6, command.jawFunctionScore)
        assertEquals(2, command.painAreas.size)

        // PainArea 검증
        assertEquals(PainLocation.LEFT_JAW, command.painAreas[0].location)
        assertEquals(8, command.painAreas[0].intensity)
        assertEquals("씹을 때 통증", command.painAreas[0].description)

        assertEquals(PainLocation.RIGHT_TEMPLE, command.painAreas[1].location)
        assertEquals(6, command.painAreas[1].intensity)
        assertNull(command.painAreas[1].description)
    }

    @Test
    @DisplayName("Request를 Command로 변환 - 통증 부위 없음")
    fun toCommand_NoPainAreas_ReturnsCommandWithEmptyList() {
        // given
        val request = CreateDailyAssessmentRequest(
            prescriptionCode = "TEST5678",
            assessmentDate = LocalDate.of(2025, 10, 20),
            painScore = 3,
            stressScore = 2,
            jawFunctionScore = 8,
            painAreas = emptyList()
        )

        // when
        val command = mapper.toCommand(request)

        // then
        assertEquals("TEST5678", command.prescriptionCode)
        assertTrue(command.painAreas.isEmpty())
    }

    @Test
    @DisplayName("Result를 Response로 변환 - 정상 케이스")
    fun toResponse_ValidResult_ReturnsResponse() {
        // given
        val result = DailyAssessmentResult(
            assessmentId = 123L,
            prescriptionCode = "ABCD1234",
            weekNumber = 3,
            assessmentDate = LocalDate.of(2025, 10, 15)
        )

        // when
        val response = mapper.toResponse(result)

        // then
        assertEquals("123", response.assessmentId)
        assertEquals("ABCD1234", response.prescriptionCode)
        assertEquals(3, response.weekNumber)
    }

    @Test
    @DisplayName("PainLocation Enum 변환 - 모든 타입")
    fun toCommand_AllPainLocationTypes_ConvertsCorrectly() {
        // given
        val painLocations = listOf(
            "LEFT_JAW", "RIGHT_JAW", "LEFT_TEMPLE",
            "RIGHT_TEMPLE", "NECK", "CHIN"
        )

        val request = CreateDailyAssessmentRequest(
            prescriptionCode = "TEST0001",
            assessmentDate = LocalDate.of(2025, 10, 1),
            painScore = 5,
            stressScore = 5,
            jawFunctionScore = 5,
            painAreas = painLocations.map { PainAreaRequest(it, 5, null) }
        )

        // when
        val command = mapper.toCommand(request)

        // then
        assertEquals(6, command.painAreas.size)
        assertEquals(PainLocation.LEFT_JAW, command.painAreas[0].location)
        assertEquals(PainLocation.RIGHT_JAW, command.painAreas[1].location)
        assertEquals(PainLocation.LEFT_TEMPLE, command.painAreas[2].location)
        assertEquals(PainLocation.RIGHT_TEMPLE, command.painAreas[3].location)
        assertEquals(PainLocation.NECK, command.painAreas[4].location)
        assertEquals(PainLocation.CHIN, command.painAreas[5].location)
    }
}
