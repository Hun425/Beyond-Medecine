
### 3.4. AssessmentAnalysisService 구현 (TDD)

#### ☐ Task 3-4-1: AssessmentAnalysisService 테스트 작성
```kotlin
// 파일: src/test/kotlin/com/beyondmedicine/application/service/AssessmentAnalysisServiceTest.kt

package com.beyondmedicine.application.service

import com.beyondmedicine.application.dto.*
import com.beyondmedicine.application.exception.PrescriptionNotFoundException
import com.beyondmedicine.application.repository.DailyAssessmentRepository
import com.beyondmedicine.application.repository.PrescriptionRepository
import com.beyondmedicine.domain.entity.Prescription
import com.beyondmedicine.domain.model.PainLocation
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("AssessmentAnalysisService 단위 테스트")
class AssessmentAnalysisServiceTest {

    @Mock
    private lateinit var prescriptionRepository: PrescriptionRepository

    @Mock
    private lateinit var dailyAssessmentRepository: DailyAssessmentRepository

    @InjectMocks
    private lateinit var assessmentAnalysisService: AssessmentAnalysisService

    @Test
    @DisplayName("주차별 추이 분석 - 정상 케이스")
    fun analyzeWeeklyTrend_ValidQuery_ReturnsData() {
        // given
        val prescription = createTestPrescription()
        val query = WeeklyTrendQuery(
            prescriptionCode = "ABCD1234",
            startWeek = 1,
            endWeek = 3
        )

        val weeklyAggregations = listOf(
            WeeklyAggregation(1, 7.0, 6.0, 5.0, 2),
            WeeklyAggregation(2, 6.0, 5.0, 6.0, 2),
            WeeklyAggregation(3, 5.0, 4.0, 7.0, 1)
        )

        val topPainAreas = listOf(
            PainAreaAggregation(PainLocation.LEFT_JAW, 3, 7.0),
            PainAreaAggregation(PainLocation.RIGHT_TEMPLE, 2, 6.0)
        )

        whenever(prescriptionRepository.findByCode(query.prescriptionCode))
            .thenReturn(prescription)
        whenever(dailyAssessmentRepository.findWeeklyAggregations(any(), any(), any()))
            .thenReturn(weeklyAggregations)
        whenever(dailyAssessmentRepository.findTopPainAreas(any(), any(), any()))
            .thenReturn(topPainAreas)

        // when
        val result = assessmentAnalysisService.analyzeWeeklyTrend(query)

        // then
        assertNotNull(result)
        assertEquals("ABCD1234", result.prescriptionCode)
        assertEquals(1, result.startWeek)
        assertEquals(3, result.endWeek)
        assertEquals(3, result.weeklyTrends.size)
        assertEquals(2, result.topPainAreas.size)
    }

    @Test
    @DisplayName("처방이 없으면 PrescriptionNotFoundException")
    fun analyzeWeeklyTrend_PrescriptionNotFound_ThrowsException() {
        // given
        val query = WeeklyTrendQuery(
            prescriptionCode = "NOTEXIST",
            startWeek = 1,
            endWeek = 6
        )

        whenever(prescriptionRepository.findByCode(query.prescriptionCode))
            .thenReturn(null)

        // when & then
        assertThrows<PrescriptionNotFoundException> {
            assessmentAnalysisService.analyzeWeeklyTrend(query)
        }
    }

    @Test
    @DisplayName("데이터가 없으면 빈 응답 반환")
    fun analyzeWeeklyTrend_NoData_ReturnsEmptyData() {
        // given
        val prescription = createTestPrescription()
        val query = WeeklyTrendQuery(
            prescriptionCode = "ABCD1234",
            startWeek = 1,
            endWeek = 6
        )

        whenever(prescriptionRepository.findByCode(query.prescriptionCode))
            .thenReturn(prescription)
        whenever(dailyAssessmentRepository.findWeeklyAggregations(any(), any(), any()))
            .thenReturn(emptyList())

        // when
        val result = assessmentAnalysisService.analyzeWeeklyTrend(query)

        // then
        assertTrue(result.weeklyTrends.isEmpty())
        assertTrue(result.topPainAreas.isEmpty())
    }

    @Test
    @DisplayName("주차 범위 검증 - startWeek > endWeek")
    fun analyzeWeeklyTrend_InvalidWeekRange_ThrowsException() {
        // given
        val prescription = createTestPrescription()
        val query = WeeklyTrendQuery(
            prescriptionCode = "ABCD1234",
            startWeek = 5,
            endWeek = 2  // 잘못된 범위
        )

        whenever(prescriptionRepository.findByCode(query.prescriptionCode))
            .thenReturn(prescription)

        // when & then
        assertThrows<IllegalArgumentException> {
            assessmentAnalysisService.analyzeWeeklyTrend(query)
        }
    }

    private fun createTestPrescription(): Prescription {
        return Prescription(
            code = "ABCD1234",
            createdAt = LocalDateTime.of(2025, 9, 25, 10, 0),
            activatedAt = LocalDateTime.of(2025, 10, 1, 9, 0)
        )
    }
}
```

**검증:**
```bash
# 테스트 실행 (실패해야 함)
./gradlew test --tests AssessmentAnalysisServiceTest
```

