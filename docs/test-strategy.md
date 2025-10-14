# 테스트 전략

## 테스트 철학

### 핵심 원칙
1. **테스트 피라미드**: 단위 테스트 > 통합 테스트 > E2E 테스트
2. **멱등성 보장**: 반복 실행 가능, 순서 무관
3. **독립성**: 각 테스트는 독립적으로 실행 가능
4. **가독성**: Given-When-Then 패턴
5. **빠른 피드백**: 빠른 실행 속도

---

## 테스트 계층 구조

```
┌─────────────────────────────────┐
│  E2E Tests (선택적)              │  ← 전체 플로우 검증
│  - @SpringBootTest (full)       │
└─────────────────────────────────┘
            ↓
┌─────────────────────────────────┐
│  Integration Tests               │  ← 계층 간 통합
│  - @SpringBootTest              │
│  - @DataJpaTest                 │
└─────────────────────────────────┘
            ↓
┌─────────────────────────────────┐
│  Unit Tests (핵심)               │  ← 빠른 단위 테스트
│  - 순수 함수 (FP)                │
│  - Service (Mock)                │
│  - Controller (MockMvc)          │
└─────────────────────────────────┘
```

---

## 1. 단위 테스트 (Unit Test)

### 1.1 순수 함수 테스트 (FP - 최우선)

> **가장 빠르고 명확한 테스트**: Mock 불필요, 의존성 없음

#### PrescriptionCalculator 테스트

```kotlin
package com.beyondmedicine.domain.calculator

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import java.time.LocalDateTime

@DisplayName("PrescriptionCalculator 테스트")
class PrescriptionCalculatorTest {

    @Test
    @DisplayName("생성 후 활성화하지 않으면 PENDING 상태다")
    fun calculateStatus_NotActivated_ReturnsPending() {
        // given
        val createdAt = LocalDateTime.of(2025, 10, 1, 10, 0)
        val activatedAt = null
        val currentTime = LocalDateTime.of(2025, 10, 2, 10, 0)

        // when
        val status = PrescriptionCalculator.calculateStatus(createdAt, activatedAt, currentTime)

        // then
        assertEquals(PrescriptionStatus.PENDING, status)
    }

    @Test
    @DisplayName("활성화 후 42일 이내면 ACTIVE 상태다")
    fun calculateStatus_WithinActiveWeeks_ReturnsActive() {
        // given
        val createdAt = LocalDateTime.of(2025, 9, 25, 10, 0)
        val activatedAt = LocalDateTime.of(2025, 10, 1, 9, 0)
        val currentTime = LocalDateTime.of(2025, 10, 15, 10, 0)  // 14일 후

        // when
        val status = PrescriptionCalculator.calculateStatus(createdAt, activatedAt, currentTime)

        // then
        assertEquals(PrescriptionStatus.ACTIVE, status)
    }

    @Test
    @DisplayName("활성화 후 D+41 23:59:59까지는 ACTIVE 상태다 (경계값)")
    fun calculateStatus_EndOfActiveWeek_ReturnsActive() {
        // given
        val createdAt = LocalDateTime.of(2025, 9, 1, 10, 0)
        val activatedAt = LocalDateTime.of(2025, 10, 1, 9, 0)
        val currentTime = LocalDateTime.of(2025, 11, 11, 23, 59, 59, 999_999_999)  // D+41 끝

        // when
        val status = PrescriptionCalculator.calculateStatus(createdAt, activatedAt, currentTime)

        // then
        assertEquals(PrescriptionStatus.ACTIVE, status)
    }

    @Test
    @DisplayName("활성화 후 D+42 00:00:00부터는 COMPLETED 상태다 (경계값)")
    fun calculateStatus_StartOfCompletedWeek_ReturnsCompleted() {
        // given
        val createdAt = LocalDateTime.of(2025, 9, 1, 10, 0)
        val activatedAt = LocalDateTime.of(2025, 10, 1, 9, 0)
        val currentTime = LocalDateTime.of(2025, 11, 12, 0, 0, 0)  // D+42 시작

        // when
        val status = PrescriptionCalculator.calculateStatus(createdAt, activatedAt, currentTime)

        // then
        assertEquals(PrescriptionStatus.COMPLETED, status)
    }

    @Test
    @DisplayName("생성 후 6주 경과하고 활성화하지 않으면 EXPIRED 상태다")
    fun calculateStatus_NotActivatedAfter6Weeks_ReturnsExpired() {
        // given
        val createdAt = LocalDateTime.of(2025, 10, 1, 10, 0)
        val activatedAt = null
        val currentTime = LocalDateTime.of(2025, 11, 13, 10, 0)  // 6주 + 1일 후

        // when
        val status = PrescriptionCalculator.calculateStatus(createdAt, activatedAt, currentTime)

        // then
        assertEquals(PrescriptionStatus.EXPIRED, status)
    }
}
```

