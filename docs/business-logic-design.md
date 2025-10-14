# 비즈니스 로직 상세 설계

## 핵심 비즈니스 로직

### 1. 처방 상태 계산
### 2. 주차 계산
### 3. 일일 검사 등록 로직
### 4. 주차별 추이 분석 로직

---

## 1. 처방 상태 계산 로직

### 상태 정의

```kotlin
enum class PrescriptionStatus {
    PENDING,    // 대기: 생성 후 활성화 안 됨
    ACTIVE,     // 활성: 활성화 후 42일(6주)까지
    COMPLETED,  // 완료: 활성화 후 43일째부터
    EXPIRED     // 만료: 생성 후 활성화 없이 6주 경과
}
```

### 상태 전이도

```
[생성]
  ↓
[PENDING] ─(활성화)→ [ACTIVE] ─(42일 경과)→ [COMPLETED]
  ↓
  (6주 경과, 활성화 안 함)
  ↓
[EXPIRED]
```

### 계산 로직 (FP: 순수 함수로 분리)

> **설계 결정**: 상태 계산 로직을 순수 함수(`PrescriptionCalculator`)로 분리하여 테스트 용이성과 재사용성 확보

#### PrescriptionCalculator (순수 함수)

```kotlin
package com.beyondmedicine.domain.calculator

/**
 * 처방 계산 순수 함수 모듈
 * - 부작용 없음 (No Side Effects)
 * - 같은 입력 → 같은 출력
 * - Mock 없이 테스트 가능
 */
object PrescriptionCalculator {

    private const val ACTIVE_PERIOD_DAYS = 42L
    private const val EXPIRATION_WEEKS = 6L

    /**
     * 처방 상태 계산 (순수 함수)
     */
    fun calculateStatus(
        createdAt: LocalDateTime,
        activatedAt: LocalDateTime?,
        currentTime: LocalDateTime
    ): PrescriptionStatus = when {
        activatedAt != null -> calculateActivatedStatus(activatedAt, currentTime)
        else -> calculateNotActivatedStatus(createdAt, currentTime)
    }

    private fun calculateActivatedStatus(
        activatedAt: LocalDateTime,
        currentTime: LocalDateTime
    ): PrescriptionStatus {
        val endOfActiveWeek = activatedAt
            .plusDays(ACTIVE_PERIOD_DAYS)
            .withHour(23).withMinute(59).withSecond(59).withNano(999_999_999)

        return if (currentTime.isAfter(endOfActiveWeek)) {
            PrescriptionStatus.COMPLETED
        } else {
            PrescriptionStatus.ACTIVE
        }
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
}
```

#### Prescription Entity (순수 함수에 위임)

```kotlin
@Entity
@Table(name = "prescriptions")
class Prescription(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 8)
    val code: String,

    @Column(nullable = false)
    val createdAt: LocalDateTime,

    @Column(nullable = true)
    var activatedAt: LocalDateTime? = null
) {
    /**
     * 처방 상태 조회
     * - 계산 로직은 순수 함수에 위임
     */
    fun getStatus(currentTime: LocalDateTime = LocalDateTime.now()): PrescriptionStatus {
        return PrescriptionCalculator.calculateStatus(createdAt, activatedAt, currentTime)
    }

    /**
     * 처방 활성화 (상태 변경 - OOP)
     */
    fun activate(activationTime: LocalDateTime = LocalDateTime.now()) {
        require(activatedAt == null) { "이미 활성화된 처방입니다" }
        require(getStatus(activationTime) == PrescriptionStatus.PENDING) {
            "활성화할 수 없는 상태입니다"
        }
        this.activatedAt = activationTime
    }
}
```

### 상태별 예시

```kotlin
// 예시 1: PENDING (생성 후 1일)
val prescription = Prescription(
    code = "ABCD1234",
    createdAt = LocalDateTime.of(2025, 10, 1, 10, 0),
    activatedAt = null
)
prescription.getStatus(LocalDateTime.of(2025, 10, 2, 10, 0))
// → PENDING

// 예시 2: ACTIVE (활성화 후 20일)
prescription.activate(LocalDateTime.of(2025, 10, 5, 9, 0))
prescription.getStatus(LocalDateTime.of(2025, 10, 25, 15, 0))
// → ACTIVE (활성화: 10/5, 종료: 11/15 23:59:59.999)

// 예시 3: COMPLETED (활성화 후 43일)
prescription.getStatus(LocalDateTime.of(2025, 11, 17, 0, 0))
// → COMPLETED (11/16 00:00:00 부터)

// 예시 4: EXPIRED (생성 후 7주, 활성화 안 함)
val expiredPrescription = Prescription(
    code = "EFGH5678",
    createdAt = LocalDateTime.of(2025, 10, 1, 10, 0),
    activatedAt = null
)
expiredPrescription.getStatus(LocalDateTime.of(2025, 11, 20, 10, 0))
// → EXPIRED (10/1 생성, 11/12 만료)
```

