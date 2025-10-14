# Service 계층 상세 설계

## Service 계층의 역할

### 핵심 책임
1. **유즈케이스 흐름 제어**: 비즈니스 로직 조합 및 순서 관리
2. **트랜잭션 관리**: `@Transactional` 경계 설정
3. **도메인 객체 조합**: 여러 엔티티 간 상호작용 조율
4. **예외 처리 및 변환**: 도메인 예외를 적절한 애플리케이션 예외로 변환

### 책임 범위 (하지 않는 것)
- ❌ 도메인 규칙 (→ Entity에 위임)
- ❌ 데이터 변환 (→ Mapper에 위임)
- ❌ HTTP 처리 (→ Controller에 위임)

---

## Service 구조

```
application/
├── service/
│   ├── AssessmentService.kt              # 일일 검사 등록
│   ├── AssessmentAnalysisService.kt      # 주차별 추이 분석
│   └── PrescriptionService.kt            # 처방 관리 (선택)
├── repository/
│   ├── PrescriptionRepository.kt
│   ├── DailyAssessmentRepository.kt
│   └── PainAreaRepository.kt
└── dto/
    ├── CreateDailyAssessmentCommand.kt
    ├── DailyAssessmentResult.kt
    ├── WeeklyTrendQuery.kt
    └── WeeklyTrendData.kt
```

---

## 1. AssessmentService (일일 검사 등록)

### 책임
- 일일 검사 데이터 등록
- 처방 상태 및 중복 검사 검증
- 주차 번호 자동 계산

### 전체 코드

