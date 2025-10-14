# FP + OOP 하이브리드 설계

## 설계 철학

### 핵심 원칙
> **"핵심 도메인 계산은 순수 함수로, 경계(트랜잭션, I/O)는 OOP로"**

### 적용 전략

```
┌─────────────────────────────────────┐
│  Presentation Layer                  │  ← OOP (Spring MVC)
│  - Controller, DTO Mapper            │
└─────────────────┬───────────────────┘
                  │
┌─────────────────▼───────────────────┐
│  Application Layer                   │  ← OOP (트랜잭션 경계)
│  - Service (유즈케이스 흐름 제어)    │
│  - 순수 계산은 FP 모듈 호출          │
└─────────────────┬───────────────────┘
                  │
┌─────────────────▼───────────────────┐
│  Domain Layer                        │  ← OOP (얇은 Entity)
│  - Entity (JPA 영속성)               │
│  - 계산 로직은 FP 모듈에 위임        │
└─────────────────┬───────────────────┘
                  │
┌─────────────────▼───────────────────┐
│  Domain Calculators (FP)             │  ← 100% FP (순수 함수)
│  - PrescriptionCalculator            │
│  - ChangeRateCalculator              │
│  - WeekCalculator                    │
└─────────────────────────────────────┘
```

---

## 1. 순수 함수 모듈 (100% FP)

### PrescriptionCalculator (처방 상태 계산)

```kotlin
package com.beyondmedicine.domain.calculator

import com.beyondmedicine.domain.model.PrescriptionStatus
import java.time.LocalDateTime

/**
 * 처방 관련 순수 함수 모음
 * - 부작용 없음 (No Side Effects)
 * - 같은 입력 → 같은 출력 (Referential Transparency)
 * - 테스트 용이 (Easy to Test)
 */
object PrescriptionCalculator {

    private const val ACTIVE_PERIOD_DAYS = 42L  // D+0 ~ D+41
    private const val EXPIRATION_WEEKS = 6L

    /**
     * 처방 상태 계산 (순수 함수)
     *
     * @param createdAt 처방 생성 일시
     * @param activatedAt 처방 활성화 일시 (nullable)
     * @param currentTime 기준 시점
     * @return 계산된 처방 상태
     */
    fun calculateStatus(
        createdAt: LocalDateTime,
        activatedAt: LocalDateTime?,
        currentTime: LocalDateTime
    ): PrescriptionStatus = when {
        activatedAt != null -> calculateActivatedStatus(activatedAt, currentTime)
        else -> calculateNotActivatedStatus(createdAt, currentTime)
    }

    /**
     * 활성화된 처방의 상태 계산
     */
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

    /**
     * 활성화 기간 종료 시점 계산
     * - D+41의 23:59:59.999
     */
    private fun calculateEndOfActiveWeek(activatedAt: LocalDateTime): LocalDateTime {
        return activatedAt
            .plusDays(ACTIVE_PERIOD_DAYS)
            .withHour(23)
            .withMinute(59)
            .withSecond(59)
            .withNano(999_999_999)
    }

    /**
     * 미활성화 처방의 상태 계산
     */
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

    /**
     * 활성화 가능 여부 판단 (순수 함수)
     */
    fun canActivate(
        createdAt: LocalDateTime,
        activatedAt: LocalDateTime?,
        currentTime: LocalDateTime
    ): Boolean {
        return calculateStatus(createdAt, activatedAt, currentTime) == PrescriptionStatus.PENDING
    }

    /**
     * 검사 수행 가능 여부 판단 (순수 함수)
     */
    fun canPerformAssessment(
        createdAt: LocalDateTime,
        activatedAt: LocalDateTime?,
        currentTime: LocalDateTime
    ): Boolean {
        return calculateStatus(createdAt, activatedAt, currentTime) == PrescriptionStatus.ACTIVE
    }
}
```

### WeekCalculator (주차 계산)