---

## 2. 주차 계산 로직

### 주차 정의
- **1주차**: 활성화 날짜(D+0) ~ D+6 (7일간)
- **2주차**: D+7 ~ D+13 (7일간)
- ...
- **6주차**: D+35 ~ D+41 (7일간)

### 계산 로직 (FP: 순수 함수로 분리)

> **설계 결정**: 주차 계산을 순수 함수(`WeekCalculator`)로 분리하여 독립적 테스트 가능

#### WeekCalculator (순수 함수)

```kotlin
package com.beyondmedicine.domain.calculator

object WeekCalculator {

    private const val DAYS_PER_WEEK = 7
    private const val MAX_ACTIVE_DAYS = 41L

    /**
     * 주차 번호 계산 (순수 함수)
     */
    fun calculateWeekNumber(
        activationDate: LocalDate,
        assessmentDate: LocalDate
    ): Int? {
        if (assessmentDate.isBefore(activationDate)) return null

        val daysSinceActivation = ChronoUnit.DAYS.between(activationDate, assessmentDate)

        if (daysSinceActivation > MAX_ACTIVE_DAYS) return null

        return (daysSinceActivation / DAYS_PER_WEEK).toInt() + 1
    }

    /**
     * 주차 유효성 검증 (순수 함수)
     */
    fun isValidWeekNumber(weekNumber: Int?): Boolean {
        return weekNumber != null && weekNumber in 1..6
    }
}
```

#### Prescription Entity (순수 함수에 위임)

```kotlin
class Prescription(...) {
    /**
     * 주차 번호 계산
     * - 계산 로직은 순수 함수에 위임
     */
    fun calculateWeekNumber(assessmentDate: LocalDate): Int? {
        val activationDate = activatedAt?.toLocalDate() ?: return null
        return WeekCalculator.calculateWeekNumber(activationDate, assessmentDate)
    }

    fun getCurrentWeek(currentDate: LocalDate = LocalDate.now()): Int? {
        return calculateWeekNumber(currentDate)
    }
}
```

### 주차 계산 예시

```kotlin
// 활성화: 2025-10-01 (수요일)
val prescription = Prescription(
    code = "ABCD1234",
    createdAt = LocalDateTime.of(2025, 9, 25, 10, 0),
    activatedAt = LocalDateTime.of(2025, 10, 1, 9, 0)
)

// 1주차: 10/1 ~ 10/7
prescription.calculateWeekNumber(LocalDate.of(2025, 10, 1))  // → 1
prescription.calculateWeekNumber(LocalDate.of(2025, 10, 7))  // → 1

// 2주차: 10/8 ~ 10/14
prescription.calculateWeekNumber(LocalDate.of(2025, 10, 8))  // → 2
prescription.calculateWeekNumber(LocalDate.of(2025, 10, 14)) // → 2

// 3주차: 10/15 ~ 10/21
prescription.calculateWeekNumber(LocalDate.of(2025, 10, 15)) // → 3

// 6주차: 11/5 ~ 11/11
prescription.calculateWeekNumber(LocalDate.of(2025, 11, 11)) // → 6

// 범위 밖: 11/12 (D+42)
prescription.calculateWeekNumber(LocalDate.of(2025, 11, 12)) // → null
```

---

## 3. 일일 검사 등록 로직

### 비즈니스 규칙

1. **처방 상태 검증**: ACTIVE 상태만 검사 가능
2. **하루 1회 제한**: 같은 날짜에 중복 검사 불가
3. **주차 범위 검증**: 1~6주차 사이만 가능
4. **점수 범위 검증**: 0~10 범위
5. **통증 부위 제한**: 최대 6개
6. **통증 부위 중복 검증**: 같은 부위 중복 불가

### Service 계층 로직

