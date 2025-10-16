package com.beyondmedicine.application.repository

import com.beyondmedicine.application.dto.PainAreaAggregation
import com.beyondmedicine.application.dto.WeeklyAggregation
import com.beyondmedicine.domain.entity.DailyAssessment
import com.beyondmedicine.domain.entity.Prescription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

/**
 * 일일 검사 리포지토리
 * - 참고: docs/repository-design.md
 */
@Repository
interface DailyAssessmentRepository : JpaRepository<DailyAssessment, Long> {

    /**
     * 중복 검사 확인 (하루 1회 제한)
     */
    fun existsByPrescriptionAndAssessmentDate(
        prescription: Prescription,
        assessmentDate: LocalDate
    ): Boolean

    /**
     * 처방별 검사 목록 조회 (날짜 오름차순)
     */
    fun findByPrescriptionOrderByAssessmentDateAsc(
        prescription: Prescription
    ): List<DailyAssessment>

    /**
     * 주차별 평균 점수 집계
     * - 과제 PDF 8페이지 요구사항
     */
    @Query("""
        SELECT new com.beyondmedicine.application.dto.WeeklyAggregation(
            da.weekNumber,
            AVG(da.painScore),
            AVG(da.stressScore),
            AVG(da.jawFunctionScore),
            COUNT(da.id)
        )
        FROM DailyAssessment da
        WHERE da.prescription = :prescription
          AND da.weekNumber BETWEEN :startWeek AND :endWeek
        GROUP BY da.weekNumber
        ORDER BY da.weekNumber ASC
    """)
    fun findWeeklyAggregations(
        @Param("prescription") prescription: Prescription,
        @Param("startWeek") startWeek: Int,
        @Param("endWeek") endWeek: Int
    ): List<WeeklyAggregation>

    /**
     * Top N 통증 부위 집계
     * - 과제 PDF 8페이지 요구사항
     */
    @Query("""
        SELECT new com.beyondmedicine.application.dto.PainAreaAggregation(
            pa.location,
            COUNT(pa.id),
            AVG(pa.intensity)
        )
        FROM DailyAssessment da
        JOIN da.painAreas pa
        WHERE da.prescription = :prescription
          AND da.weekNumber BETWEEN :startWeek AND :endWeek
        GROUP BY pa.location
        ORDER BY COUNT(pa.id) DESC, AVG(pa.intensity) DESC
    """)
    fun findTopPainAreas(
        @Param("prescription") prescription: Prescription,
        @Param("startWeek") startWeek: Int,
        @Param("endWeek") endWeek: Int
    ): List<PainAreaAggregation>
}
