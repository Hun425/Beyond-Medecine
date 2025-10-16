package com.beyondmedicine.application.repository

import com.beyondmedicine.domain.entity.Prescription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 처방 리포지토리
 * - 참고: docs/repository-design.md
 */
@Repository
interface PrescriptionRepository : JpaRepository<Prescription, String> {

    /**
     * 처방 코드로 조회
     * - Primary Key가 code이므로 findById와 동일
     */
    fun findByCode(code: String): Prescription?

    /**
     * 처방 코드 존재 여부 확인
     */
    fun existsByCode(code: String): Boolean
}
