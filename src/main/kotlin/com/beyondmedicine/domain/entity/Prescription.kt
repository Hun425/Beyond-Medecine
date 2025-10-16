package com.beyondmedicine.domain.entity

import com.beyondmedicine.domain.calculator.PrescriptionCalculator
import com.beyondmedicine.domain.calculator.WeekCalculator
import com.beyondmedicine.domain.model.PrescriptionStatus
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 처방 엔티티
 * - 참고: docs/business-logic-design.md
 * - 과제 PDF 4-5페이지
 */
@Entity
@Table(
    name = "prescriptions",
    indexes = [
        Index(name = "idx_prescription_code", columnList = "code", unique = true)
    ]
)
class Prescription(
    @Id
    @Column(nullable = false, unique = true, length = 8)
    val code: String,  // "ABCD1234" 형식 (영대문자4 + 숫자4)

    @Column(nullable = false)
    val createdAt: LocalDateTime,

    @Column(nullable = true)
    var activatedAt: LocalDateTime? = null
) {
    /**
     * 처방 상태 조회
     * - 계산 로직은 PrescriptionCalculator에 위임 (FP)
     */
    fun getStatus(currentTime: LocalDateTime = LocalDateTime.now()): PrescriptionStatus {
        return PrescriptionCalculator.calculateStatus(
            createdAt = createdAt,
            activatedAt = activatedAt,
            currentTime = currentTime
        )
    }

    /**
     * 주차 번호 계산
     * - 계산 로직은 WeekCalculator에 위임 (FP)
     * - 활성화 전이면 null 반환
     */
    fun calculateWeekNumber(assessmentDate: LocalDate): Int? {
        val activationDate = activatedAt?.toLocalDate() ?: return null
        return WeekCalculator.calculateWeekNumber(
            activationDate = activationDate,
            assessmentDate = assessmentDate
        )
    }

    /**
     * 현재 주차 조회
     */
    fun getCurrentWeek(currentDate: LocalDate = LocalDate.now()): Int? {
        return calculateWeekNumber(currentDate)
    }

    /**
     * 검사 수행 가능 여부
     * - ACTIVE 상태일 때만 true
     */
    fun canPerformAssessment(currentTime: LocalDateTime = LocalDateTime.now()): Boolean {
        return getStatus(currentTime) == PrescriptionStatus.ACTIVE
    }

    /**
     * 처방 활성화
     * - 상태 변경 메서드 (OOP)
     */
    fun activate(activationTime: LocalDateTime = LocalDateTime.now()) {
        require(activatedAt == null) {
            "이미 활성화된 처방입니다. 활성화 일시: $activatedAt"
        }
        require(getStatus(activationTime) == PrescriptionStatus.PENDING) {
            "활성화할 수 없는 상태입니다. 현재 상태: ${getStatus(activationTime)}"
        }
        this.activatedAt = activationTime
    }
}