```kotlin
package com.beyondmedicine.domain.calculator

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 주차 계산 순수 함수 모음
 */
object WeekCalculator {

    private const val DAYS_PER_WEEK = 7
    private const val MIN_WEEK = 1
    private const val MAX_WEEK = 6
    private const val MAX_ACTIVE_DAYS = 41L

    /**
     * 주차 번호 계산 (순수 함수)
     *
     * @param activationDate 활성화 날짜
     * @param assessmentDate 검사 날짜
     * @return 주차 번호 (1~6), 범위 밖이면 null
     */
    fun calculateWeekNumber(
        activationDate: LocalDate,
        assessmentDate: LocalDate
    ): Int? {
        // 활성화 이전 날짜
        if (assessmentDate.isBefore(activationDate)) return null

        val daysSinceActivation = ChronoUnit.DAYS.between(activationDate, assessmentDate)

        // 6주(42일) 초과
        if (daysSinceActivation > MAX_ACTIVE_DAYS) return null

        // 주차 계산: (경과 일수 / 7) + 1
        return (daysSinceActivation / DAYS_PER_WEEK).toInt() + 1
    }

    /**
     * 특정 주차의 날짜 범위 계산 (순수 함수)
     *
     * @param activationDate 활성화 날짜
     * @param weekNumber 주차 번호 (1~6)
     * @return 시작일 ~ 종료일 Pair
     */
    fun calculateWeekRange(
        activationDate: LocalDate,
        weekNumber: Int
    ): Pair<LocalDate, LocalDate>? {
        require(weekNumber in MIN_WEEK..MAX_WEEK) {
            "주차는 ${MIN_WEEK}~${MAX_WEEK} 사이여야 합니다. 입력: $weekNumber"
        }

        val startDate = activationDate.plusDays((weekNumber - 1) * DAYS_PER_WEEK.toLong())
        val endDate = startDate.plusDays(DAYS_PER_WEEK.toLong() - 1)

        return Pair(startDate, endDate)
    }

    /**
     * 주차 범위 검증 (순수 함수)
     */
    fun isValidWeekNumber(weekNumber: Int?): Boolean {
        return weekNumber != null && weekNumber in MIN_WEEK..MAX_WEEK
    }
}
```

### ChangeRateCalculator (변화율 계산)

```kotlin
package com.beyondmedicine.domain.calculator

/**
 * 변화율 계산 순수 함수 모음
 * - 완전한 순수 함수 (No Side Effects)
 * - 수학적 계산만 수행
 */
object ChangeRateCalculator {

    /**
     * 변화율 계산 (순수 함수)
     *
     * @param previous 이전 값
     * @param current 현재 값
     * @param metric 지표 타입
     * @return 변화율(%), 계산 불가 시 null
     */
    fun calculate(
        previous: Double,
        current: Double,
        metric: MetricType
    ): Double? {
        if (previous == 0.0) return null  // 0으로 나눌 수 없음

        val rawRate = calculateRawRate(previous, current)
        val adjustedRate = metric.adjustRate(rawRate)

        return roundToOneDecimal(adjustedRate)
    }

    /**
     * 여러 지표의 변화율 일괄 계산 (함수형 스타일)
     */
    fun calculateAll(
        previousScores: ScoreSet,
        currentScores: ScoreSet
    ): ChangeRates {
        return ChangeRates(
            pain = calculate(previousScores.pain, currentScores.pain, MetricType.LOWER_IS_BETTER),
            stress = calculate(previousScores.stress, currentScores.stress, MetricType.LOWER_IS_BETTER),
            jawFunction = calculate(previousScores.jawFunction, currentScores.jawFunction, MetricType.HIGHER_IS_BETTER)
        )
    }

    /**
     * 원시 변화율 계산
     */
    private fun calculateRawRate(previous: Double, current: Double): Double {
        return ((current - previous) / previous) * 100
    }

    /**
     * 소수점 첫째 자리 반올림
     */
    private fun roundToOneDecimal(value: Double): Double {
        return String.format("%.1f", value).toDouble()
    }
}

/**
 * 지표 타입 (Sealed Class로 타입 안전성 확보)
 */
sealed class MetricType {
    /**
     * 변화율 조정 (지표 특성에 따라)
     */
    abstract fun adjustRate(rawRate: Double): Double

    /**
     * 낮을수록 좋은 지표 (통증, 스트레스)
     * - 감소하면 호전 → 양수
     * - 증가하면 악화 → 음수
     */
    object LOWER_IS_BETTER : MetricType() {
        override fun adjustRate(rawRate: Double): Double = -rawRate
    }

    /**
     * 높을수록 좋은 지표 (턱 기능)
     * - 증가하면 호전 → 양수
     * - 감소하면 악화 → 음수
     */
    object HIGHER_IS_BETTER : MetricType() {
        override fun adjustRate(rawRate: Double): Double = rawRate
    }
}

/**
 * 불변 점수 세트 (Immutable Data)
 */
data class ScoreSet(
    val pain: Double,
    val stress: Double,
    val jawFunction: Double
)

/**
 * 변화율 결과 (Immutable Data)
 */
data class ChangeRates(
    val pain: Double?,
    val stress: Double?,
    val jawFunction: Double?
)
```