#### ☐ Task 3-4-2: AssessmentAnalysisService 구현
```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/application/service/AssessmentAnalysisService.kt

package com.beyondmedicine.application.service

import com.beyondmedicine.application.dto.*
import com.beyondmedicine.application.exception.PrescriptionNotFoundException
import com.beyondmedicine.application.repository.DailyAssessmentRepository
import com.beyondmedicine.application.repository.PrescriptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 검사 분석 서비스
 * - 참고: docs/service-layer-design.md
 * - 과제 PDF 7-8페이지
 */
@Service
@Transactional(readOnly = true)
class AssessmentAnalysisService(
    private val prescriptionRepository: PrescriptionRepository,
    private val dailyAssessmentRepository: DailyAssessmentRepository
) {

    /**
     * 주차별 추이 분석
     */
    fun analyzeWeeklyTrend(query: WeeklyTrendQuery): WeeklyTrendData {
        // 1. 처방 조회
        val prescription = prescriptionRepository.findByCode(query.prescriptionCode)
            ?: throw PrescriptionNotFoundException(
                "처방을 찾을 수 없습니다. 처방 코드: ${query.prescriptionCode}"
            )

        // 2. 주차 범위 검증
        validateWeekRange(query.startWeek, query.endWeek)

        // 3. 주차별 집계 데이터 조회
        val weeklyAggregations = dailyAssessmentRepository.findWeeklyAggregations(
            prescription = prescription,
            startWeek = query.startWeek,
            endWeek = query.endWeek
        )

        // 4. 데이터 없는 경우 빈 응답 반환
        if (weeklyAggregations.isEmpty()) {
            return WeeklyTrendData(
                prescriptionCode = prescription.code,
                startWeek = query.startWeek,
                endWeek = query.endWeek,
                weeklyTrends = emptyList(),
                topPainAreas = emptyList()
            )
        }

        // 5. 주차별 데이터 변환
        val weeklyTrends = weeklyAggregations.map { agg ->
            WeeklyData(
                weekNumber = agg.weekNumber,
                averagePainScore = agg.averagePainScore,
                averageStressScore = agg.averageStressScore,
                averageJawFunctionScore = agg.averageJawFunctionScore,
                assessmentCount = agg.assessmentCount
            )
        }

        // 6. Top 3 통증 부위 집계
        val topPainAreasAgg = dailyAssessmentRepository.findTopPainAreas(
            prescription = prescription,
            startWeek = query.startWeek,
            endWeek = query.endWeek
        )

        val topPainAreas = topPainAreasAgg.take(3).map { agg ->
            TopPainAreaData(
                location = agg.location,
                count = agg.count,
                averageIntensity = agg.averageIntensity
            )
        }

        return WeeklyTrendData(
            prescriptionCode = prescription.code,
            startWeek = query.startWeek,
            endWeek = query.endWeek,
            weeklyTrends = weeklyTrends,
            topPainAreas = topPainAreas
        )
    }

    /**
     * 주차 범위 검증
     */
    private fun validateWeekRange(startWeek: Int, endWeek: Int) {
        require(startWeek in 1..6) {
            "시작 주차는 1~6 사이여야 합니다. 입력값: $startWeek"
        }
        require(endWeek in 1..6) {
            "종료 주차는 1~6 사이여야 합니다. 입력값: $endWeek"
        }
        require(startWeek <= endWeek) {
            "시작 주차는 종료 주차보다 작거나 같아야 합니다. " +
            "시작: $startWeek, 종료: $endWeek"
        }
    }
}
```

**검증:**
```bash
# 테스트 실행 (모두 통과해야 함)
./gradlew test --tests AssessmentAnalysisServiceTest

# 결과: 4 tests passed
```

**커밋:**
```bash
git add src/test/kotlin/com/beyondmedicine/application/service/AssessmentAnalysisServiceTest.kt
git add src/main/kotlin/com/beyondmedicine/application/service/AssessmentAnalysisService.kt
git commit -m "test(service): AssessmentAnalysisService 테스트 추가

feat(service): AssessmentAnalysisService 구현
- 주차별 추이 분석 로직
- 주차 범위 검증 (1~6, start <= end)
- Top 3 통증 부위 집계
- 데이터 없는 경우 빈 응답"
```

---

### Phase 3 완료 체크리스트

```bash
# 전체 Service 테스트 실행
./gradlew test --tests "com.beyondmedicine.application.service.*"

# 예상 결과:
# AssessmentServiceTest: 5 tests passed
# AssessmentAnalysisServiceTest: 4 tests passed
# Total: 9 tests passed
```

**Phase 3 완료 커밋:**
```bash
git add .
git commit -m "feat(service): Phase 3 완료 - Service 계층 구현

- AssessmentService: 일일 검사 등록 비즈니스 로직
- AssessmentAnalysisService: 주차별 추이 분석
- Custom Exception: 4개 (404, 400, 409)
- 모든 테스트 통과 (9 tests)"
```

---

---

## Phase 4: DTO & Mapper

**목표:** Presentation Layer DTO 및 변환 로직 구현
**예상 시간:** 2-3시간
**참고 문서:** `docs/api-design.md`

### 4.1. Presentation DTO 정의 (Request/Response)

#### ☐ Task 4-1-1: CreateDailyAssessmentRequest DTO
```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/presentation/dto/request/CreateDailyAssessmentRequest.kt

package com.beyondmedicine.presentation.dto.request

import com.fasterxml.jackson.annotation.JsonFormat
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import java.time.LocalDate

/**
 * 일일 검사 등록 요청 DTO
 * - 참고: docs/api-design.md
 */
data class CreateDailyAssessmentRequest(
    @field:NotBlank(message = "처방 코드는 필수입니다")
    @field:Size(min = 8, max = 8, message = "처방 코드는 8자리여야 합니다")
    val prescriptionCode: String,

    @field:NotNull(message = "검사 일자는 필수입니다")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val assessmentDate: LocalDate,

    @field:NotNull(message = "통증 점수는 필수입니다")
    @field:Min(value = 0, message = "통증 점수는 0 이상이어야 합니다")
    @field:Max(value = 10, message = "통증 점수는 10 이하여야 합니다")
    val painScore: Int,

    @field:NotNull(message = "스트레스 점수는 필수입니다")
    @field:Min(value = 0, message = "스트레스 점수는 0 이상이어야 합니다")
    @field:Max(value = 10, message = "스트레스 점수는 10 이하여야 합니다")
    val stressScore: Int,

    @field:NotNull(message = "턱 기능 점수는 필수입니다")
    @field:Min(value = 0, message = "턱 기능 점수는 0 이상이어야 합니다")
    @field:Max(value = 10, message = "턱 기능 점수는 10 이하여야 합니다")
    val jawFunctionScore: Int,

    @field:Valid
    @field:Size(max = 6, message = "통증 부위는 최대 6개까지 등록 가능합니다")
    val painAreas: List<PainAreaRequest> = emptyList()
)

/**
 * 통증 부위 요청 DTO
 */
data class PainAreaRequest(
    @field:NotBlank(message = "통증 부위는 필수입니다")
    val location: String,

    @field:NotNull(message = "통증 강도는 필수입니다")
    @field:Min(value = 0, message = "통증 강도는 0 이상이어야 합니다")
    @field:Max(value = 10, message = "통증 강도는 10 이하여야 합니다")
    val intensity: Int,

    @field:Size(max = 500, message = "부연 설명은 500자 이하여야 합니다")
    val description: String? = null
)
```

