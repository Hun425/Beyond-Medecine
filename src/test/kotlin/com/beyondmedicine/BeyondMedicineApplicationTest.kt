package com.beyondmedicine

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class BeyondMedicineApplicationTest {

    @Test
    fun contextLoads() {
        // Spring Context가 정상적으로 로드되는지 확인
    }
}