```kotlin
package com.beyondmedicine.application.service

import com.beyondmedicine.application.dto.*
import com.beyondmedicine.application.repository.DailyAssessmentRepository
import com.beyondmedicine.application.repository.PrescriptionRepository
import com.beyondmedicine.domain.model.DailyAssessment
import com.beyondmedicine.domain.model.PainArea
import com.beyondmedicine.domain.model.Prescription
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime

/**
 * 일일 검사 서비스
 * - 일일 검사 데이터 등록 및 관리
 */
@Service
@Transactional
class AssessmentService(
    private val prescriptionRepository: PrescriptionRepository,
    private val dailyAssessmentRepository: DailyAssessmentRepository
) {

    /**
     * 일일 검사 등록
     *
     * @param command 일일 검사 생성 커맨드
     * @return 생성된 검사 결과
     * @throws PrescriptionNotFoundException 처방을 찾을 수 없는 경우
     * @throws InvalidPrescriptionStatusException 처방 상태가 유효하지 않은 경우
     * @throws DuplicateAssessmentException 중복 검사인 경우
     * @throws InvalidAssessmentDateException 유효하지 않은 검사 일자인 경우
     */
    fun createAssessment(command: CreateDailyAssessmentCommand): DailyAssessmentResult {
        // 1. 처방 조회
        val prescription = findPrescription(command.prescriptionCode)

        // 2. 처방 상태 검증 (ACTIVE 상태만 가능)
        validatePrescriptionStatus(prescription, command.assessmentDate)

        // 3. 중복 검사 검증 (하루 1회 제한)
        validateDuplicateAssessment(prescription, command.assessmentDate)

        // 4. 주차 계산 및 검증 (1~6주차)
        val weekNumber = calculateAndValidateWeekNumber(prescription, command.assessmentDate)

        // 5. 통증 부위 검증 (최대 6개, 중복 불가)
        validatePainAreas(command.painAreas)

        // 6. 엔티티 생성 및 저장
        val assessment = createDailyAssessmentEntity(prescription, command, weekNumber)
        val savedAssessment = dailyAssessmentRepository.save(assessment)

        // 7. 결과 반환
        return DailyAssessmentResult(
            assessmentId = savedAssessment.id,
            prescriptionCode = prescription.code,
            weekNumber = weekNumber,
            assessmentDate = command.assessmentDate
        )
    }

    /**
     * 처방 조회
     */
    private fun findPrescription(prescriptionCode: String): Prescription {
        return prescriptionRepository.findByCode(prescriptionCode)
            ?: throw PrescriptionNotFoundException(
                "처방을 찾을 수 없습니다. 처방 코드: $prescriptionCode"
            )
    }

    /**
     * 처방 상태 검증
     * - ACTIVE 상태만 검사 가능
     */
    private fun validatePrescriptionStatus(
        prescription: Prescription,
        assessmentDate: LocalDate
    ) {
        // 검사 일자의 현재 시간으로 변환 (테스트 용이성)
        val assessmentDateTime = assessmentDate.atTime(LocalTime.now())

        if (!prescription.canPerformAssessment(assessmentDateTime)) {
            val status = prescription.getStatus(assessmentDateTime)
            throw InvalidPrescriptionStatusException(
                "검사를 수행할 수 없는 처방 상태입니다. " +
                "현재 상태: $status, 처방 코드: ${prescription.code}, " +
                "검사 일자: $assessmentDate"
            )
        }
    }

    /**
     * 중복 검사 검증
     * - 같은 처방에 같은 날짜 검사 불가
     */
    private fun validateDuplicateAssessment(
        prescription: Prescription,
        assessmentDate: LocalDate
    ) {
        val exists = dailyAssessmentRepository.existsByPrescriptionAndAssessmentDate(
            prescription = prescription,
            assessmentDate = assessmentDate
        )

        if (exists) {
            throw DuplicateAssessmentException(
                "해당 날짜에 이미 검사가 등록되어 있습니다. " +
                "처방 코드: ${prescription.code}, 날짜: $assessmentDate"
            )
        }
    }

    /**
     * 주차 계산 및 검증
     * - 1~6주차 범위 내에서만 검사 가능
     */
    private fun calculateAndValidateWeekNumber(
        prescription: Prescription,
        assessmentDate: LocalDate
    ): Int {
        val weekNumber = prescription.calculateWeekNumber(assessmentDate)
            ?: throw InvalidAssessmentDateException(
                "유효하지 않은 검사 일자입니다. " +
                "검사는 활성화 후 1~6주차(D+0 ~ D+41) 사이에만 가능합니다. " +
                "처방 코드: ${prescription.code}, " +
                "활성화 날짜: ${prescription.activatedAt?.toLocalDate()}, " +
                "검사 일자: $assessmentDate"
            )

        require(weekNumber in 1..6) {
            "주차는 1~6 사이여야 합니다. 계산된 주차: $weekNumber"
        }

        return weekNumber
    }

    /**
     * 통증 부위 검증
     * - 최대 6개
     * - 중복 불가
     */
    private fun validatePainAreas(painAreas: List<PainAreaCommand>) {
        // 개수 검증
        require(painAreas.size <= 6) {
            "통증 부위는 최대 6개까지 등록 가능합니다. 입력 개수: ${painAreas.size}"
        }

        // 중복 검증
        val duplicateLocations = painAreas
            .groupBy { it.location }
            .filter { it.value.size > 1 }
            .keys

        require(duplicateLocations.isEmpty()) {
            "중복된 통증 부위가 있습니다: ${duplicateLocations.joinToString { it.name }}"
        }
    }

    /**
     * DailyAssessment 엔티티 생성
     * - 통증 부위 포함
     */
    private fun createDailyAssessmentEntity(
        prescription: Prescription,
        command: CreateDailyAssessmentCommand,
        weekNumber: Int
    ): DailyAssessment {
        val assessment = DailyAssessment(
            prescription = prescription,
            assessmentDate = command.assessmentDate,
            weekNumber = weekNumber,
            painScore = command.painScore,
            stressScore = command.stressScore,
            jawFunctionScore = command.jawFunctionScore
        )

        // 통증 부위 추가 (양방향 연관관계 설정)
        command.painAreas.forEach { painAreaCommand ->
            val painArea = PainArea(
                dailyAssessment = assessment,
                location = painAreaCommand.location,
                intensity = painAreaCommand.intensity,
                description = painAreaCommand.description
            )
            assessment.painAreas.add(painArea)
        }

        return assessment
    }
}
```

---

## 2. AssessmentAnalysisService (주차별 추이 분석)

### 책임
- 주차별 검사 데이터 집계
- Top 3 통증 부위 집계
- 기본값 처리 (시작/종료 주차)

### 전체 코드