#### ☐ Task 4-1-2: CreateDailyAssessmentResponse DTO
```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/presentation/dto/response/CreateDailyAssessmentResponse.kt

package com.beyondmedicine.presentation.dto.response

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

/**
 * 일일 검사 등록 응답 DTO
 */
data class CreateDailyAssessmentResponse(
    val assessmentId: String,
    val prescriptionCode: String,
    val weekNumber: Int,

    @JsonFormat(pattern = "yyyy-MM-dd")
    val assessmentDate: LocalDate
)
```

#### ☐ Task 4-1-3: WeeklyTrendRequest DTO
```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/presentation/dto/request/WeeklyTrendRequest.kt

package com.beyondmedicine.presentation.dto.request

import jakarta.validation.constraints.*

/**
 * 주차별 추이 분석 요청
 */
data class WeeklyTrendRequest(
    @field:NotBlank(message = "처방 코드는 필수입니다")
    @field:Size(min = 8, max = 8, message = "처방 코드는 8자리여야 합니다")
    val prescriptionCode: String,

    @field:Min(value = 1, message = "시작 주차는 1 이상이어야 합니다")
    @field:Max(value = 6, message = "시작 주차는 6 이하여야 합니다")
    val startWeek: Int? = null,

    @field:Min(value = 1, message = "종료 주차는 1 이상이어야 합니다")
    @field:Max(value = 6, message = "종료 주차는 6 이하여야 합니다")
    val endWeek: Int? = null
)
```

#### ☐ Task 4-1-4: WeeklyTrendResponse DTO
```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/presentation/dto/response/WeeklyTrendResponse.kt

package com.beyondmedicine.presentation.dto.response

/**
 * 주차별 추이 분석 응답
 */
data class WeeklyTrendResponse(
    val prescriptionCode: String,
    val period: PeriodInfo,
    val weeklyTrends: List<WeeklyTrendInfo>,
    val topPainAreas: List<TopPainAreaInfo>
)

data class PeriodInfo(
    val startWeek: Int,
    val endWeek: Int
)

data class WeeklyTrendInfo(
    val weekNumber: Int,
    val averagePainScore: Double,
    val averageStressScore: Double,
    val averageJawFunctionScore: Double,
    val changeRates: ChangeRateInfo?
)

data class ChangeRateInfo(
    val pain: Double,
    val stress: Double,
    val jawFunction: Double
)

data class TopPainAreaInfo(
    val location: String,
    val locationName: String,
    val count: Long,
    val averageIntensity: Double
)
```

**커밋:**
```bash
git add src/main/kotlin/com/beyondmedicine/presentation/dto/
git commit -m "feat(dto): Presentation Layer DTO 정의

- CreateDailyAssessmentRequest/Response
- WeeklyTrendRequest/Response
- Bean Validation 적용"
```

---

### 4.2. Custom Validator 구현

#### ☐ Task 4-2-1: PrescriptionCodeValidator
```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/common/validator/PrescriptionCodeValidator.kt

package com.beyondmedicine.common.validator

/**
 * 처방 코드 검증 유틸리티
 * - 영대문자 4자 + 숫자 4자 (순서 무관)
 */
object PrescriptionCodeValidator {

    private const val EXPECTED_LENGTH = 8
    private const val EXPECTED_UPPER_COUNT = 4
    private const val EXPECTED_DIGIT_COUNT = 4

    fun validate(code: String): Boolean {
        if (code.length != EXPECTED_LENGTH) return false

        val upperCount = code.count { it in 'A'..'Z' }
        val digitCount = code.count { it.isDigit() }

        return upperCount == EXPECTED_UPPER_COUNT && digitCount == EXPECTED_DIGIT_COUNT
    }

    fun validateOrThrow(code: String) {
        require(validate(code)) {
            "처방 코드는 영대문자 ${EXPECTED_UPPER_COUNT}자 + " +
            "숫자 ${EXPECTED_DIGIT_COUNT}자 조합이어야 합니다. " +
            "입력값: $code"
        }
    }
}
```

#### ☐ Task 4-2-2: PainLocationValidator
```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/common/validator/PainLocationValidator.kt

package com.beyondmedicine.common.validator

import com.beyondmedicine.domain.model.PainLocation

/**
 * 통증 부위 검증 유틸리티
 */
object PainLocationValidator {

    fun isValid(location: String): Boolean {
        return try {
            PainLocation.valueOf(location)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    fun validateOrThrow(location: String) {
        require(isValid(location)) {
            "유효하지 않은 통증 부위입니다. " +
            "입력값: $location, " +
            "허용값: ${PainLocation.values().joinToString { it.name }}"
        }
    }

    fun getAllowedLocations(): List<String> {
        return PainLocation.values().map { it.name }
    }
}
```

**커밋:**
```bash
git add src/main/kotlin/com/beyondmedicine/common/validator/
git commit -m "feat(validator): Custom Validator 구현

- PrescriptionCodeValidator: 처방 코드 검증
- PainLocationValidator: 통증 부위 검증"
```

---

### 4.3. DTO Mapper 구현 (TDD)