#### WeekCalculator 테스트

```kotlin
package com.beyondmedicine.domain.calculator

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import java.time.LocalDate

@DisplayName("WeekCalculator 테스트")
class WeekCalculatorTest {

    @Test
    @DisplayName("활성화 당일은 1주차다 (D+0)")
    fun calculateWeekNumber_SameDay_Returns1() {
        // given
        val activationDate = LocalDate.of(2025, 10, 1)
        val assessmentDate = LocalDate.of(2025, 10, 1)

        // when
        val weekNumber = WeekCalculator.calculateWeekNumber(activationDate, assessmentDate)

        // then
        assertEquals(1, weekNumber)
    }

    @Test
    @DisplayName("활성화 후 7일은 1주차다 (D+6)")
    fun calculateWeekNumber_SevenDaysLater_Returns1() {
        // given
        val activationDate = LocalDate.of(2025, 10, 1)
        val assessmentDate = LocalDate.of(2025, 10, 7)

        // when
        val weekNumber = WeekCalculator.calculateWeekNumber(activationDate, assessmentDate)

        // then
        assertEquals(1, weekNumber)
    }

    @Test
    @DisplayName("활성화 후 8일은 2주차다 (D+7)")
    fun calculateWeekNumber_EightDaysLater_Returns2() {
        // given
        val activationDate = LocalDate.of(2025, 10, 1)
        val assessmentDate = LocalDate.of(2025, 10, 8)

        // when
        val weekNumber = WeekCalculator.calculateWeekNumber(activationDate, assessmentDate)

        // then
        assertEquals(2, weekNumber)
    }

    @Test
    @DisplayName("활성화 후 14일은 3주차다 (과제 예시)")
    fun calculateWeekNumber_FourteenDaysLater_Returns3() {
        // given: 과제 PDF 예시
        val activationDate = LocalDate.of(2025, 9, 1)  // 월요일
        val assessmentDate = LocalDate.of(2025, 9, 15)  // 2주 후 월요일

        // when
        val weekNumber = WeekCalculator.calculateWeekNumber(activationDate, assessmentDate)

        // then
        assertEquals(3, weekNumber)  // (14 / 7) + 1 = 3
    }

    @Test
    @DisplayName("활성화 후 42일은 6주차다 (D+41, 마지막 주차)")
    fun calculateWeekNumber_FortyOneDaysLater_Returns6() {
        // given
        val activationDate = LocalDate.of(2025, 10, 1)
        val assessmentDate = LocalDate.of(2025, 11, 11)  // D+41

        // when
        val weekNumber = WeekCalculator.calculateWeekNumber(activationDate, assessmentDate)

        // then
        assertEquals(6, weekNumber)
    }

    @Test
    @DisplayName("활성화 후 43일은 null을 반환한다 (D+42, 범위 밖)")
    fun calculateWeekNumber_FortyTwoDaysLater_ReturnsNull() {
        // given
        val activationDate = LocalDate.of(2025, 10, 1)
        val assessmentDate = LocalDate.of(2025, 11, 12)  // D+42

        // when
        val weekNumber = WeekCalculator.calculateWeekNumber(activationDate, assessmentDate)

        // then
        assertNull(weekNumber)
    }

    @Test
    @DisplayName("활성화 이전 날짜는 null을 반환한다")
    fun calculateWeekNumber_BeforeActivation_ReturnsNull() {
        // given
        val activationDate = LocalDate.of(2025, 10, 1)
        val assessmentDate = LocalDate.of(2025, 9, 30)

        // when
        val weekNumber = WeekCalculator.calculateWeekNumber(activationDate, assessmentDate)

        // then
        assertNull(weekNumber)
    }
}
```

