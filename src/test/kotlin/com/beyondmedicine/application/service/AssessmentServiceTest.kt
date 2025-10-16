package com.beyondmedicine.application.service

import com.beyondmedicine.application.repository.DailyAssessmentRepository
import com.beyondmedicine.application.repository.PrescriptionRepository
import com.beyondmedicine.domain.entity.Prescription
import com.beyondmedicine.domain.model.PrescriptionStatus
import com.beyondmedicine.presentation.dto.CreateDailyAssessmentRequest
import com.beyondmedicine.presentation.dto.PainAreaRequest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("AssessmentService 테스트")
class AssessmentServiceTest {

    @Mock
    private lateinit var prescriptionRepository: PrescriptionRepository

    @Mock
    private lateinit var dailyAssessmentRepository: DailyAssessmentRepository

    @InjectMocks
    private lateinit var assessmentService: AssessmentService

    private lateinit var activePrescription: Prescription

    @BeforeEach
    fun setUp() {
        activePrescription = Prescription(
            code = "ABCD1234",
            createdAt = LocalDateTime.of(2025, 9, 25, 10, 0),
            activatedAt = LocalDateTime.of(2025, 10, 1, 9, 0)
        )
    }

    @Test
    @DisplayName("일일 검사 등록 - 정상 케이스")
    fun createDailyAssessment_ValidRequest_ReturnsResponse() {
        // given
        val request = CreateDailyAssessmentRequest(
            prescriptionCode = "ABCD1234",
            assessmentDate = LocalDate.of(2025, 10, 15),
            painScore = 7,
            stressScore = 5,
            jawFunctionScore = 6,
            painAreas = listOf(
                PainAreaRequest(location = "LEFT_JAW", intensity = 8, description = "씹을 때 통증")
            )
        )

        `when`(prescriptionRepository.findByCode("ABCD1234")).thenReturn(activePrescription)
        `when`(dailyAssessmentRepository.existsByPrescriptionAndAssessmentDate(any(), any())).thenReturn(false)
        `when`(dailyAssessmentRepository.save(any())).thenAnswer { it.arguments[0] }

        // when
        val response = assessmentService.createDailyAssessment(request)

        // then
        assertNotNull(response)
        assertEquals("ABCD1234", response.prescriptionCode)
        assertEquals(3, response.weekNumber)  // 10/15는 10/1부터 3주차
        verify(dailyAssessmentRepository, times(1)).save(any())
    }

    @Test
    @DisplayName("일일 검사 등록 - 존재하지 않는 처방")
    fun createDailyAssessment_PrescriptionNotFound_ThrowsException() {
        // given
        val request = CreateDailyAssessmentRequest(
            prescriptionCode = "NOTEXIST",
            assessmentDate = LocalDate.of(2025, 10, 15),
            painScore = 7,
            stressScore = 5,
            jawFunctionScore = 6
        )

        `when`(prescriptionRepository.findByCode("NOTEXIST")).thenReturn(null)

        // when & then
        assertThrows(IllegalArgumentException::class.java) {
            assessmentService.createDailyAssessment(request)
        }
    }

    @Test
    @DisplayName("일일 검사 등록 - 중복 검사 (하루 1회 제한)")
    fun createDailyAssessment_DuplicateAssessment_ThrowsException() {
        // given
        val request = CreateDailyAssessmentRequest(
            prescriptionCode = "ABCD1234",
            assessmentDate = LocalDate.of(2025, 10, 15),
            painScore = 7,
            stressScore = 5,
            jawFunctionScore = 6
        )

        `when`(prescriptionRepository.findByCode("ABCD1234")).thenReturn(activePrescription)
        `when`(dailyAssessmentRepository.existsByPrescriptionAndAssessmentDate(
            activePrescription,
            LocalDate.of(2025, 10, 15)
        )).thenReturn(true)

        // when & then
        assertThrows(IllegalStateException::class.java) {
            assessmentService.createDailyAssessment(request)
        }
    }
}
