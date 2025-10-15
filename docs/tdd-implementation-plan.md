# TDD 기반 구현 계획

## TDD 철학 및 원칙

### Red-Green-Refactor 사이클
```
🔴 RED: 실패하는 테스트 작성
    ↓
🟢 GREEN: 테스트를 통과하는 최소한의 코드 작성
    ↓
🔵 REFACTOR: 코드 개선 (테스트는 여전히 통과)
    ↓
반복
```

### 핵심 원칙
1. **테스트가 먼저**: 프로덕션 코드보다 테스트를 먼저 작성
2. **한 번에 하나**: 한 번에 하나의 작은 기능만 테스트
3. **빠른 피드백**: 테스트는 빠르게 실행되어야 함
4. **명확한 의도**: 테스트는 코드의 명세서 역할
5. **리팩토링 안전망**: 테스트가 통과하면 리팩토링 자유롭게

---

## TDD 구현 로드맵

### Phase 0: 프로젝트 환경 설정 (TDD 준비)
**목표**: 테스트 작성 환경 완성

#### 0.1 Spring Boot 프로젝트 생성
- [ ] Spring Initializr로 프로젝트 생성
  - Kotlin
  - Spring Boot 3.x
  - Gradle (Kotlin DSL)
  - Dependencies: Web, JPA, H2, Validation
- [ ] build.gradle.kts 의존성 추가
  ```kotlin
  dependencies {
      // Spring Boot
      implementation("org.springframework.boot:spring-boot-starter-web")
      implementation("org.springframework.boot:spring-boot-starter-data-jpa")
      implementation("org.springframework.boot:spring-boot-starter-validation")

      // Kotlin
      implementation("org.jetbrains.kotlin:kotlin-reflect")
      implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

      // Database
      runtimeOnly("com.h2database:h2")
      runtimeOnly("com.mysql:mysql-connector-j")

      // Test
      testImplementation("org.springframework.boot:spring-boot-starter-test")
      testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
      testImplementation("io.kotest:kotest-assertions-core-jvm:5.8.0") // 선택적
  }
  ```

#### 0.2 패키지 구조 생성
```
src/
├── main/kotlin/com/beyondmedicine/
│   ├── domain/
│   │   ├── model/
│   │   ├── calculator/
│   │   └── exception/
│   ├── application/
│   │   ├── service/
│   │   ├── repository/
│   │   └── dto/
│   ├── presentation/
│   │   ├── controller/
│   │   ├── dto/
│   │   └── exception/
│   └── BeyondMedicineApplication.kt
│
└── test/kotlin/com/beyondmedicine/
    ├── domain/
    │   └── calculator/
    ├── application/
    │   ├── service/
    │   └── repository/
    └── presentation/
        └── controller/
```

#### 0.3 테스트 설정 파일
- [ ] `src/test/resources/application-test.yml`
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
  ```

#### 0.4 첫 번째 스모크 테스트
- [ ] `ApplicationTest.kt` 작성 (Context Load 확인)
  ```kotlin
  @SpringBootTest
  class ApplicationTest {
      @Test
      fun contextLoads() {
          // Spring Context가 정상적으로 로드되는지 확인
      }
  }
  ```

**완료 조건**: `./gradlew test` 실행 시 1개 테스트 통과

---

## Phase 1: Domain Calculator (순수 함수) - TDD

### 🎯 **왜 여기서 시작?**
- 외부 의존성 없음 (DB, Spring 불필요)
- 가장 빠른 피드백 사이클
- TDD 학습에 최적
- Mock 불필요

---

### Iteration 1.1: PrescriptionCalculator - PENDING 상태

#### 🔴 RED: 테스트 작성
**파일**: `src/test/kotlin/com/beyondmedicine/domain/calculator/PrescriptionCalculatorTest.kt`

```kotlin
package com.beyondmedicine.domain.calculator

import org.junit.jupiter.api.Assertions.assertEquals
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
        val status = PrescriptionCalculator.calculateStatus(
            createdAt, activatedAt, currentTime
        )

        // then
        assertEquals(PrescriptionStatus.PENDING, status)
    }
}
```

**실행**: `./gradlew test --tests PrescriptionCalculatorTest`
**예상 결과**: ❌ 컴파일 에러 (PrescriptionCalculator, PrescriptionStatus 없음)

---

#### 🟢 GREEN: 최소한의 구현

**파일**: `src/main/kotlin/com/beyondmedicine/domain/model/PrescriptionStatus.kt`
```kotlin
package com.beyondmedicine.domain.model

