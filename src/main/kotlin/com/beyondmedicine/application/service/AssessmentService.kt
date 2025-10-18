package com.beyondmedicine.application.service

import com.beyondmedicine.application.dto.CreateDailyAssessmentCommand
import com.beyondmedicine.application.dto.DailyAssessmentResult
import com.beyondmedicine.application.exception.DuplicateAssessmentException
import com.beyondmedicine.application.exception.InvalidAssessmentDateException
import com.beyondmedicine.application.exception.InvalidPrescriptionStatusException
import com.beyondmedicine.application.exception.PrescriptionNotFoundException
import com.beyondmedicine.application.repository.DailyAssessmentRepository
import com.beyondmedicine.application.repository.PrescriptionRepository
import com.beyondmedicine.domain.entity.DailyAssessment
import com.beyondmedicine.domain.entity.PainArea
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 일일 검사 서비스
 * - 과제 PDF 5-6페이지
 * - Application 계층: Presentation DTO와 독립적인 비즈니스 로직
 */
@Service
@Transactional(readOnly = true)
class AssessmentService(
    private val prescriptionRepository: PrescriptionRepository,
    private val dailyAssessmentRepository: DailyAssessmentRepository
) {

    /**
     * 일일 검사 등록
     */
    @Transactional
    fun createDailyAssessment(command: CreateDailyAssessmentCommand): DailyAssessmentResult {
        // 1. 처방 조회
        val prescription = prescriptionRepository.findByCode(command.prescriptionCode)
            ?: throw PrescriptionNotFoundException("처방을 찾을 수 없습니다. 처방 코드: ${command.prescriptionCode}")

        // 2. 처방 상태 검증 (ACTIVE만 가능)
        if (!prescription.canPerformAssessment()) {
            throw InvalidPrescriptionStatusException(
                "검사를 수행할 수 없는 처방 상태입니다. " +
                "현재 상태: ${prescription.getStatus()}, 처방 코드: ${prescription.code}"
            )
        }

        // 3. 중복 검사 확인 (하루 1회 제한)
        val alreadyExists = dailyAssessmentRepository.existsByPrescriptionAndAssessmentDate(
            prescription,
            command.assessmentDate
        )
        if (alreadyExists) {
            throw DuplicateAssessmentException(
                "해당 날짜에 이미 검사가 등록되어 있습니다. " +
                "처방 코드: ${prescription.code}, 날짜: ${command.assessmentDate}"
            )
        }

        // 4. 주차 번호 계산
        val weekNumber = prescription.calculateWeekNumber(command.assessmentDate)
            ?: throw InvalidAssessmentDateException(
                "유효하지 않은 검사 일자입니다. " +
                "검사는 활성화 후 1~6주차(D+0 ~ D+41) 사이에만 가능합니다. " +
                "검사 일자: ${command.assessmentDate}"
            )

        // 5. 주차 범위 검증 (1~6)
        require(weekNumber in 1..6) {
            "주차는 1~6 사이여야 합니다. 계산된 주차: $weekNumber"
        }

        // 6. 통증 부위 검증 (최대 6개, 중복 불가)
        validatePainAreas(command.painAreas)

        // 7. DailyAssessment 생성
        val dailyAssessment = DailyAssessment(
            prescription = prescription,
            assessmentDate = command.assessmentDate,
            weekNumber = weekNumber,
            painScore = command.painScore,
            stressScore = command.stressScore,
            jawFunctionScore = command.jawFunctionScore
        )

        // 8. PainArea 추가
        command.painAreas.forEach { painAreaCommand ->
            val painArea = PainArea(
                dailyAssessment = dailyAssessment,
                location = painAreaCommand.location,
                intensity = painAreaCommand.intensity,
                description = painAreaCommand.description
            )
            dailyAssessment.addPainArea(painArea)
        }

        // 9. 저장
        val savedAssessment = dailyAssessmentRepository.save(dailyAssessment)

        // 10. Result 반환
        return DailyAssessmentResult(
            assessmentId = savedAssessment.id,
            prescriptionCode = prescription.code,
            weekNumber = weekNumber,
            assessmentDate = command.assessmentDate
        )
    }

    /**
     * 통증 부위 검증
     * - 최대 6개
     * - 중복 불가
     */
    private fun validatePainAreas(painAreas: List<com.beyondmedicine.application.dto.PainAreaCommand>) {
        // 개수 검증
        require(painAreas.size <= 6) {
            "통증 부위는 최대 6개까지 등록 가능합니다. 입력 개수: ${painAreas.size}"
        }

        // 중복 검증
        val duplicateLocations = painAreas
            .groupBy { it.location }
            .filter { it.value.size > 1 }
            .keys

        require(duplicateLocations.isEmpty()) {
            "중복된 통증 부위가 있습니다: ${duplicateLocations.joinToString { it.name }}"
        }
    }

    /**
     * 처방의 현재 주차 조회
     * - PDF 요구사항: endWeek가 null이면 현재 주차 사용
     */
    fun getCurrentWeek(prescriptionCode: String): Int? {
        val prescription = prescriptionRepository.findByCode(prescriptionCode)
            ?: return null

        val today = java.time.LocalDate.now()
        return prescription.calculateWeekNumber(today)?.coerceIn(1, 6)
    }
}
