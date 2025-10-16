package com.beyondmedicine.domain.entity

import com.beyondmedicine.domain.model.PainLocation
import jakarta.persistence.*

/**
 * 통증 부위 엔티티
 * - 참고: docs/business-logic-design.md
 * - 과제 PDF 6페이지
 */
@Entity
@Table(
    name = "pain_areas",
    indexes = [
        Index(
            name = "idx_pain_area_assessment",
            columnList = "daily_assessment_id"
        )
    ]
)
class PainArea(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "daily_assessment_id", nullable = false)
    val dailyAssessment: DailyAssessment,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val location: PainLocation,

    @Column(nullable = false)
    val intensity: Int,

    @Column(length = 500)
    val description: String? = null
) {
    init {
        // 통증 강도 검증: 0~10
        require(intensity in 0..10) {
            "통증 강도는 0~10 사이여야 합니다. 입력값: $intensity"
        }

        // 부연 설명 길이 검증
        description?.let {
            require(it.length <= 500) {
                "부연 설명은 500자 이하여야 합니다. 입력 길이: ${it.length}"
            }
        }
    }
}