---

## 2. 얇은 Entity (Thin Entity Pattern)

### Prescription (OOP + FP 위임)

```kotlin
package com.beyondmedicine.domain.model

import com.beyondmedicine.domain.calculator.PrescriptionCalculator
import com.beyondmedicine.domain.calculator.WeekCalculator
import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.*

/**
 * 처방 엔티티
 * - 상태 저장 (OOP)
 * - 계산 로직은 FP 모듈에 위임
 */
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
) {
    /**
     * 처방 상태 조회
     * - 계산 로직은 순수 함수에 위임
     */
    fun getStatus(currentTime: LocalDateTime = LocalDateTime.now()): PrescriptionStatus {
        return PrescriptionCalculator.calculateStatus(
            createdAt = createdAt,
            activatedAt = activatedAt,
            currentTime = currentTime
        )
    }

    /**
     * 주차 번호 계산
     * - 계산 로직은 순수 함수에 위임
     */
    fun calculateWeekNumber(assessmentDate: LocalDate): Int? {
        val activationDate = activatedAt?.toLocalDate() ?: return null
        return WeekCalculator.calculateWeekNumber(activationDate, assessmentDate)
    }

    /**
     * 현재 주차 조회
     */
    fun getCurrentWeek(currentDate: LocalDate = LocalDate.now()): Int? {
        return calculateWeekNumber(currentDate)
    }

    /**
     * 활성화 가능 여부
     * - 판단 로직은 순수 함수에 위임
     */
    fun canActivate(currentTime: LocalDateTime = LocalDateTime.now()): Boolean {
        return PrescriptionCalculator.canActivate(
            createdAt = createdAt,
            activatedAt = activatedAt,
            currentTime = currentTime
        )
    }

    /**
     * 검사 수행 가능 여부
     * - 판단 로직은 순수 함수에 위임
     */
    fun canPerformAssessment(currentTime: LocalDateTime = LocalDateTime.now()): Boolean {
        return PrescriptionCalculator.canPerformAssessment(
            createdAt = createdAt,
            activatedAt = activatedAt,
            currentTime = currentTime
        )
    }

    /**
     * 처방 활성화 (상태 변경 - OOP)
     * - 검증은 순수 함수 활용
     * - 상태 변경은 Entity가 캡슐화
     */
    fun activate(activationTime: LocalDateTime = LocalDateTime.now()) {
        require(activatedAt == null) {
            "이미 활성화된 처방입니다. 활성화 일시: $activatedAt"
        }
        require(canActivate(activationTime)) {
            "활성화할 수 없는 상태입니다. 현재 상태: ${getStatus(activationTime)}"
        }

        this.activatedAt = activationTime
    }
}
```

---

## 3. Service 계층 (OOP + FP 호출)

### AssessmentService

