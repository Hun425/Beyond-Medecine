package com.beyondmedicine.integration

import com.beyondmedicine.application.repository.DailyAssessmentRepository
import com.beyondmedicine.application.repository.PrescriptionRepository
import com.beyondmedicine.domain.entity.Prescription
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * AssessmentController 통합 테스트
 * - Phase 6: Integration Tests (TDD 가이드)
 * - 전체 Request/Response 사이클 테스트
 * - 실제 Spring Context 및 Database 사용
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Assessment API 통합 테스트")
class AssessmentIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var prescriptionRepository: PrescriptionRepository

    @Autowired
    private lateinit var dailyAssessmentRepository: DailyAssessmentRepository

    private lateinit var activePrescription: Prescription

    @BeforeEach
    fun setUp() {
        // 모든 데이터 초기화
        dailyAssessmentRepository.deleteAll()
        prescriptionRepository.deleteAll()

        // 활성화된 처방 생성 (패턴: XXXX9999 or 9999XXXX)
        activePrescription = Prescription(
            code = "TEST0001",
            createdAt = LocalDateTime.of(2025, 9, 25, 10, 0),
            activatedAt = LocalDateTime.of(2025, 10, 1, 9, 0)
        )
        prescriptionRepository.save(activePrescription)
    }

    @Test
    @DisplayName("E2E: 일일 검사 등록 → 주차별 추이 조회")
    fun endToEnd_CreateAssessmentAndGetTrend_Success() {
        // 1. 1주차 검사 등록
        val week1Request = """
            {
                "prescriptionCode": "TEST0001",
                "assessmentDate": "2025-10-02",
                "painScore": 8,
                "stressScore": 7,
                "jawFunctionScore": 4,
                "painAreas": [
                    {
                        "location": "LEFT_JAW",
                        "intensity": 9,
                        "description": "씹을 때 심한 통증"
                    }
                ]
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/assessments/daily")
                .contentType(MediaType.APPLICATION_JSON)
                .content(week1Request)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.prescriptionCode").value("TEST0001"))
            .andExpect(jsonPath("$.weekNumber").value(1))
            .andExpect(jsonPath("$.assessmentId").exists())

        // 2. 2주차 검사 등록
        val week2Request = """
            {
                "prescriptionCode": "TEST0001",
                "assessmentDate": "2025-10-09",
                "painScore": 6,
                "stressScore": 5,
                "jawFunctionScore": 6,
                "painAreas": [
                    {
                        "location": "LEFT_JAW",
                        "intensity": 7,
                        "description": "통증 감소"
                    }
                ]
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/assessments/daily")
                .contentType(MediaType.APPLICATION_JSON)
                .content(week2Request)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.weekNumber").value(2))

        // 3. 주차별 추이 조회
        mockMvc.perform(
            get("/api/v1/assessments/weekly-trend")
                .param("prescriptionCode", "TEST0001")
                .param("startWeek", "1")
                .param("endWeek", "2")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.prescriptionCode").value("TEST0001"))
            .andExpect(jsonPath("$.weeklyTrends").isArray)
            .andExpect(jsonPath("$.weeklyTrends.length()").value(2))
            .andExpect(jsonPath("$.weeklyTrends[0].weekNumber").value(1))
            .andExpect(jsonPath("$.weeklyTrends[0].averagePainScore").value(8.0))
            .andExpect(jsonPath("$.weeklyTrends[1].weekNumber").value(2))
            .andExpect(jsonPath("$.weeklyTrends[1].averagePainScore").value(6.0))
            .andExpect(jsonPath("$.weeklyTrends[1].changeRates.pain").value(25.0))  // 통증 감소는 양수(호전)
    }

    @Test
    @DisplayName("POST /daily - 정상 케이스 (201 Created)")
    fun createDailyAssessment_ValidRequest_Returns201() {
        val request = """
            {
                "prescriptionCode": "TEST0001",
                "assessmentDate": "2025-10-15",
                "painScore": 7,
                "stressScore": 5,
                "jawFunctionScore": 6,
                "painAreas": [
                    {
                        "location": "LEFT_JAW",
                        "intensity": 8,
                        "description": "씹을 때 통증"
                    },
                    {
                        "location": "RIGHT_TEMPLE",
                        "intensity": 6,
                        "description": null
                    }
                ]
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/assessments/daily")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.assessmentId").exists())
            .andExpect(jsonPath("$.prescriptionCode").value("TEST0001"))
            .andExpect(jsonPath("$.weekNumber").value(3))  // 10/15는 10/1부터 3주차
    }

    @Test
    @DisplayName("POST /daily - 존재하지 않는 처방 (404 Not Found)")
    fun createDailyAssessment_PrescriptionNotFound_Returns404() {
        // 유효한 형식이지만 존재하지 않는 처방 코드 사용
        val request = """
            {
                "prescriptionCode": "ZZZZ9999",
                "assessmentDate": "2025-10-15",
                "painScore": 7,
                "stressScore": 5,
                "jawFunctionScore": 6,
                "painAreas": []
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/assessments/daily")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("처방을 찾을 수 없습니다. 처방 코드: ZZZZ9999"))
    }

    @Test
    @DisplayName("POST /daily - 중복 검사 (409 Conflict)")
    fun createDailyAssessment_DuplicateAssessment_Returns409() {
        // 첫 번째 검사 등록
        val request = """
            {
                "prescriptionCode": "TEST0001",
                "assessmentDate": "2025-10-15",
                "painScore": 7,
                "stressScore": 5,
                "jawFunctionScore": 6,
                "painAreas": []
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/assessments/daily")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
            .andExpect(status().isCreated)

        // 동일 날짜에 두 번째 검사 시도
        mockMvc.perform(
            post("/api/v1/assessments/daily")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("해당 날짜에 이미 검사가 등록되어 있습니다. 처방 코드: TEST0001, 날짜: 2025-10-15"))
    }

    @Test
    @DisplayName("POST /daily - 잘못된 요청 형식 (400 Bad Request)")
    fun createDailyAssessment_InvalidRequest_Returns400() {
        // painScore가 범위를 벗어남 (0~10)
        val request = """
            {
                "prescriptionCode": "TEST0001",
                "assessmentDate": "2025-10-15",
                "painScore": 15,
                "stressScore": 5,
                "jawFunctionScore": 6,
                "painAreas": []
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/assessments/daily")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("POST /daily - PENDING 상태 처방 (400 Bad Request)")
    fun createDailyAssessment_PendingPrescription_Returns400() {
        // PENDING 상태 처방 생성 (activatedAt = null)
        val pendingPrescription = Prescription(
            code = "PEND1234",
            createdAt = LocalDateTime.of(2025, 10, 15, 10, 0),
            activatedAt = null
        )
        prescriptionRepository.save(pendingPrescription)

        val request = """
            {
                "prescriptionCode": "PEND1234",
                "assessmentDate": "2025-10-16",
                "painScore": 5,
                "stressScore": 5,
                "jawFunctionScore": 5,
                "painAreas": []
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/assessments/daily")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("검사를 수행할 수 없는 처방 상태입니다. 현재 상태: PENDING, 처방 코드: PEND1234"))
    }

    @Test
    @DisplayName("POST /daily - 유효하지 않은 검사 일자 (400 Bad Request)")
    fun createDailyAssessment_InvalidAssessmentDate_Returns400() {
        // 활성화 이전 날짜로 검사 시도
        val request = """
            {
                "prescriptionCode": "TEST0001",
                "assessmentDate": "2025-09-30",
                "painScore": 5,
                "stressScore": 5,
                "jawFunctionScore": 5,
                "painAreas": []
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/assessments/daily")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("유효하지 않은 검사 일자입니다. 검사는 활성화 후 1~6주차(D+0 ~ D+41) 사이에만 가능합니다. 검사 일자: 2025-09-30"))
    }

    @Test
    @DisplayName("POST /daily - 필수 필드 빈 값 (400 Bad Request)")
    fun createDailyAssessment_BlankRequiredFields_Returns400() {
        // prescriptionCode가 빈 문자열
        val request = """
            {
                "prescriptionCode": "",
                "assessmentDate": "2025-10-15",
                "painScore": 7,
                "stressScore": 5,
                "jawFunctionScore": 6,
                "painAreas": []
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/assessments/daily")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("GET /weekly-trend - 정상 케이스 (200 OK)")
    fun getWeeklyTrend_ValidRequest_Returns200() {
        // 테스트 데이터 준비: 여러 주차의 검사 등록
        createAssessment("TEST0001", "2025-10-02", 8, 7, 4)  // 1주차
        createAssessment("TEST0001", "2025-10-09", 6, 5, 6)  // 2주차
        createAssessment("TEST0001", "2025-10-16", 4, 4, 7)  // 3주차

        mockMvc.perform(
            get("/api/v1/assessments/weekly-trend")
                .param("prescriptionCode", "TEST0001")
                .param("startWeek", "1")
                .param("endWeek", "3")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.prescriptionCode").value("TEST0001"))
            .andExpect(jsonPath("$.weeklyTrends").isArray)
            .andExpect(jsonPath("$.weeklyTrends.length()").value(3))
            .andExpect(jsonPath("$.weeklyTrends[0].weekNumber").value(1))
            .andExpect(jsonPath("$.weeklyTrends[1].weekNumber").value(2))
            .andExpect(jsonPath("$.weeklyTrends[2].weekNumber").value(3))
    }

    @Test
    @DisplayName("GET /weekly-trend - 존재하지 않는 처방 (404 Not Found)")
    fun getWeeklyTrend_PrescriptionNotFound_Returns404() {
        mockMvc.perform(
            get("/api/v1/assessments/weekly-trend")
                .param("prescriptionCode", "ZZZZ9999")
                .param("startWeek", "1")
                .param("endWeek", "3")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("처방을 찾을 수 없습니다. 처방 코드: ZZZZ9999"))
    }

    @Test
    @DisplayName("GET /weekly-trend - endWeek 생략 시 현재 주차 사용")
    fun getWeeklyTrend_NoEndWeek_UsesCurrentWeek() {
        createAssessment("TEST0001", "2025-10-02", 8, 7, 4)  // 1주차

        mockMvc.perform(
            get("/api/v1/assessments/weekly-trend")
                .param("prescriptionCode", "TEST0001")
                .param("startWeek", "1")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.prescriptionCode").value("TEST0001"))
    }

    @Test
    @DisplayName("GET /weekly-trend - 잘못된 주차 범위 (400 Bad Request)")
    fun getWeeklyTrend_InvalidWeekRange_Returns400() {
        mockMvc.perform(
            get("/api/v1/assessments/weekly-trend")
                .param("prescriptionCode", "TEST0001")
                .param("startWeek", "5")
                .param("endWeek", "2")  // endWeek < startWeek
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("POST /daily - 통증 부위 중복 (400 Bad Request)")
    fun createDailyAssessment_DuplicatePainAreas_Returns400() {
        val request = """
            {
                "prescriptionCode": "TEST0001",
                "assessmentDate": "2025-10-15",
                "painScore": 7,
                "stressScore": 5,
                "jawFunctionScore": 6,
                "painAreas": [
                    {
                        "location": "LEFT_JAW",
                        "intensity": 8,
                        "description": "첫 번째"
                    },
                    {
                        "location": "LEFT_JAW",
                        "intensity": 7,
                        "description": "중복"
                    }
                ]
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/assessments/daily")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("중복된 통증 부위가 있습니다: LEFT_JAW"))
    }

    @Test
    @DisplayName("POST /daily - 통증 부위 7개 (400 Bad Request)")
    fun createDailyAssessment_TooManyPainAreas_Returns400() {
        val request = """
            {
                "prescriptionCode": "TEST0001",
                "assessmentDate": "2025-10-15",
                "painScore": 7,
                "stressScore": 5,
                "jawFunctionScore": 6,
                "painAreas": [
                    {"location": "LEFT_JAW", "intensity": 8},
                    {"location": "RIGHT_JAW", "intensity": 7},
                    {"location": "LEFT_TEMPLE", "intensity": 6},
                    {"location": "RIGHT_TEMPLE", "intensity": 5},
                    {"location": "NECK", "intensity": 4},
                    {"location": "CHIN", "intensity": 3},
                    {"location": "LEFT_JAW", "intensity": 2}
                ]
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/assessments/daily")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
            .andExpect(status().isBadRequest)
    }

    // Helper 메서드
    private fun createAssessment(
        prescriptionCode: String,
        date: String,
        painScore: Int,
        stressScore: Int,
        jawFunctionScore: Int
    ) {
        val request = """
            {
                "prescriptionCode": "$prescriptionCode",
                "assessmentDate": "$date",
                "painScore": $painScore,
                "stressScore": $stressScore,
                "jawFunctionScore": $jawFunctionScore,
                "painAreas": []
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/assessments/daily")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
    }
}