enum class PrescriptionStatus {
    PENDING,
    ACTIVE,
    COMPLETED,
    EXPIRED
}
```

**파일**: `src/main/kotlin/com/beyondmedicine/domain/calculator/PrescriptionCalculator.kt`
```kotlin
package com.beyondmedicine.domain.calculator

import com.beyondmedicine.domain.model.PrescriptionStatus
import java.time.LocalDateTime

object PrescriptionCalculator {

    fun calculateStatus(
        createdAt: LocalDateTime,
        activatedAt: LocalDateTime?,
        currentTime: LocalDateTime
    ): PrescriptionStatus {
        // 가장 단순한 구현: 항상 PENDING 반환
        return PrescriptionStatus.PENDING
    }
}
```

**실행**: `./gradlew test --tests PrescriptionCalculatorTest`
**예상 결과**: ✅ 1개 테스트 통과

---

#### 🔵 REFACTOR: (아직 불필요)

---

### Iteration 1.2: PrescriptionCalculator - ACTIVE 상태

#### 🔴 RED: 테스트 추가
```kotlin
@Test
@DisplayName("활성화 후 42일 이내면 ACTIVE 상태다")
fun calculateStatus_WithinActiveWeeks_ReturnsActive() {
    // given
    val createdAt = LocalDateTime.of(2025, 9, 25, 10, 0)
    val activatedAt = LocalDateTime.of(2025, 10, 1, 9, 0)
    val currentTime = LocalDateTime.of(2025, 10, 15, 10, 0)  // 14일 후

    // when
    val status = PrescriptionCalculator.calculateStatus(
        createdAt, activatedAt, currentTime
    )

    // then
    assertEquals(PrescriptionStatus.ACTIVE, status)
}
```

**실행**: ❌ 테스트 실패 (PENDING 반환, ACTIVE 기대)

---

#### 🟢 GREEN: 구현 확장
```kotlin
fun calculateStatus(
    createdAt: LocalDateTime,
    activatedAt: LocalDateTime?,
    currentTime: LocalDateTime
): PrescriptionStatus {
    // activatedAt이 있으면 ACTIVE, 없으면 PENDING
    return if (activatedAt != null) {
        PrescriptionStatus.ACTIVE
    } else {
        PrescriptionStatus.PENDING
    }
}
```

**실행**: ✅ 2개 테스트 모두 통과

---

#### 🔵 REFACTOR: (아직 단순하므로 생략)

---

### Iteration 1.3: PrescriptionCalculator - COMPLETED 상태 (경계값)

#### 🔴 RED: 경계값 테스트 추가
```kotlin
@Test
@DisplayName("활성화 후 D+41 23:59:59까지는 ACTIVE 상태다")
fun calculateStatus_EndOfActiveWeek_ReturnsActive() {
    // given
    val activatedAt = LocalDateTime.of(2025, 10, 1, 9, 0)
    val currentTime = LocalDateTime.of(2025, 11, 11, 23, 59, 59, 999_999_999)

    // when
    val status = PrescriptionCalculator.calculateStatus(
        LocalDateTime.now(), activatedAt, currentTime
    )

    // then
    assertEquals(PrescriptionStatus.ACTIVE, status)
}

@Test
@DisplayName("활성화 후 D+42 00:00:00부터는 COMPLETED 상태다")
fun calculateStatus_StartOfCompletedWeek_ReturnsCompleted() {
    // given
    val activatedAt = LocalDateTime.of(2025, 10, 1, 9, 0)
    val currentTime = LocalDateTime.of(2025, 11, 12, 0, 0, 0)

    // when
    val status = PrescriptionCalculator.calculateStatus(
        LocalDateTime.now(), activatedAt, currentTime
    )

    // then
    assertEquals(PrescriptionStatus.COMPLETED, status)
}
```

**실행**: ❌ 둘 다 실패 (ACTIVE만 반환)

---

#### 🟢 GREEN: 날짜 계산 로직 추가
```kotlin
private const val ACTIVE_PERIOD_DAYS = 42L

