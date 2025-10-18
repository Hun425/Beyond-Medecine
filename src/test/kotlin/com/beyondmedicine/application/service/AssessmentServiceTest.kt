package com.beyondmedicine.application.service

import com.beyondmedicine.application.dto.CreateDailyAssessmentCommand
import com.beyondmedicine.application.dto.PainAreaCommand
import com.beyondmedicine.application.exception.DuplicateAssessmentException
import com.beyondmedicine.application.exception.InvalidPrescriptionStatusException
import com.beyondmedicine.application.exception.PrescriptionNotFoundException
import com.beyondmedicine.application.repository.DailyAssessmentRepository
import com.beyondmedicine.application.repository.PrescriptionRepository
import com.beyondmedicine.domain.entity.Prescription
import com.beyondmedicine.domain.model.PainLocation
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
    fun createDailyAssessment_ValidCommand_ReturnsResult() {
        // given
        val command = CreateDailyAssessmentCommand(
            prescriptionCode = "ABCD1234",
            assessmentDate = LocalDate.of(2025, 10, 15),
            painScore = 7,
            stressScore = 5,
            jawFunctionScore = 6,
            painAreas = listOf(
                PainAreaCommand(location = PainLocation.LEFT_JAW, intensity = 8, description = "씹을 때 통증")
            )
        )

        `when`(prescriptionRepository.findByCode("ABCD1234")).thenReturn(activePrescription)
        `when`(dailyAssessmentRepository.existsByPrescriptionAndAssessmentDate(any(), any())).thenReturn(false)
        `when`(dailyAssessmentRepository.save(any())).thenAnswer { it.arguments[0] }

        // when
        val result = assessmentService.createDailyAssessment(command)

        // then
        assertNotNull(result)
        assertEquals("ABCD1234", result.prescriptionCode)
        assertEquals(3, result.weekNumber)  // 10/15는 10/1부터 3주차
        verify(dailyAssessmentRepository, times(1)).save(any())
    }

    @Test
    @DisplayName("일일 검사 등록 - 존재하지 않는 처방")
    fun createDailyAssessment_PrescriptionNotFound_ThrowsException() {
        // given
        val command = CreateDailyAssessmentCommand(
            prescriptionCode = "NOTEXIST",
            assessmentDate = LocalDate.of(2025, 10, 15),
            painScore = 7,
            stressScore = 5,
            jawFunctionScore = 6,
            painAreas = emptyList()
        )

        `when`(prescriptionRepository.findByCode("NOTEXIST")).thenReturn(null)

        // when & then
        assertThrows(PrescriptionNotFoundException::class.java) {
            assessmentService.createDailyAssessment(command)
        }
    }

    @Test
    @DisplayName("일일 검사 등록 - 중복 검사 (하루 1회 제한)")
    fun createDailyAssessment_DuplicateAssessment_ThrowsException() {
        // given
        val command = CreateDailyAssessmentCommand(
            prescriptionCode = "ABCD1234",
            assessmentDate = LocalDate.of(2025, 10, 15),
            painScore = 7,
            stressScore = 5,
            jawFunctionScore = 6,
            painAreas = emptyList()
        )

        `when`(prescriptionRepository.findByCode("ABCD1234")).thenReturn(activePrescription)
        `when`(dailyAssessmentRepository.existsByPrescriptionAndAssessmentDate(
            activePrescription,
            LocalDate.of(2025, 10, 15)
        )).thenReturn(true)

        // when & then
        assertThrows(DuplicateAssessmentException::class.java) {
            assessmentService.createDailyAssessment(command)
        }
    }

    @Test
    @DisplayName("일일 검사 등록 - PENDING 상태 처방")
    fun createDailyAssessment_PendingPrescription_ThrowsException() {
        // given
        val pendingPrescription = Prescription(
            code = "PEND1234",
            createdAt = LocalDateTime.of(2025, 10, 15, 10, 0),
            activatedAt = null  // 활성화 안됨
        )

        val command = CreateDailyAssessmentCommand(
            prescriptionCode = "PEND1234",
            assessmentDate = LocalDate.of(2025, 10, 16),
            painScore = 5,
            stressScore = 5,
            jawFunctionScore = 5,
            painAreas = emptyList()
        )

        `when`(prescriptionRepository.findByCode("PEND1234")).thenReturn(pendingPrescription)

        // when & then
        assertThrows(InvalidPrescriptionStatusException::class.java) {
            assessmentService.createDailyAssessment(command)
        }
    }

    @Test
    @DisplayName("일일 검사 등록 - 통증 부위 중복")
    fun createDailyAssessment_DuplicatePainAreas_ThrowsException() {
        // given: 같은 부위(LEFT_JAW)를 2번 등록
        val command = CreateDailyAssessmentCommand(
            prescriptionCode = "ABCD1234",
            assessmentDate = LocalDate.of(2025, 10, 15),
            painScore = 7,
            stressScore = 5,
            jawFunctionScore = 6,
            painAreas = listOf(
                PainAreaCommand(location = PainLocation.LEFT_JAW, intensity = 8, description = "첫 번째"),
                PainAreaCommand(location = PainLocation.LEFT_JAW, intensity = 7, description = "중복")
            )
        )

        `when`(prescriptionRepository.findByCode("ABCD1234")).thenReturn(activePrescription)
        `when`(dailyAssessmentRepository.existsByPrescriptionAndAssessmentDate(any(), any())).thenReturn(false)

        // when & then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            assessmentService.createDailyAssessment(command)
        }
        assertTrue(exception.message!!.contains("중복된 통증 부위"))
    }

    @Test
    @DisplayName("일일 검사 등록 - 통증 부위 7개 (최대 6개 초과)")
    fun createDailyAssessment_TooManyPainAreas_ThrowsException() {
        // given: 7개의 통증 부위
        val command = CreateDailyAssessmentCommand(
            prescriptionCode = "ABCD1234",
            assessmentDate = LocalDate.of(2025, 10, 15),
            painScore = 7,
            stressScore = 5,
            jawFunctionScore = 6,
            painAreas = listOf(
                PainAreaCommand(location = PainLocation.LEFT_JAW, intensity = 8, description = null),
                PainAreaCommand(location = PainLocation.RIGHT_JAW, intensity = 7, description = null),
                PainAreaCommand(location = PainLocation.LEFT_TEMPLE, intensity = 6, description = null),
                PainAreaCommand(location = PainLocation.RIGHT_TEMPLE, intensity = 5, description = null),
                PainAreaCommand(location = PainLocation.NECK, intensity = 4, description = null),
                PainAreaCommand(location = PainLocation.CHIN, intensity = 3, description = null),
                PainAreaCommand(location = PainLocation.LEFT_JAW, intensity = 2, description = "7번째")
            )
        )

        `when`(prescriptionRepository.findByCode("ABCD1234")).thenReturn(activePrescription)
        `when`(dailyAssessmentRepository.existsByPrescriptionAndAssessmentDate(any(), any())).thenReturn(false)

        // when & then
        val exception = assertThrows(IllegalArgumentException::class.java) {
            assessmentService.createDailyAssessment(command)
        }
        assertTrue(exception.message!!.contains("최대 6개"))
    }
}