```kotlin
package com.beyondmedicine.application.service

import com.beyondmedicine.application.dto.*
import com.beyondmedicine.application.repository.DailyAssessmentRepository
import com.beyondmedicine.application.repository.PrescriptionRepository
import com.beyondmedicine.domain.calculator.WeekCalculator
import com.beyondmedicine.domain.model.DailyAssessment
import com.beyondmedicine.domain.model.PainArea
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime

/**
 * 일일 검사 서비스
 * - OOP: 트랜잭션 경계, I/O 처리
 * - FP: 순수 계산은 Calculator 호출
 */
@Service
@Transactional
class AssessmentService(
    private val prescriptionRepository: PrescriptionRepository,
    private val dailyAssessmentRepository: DailyAssessmentRepository
) {

    fun createAssessment(command: CreateDailyAssessmentCommand): DailyAssessmentResult {
        // 1. 처방 조회 (I/O - OOP)
        val prescription = prescriptionRepository.findByCode(command.prescriptionCode)
            ?: throw PrescriptionNotFoundException("처방을 찾을 수 없습니다: ${command.prescriptionCode}")

        // 2. 처방 상태 검증 (순수 함수 활용)
        validatePrescriptionStatus(prescription, command.assessmentDate)

        // 3. 중복 검사 검증 (I/O - OOP)
        validateDuplicateAssessment(prescription, command.assessmentDate)

        // 4. 주차 계산 (순수 함수 활용)
        val weekNumber = calculateAndValidateWeekNumber(prescription, command.assessmentDate)

        // 5. 통증 부위 검증 (순수 로직)
        validatePainAreas(command.painAreas)

        // 6. 엔티티 생성 및 저장 (OOP)
        val assessment = createDailyAssessmentEntity(prescription, command, weekNumber)
        val savedAssessment = dailyAssessmentRepository.save(assessment)

        return DailyAssessmentResult(
            assessmentId = savedAssessment.id,
            prescriptionCode = prescription.code,
            weekNumber = weekNumber,
            assessmentDate = command.assessmentDate
        )
    }

    /**
     * 주차 계산 및 검증
     * - 순수 함수(WeekCalculator) 활용
     */
    private fun calculateAndValidateWeekNumber(
        prescription: Prescription,
        assessmentDate: LocalDate
    ): Int {
        val weekNumber = prescription.calculateWeekNumber(assessmentDate)
            ?: throw InvalidAssessmentDateException(
                "유효하지 않은 검사 일자입니다. " +
                "검사는 활성화 후 1~6주차(D+0 ~ D+41) 사이에만 가능합니다."
            )

        // 순수 함수로 검증
        require(WeekCalculator.isValidWeekNumber(weekNumber)) {
            "주차는 1~6 사이여야 합니다. 계산된 주차: $weekNumber"
        }

        return weekNumber
    }

    // ... 기타 메서드는 동일
}
```

---

## 4. DTO Mapper (FP Calculator 활용)

### AssessmentDtoMapper

```kotlin
package com.beyondmedicine.presentation.dto.mapper

import com.beyondmedicine.application.dto.*
import com.beyondmedicine.domain.calculator.ChangeRateCalculator
import com.beyondmedicine.domain.calculator.MetricType
import com.beyondmedicine.domain.calculator.ScoreSet
import com.beyondmedicine.presentation.dto.response.*
import org.springframework.stereotype.Component

@Component
class AssessmentDtoMapper {

    /**
     * 주차별 추이 데이터 변환
     * - 변화율 계산은 FP Calculator 활용
     */
    fun toWeeklyTrendResponse(data: WeeklyTrendData): WeeklyTrendResponse {
        return WeeklyTrendResponse(
            prescriptionCode = data.prescriptionCode,
            period = PeriodInfo(data.startWeek, data.endWeek),
            weeklyTrends = toWeeklyTrendInfoList(data.weeklyTrends),
            topPainAreas = data.topPainAreas.map { toTopPainAreaInfo(it) }
        )
    }

    /**
     * 주차별 데이터 변환 (변화율 계산 포함)
     * - 순수 함수(ChangeRateCalculator) 활용
     */
    private fun toWeeklyTrendInfoList(weeklyDataList: List<WeeklyData>): List<WeeklyTrendInfo> {
        return weeklyDataList.mapIndexed { index, current ->
            WeeklyTrendInfo(
                weekNumber = current.weekNumber,
                averagePainScore = current.averagePainScore,
                averageStressScore = current.averageStressScore,
                averageJawFunctionScore = current.averageJawFunctionScore,
                changeRates = if (index > 0) {
                    calculateChangeRatesFromPrevious(weeklyDataList[index - 1], current)
                } else null
            )
        }
    }

    /**
     * 변화율 계산
     * - 순수 함수(ChangeRateCalculator) 활용
     */
    private fun calculateChangeRatesFromPrevious(
        previous: WeeklyData,
        current: WeeklyData
    ): ChangeRateInfo? {
        val previousScores = ScoreSet(
            pain = previous.averagePainScore,
            stress = previous.averageStressScore,
            jawFunction = previous.averageJawFunctionScore
        )

        val currentScores = ScoreSet(
            pain = current.averagePainScore,
            stress = current.averageStressScore,
            jawFunction = current.averageJawFunctionScore
        )

        // 순수 함수 호출
        val changeRates = ChangeRateCalculator.calculateAll(previousScores, currentScores)

        return ChangeRateInfo(
            pain = changeRates.pain ?: 0.0,
            stress = changeRates.stress ?: 0.0,
            jawFunction = changeRates.jawFunction ?: 0.0
        )
    }
}
```