#### ChangeRateCalculator 테스트

```kotlin
package com.beyondmedicine.domain.calculator

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

@DisplayName("ChangeRateCalculator 테스트")
class ChangeRateCalculatorTest {

    @Test
    @DisplayName("통증 점수 감소 시 양수 변화율을 반환한다 (호전)")
    fun calculate_PainDecreased_ReturnsPositive() {
        // given: 통증 7.0 → 6.0
        val previous = 7.0
        val current = 6.0

        // when
        val changeRate = ChangeRateCalculator.calculate(
            previous, current, MetricType.LOWER_IS_BETTER
        )

        // then
        assertEquals(14.3, changeRate)  // -((6-7)/7)*100 = 14.3%
    }

    @Test
    @DisplayName("통증 점수 증가 시 음수 변화율을 반환한다 (악화)")
    fun calculate_PainIncreased_ReturnsNegative() {
        // given: 통증 6.0 → 8.5
        val previous = 6.0
        val current = 8.5

        // when
        val changeRate = ChangeRateCalculator.calculate(
            previous, current, MetricType.LOWER_IS_BETTER
        )

        // then
        assertEquals(-41.7, changeRate)  // -((8.5-6)/6)*100 = -41.7%
    }

    @Test
    @DisplayName("턱 기능 점수 증가 시 양수 변화율을 반환한다 (호전)")
    fun calculate_JawFunctionIncreased_ReturnsPositive() {
        // given: 턱 기능 6.0 → 7.0
        val previous = 6.0
        val current = 7.0

        // when
        val changeRate = ChangeRateCalculator.calculate(
            previous, current, MetricType.HIGHER_IS_BETTER
        )

        // then
        assertEquals(16.7, changeRate)  // ((7-6)/6)*100 = 16.7%
    }

    @Test
    @DisplayName("턱 기능 점수 감소 시 음수 변화율을 반환한다 (악화)")
    fun calculate_JawFunctionDecreased_ReturnsNegative() {
        // given: 턱 기능 6.0 → 5.5
        val previous = 6.0
        val current = 5.5

        // when
        val changeRate = ChangeRateCalculator.calculate(
            previous, current, MetricType.HIGHER_IS_BETTER
        )

        // then
        assertEquals(-8.3, changeRate)  // ((5.5-6)/6)*100 = -8.3%
    }

    @Test
    @DisplayName("이전 값이 0이면 null을 반환한다")
    fun calculate_PreviousIsZero_ReturnsNull() {
        // given
        val previous = 0.0
        val current = 5.0

        // when
        val changeRate = ChangeRateCalculator.calculate(
            previous, current, MetricType.LOWER_IS_BETTER
        )

        // then
        assertNull(changeRate)
    }

    @Test
    @DisplayName("소수점 첫째 자리까지 반올림한다")
    fun calculate_RoundsToOneDecimal() {
        // given: 5.0 → 4.0
        val previous = 5.0
        val current = 4.0

        // when
        val changeRate = ChangeRateCalculator.calculate(
            previous, current, MetricType.LOWER_IS_BETTER
        )

        // then
        assertEquals(20.0, changeRate)  // -((4-5)/5)*100 = 20.0%
    }
}
```

---

### 1.2 Service 단위 테스트 (Mock 사용)