#### ☐ Task 4-3-1: AssessmentDtoMapper 테스트 작성
```kotlin
// 파일: src/test/kotlin/com/beyondmedicine/presentation/dto/mapper/AssessmentDtoMapperTest.kt

package com.beyondmedicine.presentation.dto.mapper

import com.beyondmedicine.application.dto.*
import com.beyondmedicine.domain.model.PainLocation
import com.beyondmedicine.presentation.dto.request.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import java.time.LocalDate

@DisplayName("AssessmentDtoMapper 테스트")
class AssessmentDtoMapperTest {

    private val mapper = AssessmentDtoMapper()

    @Test
    @DisplayName("Request DTO를 Service Command로 변환")
    fun toServiceCommand_ValidRequest_ReturnsCommand() {
        // given
        val request = CreateDailyAssessmentRequest(
            prescriptionCode = "ABCD1234",
            assessmentDate = LocalDate.of(2025, 10, 15),
            painScore = 7,
            stressScore = 5,
            jawFunctionScore = 6,
            painAreas = listOf(
                PainAreaRequest("LEFT_JAW", 8, "씹을 때 통증")
            )
        )

        // when
        val command = mapper.toServiceCommand(request)

        // then
        assertEquals("ABCD1234", command.prescriptionCode)
        assertEquals(7, command.painScore)
        assertEquals(1, command.painAreas.size)
        assertEquals(PainLocation.LEFT_JAW, command.painAreas[0].location)
    }

    @Test
    @DisplayName("유효하지 않은 처방 코드는 예외 발생")
    fun toServiceCommand_InvalidCode_ThrowsException() {
        // given
        val request = CreateDailyAssessmentRequest(
            prescriptionCode = "INVALID",
            assessmentDate = LocalDate.now(),
            painScore = 7,
            stressScore = 5,
            jawFunctionScore = 6,
            painAreas = emptyList()
        )

        // when & then
        assertThrows<IllegalArgumentException> {
            mapper.toServiceCommand(request)
        }
    }

    @Test
    @DisplayName("Result를 Response DTO로 변환")
    fun toResponse_ValidResult_ReturnsResponse() {
        // given
        val result = DailyAssessmentResult(
            assessmentId = 123L,
            prescriptionCode = "ABCD1234",
            weekNumber = 3,
            assessmentDate = LocalDate.of(2025, 10, 15)
        )

        // when
        val response = mapper.toResponse(result)

        // then
        assertEquals("123", response.assessmentId)
        assertEquals("ABCD1234", response.prescriptionCode)
        assertEquals(3, response.weekNumber)
    }

    @Test
    @DisplayName("WeeklyTrendData를 Response로 변환 - 변화율 포함")
    fun toWeeklyTrendResponse_WithChangeRates_ReturnsResponse() {
        // given
        val data = WeeklyTrendData(
            prescriptionCode = "ABCD1234",
            startWeek = 1,
            endWeek = 2,
            weeklyTrends = listOf(
                WeeklyData(1, 7.0, 6.0, 5.0, 2),
                WeeklyData(2, 6.0, 5.0, 6.0, 2)
            ),
            topPainAreas = listOf(
                TopPainAreaData(PainLocation.LEFT_JAW, 3, 7.0)
            )
        )

        // when
        val response = mapper.toWeeklyTrendResponse(data)

        // then
        assertEquals(2, response.weeklyTrends.size)
        assertNull(response.weeklyTrends[0].changeRates) // 첫 주차
        assertNotNull(response.weeklyTrends[1].changeRates) // 두 번째 주차

        // 변화율 검증: 통증 7.0 → 6.0 = 14.3% 호전
        assertEquals(14.3, response.weeklyTrends[1].changeRates?.pain, 0.1)
    }
}
```

**검증:**
```bash
./gradlew test --tests AssessmentDtoMapperTest
```

#### ☐ Task 4-3-2: AssessmentDtoMapper 구현
```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/presentation/dto/mapper/AssessmentDtoMapper.kt

package com.beyondmedicine.presentation.dto.mapper

import com.beyondmedicine.application.dto.*
import com.beyondmedicine.common.validator.PainLocationValidator
import com.beyondmedicine.common.validator.PrescriptionCodeValidator
import com.beyondmedicine.domain.calculator.ChangeRateCalculator
import com.beyondmedicine.domain.model.PainLocation
import com.beyondmedicine.presentation.dto.request.*
import com.beyondmedicine.presentation.dto.response.*
import org.springframework.stereotype.Component

/**
 * DTO 변환 Mapper
 * - API DTO ↔ Service Command/Result 변환
 */
@Component
class AssessmentDtoMapper {

    /**
     * Request DTO → Command 변환
     */
    fun toServiceCommand(request: CreateDailyAssessmentRequest): CreateDailyAssessmentCommand {
        // 추가 검증
        PrescriptionCodeValidator.validateOrThrow(request.prescriptionCode)

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
        PainLocationValidator.validateOrThrow(request.location)

        return PainAreaCommand(
            location = PainLocation.valueOf(request.location),
            intensity = request.intensity,
            description = request.description
        )
    }

    /**
     * Result → Response DTO 변환
     */
    fun toResponse(result: DailyAssessmentResult): CreateDailyAssessmentResponse {
        return CreateDailyAssessmentResponse(
            assessmentId = result.assessmentId.toString(),
            prescriptionCode = result.prescriptionCode,
            weekNumber = result.weekNumber,
            assessmentDate = result.assessmentDate
        )
    }

    /**
     * Request → Query 변환 (기본값 처리)
     */
    fun toQuery(request: WeeklyTrendRequest, currentWeek: Int): WeeklyTrendQuery {
        PrescriptionCodeValidator.validateOrThrow(request.prescriptionCode)

        return WeeklyTrendQuery(
            prescriptionCode = request.prescriptionCode,
            startWeek = request.startWeek ?: 1,
            endWeek = request.endWeek ?: currentWeek
        )
    }

    /**
     * WeeklyTrendData → Response DTO 변환
     */
    fun toWeeklyTrendResponse(data: WeeklyTrendData): WeeklyTrendResponse {
        return WeeklyTrendResponse(
            prescriptionCode = data.prescriptionCode,
            period = PeriodInfo(
                startWeek = data.startWeek,
                endWeek = data.endWeek
            ),
            weeklyTrends = toWeeklyTrendInfoList(data.weeklyTrends),
            topPainAreas = data.topPainAreas.map { toTopPainAreaInfo(it) }
        )
    }

    /**
     * 주차별 데이터 변환 (변화율 계산 포함)
     */
    private fun toWeeklyTrendInfoList(weeklyDataList: List<WeeklyData>): List<WeeklyTrendInfo> {
        return weeklyDataList.mapIndexed { index, current ->
            WeeklyTrendInfo(
                weekNumber = current.weekNumber,
                averagePainScore = current.averagePainScore,
                averageStressScore = current.averageStressScore,
                averageJawFunctionScore = current.averageJawFunctionScore,
                changeRates = if (index > 0) {
                    val previous = weeklyDataList[index - 1]
                    calculateChangeRates(previous, current)
                } else null
            )
        }
    }

    /**
     * 변화율 계산
     */
    private fun calculateChangeRates(previous: WeeklyData, current: WeeklyData): ChangeRateInfo {
        return ChangeRateInfo(
            pain = ChangeRateCalculator.calculate(
                previous = previous.averagePainScore,
                current = current.averagePainScore,
                isHigherBetter = false
            ),
            stress = ChangeRateCalculator.calculate(
                previous = previous.averageStressScore,
                current = current.averageStressScore,
                isHigherBetter = false
            ),
            jawFunction = ChangeRateCalculator.calculate(
                previous = previous.averageJawFunctionScore,
                current = current.averageJawFunctionScore,
                isHigherBetter = true
            )
        )
    }

    /**
     * TopPainAreaData → TopPainAreaInfo 변환
     */
    private fun toTopPainAreaInfo(data: TopPainAreaData): TopPainAreaInfo {
        return TopPainAreaInfo(
            location = data.location.name,
            locationName = data.location.koreanName,
            count = data.count,
            averageIntensity = data.averageIntensity
        )
    }
}
```