fun calculateStatus(
    createdAt: LocalDateTime,
    activatedAt: LocalDateTime?,
    currentTime: LocalDateTime
): PrescriptionStatus {
    if (activatedAt != null) {
        val endOfActiveWeek = activatedAt
            .plusDays(ACTIVE_PERIOD_DAYS - 1)
            .withHour(23)
            .withMinute(59)
            .withSecond(59)
            .withNano(999_999_999)

        return if (currentTime.isAfter(endOfActiveWeek)) {
            PrescriptionStatus.COMPLETED
        } else {
            PrescriptionStatus.ACTIVE
        }
    }

    return PrescriptionStatus.PENDING
}
```

**실행**: ✅ 4개 테스트 모두 통과

---

#### 🔵 REFACTOR: 함수 분리
```kotlin
object PrescriptionCalculator {

    private const val ACTIVE_PERIOD_DAYS = 42L

    fun calculateStatus(
        createdAt: LocalDateTime,
        activatedAt: LocalDateTime?,
        currentTime: LocalDateTime
    ): PrescriptionStatus = when {
        activatedAt != null -> calculateActivatedStatus(activatedAt, currentTime)
        else -> PrescriptionStatus.PENDING
    }

    private fun calculateActivatedStatus(
        activatedAt: LocalDateTime,
        currentTime: LocalDateTime
    ): PrescriptionStatus {
        val endOfActiveWeek = calculateEndOfActiveWeek(activatedAt)

        return if (currentTime.isAfter(endOfActiveWeek)) {
            PrescriptionStatus.COMPLETED
        } else {
            PrescriptionStatus.ACTIVE
        }
    }

    private fun calculateEndOfActiveWeek(activatedAt: LocalDateTime): LocalDateTime {
        return activatedAt
            .plusDays(ACTIVE_PERIOD_DAYS - 1)
            .withHour(23)
            .withMinute(59)
            .withSecond(59)
            .withNano(999_999_999)
    }
}
```

**실행**: ✅ 4개 테스트 여전히 통과 (리팩토링 성공)

---

### Iteration 1.4: PrescriptionCalculator - EXPIRED 상태

#### 🔴 RED: 테스트 추가
```kotlin
@Test
@DisplayName("생성 후 6주 경과하고 활성화하지 않으면 EXPIRED 상태다")
fun calculateStatus_NotActivatedAfter6Weeks_ReturnsExpired() {
    // given
    val createdAt = LocalDateTime.of(2025, 10, 1, 10, 0)
    val activatedAt = null
    val currentTime = LocalDateTime.of(2025, 11, 13, 10, 0)  // 6주 + 1일 후

    // when
    val status = PrescriptionCalculator.calculateStatus(
        createdAt, activatedAt, currentTime
    )

    // then
    assertEquals(PrescriptionStatus.EXPIRED, status)
}
```

**실행**: ❌ 테스트 실패 (PENDING 반환)

---

#### 🟢 GREEN: EXPIRED 로직 추가
```kotlin
private const val EXPIRATION_WEEKS = 6L

fun calculateStatus(
    createdAt: LocalDateTime,
    activatedAt: LocalDateTime?,
    currentTime: LocalDateTime
): PrescriptionStatus = when {
    activatedAt != null -> calculateActivatedStatus(activatedAt, currentTime)
    else -> calculateNotActivatedStatus(createdAt, currentTime)
}