```kotlin
@Service
@Transactional
class AssessmentService(
    private val prescriptionRepository: PrescriptionRepository,
    private val dailyAssessmentRepository: DailyAssessmentRepository
) {

    /**
     * 일일 검사 등록
     */
    fun createAssessment(command: CreateDailyAssessmentCommand): DailyAssessmentResult {
        // 1. 처방 조회
        val prescription = prescriptionRepository.findByCode(command.prescriptionCode)
            ?: throw PrescriptionNotFoundException("처방을 찾을 수 없습니다: ${command.prescriptionCode}")

        // 2. 처방 상태 검증
        validatePrescriptionStatus(prescription, command.assessmentDate)

        // 3. 중복 검사 검증
        validateDuplicateAssessment(prescription, command.assessmentDate)

        // 4. 주차 계산 및 검증
        val weekNumber = calculateAndValidateWeekNumber(prescription, command.assessmentDate)

        // 5. 통증 부위 검증
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
     * 처방 상태 검증
     */
    private fun validatePrescriptionStatus(prescription: Prescription, assessmentDate: LocalDate) {
        val currentDateTime = assessmentDate.atTime(LocalTime.now())

        if (!prescription.canPerformAssessment(currentDateTime)) {
            val status = prescription.getStatus(currentDateTime)
            throw InvalidPrescriptionStatusException(
                "검사를 수행할 수 없는 처방 상태입니다. " +
                "현재 상태: $status, 처방 코드: ${prescription.code}"
            )
        }
    }

    /**
     * 중복 검사 검증
     */
    private fun validateDuplicateAssessment(prescription: Prescription, assessmentDate: LocalDate) {
        val exists = dailyAssessmentRepository.existsByPrescriptionAndAssessmentDate(
            prescription, assessmentDate
        )

        if (exists) {
            throw DuplicateAssessmentException(
                "해당 날짜에 이미 검사가 등록되어 있습니다. " +
                "날짜: $assessmentDate, 처방 코드: ${prescription.code}"
            )
        }
    }

    /**
     * 주차 계산 및 검증
     */
    private fun calculateAndValidateWeekNumber(
        prescription: Prescription,
        assessmentDate: LocalDate
    ): Int {
        val weekNumber = prescription.calculateWeekNumber(assessmentDate)
            ?: throw InvalidAssessmentDateException(
                "유효하지 않은 검사 일자입니다. " +
                "검사는 활성화 후 1~6주차 사이에만 가능합니다. " +
                "검사 일자: $assessmentDate"
            )

        require(weekNumber in 1..6) {
            "주차는 1~6 사이여야 합니다. 계산된 주차: $weekNumber"
        }

        return weekNumber
    }

    /**
     * 통증 부위 검증
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
            "중복된 통증 부위가 있습니다: ${duplicateLocations.joinToString()}"
        }
    }

    /**
     * DailyAssessment 엔티티 생성
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

        // 통증 부위 추가
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

### 검증 순서 및 예외 처리

```kotlin
// 1. 처방 없음 → PrescriptionNotFoundException (404)
// 2. 처방 상태 오류 → InvalidPrescriptionStatusException (400)
// 3. 중복 검사 → DuplicateAssessmentException (409)
// 4. 주차 범위 오류 → InvalidAssessmentDateException (400)
// 5. 통증 부위 검증 오류 → IllegalArgumentException (400)
```

---

## 4. 주차별 추이 분석 로직

### 비즈니스 규칙

1. **시작/종료 주차 기본값 처리**
   - 시작 주차 null → 1
   - 종료 주차 null → 현재 주차

2. **주차 범위 검증**
   - 시작 주차 ≤ 종료 주차
   - 1 ≤ 주차 ≤ 6

3. **데이터 없는 주차 제외**
   - 검사 데이터가 없는 주차는 응답에서 제외

4. **변화율 계산**
   - 첫 번째 주차: null
   - 이후 주차: 이전 주차 대비 변화율 계산
   - **중요**: 데이터 있는 가장 최근 주차 기준

5. **Top 3 통증 부위 집계**
   - 기록 횟수 기준 내림차순
   - 동일 횟수면 평균 강도 높은 순

### Service 계층 로직

```kotlin
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
            ?: throw PrescriptionNotFoundException("처방을 찾을 수 없습니다: ${query.prescriptionCode}")

        // 2. 주차 범위 검증
        validateWeekRange(query.startWeek, query.endWeek)

        // 3. 주차별 집계 데이터 조회
        val weeklyAggregations = dailyAssessmentRepository.findWeeklyAggregations(
            prescription = prescription,
            startWeek = query.startWeek,
            endWeek = query.endWeek
        )

        // 4. 데이터 없는 경우 처리
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

    /**
     * Top 3 통증 부위 집계
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

### 변화율 계산 로직 (Mapper에서 처리)

```kotlin
/**
 * 변화율 계산
 * - 통증/스트레스: 낮을수록 좋음 (감소 = 호전 = 양수)
 * - 턱 기능: 높을수록 좋음 (증가 = 호전 = 양수)
 */