**검증:**
```bash
./gradlew test --tests AssessmentDtoMapperTest

# 결과: 4 tests passed
```

**커밋:**
```bash
git add src/test/kotlin/com/beyondmedicine/presentation/dto/mapper/AssessmentDtoMapperTest.kt
git add src/main/kotlin/com/beyondmedicine/presentation/dto/mapper/AssessmentDtoMapper.kt
git commit -m "test(mapper): AssessmentDtoMapper 테스트 추가

feat(mapper): AssessmentDtoMapper 구현
- Request DTO → Service Command 변환
- Result → Response DTO 변환
- 변화율 계산 로직 (ChangeRateCalculator 활용)"
```

---

### Phase 4 완료 체크리스트

```bash
# 전체 Mapper 테스트 실행
./gradlew test --tests "com.beyondmedicine.presentation.dto.mapper.*"

# 예상 결과:
# AssessmentDtoMapperTest: 4 tests passed
```

**Phase 4 완료 커밋:**
```bash
git add .
git commit -m "feat(dto): Phase 4 완료 - DTO & Mapper 구현

- Presentation DTO: Request/Response
- Custom Validator: 처방 코드, 통증 부위
- DTO Mapper: 변환 로직 + 변화율 계산
- 모든 테스트 통과 (4 tests)"
```

---

## Phase 5: Controller 계층

**목표:** REST API 엔드포인트 구현
**예상 시간:** 2-3시간
**참고 문서:** `docs/api-design.md`

### 5.1. GlobalExceptionHandler 구현

#### ☐ Task 5-1-1: ErrorResponse DTO
```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/presentation/dto/response/ErrorResponse.kt

package com.beyondmedicine.presentation.dto.response

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

/**
 * 표준 에러 응답
 */
data class ErrorResponse(
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val timestamp: LocalDateTime,
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val validationErrors: List<ValidationError>? = null
)

data class ValidationError(
    val field: String,
    val rejectedValue: Any?,
    val message: String
)
```

#### ☐ Task 5-1-2: GlobalExceptionHandler 구현
```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/presentation/exception/GlobalExceptionHandler.kt

package com.beyondmedicine.presentation.exception

import com.beyondmedicine.application.exception.*
import com.beyondmedicine.presentation.dto.response.ErrorResponse
import com.beyondmedicine.presentation.dto.response.ValidationError
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import java.time.LocalDateTime

/**
 * 전역 예외 처리 핸들러
 * - 참고: docs/exception-handling-design.md
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    /**
     * 처방 없음 (404)
     */
    @ExceptionHandler(PrescriptionNotFoundException::class)
    fun handlePrescriptionNotFound(
        ex: PrescriptionNotFoundException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.NOT_FOUND.value(),
            error = "Not Found",
            message = ex.message ?: "처방을 찾을 수 없습니다",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }

    /**
     * 처방 상태 오류 (400)
     */
    @ExceptionHandler(InvalidPrescriptionStatusException::class)
    fun handleInvalidPrescriptionStatus(
        ex: InvalidPrescriptionStatusException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = ex.message ?: "유효하지 않은 처방 상태입니다",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    /**
     * 중복 검사 (409)
     */
    @ExceptionHandler(DuplicateAssessmentException::class)
    fun handleDuplicateAssessment(
        ex: DuplicateAssessmentException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.CONFLICT.value(),
            error = "Conflict",
            message = ex.message ?: "중복된 검사입니다",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse)
    }

    /**
     * 검사 일자 오류 (400)
     */
    @ExceptionHandler(InvalidAssessmentDateException::class)
    fun handleInvalidAssessmentDate(
        ex: InvalidAssessmentDateException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = ex.message ?: "유효하지 않은 검사 일자입니다",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    /**
     * Bean Validation 오류 (400)
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val validationErrors = ex.bindingResult.fieldErrors.map { error ->
            ValidationError(
                field = error.field,
                rejectedValue = error.rejectedValue,
                message = error.defaultMessage ?: "유효하지 않은 값입니다"
            )
        }

        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = "입력 값 검증 실패",
            path = request.getDescription(false).removePrefix("uri="),
            validationErrors = validationErrors
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    /**
     * IllegalArgumentException (400)
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        ex: IllegalArgumentException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = ex.message ?: "잘못된 요청입니다",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    /**
     * 기타 예외 (500)
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "서버 내부 오류가 발생했습니다",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
}
```