private fun calculateNotActivatedStatus(
    createdAt: LocalDateTime,
    currentTime: LocalDateTime
): PrescriptionStatus {
    val expirationTime = createdAt.plusWeeks(EXPIRATION_WEEKS)
    return if (currentTime.isAfter(expirationTime)) {
        PrescriptionStatus.EXPIRED
    } else {
        PrescriptionStatus.PENDING
    }
}
```

**실행**: ✅ 5개 테스트 모두 통과

---

### 🎉 Iteration 1 완료 체크리스트
- [x] PrescriptionStatus Enum 생성
- [x] PrescriptionCalculator.calculateStatus() 구현
- [x] 5개 테스트 케이스 작성 및 통과
  - PENDING
  - ACTIVE
  - ACTIVE (경계값 D+41)
  - COMPLETED (경계값 D+42)
  - EXPIRED
- [x] 리팩토링 완료

**커밋**: `git commit -m "feat: implement PrescriptionCalculator with TDD"`

---

### Iteration 2: WeekCalculator

#### Iteration 2.1: 기본 주차 계산 (D+0 = 1주차)

#### 🔴 RED
```kotlin
@Test
@DisplayName("활성화 당일은 1주차다")
fun calculateWeekNumber_SameDay_Returns1() {
    // given
    val activationDate = LocalDate.of(2025, 10, 1)
    val assessmentDate = LocalDate.of(2025, 10, 1)

    // when
    val weekNumber = WeekCalculator.calculateWeekNumber(
        activationDate, assessmentDate
    )

    // then
    assertEquals(1, weekNumber)
}
```

#### 🟢 GREEN
```kotlin
object WeekCalculator {
    fun calculateWeekNumber(
        activationDate: LocalDate,
        assessmentDate: LocalDate
    ): Int? {
        return 1  // 하드코딩
    }
}
```

#### 🔵 REFACTOR: (생략)

---

#### Iteration 2.2: 주차 계산 로직 (과제 예시 검증)

#### 🔴 RED
```kotlin
@Test
@DisplayName("활성화 후 14일은 3주차다 (과제 예시)")
fun calculateWeekNumber_FourteenDaysLater_Returns3() {
    // given
    val activationDate = LocalDate.of(2025, 9, 1)
    val assessmentDate = LocalDate.of(2025, 9, 15)  // 14일 후

    // when
    val weekNumber = WeekCalculator.calculateWeekNumber(
        activationDate, assessmentDate
    )

    // then
    assertEquals(3, weekNumber)  // (14 / 7) + 1 = 3
}
```

#### 🟢 GREEN
```kotlin
import java.time.temporal.ChronoUnit

object WeekCalculator {
    private const val DAYS_PER_WEEK = 7

    fun calculateWeekNumber(
        activationDate: LocalDate,
        assessmentDate: LocalDate
    ): Int? {
        val daysSinceActivation = ChronoUnit.DAYS.between(
            activationDate, assessmentDate
        )
        return (daysSinceActivation / DAYS_PER_WEEK).toInt() + 1
    }
}
```

**실행**: ✅ 2개 테스트 통과

---

#### Iteration 2.3: 경계 조건 (null 반환)

#### 🔴 RED
```kotlin
@Test
@DisplayName("활성화 이전 날짜는 null을 반환한다")
fun calculateWeekNumber_BeforeActivation_ReturnsNull() {
    // given
    val activationDate = LocalDate.of(2025, 10, 1)
    val assessmentDate = LocalDate.of(2025, 9, 30)

    // when
    val weekNumber = WeekCalculator.calculateWeekNumber(
        activationDate, assessmentDate
    )

    // then
    assertNull(weekNumber)
}

@Test
@DisplayName("활성화 후 43일은 null을 반환한다")
fun calculateWeekNumber_AfterActiveWeek_ReturnsNull() {
    // given
    val activationDate = LocalDate.of(2025, 10, 1)
    val assessmentDate = LocalDate.of(2025, 11, 13)  // D+43

    // when
    val weekNumber = WeekCalculator.calculateWeekNumber(
        activationDate, assessmentDate
    )

    // then
    assertNull(weekNumber)
}
```

#### 🟢 GREEN
```kotlin
object WeekCalculator {
    private const val DAYS_PER_WEEK = 7
    private const val MAX_ACTIVE_DAYS = 41L

    fun calculateWeekNumber(
        activationDate: LocalDate,
        assessmentDate: LocalDate
    ): Int? {
        if (assessmentDate.isBefore(activationDate)) return null

        val daysSinceActivation = ChronoUnit.DAYS.between(
            activationDate, assessmentDate
        )

        if (daysSinceActivation > MAX_ACTIVE_DAYS) return null

        return (daysSinceActivation / DAYS_PER_WEEK).toInt() + 1
    }
}
```

**실행**: ✅ 4개 테스트 통과

---

### Iteration 3: ChangeRateCalculator

#### Iteration 3.1: 기본 변화율 계산

#### 🔴 RED
```kotlin
@Test
@DisplayName("통증 점수 감소 시 양수 변화율을 반환한다")
fun calculate_PainDecreased_ReturnsPositive() {
    // given
    val previous = 7.0
    val current = 6.0

    // when
    val changeRate = ChangeRateCalculator.calculate(
        previous, current, MetricType.LOWER_IS_BETTER
    )

    // then
    assertEquals(14.3, changeRate)
}
```

#### 🟢 GREEN
```kotlin
object ChangeRateCalculator {
    fun calculate(
        previous: Double,
        current: Double,
        metric: MetricType
    ): Double? {
        if (previous == 0.0) return null

        val rawRate = ((current - previous) / previous) * 100
        val adjustedRate = metric.adjustRate(rawRate)

        return String.format("%.1f", adjustedRate).toDouble()
    }
}