```kotlin
package com.beyondmedicine.application.service

import com.beyondmedicine.application.dto.*
import com.beyondmedicine.application.repository.DailyAssessmentRepository
import com.beyondmedicine.application.repository.PrescriptionRepository
import com.beyondmedicine.domain.model.Prescription
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 검사 분석 서비스
 * - 주차별 추이 분석
 * - 통증 부위 집계
 */
@Service
@Transactional(readOnly = true)
class AssessmentAnalysisService(
    private val prescriptionRepository: PrescriptionRepository,
    private val dailyAssessmentRepository: DailyAssessmentRepository
) {

    /**
     * 주차별 추이 분석
     *
     * @param query 주차별 추이 쿼리
     * @return 주차별 추이 데이터
     * @throws PrescriptionNotFoundException 처방을 찾을 수 없는 경우
     */
    fun analyzeWeeklyTrend(query: WeeklyTrendQuery): WeeklyTrendData {
        // 1. 처방 조회
        val prescription = findPrescription(query.prescriptionCode)

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
        val topPainAreas = aggregateTopPainAreas(
            prescription = prescription,
            startWeek = query.startWeek,
            endWeek = query.endWeek
        )

        return WeeklyTrendData(
            prescriptionCode = prescription.code,
            startWeek = query.startWeek,
            endWeek = query.endWeek,
            weeklyTrends = weeklyTrends,
            topPainAreas = topPainAreas
        )
    }

    /**
     * 처방 조회
     */
    private fun findPrescription(prescriptionCode: String): Prescription {
        return prescriptionRepository.findByCode(prescriptionCode)
            ?: throw PrescriptionNotFoundException(
                "처방을 찾을 수 없습니다. 처방 코드: $prescriptionCode"
            )
    }

    /**
     * 주차 범위 검증
     * - 1~6 사이
     * - 시작 주차 ≤ 종료 주차
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

    /**
     * Top 3 통증 부위 집계
     * - 기록 횟수 기준 내림차순
     * - 동일 횟수면 평균 강도 높은 순
     */
    private fun aggregateTopPainAreas(
        prescription: Prescription,
        startWeek: Int,
        endWeek: Int
    ): List<TopPainAreaData> {
        val aggregations = dailyAssessmentRepository.findTopPainAreas(
            prescription = prescription,
            startWeek = startWeek,
            endWeek = endWeek,
            limit = 3
        )

        return aggregations.map { agg ->
            TopPainAreaData(
                location = agg.location,
                count = agg.count,
                averageIntensity = agg.averageIntensity
            )
        }
    }
}
```

---

## 3. PrescriptionService (선택적, 테스트 데이터용)

### 책임
- 처방 생성 (테스트 데이터 삽입용)
- 처방 활성화
- 처방 조회

### 코드 (선택적 구현)

```kotlin
package com.beyondmedicine.application.service

import com.beyondmedicine.application.repository.PrescriptionRepository
import com.beyondmedicine.domain.model.Prescription
import com.beyondmedicine.domain.model.PrescriptionStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 처방 서비스
 * - 처방 생성 및 활성화
 * - 주로 테스트 데이터 삽입용
 */
@Service
@Transactional
class PrescriptionService(
    private val prescriptionRepository: PrescriptionRepository
) {

    /**
     * 처방 생성
     */
    fun createPrescription(
        code: String,
        createdAt: LocalDateTime = LocalDateTime.now()
    ): Prescription {
        // 중복 코드 검증
        if (prescriptionRepository.existsByCode(code)) {
            throw IllegalArgumentException("이미 존재하는 처방 코드입니다: $code")
        }

        val prescription = Prescription(
            code = code,
            createdAt = createdAt
        )

        return prescriptionRepository.save(prescription)
    }

    /**
     * 처방 활성화
     */
    fun activatePrescription(
        prescriptionCode: String,
        activationTime: LocalDateTime = LocalDateTime.now()
    ): Prescription {
        val prescription = prescriptionRepository.findByCode(prescriptionCode)
            ?: throw PrescriptionNotFoundException("처방을 찾을 수 없습니다: $prescriptionCode")

        prescription.activate(activationTime)

        return prescriptionRepository.save(prescription)
    }

    /**
     * 처방 조회 (상태 포함)
     */
    @Transactional(readOnly = true)
    fun getPrescriptionWithStatus(prescriptionCode: String): PrescriptionInfo {
        val prescription = prescriptionRepository.findByCode(prescriptionCode)
            ?: throw PrescriptionNotFoundException("처방을 찾을 수 없습니다: $prescriptionCode")

        return PrescriptionInfo(
            code = prescription.code,
            createdAt = prescription.createdAt,
            activatedAt = prescription.activatedAt,
            status = prescription.getStatus()
        )
    }
}

/**
 * 처방 정보 DTO
 */
data class PrescriptionInfo(
    val code: String,
    val createdAt: LocalDateTime,
    val activatedAt: LocalDateTime?,
    val status: PrescriptionStatus
)
```