**커밋:**
```bash
git add src/main/kotlin/com/beyondmedicine/presentation/exception/
git commit -m "feat(exception): GlobalExceptionHandler 구현

- ErrorResponse DTO
- Custom Exception → HTTP Status 매핑
- Bean Validation 오류 처리"
```

---

### 5.2. AssessmentController 구현 (TDD)

#### ☐ Task 5-2-1: AssessmentController 테스트 작성
```kotlin
// 파일: src/test/kotlin/com/beyondmedicine/presentation/controller/AssessmentControllerTest.kt

package com.beyondmedicine.presentation.controller

import com.beyondmedicine.application.dto.*
import com.beyondmedicine.application.service.AssessmentService
import com.beyondmedicine.presentation.dto.mapper.AssessmentDtoMapper
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDate

@WebMvcTest(AssessmentController::class)
@DisplayName("AssessmentController 테스트")
class AssessmentControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var assessmentService: AssessmentService

    @MockBean
    private lateinit var dtoMapper: AssessmentDtoMapper

    @Test
    @DisplayName("일일 검사 정상 등록 - 201 Created")
    fun createAssessment_ValidRequest_Returns201() {
        // given
        val requestBody = """
            {
              "prescriptionCode": "ABCD1234",
              "assessmentDate": "2025-10-15",
              "painScore": 7,
              "stressScore": 5,
              "jawFunctionScore": 6,
              "painAreas": []
            }
        """.trimIndent()

        val result = DailyAssessmentResult(
            assessmentId = 123L,
            prescriptionCode = "ABCD1234",
            weekNumber = 3,
            assessmentDate = LocalDate.of(2025, 10, 15)
        )

        whenever(dtoMapper.toServiceCommand(any())).thenReturn(mock())
        whenever(assessmentService.createAssessment(any())).thenReturn(result)
        whenever(dtoMapper.toResponse(any())).thenCallRealMethod()

        // when & then
        mockMvc.perform(
            post("/api/v1/assessments/daily")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.prescriptionCode").value("ABCD1234"))
            .andExpect(jsonPath("$.weekNumber").value(3))
    }

    @Test
    @DisplayName("Bean Validation 실패 - 400 Bad Request")
    fun createAssessment_InvalidRequest_Returns400() {
        // given: painScore = 11 (범위 초과)
        val requestBody = """
            {
              "prescriptionCode": "ABCD1234",
              "assessmentDate": "2025-10-15",
              "painScore": 11,
              "stressScore": 5,
              "jawFunctionScore": 6,
              "painAreas": []
            }
        """.trimIndent()

        // when & then
        mockMvc.perform(
            post("/api/v1/assessments/daily")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").exists())
    }
}
```

**검증:**
```bash
./gradlew test --tests AssessmentControllerTest
```

#### ☐ Task 5-2-2: AssessmentController 구현
```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/presentation/controller/AssessmentController.kt

package com.beyondmedicine.presentation.controller

import com.beyondmedicine.application.service.AssessmentService
import com.beyondmedicine.application.service.AssessmentAnalysisService
import com.beyondmedicine.application.repository.PrescriptionRepository
import com.beyondmedicine.presentation.dto.mapper.AssessmentDtoMapper
import com.beyondmedicine.presentation.dto.request.CreateDailyAssessmentRequest
import com.beyondmedicine.presentation.dto.request.WeeklyTrendRequest
import com.beyondmedicine.presentation.dto.response.CreateDailyAssessmentResponse
import com.beyondmedicine.presentation.dto.response.WeeklyTrendResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

/**
 * 일일 검사 API 컨트롤러
 * - 참고: docs/api-design.md
 */
@RestController
@RequestMapping("/api/v1/assessments")
class AssessmentController(
    private val assessmentService: AssessmentService,
    private val assessmentAnalysisService: AssessmentAnalysisService,
    private val prescriptionRepository: PrescriptionRepository,
    private val dtoMapper: AssessmentDtoMapper
) {

    /**
     * 일일 검사 등록
     * POST /api/v1/assessments/daily
     */
    @PostMapping("/daily")
    fun createAssessment(
        @Valid @RequestBody request: CreateDailyAssessmentRequest
    ): ResponseEntity<CreateDailyAssessmentResponse> {
        // 1. DTO 변환
        val command = dtoMapper.toServiceCommand(request)

        // 2. Service 호출
        val result = assessmentService.createAssessment(command)

        // 3. Response 변환
        val response = dtoMapper.toResponse(result)

        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    /**
     * 주차별 추이 분석
     * GET /api/v1/assessments/weekly-trend
     */
    @GetMapping("/weekly-trend")
    fun getWeeklyTrend(
        @Valid @ModelAttribute request: WeeklyTrendRequest
    ): ResponseEntity<WeeklyTrendResponse> {
        // 1. 현재 주차 계산
        val prescription = prescriptionRepository.findByCode(request.prescriptionCode)
            ?: throw com.beyondmedicine.application.exception.PrescriptionNotFoundException(
                "처방을 찾을 수 없습니다. 처방 코드: ${request.prescriptionCode}"
            )

        val currentWeek = prescription.getCurrentWeek(LocalDate.now()) ?: 6

        // 2. DTO 변환
        val query = dtoMapper.toQuery(request, currentWeek)

        // 3. Service 호출
        val data = assessmentAnalysisService.analyzeWeeklyTrend(query)

        // 4. Response 변환
        val response = dtoMapper.toWeeklyTrendResponse(data)

        return ResponseEntity.ok(response)
    }
}
```

**검증:**
```bash
./gradlew test --tests AssessmentControllerTest

# 결과: 2 tests passed
```

**커밋:**
```bash
git add src/test/kotlin/com/beyondmedicine/presentation/controller/AssessmentControllerTest.kt
git add src/main/kotlin/com/beyondmedicine/presentation/controller/AssessmentController.kt
git commit -m "test(controller): AssessmentController 테스트 추가

feat(controller): AssessmentController 구현
- POST /api/v1/assessments/daily
- GET /api/v1/assessments/weekly-trend
- Bean Validation 적용"
```

---

### Phase 5 완료 체크리스트