---

## 5. 테스트 전략

### 순수 함수 테스트 (Mock 불필요)

```kotlin
class PrescriptionCalculatorTest {

    @Test
    fun `PENDING 상태를 계산한다`() {
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
    fun `ACTIVE 상태를 계산한다`() {
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
    fun `COMPLETED 상태를 계산한다`() {
        // given
        val createdAt = LocalDateTime.of(2025, 9, 25, 10, 0)
        val activatedAt = LocalDateTime.of(2025, 10, 1, 9, 0)
        val currentTime = LocalDateTime.of(2025, 11, 13, 0, 0)  // 43일 후

        // when
        val status = PrescriptionCalculator.calculateStatus(createdAt, activatedAt, currentTime)

        // then
        assertEquals(PrescriptionStatus.COMPLETED, status)
    }
}

class WeekCalculatorTest {

    @Test
    fun `1주차를 계산한다`() {
        // given
        val activationDate = LocalDate.of(2025, 10, 1)
        val assessmentDate = LocalDate.of(2025, 10, 7)

        // when
        val weekNumber = WeekCalculator.calculateWeekNumber(activationDate, assessmentDate)

        // then
        assertEquals(1, weekNumber)
    }

    @Test
    fun `3주차를 계산한다`() {
        // given
        val activationDate = LocalDate.of(2025, 9, 1)
        val assessmentDate = LocalDate.of(2025, 9, 15)  // 14일 후

        // when
        val weekNumber = WeekCalculator.calculateWeekNumber(activationDate, assessmentDate)

        // then
        assertEquals(3, weekNumber)  // (14 / 7) + 1 = 3
    }
}

class ChangeRateCalculatorTest {

    @Test
    fun `통증 점수 감소 시 양수 변화율을 반환한다`() {
        // given (낮을수록 좋음)
        val previous = 7.0
        val current = 6.0

        // when
        val changeRate = ChangeRateCalculator.calculate(previous, current, MetricType.LOWER_IS_BETTER)

        // then
        assertEquals(14.3, changeRate)  // -((6-7)/7)*100 = 14.3% (호전)
    }

    @Test
    fun `턱 기능 점수 증가 시 양수 변화율을 반환한다`() {
        // given (높을수록 좋음)
        val previous = 6.0
        val current = 7.0

        // when
        val changeRate = ChangeRateCalculator.calculate(previous, current, MetricType.HIGHER_IS_BETTER)

        // then
        assertEquals(16.7, changeRate)  // ((7-6)/6)*100 = 16.7% (호전)
    }
}
```

---

## 설계 장점 정리

### 1. 테스트 용이성
- ✅ 순수 함수는 Mock 없이 단위 테스트
- ✅ 빠른 테스트 실행 (I/O 없음)
- ✅ 엣지 케이스 검증 용이

### 2. 재사용성
- ✅ Calculator는 어디서든 재사용 가능 (Batch, Scheduler 등)
- ✅ Service, Mapper에서 공통 사용

### 3. 가독성
- ✅ 계산 로직이 명확히 분리됨
- ✅ Entity는 얇고 이해하기 쉬움

### 4. 유지보수성
- ✅ 계산 로직 변경 시 Calculator만 수정
- ✅ Entity, Service는 영향 없음

### 5. 타입 안전성
- ✅ Sealed Class로 실수 방지 (MetricType)
- ✅ Nullable 처리 명확

---

## 요구사항 추적표

| 기능 | FP 적용 | 구현 위치 | 상태 |
|-----|---------|----------|------|
| 처방 상태 계산 | ✅ | `PrescriptionCalculator` | ✅ |
| 주차 계산 | ✅ | `WeekCalculator` | ✅ |
| 변화율 계산 | ✅ | `ChangeRateCalculator` | ✅ |
| Entity 상태 저장 | ❌ (OOP) | `Prescription` | ✅ |
| 트랜잭션 관리 | ❌ (OOP) | `@Service` | ✅ |
| 영속성 처리 | ❌ (OOP) | `Repository` | ✅ |

---

## 참고 문서
- [설계 의사결정 문서](./design-decisions.md)
- [비즈니스 로직 설계](./business-logic-design.md)
- [Service 계층 설계](./service-layer-design.md)