```kotlin
package com.beyondmedicine.application.service

import com.beyondmedicine.application.dto.*
import com.beyondmedicine.application.exception.*
import com.beyondmedicine.application.repository.*
import com.beyondmedicine.domain.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import java.time.LocalDate
import java.time.LocalDateTime

@ExtendWith(MockitoExtension::class)
@DisplayName("AssessmentService 단위 테스트")
class AssessmentServiceTest {

    @Mock
    private lateinit var prescriptionRepository: PrescriptionRepository

    @Mock
    private lateinit var dailyAssessmentRepository: DailyAssessmentRepository

    @InjectMocks
    private lateinit var assessmentService: AssessmentService

    @Test
    @DisplayName("일일 검사를 정상 등록한다")
    fun createAssessment_ValidCommand_Success() {
        // given
        val prescription = createActivePrescription()
        val command = createValidCommand()

        whenever(prescriptionRepository.findByCode(any())).thenReturn(prescription)
        whenever(dailyAssessmentRepository.existsByPrescriptionAndAssessmentDate(any(), any()))
            .thenReturn(false)
        whenever(dailyAssessmentRepository.save(any())).thenAnswer { it.arguments[0] }

        // when
        val result = assessmentService.createAssessment(command)

        // then
        assertNotNull(result)
        assertEquals(command.prescriptionCode, result.prescriptionCode)
        verify(dailyAssessmentRepository).save(any())
    }

    @Test
    @DisplayName("처방이 없으면 PrescriptionNotFoundException을 던진다")
    fun createAssessment_PrescriptionNotFound_ThrowsException() {
        // given
        val command = createValidCommand()
        whenever(prescriptionRepository.findByCode(any())).thenReturn(null)

        // when & then
        assertThrows<PrescriptionNotFoundException> {
            assessmentService.createAssessment(command)
        }
    }

    @Test
    @DisplayName("PENDING 상태 처방은 InvalidPrescriptionStatusException을 던진다")
    fun createAssessment_PendingStatus_ThrowsException() {
        // given
        val pendingPrescription = createPendingPrescription()
        val command = createValidCommand()

        whenever(prescriptionRepository.findByCode(any())).thenReturn(pendingPrescription)

        // when & then
        assertThrows<InvalidPrescriptionStatusException> {
            assessmentService.createAssessment(command)
        }
    }

    @Test
    @DisplayName("중복 검사는 DuplicateAssessmentException을 던진다")
    fun createAssessment_DuplicateAssessment_ThrowsException() {
        // given
        val prescription = createActivePrescription()
        val command = createValidCommand()

        whenever(prescriptionRepository.findByCode(any())).thenReturn(prescription)
        whenever(dailyAssessmentRepository.existsByPrescriptionAndAssessmentDate(any(), any()))
            .thenReturn(true)

        // when & then
        assertThrows<DuplicateAssessmentException> {
            assessmentService.createAssessment(command)
        }
    }

    // 헬퍼 메서드
    private fun createActivePrescription() = Prescription(
        code = "ABCD1234",
        createdAt = LocalDateTime.now().minusDays(10),
        activatedAt = LocalDateTime.now().minusDays(5)
    )

    private fun createPendingPrescription() = Prescription(
        code = "EFGH5678",
        createdAt = LocalDateTime.now().minusDays(1),
        activatedAt = null
    )

    private fun createValidCommand() = CreateDailyAssessmentCommand(
        prescriptionCode = "ABCD1234",
        assessmentDate = LocalDate.now(),
        painScore = 7,
        stressScore = 5,
        jawFunctionScore = 6,
        painAreas = emptyList()
    )
}
```

---

## 2. 통합 테스트 (Integration Test)

### 2.1 Repository 테스트 (@DataJpaTest)