private fun calculateChangeRate(
    previous: Double,
    current: Double,
    isHigherBetter: Boolean
): Double {
    // 이전 값이 0인 경우 변화율 계산 불가
    if (previous == 0.0) return 0.0

    // 기본 변화율: ((현재 - 이전) / 이전) * 100
    val rawChangeRate = ((current - previous) / previous) * 100

    // 낮을수록 좋은 지표는 부호 반전
    val adjustedChangeRate = if (isHigherBetter) rawChangeRate else -rawChangeRate

    // 소수점 첫째 자리까지 반올림
    return String.format("%.1f", adjustedChangeRate).toDouble()
}
```

### 변화율 예시

```kotlin
// 통증 점수 (낮을수록 좋음)
// 1주차: 7.0 → 2주차: 6.0
// 변화율 = -((6.0 - 7.0) / 7.0) * 100 = 14.3% (호전)

// 스트레스 점수 (낮을수록 좋음)
// 1주차: 5.0 → 2주차: 4.0
// 변화율 = -((4.0 - 5.0) / 5.0) * 100 = 20.0% (호전)

// 턱 기능 점수 (높을수록 좋음)
// 1주차: 6.0 → 2주차: 5.5
// 변화율 = ((5.5 - 6.0) / 6.0) * 100 = -8.3% (악화)

// 4주차 계산 시 3주차 데이터 없으면?
// → 2주차 기준으로 변화율 계산 (가장 최근 데이터 기준)
```

---

## 비즈니스 로직 배치 원칙 (FP + OOP 하이브리드)

### Domain Calculator (FP - 순수 함수)
- ✅ 상태 계산 (`PrescriptionCalculator`)
- ✅ 주차 계산 (`WeekCalculator`)
- ✅ 변화율 계산 (`ChangeRateCalculator`)
- ✅ 부작용 없는 순수 계산 로직

### Domain Entity (OOP - 얇은 Entity)
- ✅ 상태 저장 (JPA 영속성)
- ✅ 상태 변경 (활성화)
- ✅ 순수 함수에 계산 위임

### Application Layer (OOP - Service)
- ✅ 유즈케이스 흐름 제어
- ✅ 트랜잭션 경계
- ✅ 여러 엔티티 간 조합
- ✅ I/O 처리 (Repository 호출)
- ✅ 순수 함수 활용

### Presentation Layer (OOP - Mapper)
- ✅ DTO 변환
- ✅ 순수 함수(`ChangeRateCalculator`) 활용
- ✅ API 스펙에 맞는 데이터 가공

---

## 예외 처리 전략

### Custom Exception

```kotlin
// 도메인 예외
sealed class DomainException(message: String) : RuntimeException(message)

class PrescriptionNotFoundException(message: String) : DomainException(message)
class InvalidPrescriptionStatusException(message: String) : DomainException(message)
class DuplicateAssessmentException(message: String) : DomainException(message)
class InvalidAssessmentDateException(message: String) : DomainException(message)

