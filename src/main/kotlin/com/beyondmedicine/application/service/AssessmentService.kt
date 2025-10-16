package com.beyondmedicine.application.service

import com.beyondmedicine.application.repository.DailyAssessmentRepository
import com.beyondmedicine.application.repository.PrescriptionRepository
import com.beyondmedicine.domain.entity.DailyAssessment
import com.beyondmedicine.domain.entity.PainArea
import com.beyondmedicine.domain.model.PainLocation
import com.beyondmedicine.presentation.dto.CreateDailyAssessmentRequest
import com.beyondmedicine.presentation.dto.CreateDailyAssessmentResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 일일 검사 서비스
 * - 과제 PDF 5-6페이지
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
    fun createDailyAssessment(request: CreateDailyAssessmentRequest): CreateDailyAssessmentResponse {
        // 1. 처방 조회
        val prescription = prescriptionRepository.findByCode(request.prescriptionCode)
            ?: throw IllegalArgumentException("처방을 찾을 수 없습니다: ${request.prescriptionCode}")

        // 2. 처방 상태 검증 (ACTIVE만 가능)
        require(prescription.canPerformAssessment()) {
            "검사를 수행할 수 없는 상태입니다. 현재 상태: ${prescription.getStatus()}"
        }

        // 3. 중복 검사 확인 (하루 1회 제한)
        val alreadyExists = dailyAssessmentRepository.existsByPrescriptionAndAssessmentDate(
            prescription,
            request.assessmentDate
        )
        if (alreadyExists) {
            throw IllegalStateException("해당 날짜에 이미 검사가 등록되어 있습니다: ${request.assessmentDate}")
        }

        // 4. 주차 번호 계산
        val weekNumber = prescription.calculateWeekNumber(request.assessmentDate)
            ?: throw IllegalArgumentException("활성화되지 않은 처방입니다")

        // 5. 점수 범위 검증 (0~10)
        validateScores(request)

        // 6. DailyAssessment 생성
        val dailyAssessment = DailyAssessment(
            prescription = prescription,
            assessmentDate = request.assessmentDate,
            weekNumber = weekNumber,
            painScore = request.painScore,
            stressScore = request.stressScore,
            jawFunctionScore = request.jawFunctionScore
        )

        // 7. PainArea 추가
        request.painAreas.forEach { painAreaReq ->
            val painArea = PainArea(
                dailyAssessment = dailyAssessment,
                location = PainLocation.valueOf(painAreaReq.location),
                intensity = painAreaReq.intensity,
                description = painAreaReq.description
            )
            dailyAssessment.addPainArea(painArea)
        }

        // 8. 저장
        val savedAssessment = dailyAssessmentRepository.save(dailyAssessment)

        // 9. 응답 생성
        return CreateDailyAssessmentResponse(
            assessmentId = savedAssessment.id.toString(),
            prescriptionCode = prescription.code,
            weekNumber = weekNumber
        )
    }

    private fun validateScores(request: CreateDailyAssessmentRequest) {
        require(request.painScore in 0..10) {
            "통증 점수는 0~10 사이여야 합니다: ${request.painScore}"
        }
        require(request.stressScore in 0..10) {
            "스트레스 점수는 0~10 사이여야 합니다: ${request.stressScore}"
        }
        require(request.jawFunctionScore in 0..10) {
            "턱 기능 점수는 0~10 사이여야 합니다: ${request.jawFunctionScore}"
        }
        require(request.painAreas.size <= 6) {
            "통증 부위는 최대 6개까지만 등록 가능합니다: ${request.painAreas.size}"
        }
    }
}