```kotlin
package com.beyondmedicine.application.repository

import com.beyondmedicine.domain.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("DailyAssessmentRepository 통합 테스트")
class DailyAssessmentRepositoryTest {

    @Autowired
    private lateinit var dailyAssessmentRepository: DailyAssessmentRepository

    @Autowired
    private lateinit var prescriptionRepository: PrescriptionRepository

    @Test
    @DisplayName("주차별 평균을 집계한다")
    fun findWeeklyAggregations_Success() {
        // given
        val prescription = createAndSavePrescription()
        createAssessmentsForMultipleWeeks(prescription)

        // when
        val aggregations = dailyAssessmentRepository.findWeeklyAggregations(
            prescription = prescription,
            startWeek = 1,
            endWeek = 3
        )

        // then
        assertEquals(3, aggregations.size)

        // 1주차
        assertEquals(1, aggregations[0].weekNumber)
        assertEquals(2, aggregations[0].assessmentCount)

        // 2주차
        assertEquals(2, aggregations[1].weekNumber)
        assertEquals(2, aggregations[1].assessmentCount)

        // 3주차
        assertEquals(3, aggregations[2].weekNumber)
        assertEquals(1, aggregations[2].assessmentCount)
    }

    @Test
    @DisplayName("데이터가 없는 주차는 결과에 포함되지 않는다")
    fun findWeeklyAggregations_NoDataWeeks_NotIncluded() {
        // given
        val prescription = createAndSavePrescription()
        createAssessment(prescription, 1)  // 1주차만 있음

        // when
        val aggregations = dailyAssessmentRepository.findWeeklyAggregations(
            prescription = prescription,
            startWeek = 1,
            endWeek = 6
        )

        // then
        assertEquals(1, aggregations.size)
        assertEquals(1, aggregations[0].weekNumber)
    }

    // 헬퍼 메서드 생략...
}
```

### 2.2 Service 통합 테스트 (@SpringBootTest)

```kotlin
package com.beyondmedicine.application.service

import com.beyondmedicine.application.dto.*
import com.beyondmedicine.application.repository.*
import com.beyondmedicine.domain.model.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@DisplayName("AssessmentService 통합 테스트")
class AssessmentServiceIntegrationTest {

    @Autowired
    private lateinit var assessmentService: AssessmentService

    @Autowired
    private lateinit var prescriptionRepository: PrescriptionRepository

    @Autowired
    private lateinit var dailyAssessmentRepository: DailyAssessmentRepository

    @Test
    @DisplayName("일일 검사 등록 전체 플로우가 정상 동작한다")
    fun createAssessment_FullFlow_Success() {
        // given
        val prescription = Prescription(
            code = "TEST1234",
            createdAt = LocalDateTime.now().minusDays(10),
            activatedAt = LocalDateTime.now().minusDays(5)
        )
        prescriptionRepository.save(prescription)

        val command = CreateDailyAssessmentCommand(
            prescriptionCode = "TEST1234",
            assessmentDate = LocalDate.now(),
            painScore = 7,
            stressScore = 5,
            jawFunctionScore = 6,
            painAreas = listOf(
                PainAreaCommand(PainLocation.LEFT_JAW, 8, "씹을 때 통증")
            )
        )

        // when
        val result = assessmentService.createAssessment(command)

        // then
        assertNotNull(result.assessmentId)
        assertEquals("TEST1234", result.prescriptionCode)
        assertEquals(1, result.weekNumber)

        // DB 검증
        val savedAssessment = dailyAssessmentRepository.findById(result.assessmentId).get()
        assertEquals(7, savedAssessment.painScore)
        assertEquals(1, savedAssessment.painAreas.size)
    }
}
```

---

## 3. API 테스트 (Controller)

### @WebMvcTest (경량 테스트)

```kotlin
package com.beyondmedicine.presentation.controller

import com.beyondmedicine.application.service.*
import com.beyondmedicine.presentation.dto.mapper.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(AssessmentController::class)
@DisplayName("AssessmentController API 테스트")
class AssessmentControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var assessmentService: AssessmentService

    @MockBean
    private lateinit var mapper: AssessmentDtoMapper

    @Test
    @DisplayName("POST /api/v1/assessments/daily - 정상 요청")
    fun createAssessment_ValidRequest_Returns200() {
        // given
        val command = createValidCommand()
        val result = createValidResult()

        whenever(mapper.toServiceCommand(any())).thenReturn(command)
        whenever(assessmentService.createAssessment(any())).thenReturn(result)
        whenever(mapper.toResponse(any())).thenReturn(createValidResponse())

        // when & then
        mockMvc.perform(
            post("/api/v1/assessments/daily")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson())
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.assessmentId").exists())
            .andExpect(jsonPath("$.prescriptionCode").value("ABCD1234"))
            .andExpect(jsonPath("$.weekNumber").value(1))
    }

    @Test
    @DisplayName("POST /api/v1/assessments/daily - 잘못된 처방 코드")
    fun createAssessment_InvalidCode_Returns400() {
        // when & then
        mockMvc.perform(
            post("/api/v1/assessments/daily")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "prescriptionCode": "ABC",
                        "assessmentDate": "2025-10-14",
                        "painScore": 7,
                        "stressScore": 5,
                        "jawFunctionScore": 6
                    }
                """)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
    }
}
```