sealed class MetricType {
    abstract fun adjustRate(rawRate: Double): Double

    object LOWER_IS_BETTER : MetricType() {
        override fun adjustRate(rawRate: Double): Double = -rawRate
    }

    object HIGHER_IS_BETTER : MetricType() {
        override fun adjustRate(rawRate: Double): Double = rawRate
    }
}
```

**계속 반복...**

---

## Phase 2: Domain Entity - TDD

### Iteration 4: Prescription Entity

#### 🔴 RED: Entity 테스트 작성
```kotlin
@DataJpaTest
class PrescriptionTest {

    @Autowired
    private lateinit var entityManager: TestEntityManager

    @Test
    @DisplayName("처방을 생성하고 저장할 수 있다")
    fun createAndSave_Success() {
        // given
        val prescription = Prescription(
            code = "ABCD1234",
            createdAt = LocalDateTime.now()
        )

        // when
        val saved = entityManager.persistAndFlush(prescription)

        // then
        assertNotNull(saved.id)
        assertEquals("ABCD1234", saved.code)
    }
}
```

#### 🟢 GREEN: Entity 구현
```kotlin
@Entity
@Table(name = "prescriptions")
class Prescription(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 8)
    val code: String,

    @Column(nullable = false)
    val createdAt: LocalDateTime,

    @Column(nullable = true)
    var activatedAt: LocalDateTime? = null
)
```

---

### Iteration 5: Prescription - getStatus() 위임

#### 🔴 RED
```kotlin
@Test
@DisplayName("getStatus()는 PrescriptionCalculator에 위임한다")
fun getStatus_DelegatesToCalculator() {
    // given
    val prescription = Prescription(
        code = "TEST1234",
        createdAt = LocalDateTime.of(2025, 10, 1, 10, 0)
    )

    // when
    val status = prescription.getStatus(
        LocalDateTime.of(2025, 10, 2, 10, 0)
    )

    // then
    assertEquals(PrescriptionStatus.PENDING, status)
}
```

#### 🟢 GREEN
```kotlin
@Entity
class Prescription(...) {
    fun getStatus(currentTime: LocalDateTime = LocalDateTime.now()): PrescriptionStatus {
        return PrescriptionCalculator.calculateStatus(
            createdAt, activatedAt, currentTime
        )
    }
}
```

---

## Phase 3: Service Layer - TDD (Outside-In)

### Iteration 6: AssessmentService - createAssessment (Happy Path)

#### 🔴 RED: Service 테스트
```kotlin
@ExtendWith(MockitoExtension::class)
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

        whenever(prescriptionRepository.findByCode(any()))
            .thenReturn(prescription)
        whenever(dailyAssessmentRepository.existsByPrescriptionAndAssessmentDate(any(), any()))
            .thenReturn(false)
        whenever(dailyAssessmentRepository.save(any()))
            .thenAnswer { it.arguments[0] }

        // when
        val result = assessmentService.createAssessment(command)

        // then
        assertNotNull(result)
        assertEquals(command.prescriptionCode, result.prescriptionCode)
        verify(dailyAssessmentRepository).save(any())
    }

    private fun createActivePrescription() = Prescription(
        code = "TEST1234",
        createdAt = LocalDateTime.now().minusDays(10),
        activatedAt = LocalDateTime.now().minusDays(5)
    )

    private fun createValidCommand() = CreateDailyAssessmentCommand(
        prescriptionCode = "TEST1234",
        assessmentDate = LocalDate.now(),
        painScore = 7,
        stressScore = 5,
        jawFunctionScore = 6,
        painAreas = emptyList()
    )
}
```

#### 🟢 GREEN: Service 구현
```kotlin
@Service
@Transactional
class AssessmentService(
    private val prescriptionRepository: PrescriptionRepository,
    private val dailyAssessmentRepository: DailyAssessmentRepository
) {
    fun createAssessment(command: CreateDailyAssessmentCommand): DailyAssessmentResult {
        val prescription = prescriptionRepository.findByCode(command.prescriptionCode)
            ?: throw PrescriptionNotFoundException("처방을 찾을 수 없습니다")

        // 최소 구현
        val assessment = DailyAssessment(...)
        val saved = dailyAssessmentRepository.save(assessment)

        return DailyAssessmentResult(...)
    }
}
```

---

### Iteration 7: AssessmentService - 예외 케이스 (처방 없음)

#### 🔴 RED
```kotlin
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
```

#### 🟢 GREEN
```kotlin
fun createAssessment(command: CreateDailyAssessmentCommand): DailyAssessmentResult {
    val prescription = prescriptionRepository.findByCode(command.prescriptionCode)
        ?: throw PrescriptionNotFoundException("처방을 찾을 수 없습니다: ${command.prescriptionCode}")

    // ... 나머지 로직
}
```

---

## Phase 4: API Layer - TDD (Outside-In)

### Iteration 8: AssessmentController - POST /daily (Happy Path)

#### 🔴 RED: Controller 테스트
```kotlin
@WebMvcTest(AssessmentController::class)
class AssessmentControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var assessmentService: AssessmentService

    @MockBean
    private lateinit var mapper: AssessmentDtoMapper

    @Test
    @DisplayName("POST /api/v1/assessments/daily - 정상 요청")
    fun createAssessment_ValidRequest_Returns201() {
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
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.assessmentId").exists())
            .andExpect(jsonPath("$.prescriptionCode").value("ABCD1234"))
    }
}
```

#### 🟢 GREEN: Controller 구현
```kotlin
@RestController
@RequestMapping("/api/v1/assessments")
class AssessmentController(
    private val assessmentService: AssessmentService,
    private val mapper: AssessmentDtoMapper
) {
    @PostMapping("/daily")
    @ResponseStatus(HttpStatus.CREATED)
    fun createDailyAssessment(
        @Valid @RequestBody request: CreateDailyAssessmentRequest
    ): CreateDailyAssessmentResponse {
        val command = mapper.toServiceCommand(request)
        val result = assessmentService.createAssessment(command)
        return mapper.toResponse(result)
    }
}
```

---

## TDD 체크리스트 요약

### ✅ TDD 원칙 준수
- [ ] 항상 테스트를 먼저 작성
- [ ] Red-Green-Refactor 사이클 엄수
- [ ] 한 번에 하나의 기능만 테스트
- [ ] 테스트 이름은 명확한 명세 (DisplayName 활용)

### ✅ 테스트 품질
- [ ] Given-When-Then 구조
- [ ] 테스트는 독립적 (순서 무관)
- [ ] 테스트는 빠르게 실행 (< 1초)
- [ ] 테스트는 실패 이유가 명확

### ✅ 커버리지 목표
- [ ] 순수 함수: 100%
- [ ] Service: 90%+
- [ ] Controller: 80%+
- [ ] Repository: 집계 쿼리 검증

---

## 예상 소요 시간 (TDD 기준)

| Phase | 작업 | 예상 시간 | 누적 |
|-------|------|---------|------|
| Phase 0 | 환경 설정 | 2h | 2h |
| Phase 1 | Calculator TDD | 8h | 10h |
| Phase 2 | Entity TDD | 6h | 16h |
| Phase 3 | Service TDD | 12h | 28h |
| Phase 4 | Controller TDD | 10h | 38h |
| Phase 5 | 통합 테스트 | 4h | 42h |
| Phase 6 | 리팩토링 | 4h | 46h |
| **Total** | | **46h** | |

**여유 시간**: 120h - 46h = **74h** (충분한 버퍼)

---

## 다음 단계

**지금 바로 시작할까요?**

1. **Phase 0 시작**: 프로젝트 생성 + 환경 설정
2. **Phase 1.1 시작**: PrescriptionCalculator 첫 번째 테스트 작성

어떤 방식으로 진행하시겠습니까?
- **함께 페어 프로그래밍**: 제가 Red-Green-Refactor 단계별로 가이드
- **자동 구현**: 제가 TDD 사이클 따라 자동 구현 후 커밋별로 확인