// HTTP 상태 코드 매핑
// PrescriptionNotFoundException → 404
// InvalidPrescriptionStatusException → 400
// DuplicateAssessmentException → 409
// InvalidAssessmentDateException → 400
```

---

## 테스트 시나리오

### 처방 상태 계산 테스트
- [ ] PENDING: 생성 후 활성화 전
- [ ] ACTIVE: 활성화 후 42일까지
- [ ] COMPLETED: 활성화 후 43일부터
- [ ] EXPIRED: 생성 후 6주 경과, 활성화 안 함
- [ ] 경계값: D+41 23:59:59 (ACTIVE), D+42 00:00:00 (COMPLETED)

### 주차 계산 테스트
- [ ] 1주차: D+0 ~ D+6
- [ ] 6주차: D+35 ~ D+41
- [ ] 범위 밖: D+42 → null
- [ ] 활성화 전: null

### 일일 검사 등록 테스트
- [ ] 정상 등록
- [ ] 처방 없음 → 404
- [ ] 처방 상태 오류 (PENDING, COMPLETED, EXPIRED) → 400
- [ ] 중복 검사 → 409
- [ ] 통증 부위 7개 → 400
- [ ] 통증 부위 중복 → 400

### 주차별 추이 분석 테스트
- [ ] 전체 주차 조회
- [ ] 부분 주차 조회
- [ ] 데이터 없는 주차 제외
- [ ] 변화율 계산 (첫 주차 null)
- [ ] Top 3 통증 부위 집계

---

## 요구사항 추적표

### 1. 처방 상태 계산 (과제 PDF 5페이지)

| 과제 요구사항 | 구현 위치 | 상태 |
|-------------|----------|------|
| 대기: 생성 후 활성화 되지 않음 | `Prescription.getStatus()` | ✅ |
| 활성: 활성화 후 D+41의 23:59:59.999 까지 | `Prescription.getStatus()` | ✅ |
| 완료: 활성화 후 D+42의 00:00:00.000 부터 | `Prescription.getStatus()` | ✅ |
| 만료: 생성 후 활성화 없이 6주 경과 | `Prescription.getStatus()` | ✅ |

### 2. 주차 계산 (과제 PDF 6페이지)

| 과제 요구사항 | 구현 위치 | 상태 |
|-------------|----------|------|
| 활성화 날짜로부터 경과한 주 수 + 1 | `Prescription.calculateWeekNumber()` | ✅ |
| 예시 검증: 09-01 활성화, 09-15 검사 → 3주차 | 주차 계산 예시 코드 | ✅ |

### 3. 일일 검사 등록 (과제 PDF 6페이지)

| 과제 요구사항 | 구현 위치 | 상태 |
|-------------|----------|------|
| 처방 코드: 영대문자 4자 + 숫자 4자 | `PrescriptionCodeValidator` | ✅ |
| 처방 상태 검증: 유효한 상태인지 확인 | `validatePrescriptionStatus()` | ✅ |
| 하루 1회 제한 | `validateDuplicateAssessment()` | ✅ |
| 점수 범위: 0~10 | Bean Validation + Entity init | ✅ |
| 통증 부위: 최소 0개, 최대 6개 | `validatePainAreas()` | ✅ |
| 통증 강도: 0~10 | Entity init | ✅ |
| 주차 자동 계산 | `calculateAndValidateWeekNumber()` | ✅ |
| 등록 성공 시 검사 ID, 주차, 처방 코드 응답 | `DailyAssessmentResult` | ✅ |

### 4. 주차별 추이 분석 (과제 PDF 7-8페이지)

| 과제 요구사항 | 구현 위치 | 상태 |
|-------------|----------|------|
| 처방코드 유효성 검증 | Service 계층 | ✅ |
| 시작/종료 주차: 1~6 범위 | `validateWeekRange()` | ✅ |
| 시작 주차 null → 1 처리 | Mapper | ✅ |
| 종료 주차 null → 현재 주차 처리 | Mapper | ✅ |
| 주차별 평균 계산 (통증, 스트레스, 턱 기능) | Repository 집계 | ✅ |
| 전주 대비 변화율(%) | Mapper `calculateChangeRate()` | ✅ |
| 통증/스트레스: 낮을수록 좋음 (부호 반전) | `isHigherBetter = false` | ✅ |
| 턱 기능: 높을수록 좋음 | `isHigherBetter = true` | ✅ |
| 데이터 없는 주차 제외 | Service 로직 | ✅ |
| 변화율 소수점 첫째 자리 반올림 | `String.format("%.1f")` | ✅ |
| 지난 주차 = 데이터 있는 가장 최근 주차 | Mapper `mapIndexed` | ✅ |
| Top 3 통증 부위 집계 | `aggregateTopPainAreas()` | ✅ |

### 5. 추가 구현 사항 (과제 명시 없음, 합리적 판단)

| 추가 구현 | 구현 위치 | 이유 |
|---------|----------|------|
| 통증 부위 중복 검증 | `validatePainAreas()` | 논리적 오류 방지 |
| 처방 활성화 메서드 | `Prescription.activate()` | 도메인 로직 완성도 |
| 예외 타입 세분화 | Custom Exception | 명확한 에러 처리 |
| 주차 범위 검증 (1~6) | `calculateWeekNumber()` | 데이터 무결성 |

---

## 참고 문서
- [설계 의사결정 문서](./design-decisions.md)
- [FP + OOP 하이브리드 설계](./fp-hybrid-design.md)
- [API 설계 문서](./api-design.md)
