package com.beyondmedicine.domain.entity

import jakarta.persistence.*
import java.time.LocalDate

/**
 * 일일 검사 엔티티
 * - 참고: docs/business-logic-design.md
 * - 과제 PDF 6페이지
 */
@Entity
@Table(
    name = "daily_assessments",
    indexes = [
        Index(
            name = "idx_daily_assessment_unique",
            columnList = "prescription_code, assessment_date",
            unique = true
        ),
        Index(
            name = "idx_daily_assessment_week",
            columnList = "prescription_code, week_number"
        )
    ]
)
class DailyAssessment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_code", referencedColumnName = "code", nullable = false)
    val prescription: Prescription,

    @Column(nullable = false)
    val assessmentDate: LocalDate,

    @Column(nullable = false)
    val weekNumber: Int,

    @Column(nullable = false)
    val painScore: Int,

    @Column(nullable = false)
    val stressScore: Int,

    @Column(nullable = false)
    val jawFunctionScore: Int,

    @OneToMany(
        mappedBy = "dailyAssessment",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    val painAreas: MutableList<PainArea> = mutableListOf()
) {
    init {
        // 점수 범위 검증: 0~10
        require(painScore in 0..10) {
            "통증 점수는 0~10 사이여야 합니다. 입력값: $painScore"
        }
        require(stressScore in 0..10) {
            "스트레스 점수는 0~10 사이여야 합니다. 입력값: $stressScore"
        }
        require(jawFunctionScore in 0..10) {
            "턱 기능 점수는 0~10 사이여야 합니다. 입력값: $jawFunctionScore"
        }

        // 주차 번호 검증: 1~6
        require(weekNumber in 1..6) {
            "주차 번호는 1~6 사이여야 합니다. 입력값: $weekNumber"
        }
    }

    /**
     * 통증 부위 추가
     * - 최대 6개까지만 허용
     */
    fun addPainArea(painArea: PainArea) {
        require(painAreas.size < 6) {
            "통증 부위는 최대 6개까지만 등록 가능합니다. 현재: ${painAreas.size}"
        }
        painAreas.add(painArea)
    }
}