```bash
# 전체 Controller 테스트 실행
./gradlew test --tests "com.beyondmedicine.presentation.controller.*"

# 예상 결과:
# AssessmentControllerTest: 2 tests passed
```

**Phase 5 완료 커밋:**
```bash
git add .
git commit -m "feat(controller): Phase 5 완료 - Controller 계층 구현

- AssessmentController: REST API 엔드포인트
- GlobalExceptionHandler: 전역 예외 처리
- 모든 테스트 통과 (2 tests)"
```

---

## Phase 6: 예외 처리 & 통합 테스트

**목표:** 통합 테스트 및 최종 검증
**예상 시간:** 2-3시간

### 6.1. 통합 테스트 (E2E)

#### ☐ Task 6-1-1: AssessmentIntegrationTest 작성
```kotlin
// 파일: src/test/kotlin/com/beyondmedicine/integration/AssessmentIntegrationTest.kt

package com.beyondmedicine.integration

import com.beyondmedicine.application.repository.PrescriptionRepository
import com.beyondmedicine.domain.entity.Prescription
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("일일 검사 통합 테스트")
class AssessmentIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var prescriptionRepository: PrescriptionRepository

    private lateinit var testPrescription: Prescription

    @BeforeEach
    fun setUp() {
        // 테스트용 처방 생성
        testPrescription = Prescription(
            code = "TEST1234",
            createdAt = LocalDateTime.now().minusDays(10),
            activatedAt = LocalDateTime.now().minusDays(5)
        )
        prescriptionRepository.save(testPrescription)
    }

    @Test
    @DisplayName("일일 검사 등록 → 주차별 추이 조회 (전체 플로우)")
    fun fullFlow_CreateAssessmentAndAnalyzeTrend_Success() {
        // 1. 일일 검사 등록
        val requestBody = """
            {
              "prescriptionCode": "TEST1234",
              "assessmentDate": "2025-10-15",
              "painScore": 7,
              "stressScore": 5,
              "jawFunctionScore": 6,
              "painAreas": [
                {
                  "location": "LEFT_JAW",
                  "intensity": 8,
                  "description": "씹을 때 통증"
                }
              ]
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/v1/assessments/daily")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.prescriptionCode").value("TEST1234"))

        // 2. 주차별 추이 조회
        mockMvc.perform(
            get("/api/v1/assessments/weekly-trend")
                .param("prescriptionCode", "TEST1234")
                .param("startWeek", "1")
                .param("endWeek", "3")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.prescriptionCode").value("TEST1234"))
            .andExpect(jsonPath("$.weeklyTrends").isArray)
    }

    @Test
    @DisplayName("존재하지 않는 처방 코드 - 404")
    fun createAssessment_PrescriptionNotFound_Returns404() {
        val requestBody = """
            {
              "prescriptionCode": "NOTEXIST",
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
                .content(requestBody)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Not Found"))
    }

    @Test
    @DisplayName("Bean Validation 실패 - 400")
    fun createAssessment_ValidationFail_Returns400() {
        val requestBody = """
            {
              "prescriptionCode": "INVALID",
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
                .content(requestBody)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.validationErrors").exists())
    }
}
```

**검증:**
```bash
./gradlew test --tests AssessmentIntegrationTest

# 결과: 3 tests passed
```

**커밋:**
```bash
git add src/test/kotlin/com/beyondmedicine/integration/AssessmentIntegrationTest.kt
git commit -m "test(integration): 통합 테스트 추가

- 전체 플로우 테스트 (등록 → 조회)
- 예외 케이스 테스트 (404, 400)
- E2E 시나리오 검증"
```

---

### Phase 6 완료 체크리스트

```bash
# 전체 통합 테스트 실행
./gradlew test --tests "com.beyondmedicine.integration.*"

# 예상 결과:
# AssessmentIntegrationTest: 3 tests passed
```

**Phase 6 완료 커밋:**
```bash
git add .
git commit -m "feat(test): Phase 6 완료 - 통합 테스트

- 통합 테스트: E2E 시나리오
- 예외 처리 검증
- 모든 테스트 통과 (3 tests)"
```

---

## Phase 7: 최종 검증 & 데이터

**목표:** 테스트 데이터 삽입 및 최종 검증
**예상 시간:** 1-2시간

### 7.1. 테스트 데이터 삽입

#### ☐ Task 7-1-1: DataInitializer 구현
```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/config/DataInitializer.kt

package com.beyondmedicine.config

import com.beyondmedicine.application.repository.PrescriptionRepository
import com.beyondmedicine.domain.entity.Prescription
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.time.LocalDateTime

/**
 * 테스트 데이터 초기화
 * - 참고: 과제 PDF 4-5페이지
 */
@Configuration
@Profile("!test") // 테스트 프로파일이 아닐 때만 실행
class DataInitializer {

    @Bean
    fun initData(prescriptionRepository: PrescriptionRepository): CommandLineRunner {
        return CommandLineRunner {
            // 1. ACTIVE 상태 처방 (현재 사용 가능)
            val activePrescription = Prescription(
                code = "ABCD1234",
                createdAt = LocalDateTime.now().minusDays(10),
                activatedAt = LocalDateTime.now().minusDays(5)
            )
            prescriptionRepository.save(activePrescription)

            // 2. PENDING 상태 처방 (활성화 전)
            val pendingPrescription = Prescription(
                code = "EFGH5678",
                createdAt = LocalDateTime.now().minusDays(2),
                activatedAt = null
            )
            prescriptionRepository.save(pendingPrescription)

            // 3. COMPLETED 상태 처방 (6주 경과)
            val completedPrescription = Prescription(
                code = "IJKL9012",
                createdAt = LocalDateTime.now().minusDays(60),
                activatedAt = LocalDateTime.now().minusDays(50)
            )
            prescriptionRepository.save(completedPrescription)

            // 4. EXPIRED 상태 처방 (활성화 안 하고 6주 경과)
            val expiredPrescription = Prescription(
                code = "MNOP3456",
                createdAt = LocalDateTime.now().minusDays(50),
                activatedAt = null
            )
            prescriptionRepository.save(expiredPrescription)

            println("✅ 테스트 데이터 삽입 완료:")
            println("   - ACTIVE: ABCD1234")
            println("   - PENDING: EFGH5678")
            println("   - COMPLETED: IJKL9012")
            println("   - EXPIRED: MNOP3456")
        }
    }
}
```

