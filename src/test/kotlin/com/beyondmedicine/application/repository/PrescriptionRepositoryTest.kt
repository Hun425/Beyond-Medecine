package com.beyondmedicine.application.repository

import com.beyondmedicine.domain.entity.Prescription
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("PrescriptionRepository 테스트")
class PrescriptionRepositoryTest {

    @Autowired
    private lateinit var prescriptionRepository: PrescriptionRepository

    @Test
    @DisplayName("처방 코드로 조회")
    fun findByCode_ExistingCode_ReturnsPrescription() {
        // given
        val prescription = Prescription(
            code = "ABCD1234",
            createdAt = LocalDateTime.now(),
            activatedAt = null
        )
        prescriptionRepository.save(prescription)

        // when
        val found = prescriptionRepository.findByCode("ABCD1234")

        // then
        assertNotNull(found)
        assertEquals("ABCD1234", found?.code)
    }

    @Test
    @DisplayName("존재하지 않는 코드 조회 시 null 반환")
    fun findByCode_NonExistingCode_ReturnsNull() {
        // when
        val found = prescriptionRepository.findByCode("NOTEXIST")

        // then
        assertNull(found)
    }

    @Test
    @DisplayName("처방 코드 존재 여부 확인")
    fun existsByCode_ExistingCode_ReturnsTrue() {
        // given
        val prescription = Prescription(
            code = "TEST5678",
            createdAt = LocalDateTime.now(),
            activatedAt = null
        )
        prescriptionRepository.save(prescription)

        // when
        val exists = prescriptionRepository.existsByCode("TEST5678")

        // then
        assertTrue(exists)
    }

}