---

## 4. 테스트 픽스처 (Fixture)

### 테스트 데이터 생성 유틸

```kotlin
package com.beyondmedicine.test.fixture

import com.beyondmedicine.domain.model.*
import java.time.LocalDate
import java.time.LocalDateTime

object PrescriptionFixture {

    fun createActivePrescription(
        code: String = "TEST1234",
        activatedDaysAgo: Long = 5
    ): Prescription {
        return Prescription(
            code = code,
            createdAt = LocalDateTime.now().minusDays(activatedDaysAgo + 5),
            activatedAt = LocalDateTime.now().minusDays(activatedDaysAgo)
        )
    }

    fun createPendingPrescription(
        code: String = "PEND1234"
    ): Prescription {
        return Prescription(
            code = code,
            createdAt = LocalDateTime.now().minusDays(1),
            activatedAt = null
        )
    }
}

object AssessmentFixture {

    fun createValidCommand(
        prescriptionCode: String = "TEST1234",
        assessmentDate: LocalDate = LocalDate.now()
    ): CreateDailyAssessmentCommand {
        return CreateDailyAssessmentCommand(
            prescriptionCode = prescriptionCode,
            assessmentDate = assessmentDate,
            painScore = 7,
            stressScore = 5,
            jawFunctionScore = 6,
            painAreas = emptyList()
        )
    }
}
```

---

## 5. 테스트 설정

### application-test.yml

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:

  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
    properties:
      hibernate:
        format_sql: true

  h2:
    console:
      enabled: true

logging:
  level:
    com.beyondmedicine: DEBUG
    org.hibernate.SQL: DEBUG
```

---

## 요구사항 추적표

### 테스트 커버리지 (과제 요구사항)

| 계층 | 테스트 대상 | 테스트 방법 | 상태 |
|-----|-----------|------------|------|
| 순수 함수 | PrescriptionCalculator | 단위 테스트 (Mock 없음) | ✅ |
| 순수 함수 | WeekCalculator | 단위 테스트 (Mock 없음) | ✅ |
| 순수 함수 | ChangeRateCalculator | 단위 테스트 (Mock 없음) | ✅ |
| Service | AssessmentService | 단위 테스트 (Mock) | ✅ |
| Service | AssessmentAnalysisService | 단위 테스트 (Mock) | ✅ |
| Repository | DailyAssessmentRepository | @DataJpaTest | ✅ |
| Controller | AssessmentController | @WebMvcTest | ✅ |
| 통합 | 전체 플로우 | @SpringBootTest | ✅ |

### 주요 시나리오 테스트

| 시나리오 | 테스트 케이스 | 상태 |
|---------|-------------|------|
| 처방 상태 계산 | PENDING, ACTIVE, COMPLETED, EXPIRED | ✅ |
| 주차 계산 | 1~6주차, 범위 밖, 경계값 | ✅ |
| 변화율 계산 | 호전, 악화, 경계값 | ✅ |
| 일일 검사 등록 | 정상, 처방 없음, 중복, 상태 오류 | ✅ |
| 주차별 추이 | 집계, 데이터 없는 주차 제외 | ✅ |
| 예외 처리 | 404, 400, 409 응답 | ✅ |

---

## 참고 문서
- [설계 의사결정 문서](./design-decisions.md)
- [FP + OOP 하이브리드 설계](./fp-hybrid-design.md)
- [예외 처리 전략](./exception-handling-design.md)