**커밋:**
```bash
git add src/main/kotlin/com/beyondmedicine/config/DataInitializer.kt
git commit -m "feat(config): DataInitializer 구현

- 테스트용 처방 데이터 자동 삽입
- ACTIVE, PENDING, COMPLETED, EXPIRED 상태별 처방"
```

---

### 7.2. application.yml 설정

#### ☐ Task 7-2-1: application.yml 작성
```yaml
# 파일: src/main/resources/application.yml

spring:
  application:
    name: beyond-medicine-assignment

  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:

  h2:
    console:
      enabled: true
      path: /h2-console

  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        use_sql_comments: true

  jackson:
    serialization:
      write-dates-as-timestamps: false

logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
```

#### ☐ Task 7-2-2: application-test.yml 작성
```yaml
# 파일: src/test/resources/application-test.yml

spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:

  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: false

logging:
  level:
    org.springframework: WARN
    com.beyondmedicine: DEBUG
```

**커밋:**
```bash
git add src/main/resources/application.yml
git add src/test/resources/application-test.yml
git commit -m "feat(config): Spring Boot 설정 추가

- H2 Database 설정
- JPA/Hibernate 설정
- Jackson 날짜 포맷 설정
- 테스트 프로파일 분리"
```

---

### 7.3. 최종 전체 테스트 실행

#### ☐ Task 7-3-1: 전체 테스트 실행
```bash
# 전체 테스트 실행
./gradlew clean test

# 예상 결과:
# Domain Tests: 36 tests passed
# Repository Tests: 10 tests passed
# Service Tests: 9 tests passed
# Mapper Tests: 4 tests passed
# Controller Tests: 2 tests passed
# Integration Tests: 3 tests passed
# =====================
# Total: 64 tests passed
```

#### ☐ Task 7-3-2: 애플리케이션 실행 확인
```bash
# 애플리케이션 실행
./gradlew bootRun

# 확인 사항:
# ✅ 애플리케이션 정상 기동
# ✅ 테스트 데이터 자동 삽입
# ✅ H2 Console 접근 가능 (http://localhost:8080/h2-console)
# ✅ API 엔드포인트 동작
```

**최종 커밋:**
```bash
git add .
git commit -m "feat: Phase 7 완료 - 최종 검증

- 테스트 데이터 자동 삽입
- application.yml 설정
- 전체 테스트 통과 (64 tests)
- 애플리케이션 정상 실행 확인"
```

---

## 📊 최종 진행 상황 체크리스트

### Phase 별 완료 기준

- [ ] **Phase 1 완료**: 도메인 모델 (36 tests passed)
- [ ] **Phase 2 완료**: Repository (10 tests passed)
- [ ] **Phase 3 완료**: Service (9 tests passed)
- [ ] **Phase 4 완료**: DTO & Mapper (4 tests passed)
- [ ] **Phase 5 완료**: Controller (2 tests passed)
- [ ] **Phase 6 완료**: 예외 처리 & 통합 테스트 (3 tests passed)
- [ ] **Phase 7 완료**: 최종 검증 (전체 64 tests passed)

### 전체 진행률

```
Phase 1: [##########] 100% - 도메인 모델 ✅
Phase 2: [##########] 100% - Repository ✅
Phase 3: [##########] 100% - Service ✅
Phase 4: [##########] 100% - DTO & Mapper ✅
Phase 5: [##########] 100% - Controller ✅
Phase 6: [##########] 100% - 예외 처리 & 통합 ✅
Phase 7: [##########] 100% - 최종 검증 ✅

전체: [##########] 100% 완료 🎉
```

---

## 🎯 구현 완료 후 체크리스트

### 필수 확인 사항

- [ ] 전체 테스트 통과 (./gradlew clean test)
- [ ] 애플리케이션 정상 실행 (./gradlew bootRun)
- [ ] H2 Console 접근 가능
- [ ] 테스트 데이터 자동 삽입 확인
- [ ] API 엔드포인트 동작 확인
    - [ ] POST /api/v1/assessments/daily
    - [ ] GET /api/v1/assessments/weekly-trend

### API 테스트 (curl 또는 Postman)

#### 일일 검사 등록
```bash
curl -X POST http://localhost:8080/api/v1/assessments/daily \
  -H "Content-Type: application/json" \
  -d '{
    "prescriptionCode": "ABCD1234",
    "assessmentDate": "2025-10-15",
    "painScore": 7,
    "stressScore": 5,
    "jawFunctionScore": 6,
    "painAreas": [
      {
        "location": "LEFT_JAW",
        "intensity": 8,
        "description": "씹을 때 통증"
      }
    ]
  }'
```

#### 주차별 추이 조회
```bash
curl -X GET "http://localhost:8080/api/v1/assessments/weekly-trend?prescriptionCode=ABCD1234&startWeek=1&endWeek=3"
```

---

## 📚 참고 문서

- [과제 PDF](./비욘드_메디슨_백엔드_개발자_채용_과제.pdf)
- [API 설계](./api-design.md)
- [비즈니스 로직 설계](./business-logic-design.md)
- [Service 계층 설계](./service-layer-design.md)
- [Repository 설계](./repository-design.md)
- [예외 처리 설계](./exception-handling-design.md)
- [테스트 전략](./test-strategy.md)
- [FP + OOP 하이브리드 설계](./fp-hybrid-design.md)

---

## 🎉 완료!

이 가이드를 따라 구현하면 **TDD 방식**으로 **Phase 1~7**을 완료할 수 있습니다.

각 Phase마다:
1. **테스트 먼저 작성** (Red)
2. **구현** (Gr een)
3. **리팩토링** (Refactor)
4. **커밋**

이 사이클을 반복하여 안정적이고 테스트 가능한 코드를 작성할 수 있습니다.