---

## 4. 예외 정의

### Custom Exception 계층 구조

```kotlin
package com.beyondmedicine.application.exception

/**
 * 도메인 예외 기본 클래스
 */
sealed class DomainException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * 처방 없음 예외 (404)
 */
class PrescriptionNotFoundException(
    message: String
) : DomainException(message)

/**
 * 유효하지 않은 처방 상태 예외 (400)
 */
class InvalidPrescriptionStatusException(
    message: String
) : DomainException(message)

/**
 * 중복 검사 예외 (409)
 */
class DuplicateAssessmentException(
    message: String
) : DomainException(message)

/**
 * 유효하지 않은 검사 일자 예외 (400)
 */
class InvalidAssessmentDateException(
    message: String
) : DomainException(message)
```

### HTTP 상태 코드 매핑

| 예외 | HTTP 상태 코드 | 설명 |
|-----|---------------|------|
| `PrescriptionNotFoundException` | 404 Not Found | 처방을 찾을 수 없음 |
| `InvalidPrescriptionStatusException` | 400 Bad Request | 검사 불가능한 처방 상태 |
| `DuplicateAssessmentException` | 409 Conflict | 중복 검사 |
| `InvalidAssessmentDateException` | 400 Bad Request | 유효하지 않은 검사 일자 |
| `IllegalArgumentException` | 400 Bad Request | 잘못된 입력 값 |

---

## 5. 트랜잭션 전략

### 읽기 전용 트랜잭션
```kotlin
@Transactional(readOnly = true)
class AssessmentAnalysisService {
    // 조회 전용 서비스
}
```

**장점:**
- 성능 최적화 (flush 생략)
- 의도 명확화 (읽기 전용)
- 실수로 데이터 수정 방지

### 쓰기 트랜잭션
```kotlin
@Transactional
class AssessmentService {
    fun createAssessment(...) {
        // 검증 실패 시 자동 롤백
        // 저장 성공 시 자동 커밋
    }
}
```

**전파 레벨:** 기본값 `REQUIRED` 사용

---

## 6. 테스트 전략

