package com.beyondmedicine.presentation.mapper

import com.beyondmedicine.application.dto.CreateDailyAssessmentCommand
import com.beyondmedicine.application.dto.DailyAssessmentResult
import com.beyondmedicine.application.dto.PainAreaCommand
import com.beyondmedicine.domain.model.PainLocation
import com.beyondmedicine.presentation.dto.CreateDailyAssessmentRequest
import com.beyondmedicine.presentation.dto.CreateDailyAssessmentResponse
import com.beyondmedicine.presentation.dto.PainAreaRequest
import org.springframework.stereotype.Component

/**
 * Assessment DTO 변환 Mapper
 * - Presentation DTO ↔ Application DTO 변환
 * - 계층 분리: Controller-Service 간 DTO 독립성 유지
 * - 참고: docs/fp-hybrid-design.md
 */
@Component
class AssessmentDtoMapper {

    /**
     * Presentation Request → Application Command 변환
     */
    fun toCommand(request: CreateDailyAssessmentRequest): CreateDailyAssessmentCommand {
        return CreateDailyAssessmentCommand(
            prescriptionCode = request.prescriptionCode,
            assessmentDate = request.assessmentDate,
            painScore = request.painScore,
            stressScore = request.stressScore,
            jawFunctionScore = request.jawFunctionScore,
            painAreas = request.painAreas.map { toPainAreaCommand(it) }
        )
    }

    /**
     * PainAreaRequest → PainAreaCommand 변환
     */
    private fun toPainAreaCommand(request: PainAreaRequest): PainAreaCommand {
        return PainAreaCommand(
            location = PainLocation.valueOf(request.location),
            intensity = request.intensity,
            description = request.description
        )
    }

    /**
     * Application Result → Presentation Response 변환
     */
    fun toResponse(result: DailyAssessmentResult): CreateDailyAssessmentResponse {
        return CreateDailyAssessmentResponse(
            assessmentId = result.assessmentId.toString(),
            prescriptionCode = result.prescriptionCode,
            weekNumber = result.weekNumber
        )
    }
}