### 단위 테스트 (Service Layer)

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
    fun `일일 검사를 정상 등록한다`() {
        // given
        val prescription = createActivePrescription()
        val command = createValidCommand()

        given(prescriptionRepository.findByCode(any())).willReturn(prescription)
        given(dailyAssessmentRepository.existsByPrescriptionAndAssessmentDate(any(), any()))
            .willReturn(false)
        given(dailyAssessmentRepository.save(any())).willAnswer { it.arguments[0] }

        // when
        val result = assessmentService.createAssessment(command)

        // then
        assertNotNull(result)
        assertEquals(command.prescriptionCode, result.prescriptionCode)
        verify(dailyAssessmentRepository).save(any())
    }

    @Test
    fun `처방이 없으면 예외가 발생한다`() {
        // given
        given(prescriptionRepository.findByCode(any())).willReturn(null)

        // when & then
        assertThrows<PrescriptionNotFoundException> {
            assessmentService.createAssessment(createValidCommand())
        }
    }

    @Test
    fun `PENDING 상태 처방은 검사할 수 없다`() {
        // given
        val pendingPrescription = createPendingPrescription()
        given(prescriptionRepository.findByCode(any())).willReturn(pendingPrescription)

        // when & then
        assertThrows<InvalidPrescriptionStatusException> {
            assessmentService.createAssessment(createValidCommand())
        }
    }

    @Test
    fun `중복 검사는 등록할 수 없다`() {
        // given
        val prescription = createActivePrescription()
        given(prescriptionRepository.findByCode(any())).willReturn(prescription)
        given(dailyAssessmentRepository.existsByPrescriptionAndAssessmentDate(any(), any()))
            .willReturn(true)

        // when & then
        assertThrows<DuplicateAssessmentException> {
            assessmentService.createAssessment(createValidCommand())
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

### 통합 테스트 (Service + Repository)

```kotlin
@SpringBootTest
@Transactional
class AssessmentServiceIntegrationTest {

    @Autowired
    private lateinit var assessmentService: AssessmentService

    @Autowired
    private lateinit var prescriptionRepository: PrescriptionRepository

    @Autowired
    private lateinit var dailyAssessmentRepository: DailyAssessmentRepository

    @Test
    fun `일일 검사 등록 전체 플로우 테스트`() {
        // given: 활성화된 처방 생성
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

        // when: 검사 등록
        val result = assessmentService.createAssessment(command)

        // then: 검증
        assertNotNull(result.assessmentId)
        assertEquals("TEST1234", result.prescriptionCode)
        assertEquals(1, result.weekNumber) // 5일 후 = 1주차

        // DB 확인
        val savedAssessment = dailyAssessmentRepository.findById(result.assessmentId).get()
        assertEquals(7, savedAssessment.painScore)
        assertEquals(1, savedAssessment.painAreas.size)
    }
}
```

---

## 7. Service 레이어 설계 원칙 정리

### ✅ Service는 이렇게
1. **유즈케이스 흐름 제어**: 검증 → 조회 → 생성 → 저장 순서 관리
2. **트랜잭션 경계 설정**: `@Transactional` 명시
3. **예외 변환**: 도메인 예외를 애플리케이션 예외로 변환
4. **여러 Repository 조합**: 복잡한 조회/집계 처리

### ❌ Service는 이렇게 하지 않기
1. **도메인 규칙 구현**: Entity에 위임 (`prescription.canPerformAssessment()`)
2. **데이터 변환**: Mapper에 위임
3. **복잡한 계산**: 단순 비즈니스 계산은 도메인에, 복잡한 집계는 Repository에
4. **직접 쿼리 작성**: Repository 메서드로 추상화

---

## 요구사항 추적표

### 1. 일일 검사 등록 서비스 (과제 PDF 6페이지)

| 과제 요구사항 | 구현 위치 | 상태 |
|-------------|----------|------|
| 처방 조회 및 검증 | `findPrescription()` | ✅ |
| 처방 상태 검증 (ACTIVE만 가능) | `validatePrescriptionStatus()` | ✅ |
| 하루 1회 제한 검증 | `validateDuplicateAssessment()` | ✅ |
| 주차 자동 계산 | `calculateAndValidateWeekNumber()` | ✅ |
| 통증 부위 검증 (최대 6개) | `validatePainAreas()` | ✅ |
| 통증 부위 중복 검증 | `validatePainAreas()` | ✅ |
| 엔티티 생성 및 저장 | `createDailyAssessmentEntity()` | ✅ |
| 결과 반환 (ID, 주차, 처방코드) | `DailyAssessmentResult` | ✅ |

### 2. 주차별 추이 분석 서비스 (과제 PDF 7-8페이지)

| 과제 요구사항 | 구현 위치 | 상태 |
|-------------|----------|------|
| 처방 조회 및 검증 | `findPrescription()` | ✅ |
| 주차 범위 검증 (1~6) | `validateWeekRange()` | ✅ |
| 시작 ≤ 종료 검증 | `validateWeekRange()` | ✅ |
| 주차별 집계 조회 | `findWeeklyAggregations()` | ✅ |
| 데이터 없는 경우 빈 응답 | Service 로직 | ✅ |
| Top 3 통증 부위 집계 | `aggregateTopPainAreas()` | ✅ |

### 3. 예외 처리 (과제 명시 없음, 합리적 추가)

| 구현 사항 | 구현 위치 | 상태 |
|---------|----------|------|
| 처방 없음 예외 (404) | `PrescriptionNotFoundException` | ✅ |
| 처방 상태 오류 (400) | `InvalidPrescriptionStatusException` | ✅ |
| 중복 검사 (409) | `DuplicateAssessmentException` | ✅ |
| 검사 일자 오류 (400) | `InvalidAssessmentDateException` | ✅ |

### 4. 트랜잭션 관리 (Spring 표준)

| 구현 사항 | 구현 위치 | 상태 |
|---------|----------|------|
| 쓰기 트랜잭션 | `@Transactional` | ✅ |
| 읽기 전용 트랜잭션 | `@Transactional(readOnly = true)` | ✅ |

---

## 참고 문서
- [설계 의사결정 문서](./design-decisions.md)
- [FP + OOP 하이브리드 설계](./fp-hybrid-design.md)
- [비즈니스 로직 설계](./business-logic-design.md)
- [API 설계 문서](./api-design.md)
