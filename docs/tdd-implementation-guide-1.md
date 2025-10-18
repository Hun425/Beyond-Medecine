# TDD 구현 가이드 (단계별 체크리스트)

## 📋 목차
1. [시작 전 준비사항](#시작-전-준비사항)
2. [Phase 1: 도메인 모델 (Calculator + Entity)](#phase-1-도메인-모델)
3. [Phase 2: Repository 계층](#phase-2-repository-계층)
4. [Phase 3: Service 계층](#phase-3-service-계층)
5. [Phase 4: DTO & Mapper](#phase-4-dto--mapper)
6. [Phase 5: Controller 계층](#phase-5-controller-계층)
7. [Phase 6: 예외 처리 & 통합 테스트](#phase-6-예외-처리--통합-테스트)
8. [Phase 7: 최종 검증 & 데이터](#phase-7-최종-검증--데이터)

---

## 시작 전 준비사항

### Git Reset 실행
```bash
# Calculator 구현 완료 시점으로 이동
git reset --hard 71e97a7

# 테스트 실행 확인
./gradlew test

# 현재 커밋 확인
git log --oneline -5
```

### 현재 상태 확인
```bash
# 있어야 하는 파일들
✅ src/main/kotlin/com/beyondmedicine/domain/calculator/PrescriptionCalculator.kt
✅ src/main/kotlin/com/beyondmedicine/domain/calculator/WeekCalculator.kt
✅ src/main/kotlin/com/beyondmedicine/domain/model/PrescriptionStatus.kt
✅ src/test/kotlin/com/beyondmedicine/domain/calculator/PrescriptionCalculatorTest.kt
✅ src/test/kotlin/com/beyondmedicine/domain/calculator/WeekCalculatorTest.kt
✅ docs/ (모든 설계 문서)
```

---

## Phase 1: 도메인 모델

**목표:** 순수 도메인 로직과 Entity 구현
**예상 시간:** 3-4시간
**참고 문서:** `docs/business-logic-design.md`, `docs/fp-hybrid-design.md`

### 1.1. Enum 및 Value Object 생성

#### ☐ Task 1-1-1: PainLocation Enum 생성
```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/domain/model/PainLocation.kt

package com.beyondmedicine.domain.model

/**
 * 통증 부위 Enum
 * - 과제 PDF 7페이지 부록 참조
 */
enum class PainLocation(val koreanName: String) {
    LEFT_JAW("좌측 턱 관절"),
    RIGHT_JAW("우측 턱 관절"),
    LEFT_TEMPLE("좌측 관자놀이"),
    RIGHT_TEMPLE("우측 관자놀이"),
    NECK("목"),
    CHIN("턱 끝")
}
```

**검증:**
```bash
# 컴파일 확인
./gradlew compileKotlin
```

**커밋:**
```bash
git add src/main/kotlin/com/beyondmedicine/domain/model/PainLocation.kt
git commit -m "feat(domain): PainLocation Enum 추가"
```

---

### 1.2. ChangeRateCalculator 구현 (TDD)

#### ☐ Task 1-2-1: ChangeRateCalculator 테스트 작성
```kotlin
// 파일: src/test/kotlin/com/beyondmedicine/domain/calculator/ChangeRateCalculatorTest.kt

package com.beyondmedicine.domain.calculator

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName

@DisplayName("ChangeRateCalculator 테스트")
class ChangeRateCalculatorTest {

    @Test
    @DisplayName("통증 점수 감소 시 양수 변화율 반환 (호전)")
    fun calculate_PainDecreased_ReturnsPositive() {
        // given: 통증 7.0 → 6.0 (14.3% 감소 = 호전)
        val previous = 7.0
        val current = 6.0

        // when
        val changeRate = ChangeRateCalculator.calculate(
            previous = previous,
            current = current,
            isHigherBetter = false
        )

        // then
        assertEquals(14.3, changeRate, 0.01)
    }

    @Test
    @DisplayName("통증 점수 증가 시 음수 변화율 반환 (악화)")
    fun calculate_PainIncreased_ReturnsNegative() {
        // given: 통증 6.0 → 8.5 (41.7% 증가 = 악화)
        val previous = 6.0
        val current = 8.5

        // when
        val changeRate = ChangeRateCalculator.calculate(
            previous = previous,
            current = current,
            isHigherBetter = false
        )

        // then
        assertEquals(-41.7, changeRate, 0.01)
    }

    @Test
    @DisplayName("턱 기능 점수 증가 시 양수 변화율 반환 (호전)")
    fun calculate_JawFunctionIncreased_ReturnsPositive() {
        // given: 턱 기능 6.0 → 7.0 (16.7% 증가 = 호전)
        val previous = 6.0
        val current = 7.0

        // when
        val changeRate = ChangeRateCalculator.calculate(
            previous = previous,
            current = current,
            isHigherBetter = true
        )

        // then
        assertEquals(16.7, changeRate, 0.01)
    }

    @Test
    @DisplayName("턱 기능 점수 감소 시 음수 변화율 반환 (악화)")
    fun calculate_JawFunctionDecreased_ReturnsNegative() {
        // given: 턱 기능 6.0 → 5.5 (-8.3% 감소 = 악화)
        val previous = 6.0
        val current = 5.5

        // when
        val changeRate = ChangeRateCalculator.calculate(
            previous = previous,
            current = current,
            isHigherBetter = true
        )

        // then
        assertEquals(-8.3, changeRate, 0.01)
    }

    @Test
    @DisplayName("이전 값이 0이면 0.0을 반환")
    fun calculate_PreviousIsZero_ReturnsZero() {
        // given
        val previous = 0.0
        val current = 5.0

        // when
        val changeRate = ChangeRateCalculator.calculate(
            previous = previous,
            current = current,
            isHigherBetter = false
        )

        // then
        assertEquals(0.0, changeRate)
    }

    @Test
    @DisplayName("스트레스 점수 감소 시 양수 변화율 (과제 예시)")
    fun calculate_StressDecreased_ExampleFromPdf() {
        // given: 과제 PDF 9페이지 예시
        // 1주차: 5.0 → 2주차: 4.0 (20% 감소 = 호전)
        val previous = 5.0
        val current = 4.0

        // when
        val changeRate = ChangeRateCalculator.calculate(
            previous = previous,
            current = current,
            isHigherBetter = false
        )

        // then
        assertEquals(20.0, changeRate, 0.01)
    }
}
```

**검증:**
```bash
# 테스트 실행 (당연히 실패해야 함)
./gradlew test --tests ChangeRateCalculatorTest
```

#### ☐ Task 1-2-2: ChangeRateCalculator 구현
```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/domain/calculator/ChangeRateCalculator.kt

package com.beyondmedicine.domain.calculator

/**
 * 변화율 계산 순수 함수
 * - 참고: docs/fp-hybrid-design.md
 * - 과제 PDF 8페이지 변화율 계산 로직
 */
object ChangeRateCalculator {

    /**
     * 변화율 계산
     *
     * @param previous 이전 값
     * @param current 현재 값
     * @param isHigherBetter true: 높을수록 좋음(턱기능), false: 낮을수록 좋음(통증/스트레스)
     * @return 변화율(%), 소수점 첫째 자리 반올림
     */
    fun calculate(
        previous: Double,
        current: Double,
        isHigherBetter: Boolean
    ): Double {
        // 0으로 나눌 수 없음
        if (previous == 0.0) return 0.0

        // 기본 변화율: ((현재 - 이전) / 이전) * 100
        val rawChangeRate = ((current - previous) / previous) * 100

        // 낮을수록 좋은 지표는 부호 반전
        // 예: 통증 7→6 = -14.3% → 14.3% (호전)
        val adjustedChangeRate = if (isHigherBetter) {
            rawChangeRate
        } else {
            -rawChangeRate
        }

        // 소수점 첫째 자리까지 반올림
        return String.format("%.1f", adjustedChangeRate).toDouble()
    }
}
```

**검증:**
```bash
# 테스트 실행 (모두 통과해야 함)
./gradlew test --tests ChangeRateCalculatorTest

# 결과: 6 tests passed
```

**커밋:**
```bash
git add src/test/kotlin/com/beyondmedicine/domain/calculator/ChangeRateCalculatorTest.kt
git add src/main/kotlin/com/beyondmedicine/domain/calculator/ChangeRateCalculator.kt
git commit -m "test(calculator): ChangeRateCalculator 테스트 추가

feat(calculator): ChangeRateCalculator 구현
- 변화율 계산 순수 함수
- 통증/스트레스: 낮을수록 좋음 (부호 반전)
- 턱 기능: 높을수록 좋음
- 소수점 첫째 자리 반올림"
```

---

### 1.3. Prescription Entity 재설계

#### ☐ Task 1-3-1: Prescription 테스트 작성
```kotlin
// 파일: src/test/kotlin/com/beyondmedicine/domain/entity/PrescriptionTest.kt

package com.beyondmedicine.domain.entity

import com.beyondmedicine.domain.model.PrescriptionStatus
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("Prescription Entity 테스트")
class PrescriptionTest {

    @Test
    @DisplayName("처방 생성 시 PENDING 상태")
    fun create_NewPrescription_StatusIsPending() {
        // given
        val createdAt = LocalDateTime.of(2025, 10, 1, 10, 0)

        // when
        val prescription = Prescription(
            code = "ABCD1234",
            createdAt = createdAt,
            activatedAt = null
        )

        // then
        assertEquals(PrescriptionStatus.PENDING, prescription.getStatus(createdAt))
    }

    @Test
    @DisplayName("처방 활성화 후 ACTIVE 상태")
    fun activate_Prescription_StatusIsActive() {
        // given
        val createdAt = LocalDateTime.of(2025, 10, 1, 10, 0)
        val activatedAt = LocalDateTime.of(2025, 10, 5, 9, 0)

        val prescription = Prescription(
            code = "ABCD1234",
            createdAt = createdAt,
            activatedAt = null
        )

        // when
        prescription.activate(activatedAt)

        // then
        assertEquals(activatedAt, prescription.activatedAt)
        assertEquals(PrescriptionStatus.ACTIVE, prescription.getStatus(activatedAt))
    }

    @Test
    @DisplayName("주차 번호 계산 - 활성화 후 가능")
    fun calculateWeekNumber_AfterActivation_ReturnsWeekNumber() {
        // given
        val prescription = Prescription(
            code = "ABCD1234",
            createdAt = LocalDateTime.of(2025, 9, 25, 10, 0),
            activatedAt = LocalDateTime.of(2025, 10, 1, 9, 0)
        )

        // when
        val weekNumber = prescription.calculateWeekNumber(LocalDate.of(2025, 10, 15))

        // then
        assertEquals(3, weekNumber)  // (14일 / 7) + 1 = 3주차
    }

    @Test
    @DisplayName("주차 번호 계산 - 활성화 전이면 null")
    fun calculateWeekNumber_BeforeActivation_ReturnsNull() {
        // given
        val prescription = Prescription(
            code = "ABCD1234",
            createdAt = LocalDateTime.of(2025, 10, 1, 10, 0),
            activatedAt = null
        )

        // when
        val weekNumber = prescription.calculateWeekNumber(LocalDate.of(2025, 10, 15))

        // then
        assertNull(weekNumber)
    }

    @Test
    @DisplayName("검사 수행 가능 여부 - ACTIVE 상태만 true")
    fun canPerformAssessment_OnlyActiveStatus_ReturnsTrue() {
        // given: PENDING 상태
        val pendingPrescription = Prescription(
            code = "PEND1234",
            createdAt = LocalDateTime.of(2025, 10, 1, 10, 0),
            activatedAt = null
        )

        // when & then
        assertFalse(pendingPrescription.canPerformAssessment(LocalDateTime.of(2025, 10, 2, 10, 0)))

        // given: ACTIVE 상태
        val activePrescription = Prescription(
            code = "ACTV1234",
            createdAt = LocalDateTime.of(2025, 9, 25, 10, 0),
            activatedAt = LocalDateTime.of(2025, 10, 1, 9, 0)
        )

        // when & then
        assertTrue(activePrescription.canPerformAssessment(LocalDateTime.of(2025, 10, 15, 10, 0)))
    }
}
```

**검증:**
```bash
# 테스트 실행 (실패해야 함)
./gradlew test --tests PrescriptionTest
```

#### ☐ Task 1-3-2: Prescription Entity 구현
```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/domain/entity/Prescription.kt

package com.beyondmedicine.domain.entity

import com.beyondmedicine.domain.calculator.PrescriptionCalculator
import com.beyondmedicine.domain.calculator.WeekCalculator
import com.beyondmedicine.domain.model.PrescriptionStatus
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 처방 엔티티
 * - 참고: docs/business-logic-design.md
 * - 과제 PDF 4-5페이지
 */
@Entity
@Table(
    name = "prescriptions",
    indexes = [
        Index(name = "idx_prescription_code", columnList = "code", unique = true)
    ]
)
class Prescription(
    @Id
    @Column(nullable = false, unique = true, length = 8)
    val code: String,  // "ABCD1234" 형식 (영대문자4 + 숫자4)

    @Column(nullable = false)
    val createdAt: LocalDateTime,

    @Column(nullable = true)
    var activatedAt: LocalDateTime? = null
) {
    /**
     * 처방 상태 조회
     * - 계산 로직은 PrescriptionCalculator에 위임 (FP)
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
     * - 계산 로직은 WeekCalculator에 위임 (FP)
     * - 활성화 전이면 null 반환
     */
    fun calculateWeekNumber(assessmentDate: LocalDate): Int? {
        val activationDate = activatedAt?.toLocalDate() ?: return null
        return WeekCalculator.calculateWeekNumber(
            activationDate = activationDate,
            assessmentDate = assessmentDate
        )
    }

    /**
     * 현재 주차 조회
     */
    fun getCurrentWeek(currentDate: LocalDate = LocalDate.now()): Int? {
        return calculateWeekNumber(currentDate)
    }

    /**
     * 검사 수행 가능 여부
     * - ACTIVE 상태일 때만 true
     */
    fun canPerformAssessment(currentTime: LocalDateTime = LocalDateTime.now()): Boolean {
        return getStatus(currentTime) == PrescriptionStatus.ACTIVE
    }

    /**
     * 처방 활성화
     * - 상태 변경 메서드 (OOP)
     */
    fun activate(activationTime: LocalDateTime = LocalDateTime.now()) {
        require(activatedAt == null) {
            "이미 활성화된 처방입니다. 활성화 일시: $activatedAt"
        }
        require(getStatus(activationTime) == PrescriptionStatus.PENDING) {
            "활성화할 수 없는 상태입니다. 현재 상태: ${getStatus(activationTime)}"
        }
        this.activatedAt = activationTime
    }
}
```

**검증:**
```bash
# 테스트 실행 (모두 통과해야 함)
./gradlew test --tests PrescriptionTest

# 결과: 5 tests passed
```

**커밋:**
```bash
git add src/test/kotlin/com/beyondmedicine/domain/entity/PrescriptionTest.kt
git add src/main/kotlin/com/beyondmedicine/domain/entity/Prescription.kt
git commit -m "test(entity): Prescription Entity 테스트 추가

feat(entity): Prescription Entity 재설계
- code를 Primary Key로 변경 (영대문자4+숫자4)
- 순수 함수(Calculator)에 계산 위임
- FP + OOP 하이브리드 패턴 적용"
```

---

### 1.4. DailyAssessment Entity 구현

#### ☐ Task 1-4-1: DailyAssessment 테스트 작성
```kotlin
// 파일: src/test/kotlin/com/beyondmedicine/domain/entity/DailyAssessmentTest.kt

package com.beyondmedicine.domain.entity

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("DailyAssessment Entity 테스트")
class DailyAssessmentTest {

    @Test
    @DisplayName("일일 검사 생성 - 모든 필드 설정")
    fun create_DailyAssessment_AllFieldsSet() {
        // given
        val prescription = createTestPrescription()
        val assessmentDate = LocalDate.of(2025, 10, 15)

        // when
        val assessment = DailyAssessment(
            prescription = prescription,
            assessmentDate = assessmentDate,
            weekNumber = 3,
            painScore = 7,
            stressScore = 5,
            jawFunctionScore = 6
        )

        // then
        assertEquals(prescription, assessment.prescription)
        assertEquals(assessmentDate, assessment.assessmentDate)
        assertEquals(3, assessment.weekNumber)
        assertEquals(7, assessment.painScore)
        assertEquals(5, assessment.stressScore)
        assertEquals(6, assessment.jawFunctionScore)
        assertTrue(assessment.painAreas.isEmpty())
    }

    @Test
    @DisplayName("점수 범위 검증 - painScore 0~10")
    fun init_InvalidPainScore_ThrowsException() {
        // given
        val prescription = createTestPrescription()

        // when & then: -1 (범위 밖)
        assertThrows<IllegalArgumentException> {
            DailyAssessment(
                prescription = prescription,
                assessmentDate = LocalDate.now(),
                weekNumber = 1,
                painScore = -1,
                stressScore = 5,
                jawFunctionScore = 6
            )
        }

        // when & then: 11 (범위 밖)
        assertThrows<IllegalArgumentException> {
            DailyAssessment(
                prescription = prescription,
                assessmentDate = LocalDate.now(),
                weekNumber = 1,
                painScore = 11,
                stressScore = 5,
                jawFunctionScore = 6
            )
        }
    }

    @Test
    @DisplayName("점수 범위 검증 - stressScore 0~10")
    fun init_InvalidStressScore_ThrowsException() {
        // given
        val prescription = createTestPrescription()

        // when & then
        assertThrows<IllegalArgumentException> {
            DailyAssessment(
                prescription = prescription,
                assessmentDate = LocalDate.now(),
                weekNumber = 1,
                painScore = 5,
                stressScore = 15,  // 범위 밖
                jawFunctionScore = 6
            )
        }
    }

    @Test
    @DisplayName("점수 범위 검증 - jawFunctionScore 0~10")
    fun init_InvalidJawFunctionScore_ThrowsException() {
        // given
        val prescription = createTestPrescription()

        // when & then
        assertThrows<IllegalArgumentException> {
            DailyAssessment(
                prescription = prescription,
                assessmentDate = LocalDate.now(),
                weekNumber = 1,
                painScore = 5,
                stressScore = 5,
                jawFunctionScore = -5  // 범위 밖
            )
        }
    }

    @Test
    @DisplayName("주차 번호 검증 - 1~6")
    fun init_InvalidWeekNumber_ThrowsException() {
        // given
        val prescription = createTestPrescription()

        // when & then: 0 (범위 밖)
        assertThrows<IllegalArgumentException> {
            DailyAssessment(
                prescription = prescription,
                assessmentDate = LocalDate.now(),
                weekNumber = 0,
                painScore = 5,
                stressScore = 5,
                jawFunctionScore = 6
            )
        }

        // when & then: 7 (범위 밖)
        assertThrows<IllegalArgumentException> {
            DailyAssessment(
                prescription = prescription,
                assessmentDate = LocalDate.now(),
                weekNumber = 7,
                painScore = 5,
                stressScore = 5,
                jawFunctionScore = 6
            )
        }
    }

    private fun createTestPrescription(): Prescription {
        return Prescription(
            code = "TEST1234",
            createdAt = LocalDateTime.of(2025, 9, 25, 10, 0),
            activatedAt = LocalDateTime.of(2025, 10, 1, 9, 0)
        )
    }
}
```

**검증:**
```bash
# 테스트 실행 (실패해야 함)
./gradlew test --tests DailyAssessmentTest
```

#### ☐ Task 1-4-2: DailyAssessment Entity 구현
```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/domain/entity/DailyAssessment.kt

package com.beyondmedicine.domain.entity

import jakarta.persistence.*
import java.time.LocalDate

/**
 * 일일 검사 엔티티
 * - 참고: docs/business-logic-design.md
 * - 과제 PDF 6페이지
 */
@Entity
@Table(
    name = "daily_assessments",
    indexes = [
        Index(
            name = "idx_daily_assessment_unique",
            columnList = "prescription_code, assessment_date",
            unique = true
        ),
        Index(
            name = "idx_daily_assessment_week",
            columnList = "prescription_code, week_number"
        )
    ]
)
class DailyAssessment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_code", referencedColumnName = "code", nullable = false)
    val prescription: Prescription,

    @Column(nullable = false)
    val assessmentDate: LocalDate,

    @Column(nullable = false)
    val weekNumber: Int,

    @Column(nullable = false)
    val painScore: Int,

    @Column(nullable = false)
    val stressScore: Int,

    @Column(nullable = false)
    val jawFunctionScore: Int,

    @OneToMany(
        mappedBy = "dailyAssessment",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    val painAreas: MutableList<PainArea> = mutableListOf()
) {
    init {
        // 점수 범위 검증: 0~10
        require(painScore in 0..10) {
            "통증 점수는 0~10 사이여야 합니다. 입력값: $painScore"
        }
        require(stressScore in 0..10) {
            "스트레스 점수는 0~10 사이여야 합니다. 입력값: $stressScore"
        }
        require(jawFunctionScore in 0..10) {
            "턱 기능 점수는 0~10 사이여야 합니다. 입력값: $jawFunctionScore"
        }

        // 주차 번호 검증: 1~6
        require(weekNumber in 1..6) {
            "주차 번호는 1~6 사이여야 합니다. 입력값: $weekNumber"
        }
    }

    /**
     * 통증 부위 추가
     * - 최대 6개까지만 허용
     */
    fun addPainArea(painArea: PainArea) {
        require(painAreas.size < 6) {
            "통증 부위는 최대 6개까지만 등록 가능합니다. 현재: ${painAreas.size}"
        }
        painAreas.add(painArea)
    }
}
```

**검증:**
```bash
# 테스트 실행 (모두 통과해야 함)
./gradlew test --tests DailyAssessmentTest

# 결과: 7 tests passed
```

**커밋:**
```bash
git add src/test/kotlin/com/beyondmedicine/domain/entity/DailyAssessmentTest.kt
git add src/main/kotlin/com/beyondmedicine/domain/entity/DailyAssessment.kt
git commit -m "test(entity): DailyAssessment Entity 테스트 추가

feat(entity): DailyAssessment Entity 구현
- 통증/스트레스/턱기능 점수 필드 추가
- 주차 번호 자동 저장
- 점수 범위 검증 (0~10)
- 통증 부위 OneToMany 관계"
```

---

### 1.5. PainArea Entity 구현

#### ☐ Task 1-5-1: PainArea 테스트 작성
```kotlin
// 파일: src/test/kotlin/com/beyondmedicine/domain/entity/PainAreaTest.kt

package com.beyondmedicine.domain.entity

import com.beyondmedicine.domain.model.PainLocation
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("PainArea Entity 테스트")
class PainAreaTest {

    @Test
    @DisplayName("통증 부위 생성 - 모든 필드 설정")
    fun create_PainArea_AllFieldsSet() {
        // given
        val assessment = createTestAssessment()

        // when
        val painArea = PainArea(
            dailyAssessment = assessment,
            location = PainLocation.LEFT_JAW,
            intensity = 8,
            description = "씹을 때 통증"
        )

        // then
        assertEquals(assessment, painArea.dailyAssessment)
        assertEquals(PainLocation.LEFT_JAW, painArea.location)
        assertEquals(8, painArea.intensity)
        assertEquals("씹을 때 통증", painArea.description)
    }

    @Test
    @DisplayName("통증 부위 생성 - 부연 설명 null 가능")
    fun create_PainArea_DescriptionCanBeNull() {
        // given
        val assessment = createTestAssessment()

        // when
        val painArea = PainArea(
            dailyAssessment = assessment,
            location = PainLocation.NECK,
            intensity = 5,
            description = null
        )

        // then
        assertNull(painArea.description)
    }

    @Test
    @DisplayName("통증 강도 검증 - 0~10")
    fun init_InvalidIntensity_ThrowsException() {
        // given
        val assessment = createTestAssessment()

        // when & then: -1
        assertThrows<IllegalArgumentException> {
            PainArea(
                dailyAssessment = assessment,
                location = PainLocation.CHIN,
                intensity = -1,
                description = null
            )
        }

        // when & then: 11
        assertThrows<IllegalArgumentException> {
            PainArea(
                dailyAssessment = assessment,
                location = PainLocation.CHIN,
                intensity = 11,
                description = null
            )
        }
    }

    @Test
    @DisplayName("부연 설명 길이 검증 - 최대 500자")
    fun init_DescriptionTooLong_ThrowsException() {
        // given
        val assessment = createTestAssessment()
        val longDescription = "a".repeat(501)

        // when & then
        assertThrows<IllegalArgumentException> {
            PainArea(
                dailyAssessment = assessment,
                location = PainLocation.LEFT_TEMPLE,
                intensity = 7,
                description = longDescription
            )
        }
    }

    private fun createTestAssessment(): DailyAssessment {
        val prescription = Prescription(
            code = "TEST1234",
            createdAt = LocalDateTime.of(2025, 9, 25, 10, 0),
            activatedAt = LocalDateTime.of(2025, 10, 1, 9, 0)
        )

        return DailyAssessment(
            prescription = prescription,
            assessmentDate = LocalDate.of(2025, 10, 15),
            weekNumber = 3,
            painScore = 7,
            stressScore = 5,
            jawFunctionScore = 6
        )
    }
}
```

**검증:**
```bash
# 테스트 실행 (실패해야 함)
./gradlew test --tests PainAreaTest
```

#### ☐ Task 1-5-2: PainArea Entity 구현
```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/domain/entity/PainArea.kt

package com.beyondmedicine.domain.entity

import com.beyondmedicine.domain.model.PainLocation
import jakarta.persistence.*

/**
 * 통증 부위 엔티티
 * - 참고: docs/business-logic-design.md
 * - 과제 PDF 6페이지
 */
@Entity
@Table(
    name = "pain_areas",
    indexes = [
        Index(
            name = "idx_pain_area_assessment",
            columnList = "daily_assessment_id"
        )
    ]
)
class PainArea(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "daily_assessment_id", nullable = false)
    val dailyAssessment: DailyAssessment,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val location: PainLocation,

    @Column(nullable = false)
    val intensity: Int,

    @Column(length = 500)
    val description: String? = null
) {
    init {
        // 통증 강도 검증: 0~10
        require(intensity in 0..10) {
            "통증 강도는 0~10 사이여야 합니다. 입력값: $intensity"
        }

        // 부연 설명 길이 검증
        description?.let {
            require(it.length <= 500) {
                "부연 설명은 500자 이하여야 합니다. 입력 길이: ${it.length}"
            }
        }
    }
}
```

**검증:**
```bash
# 테스트 실행 (모두 통과해야 함)
./gradlew test --tests PainAreaTest

# 결과: 5 tests passed
```

**커밋:**
```bash
git add src/test/kotlin/com/beyondmedicine/domain/entity/PainAreaTest.kt
git add src/main/kotlin/com/beyondmedicine/domain/entity/PainArea.kt
git commit -m "test(entity): PainArea Entity 테스트 추가

feat(entity): PainArea Entity 구현
- PainLocation Enum 연결
- 통증 강도 검증 (0~10)
- 부연 설명 길이 제한 (500자)
- DailyAssessment ManyToOne 관계"
```

---

### Phase 1 완료 체크리스트

```bash
# 전체 도메인 테스트 실행
./gradlew test --tests "com.beyondmedicine.domain.*"

# 예상 결과:
# PrescriptionCalculatorTest: 6 tests passed
# WeekCalculatorTest: 7 tests passed
# ChangeRateCalculatorTest: 6 tests passed
# PrescriptionTest: 5 tests passed
# DailyAssessmentTest: 7 tests passed
# PainAreaTest: 5 tests passed
# Total: 36 tests passed
```

**Phase 1 완료 커밋:**
```bash
git add .
git commit -m "feat(domain): Phase 1 완료 - 도메인 모델 구현

- Calculator: 순수 함수 3개 (Prescription, Week, ChangeRate)
- Entity: 3개 (Prescription, DailyAssessment, PainArea)
- 모든 테스트 통과 (36 tests)
- TDD 방식으로 테스트 먼저 작성 후 구현"
```

---

## Phase 2: Repository 계층

**목표:** 데이터 접근 계층 구현 (JPQL 집계 쿼리 포함)
**예상 시간:** 2-3시간
**참고 문서:** `docs/repository-design.md`

### 2.1. Repository Interface 정의

#### ☐ Task 2-1-1: PrescriptionRepository 구현
```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/application/repository/PrescriptionRepository.kt

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
```

#### ☐ Task 2-1-2: DailyAssessmentRepository 인터페이스 정의
```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/application/repository/DailyAssessmentRepository.kt

package com.beyondmedicine.application.repository

import com.beyondmedicine.domain.entity.DailyAssessment
import com.beyondmedicine.domain.entity.Prescription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

/**
 * 일일 검사 리포지토리
 * - 참고: docs/repository-design.md
 */
@Repository
interface DailyAssessmentRepository : JpaRepository<DailyAssessment, Long> {

    /**
     * 중복 검사 확인 (하루 1회 제한)
     */
    fun existsByPrescriptionAndAssessmentDate(
        prescription: Prescription,
        assessmentDate: LocalDate
    ): Boolean

    /**
     * 처방별 검사 목록 조회 (날짜 오름차순)
     */
    fun findByPrescriptionOrderByAssessmentDateAsc(
        prescription: Prescription
    ): List<DailyAssessment>

    /**
     * 주차별 평균 점수 집계
     * - 과제 PDF 8페이지 요구사항
     */
    @Query("""
        SELECT new com.beyondmedicine.application.dto.WeeklyAggregation(
            da.weekNumber,
            AVG(da.painScore),
            AVG(da.stressScore),
            AVG(da.jawFunctionScore),
            COUNT(da.id)
        )
        FROM DailyAssessment da
        WHERE da.prescription = :prescription
          AND da.weekNumber BETWEEN :startWeek AND :endWeek
        GROUP BY da.weekNumber
        ORDER BY da.weekNumber ASC
    """)
    fun findWeeklyAggregations(
        @Param("prescription") prescription: Prescription,
        @Param("startWeek") startWeek: Int,
        @Param("endWeek") endWeek: Int
    ): List<WeeklyAggregation>

    /**
     * Top N 통증 부위 집계
     * - 과제 PDF 8페이지 요구사항
     */
    @Query("""
        SELECT new com.beyondmedicine.application.dto.PainAreaAggregation(
            pa.location,
            COUNT(pa.id),
            AVG(pa.intensity)
        )
        FROM DailyAssessment da
        JOIN da.painAreas pa
        WHERE da.prescription = :prescription
          AND da.weekNumber BETWEEN :startWeek AND :endWeek
        GROUP BY pa.location
        ORDER BY COUNT(pa.id) DESC, AVG(pa.intensity) DESC
    """)
    fun findTopPainAreas(
        @Param("prescription") prescription: Prescription,
        @Param("startWeek") startWeek: Int,
        @Param("endWeek") endWeek: Int
    ): List<PainAreaAggregation>
}
```

#### ☐ Task 2-1-3: Aggregation DTO 생성
```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/application/dto/WeeklyAggregation.kt

package com.beyondmedicine.application.dto

/**
 * 주차별 집계 결과
 * - JPQL 생성자 표현식용 DTO
 */
data class WeeklyAggregation(
    val weekNumber: Int,
    val averagePainScore: Double,
    val averageStressScore: Double,
    val averageJawFunctionScore: Double,
    val assessmentCount: Long
)
```

```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/application/dto/PainAreaAggregation.kt

package com.beyondmedicine.application.dto

import com.beyondmedicine.domain.model.PainLocation

/**
 * 통증 부위 집계 결과
 * - JPQL 생성자 표현식용 DTO
 */
data class PainAreaAggregation(
    val location: PainLocation,
    val count: Long,
    val averageIntensity: Double
)
```

**커밋:**
```bash
git add src/main/kotlin/com/beyondmedicine/application/repository/
git add src/main/kotlin/com/beyondmedicine/application/dto/
git commit -m "feat(repository): Repository 인터페이스 정의

- PrescriptionRepository: code 기반 조회
- DailyAssessmentRepository: 집계 쿼리 포함
- WeeklyAggregation, PainAreaAggregation DTO"
```

---

### 2.2. Repository 테스트 (@DataJpaTest)

#### ☐ Task 2-2-1: PrescriptionRepository 테스트
```kotlin
// 파일: src/test/kotlin/com/beyondmedicine/application/repository/PrescriptionRepositoryTest.kt

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

    @Test
    @DisplayName("중복 코드 저장 시 예외 발생")
    fun save_DuplicateCode_ThrowsException() {
        // given
        val prescription1 = Prescription(
            code = "DUPL1234",
            createdAt = LocalDateTime.now(),
            activatedAt = null
        )
        prescriptionRepository.save(prescription1)

        // when & then
        val prescription2 = Prescription(
            code = "DUPL1234",  // 중복
            createdAt = LocalDateTime.now(),
            activatedAt = null
        )

        assertThrows<Exception> {
            prescriptionRepository.saveAndFlush(prescription2)
        }
    }
}
```

**검증:**
```bash
./gradlew test --tests PrescriptionRepositoryTest

# 결과: 4 tests passed
```

**커밋:**
```bash
git add src/test/kotlin/com/beyondmedicine/application/repository/PrescriptionRepositoryTest.kt
git commit -m "test(repository): PrescriptionRepository 테스트 추가"
```

---

#### ☐ Task 2-2-2: DailyAssessmentRepository 테스트
```kotlin
// 파일: src/test/kotlin/com/beyondmedicine/application/repository/DailyAssessmentRepositoryTest.kt

package com.beyondmedicine.application.repository

import com.beyondmedicine.domain.entity.DailyAssessment
import com.beyondmedicine.domain.entity.PainArea
import com.beyondmedicine.domain.entity.Prescription
import com.beyondmedicine.domain.model.PainLocation
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("DailyAssessmentRepository 테스트")
class DailyAssessmentRepositoryTest {

    @Autowired
    private lateinit var prescriptionRepository: PrescriptionRepository

    @Autowired
    private lateinit var dailyAssessmentRepository: DailyAssessmentRepository

    private lateinit var testPrescription: Prescription

    @BeforeEach
    fun setUp() {
        testPrescription = Prescription(
            code = "TEST1234",
            createdAt = LocalDateTime.of(2025, 9, 25, 10, 0),
            activatedAt = LocalDateTime.of(2025, 10, 1, 9, 0)
        )
        prescriptionRepository.save(testPrescription)
    }

    @Test
    @DisplayName("중복 검사 확인 - 같은 날짜 존재")
    fun existsByPrescriptionAndAssessmentDate_SameDate_ReturnsTrue() {
        // given
        val assessmentDate = LocalDate.of(2025, 10, 15)
        val assessment = DailyAssessment(
            prescription = testPrescription,
            assessmentDate = assessmentDate,
            weekNumber = 3,
            painScore = 7,
            stressScore = 5,
            jawFunctionScore = 6
        )
        dailyAssessmentRepository.save(assessment)

        // when
        val exists = dailyAssessmentRepository.existsByPrescriptionAndAssessmentDate(
            testPrescription,
            assessmentDate
        )

        // then
        assertTrue(exists)
    }

    @Test
    @DisplayName("중복 검사 확인 - 다른 날짜는 false")
    fun existsByPrescriptionAndAssessmentDate_DifferentDate_ReturnsFalse() {
        // given
        val assessment = DailyAssessment(
            prescription = testPrescription,
            assessmentDate = LocalDate.of(2025, 10, 15),
            weekNumber = 3,
            painScore = 7,
            stressScore = 5,
            jawFunctionScore = 6
        )
        dailyAssessmentRepository.save(assessment)

        // when
        val exists = dailyAssessmentRepository.existsByPrescriptionAndAssessmentDate(
            testPrescription,
            LocalDate.of(2025, 10, 16)  // 다른 날짜
        )

        // then
        assertFalse(exists)
    }

    @Test
    @DisplayName("주차별 집계 - 여러 주차 데이터")
    fun findWeeklyAggregations_MultipleWeeks_ReturnsAggregatedData() {
        // given: 1주차 2개, 2주차 2개, 3주차 1개
        createAssessment(testPrescription, 1, LocalDate.of(2025, 10, 1), 8, 7, 5)
        createAssessment(testPrescription, 1, LocalDate.of(2025, 10, 3), 6, 5, 7)
        createAssessment(testPrescription, 2, LocalDate.of(2025, 10, 8), 7, 6, 6)
        createAssessment(testPrescription, 2, LocalDate.of(2025, 10, 10), 5, 4, 8)
        createAssessment(testPrescription, 3, LocalDate.of(2025, 10, 15), 4, 3, 9)

        // when
        val aggregations = dailyAssessmentRepository.findWeeklyAggregations(
            prescription = testPrescription,
            startWeek = 1,
            endWeek = 3
        )

        // then
        assertEquals(3, aggregations.size)

        // 1주차: (8+6)/2=7.0, (7+5)/2=6.0, (5+7)/2=6.0
        assertEquals(1, aggregations[0].weekNumber)
        assertEquals(7.0, aggregations[0].averagePainScore, 0.01)
        assertEquals(6.0, aggregations[0].averageStressScore, 0.01)
        assertEquals(6.0, aggregations[0].averageJawFunctionScore, 0.01)
        assertEquals(2, aggregations[0].assessmentCount)

        // 2주차: (7+5)/2=6.0, (6+4)/2=5.0, (6+8)/2=7.0
        assertEquals(2, aggregations[1].weekNumber)
        assertEquals(6.0, aggregations[1].averagePainScore, 0.01)
        assertEquals(5.0, aggregations[1].averageStressScore, 0.01)
        assertEquals(7.0, aggregations[1].averageJawFunctionScore, 0.01)
        assertEquals(2, aggregations[1].assessmentCount)

        // 3주차: 4.0, 3.0, 9.0
        assertEquals(3, aggregations[2].weekNumber)
        assertEquals(4.0, aggregations[2].averagePainScore, 0.01)
        assertEquals(3.0, aggregations[2].averageStressScore, 0.01)
        assertEquals(9.0, aggregations[2].averageJawFunctionScore, 0.01)
        assertEquals(1, aggregations[2].assessmentCount)
    }

    @Test
    @DisplayName("주차별 집계 - 데이터 없는 주차는 제외")
    fun findWeeklyAggregations_MissingWeeks_OnlyReturnsExistingWeeks() {
        // given: 1주차와 3주차만 데이터 (2주차 없음)
        createAssessment(testPrescription, 1, LocalDate.of(2025, 10, 1), 8, 7, 5)
        createAssessment(testPrescription, 3, LocalDate.of(2025, 10, 15), 4, 3, 9)

        // when
        val aggregations = dailyAssessmentRepository.findWeeklyAggregations(
            prescription = testPrescription,
            startWeek = 1,
            endWeek = 6
        )

        // then
        assertEquals(2, aggregations.size)  // 2주차는 제외
        assertEquals(1, aggregations[0].weekNumber)
        assertEquals(3, aggregations[1].weekNumber)
    }

    @Test
    @DisplayName("Top 통증 부위 집계 - 횟수 기준 내림차순")
    fun findTopPainAreas_OrderedByCount_ReturnsTopAreas() {
        // given
        val assessment1 = createAssessment(testPrescription, 1, LocalDate.of(2025, 10, 1), 8, 7, 5)
        val assessment2 = createAssessment(testPrescription, 1, LocalDate.of(2025, 10, 2), 7, 6, 6)
        val assessment3 = createAssessment(testPrescription, 2, LocalDate.of(2025, 10, 8), 6, 5, 7)

        // LEFT_JAW: 3회 (평균 강도: (8+7+6)/3 = 7.0)
        addPainArea(assessment1, PainLocation.LEFT_JAW, 8)
        addPainArea(assessment2, PainLocation.LEFT_JAW, 7)
        addPainArea(assessment3, PainLocation.LEFT_JAW, 6)

        // RIGHT_TEMPLE: 2회 (평균 강도: (6+5)/2 = 5.5)
        addPainArea(assessment1, PainLocation.RIGHT_TEMPLE, 6)
        addPainArea(assessment2, PainLocation.RIGHT_TEMPLE, 5)

        // NECK: 1회 (평균 강도: 9.0)
        addPainArea(assessment3, PainLocation.NECK, 9)

        dailyAssessmentRepository.saveAll(listOf(assessment1, assessment2, assessment3))

        // when
        val topPainAreas = dailyAssessmentRepository.findTopPainAreas(
            prescription = testPrescription,
            startWeek = 1,
            endWeek = 6
        )

        // then
        assertEquals(3, topPainAreas.size)

        // 1위: LEFT_JAW (3회)
        assertEquals(PainLocation.LEFT_JAW, topPainAreas[0].location)
        assertEquals(3, topPainAreas[0].count)
        assertEquals(7.0, topPainAreas[0].averageIntensity, 0.1)

        // 2위: RIGHT_TEMPLE (2회)
        assertEquals(PainLocation.RIGHT_TEMPLE, topPainAreas[1].location)
        assertEquals(2, topPainAreas[1].count)
        assertEquals(5.5, topPainAreas[1].averageIntensity, 0.1)

        // 3위: NECK (1회)
        assertEquals(PainLocation.NECK, topPainAreas[2].location)
        assertEquals(1, topPainAreas[2].count)
        assertEquals(9.0, topPainAreas[2].averageIntensity, 0.1)
    }

    // 헬퍼 메서드
    private fun createAssessment(
        prescription: Prescription,
        weekNumber: Int,
        assessmentDate: LocalDate,
        painScore: Int,
        stressScore: Int,
        jawFunctionScore: Int
    ): DailyAssessment {
        return DailyAssessment(
            prescription = prescription,
            assessmentDate = assessmentDate,
            weekNumber = weekNumber,
            painScore = painScore,
            stressScore = stressScore,
            jawFunctionScore = jawFunctionScore
        )
    }

    private fun addPainArea(
        assessment: DailyAssessment,
        location: PainLocation,
        intensity: Int
    ) {
        val painArea = PainArea(
            dailyAssessment = assessment,
            location = location,
            intensity = intensity,
            description = null
        )
        assessment.addPainArea(painArea)
    }
}
```

**검증:**
```bash
./gradlew test --tests DailyAssessmentRepositoryTest

# 결과: 6 tests passed
```

**커밋:**
```bash
git add src/test/kotlin/com/beyondmedicine/application/repository/DailyAssessmentRepositoryTest.kt
git commit -m "test(repository): DailyAssessmentRepository 테스트 추가

- 중복 검사 확인 테스트
- 주차별 집계 쿼리 테스트
- Top 통증 부위 집계 테스트"
```

---

### Phase 2 완료 체크리스트

```bash
# 전체 Repository 테스트 실행
./gradlew test --tests "com.beyondmedicine.application.repository.*"

# 예상 결과:
# PrescriptionRepositoryTest: 4 tests passed
# DailyAssessmentRepositoryTest: 6 tests passed
# Total: 10 tests passed
```

**Phase 2 완료 커밋:**
```bash
git add .
git commit -m "feat(repository): Phase 2 완료 - Repository 계층 구현

- PrescriptionRepository: code 기반 조회
- DailyAssessmentRepository: 집계 쿼리 포함
- 모든 테스트 통과 (10 tests)
- JPQL 집계 쿼리 동작 확인"
```

---

## Phase 3: Service 계층

**목표:** 비즈니스 로직 구현 (검증, 상태 관리)
**예상 시간:** 3-4시간
**참고 문서:** `docs/service-layer-design.md`, `docs/business-logic-design.md`

### 3.1. Custom Exception 정의

#### ☐ Task 3-1-1: DomainException 기본 클래스
```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/application/exception/DomainException.kt

package com.beyondmedicine.application.exception

/**
 * 도메인 예외 기본 클래스
 * - 참고: docs/exception-handling-design.md
 */
sealed class DomainException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
```

#### ☐ Task 3-1-2: 구체적인 예외 클래스들
```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/application/exception/PrescriptionNotFoundException.kt

package com.beyondmedicine.application.exception

/**
 * 처방을 찾을 수 없음 (404)
 */
class PrescriptionNotFoundException(
    message: String
) : DomainException(message)
```

```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/application/exception/InvalidPrescriptionStatusException.kt

package com.beyondmedicine.application.exception

/**
 * 유효하지 않은 처방 상태 (400)
 * - ACTIVE 상태가 아닌 처방에 검사 시도
 */
class InvalidPrescriptionStatusException(
    message: String
) : DomainException(message)
```

```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/application/exception/DuplicateAssessmentException.kt

package com.beyondmedicine.application.exception

/**
 * 중복 검사 (409)
 * - 하루 1회 제한 위반
 */
class DuplicateAssessmentException(
    message: String
) : DomainException(message)
```

```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/application/exception/InvalidAssessmentDateException.kt

package com.beyondmedicine.application.exception

/**
 * 유효하지 않은 검사 일자 (400)
 * - 1~6주차 범위 밖
 */
class InvalidAssessmentDateException(
    message: String
) : DomainException(message)
```

**커밋:**
```bash
git add src/main/kotlin/com/beyondmedicine/application/exception/
git commit -m "feat(exception): Custom Exception 정의

- DomainException 기본 클래스
- PrescriptionNotFoundException (404)
- InvalidPrescriptionStatusException (400)
- DuplicateAssessmentException (409)
- InvalidAssessmentDateException (400)"
```

---

### 3.2. Command/Result DTO 정의

#### ☐ Task 3-2-1: Command DTO 생성
```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/application/dto/CreateDailyAssessmentCommand.kt

package com.beyondmedicine.application.dto

import com.beyondmedicine.domain.model.PainLocation
import java.time.LocalDate

/**
 * 일일 검사 생성 커맨드
 * - Application 계층 진입점
 * - 참고: docs/api-design.md
 */
data class CreateDailyAssessmentCommand(
    val prescriptionCode: String,
    val assessmentDate: LocalDate,
    val painScore: Int,
    val stressScore: Int,
    val jawFunctionScore: Int,
    val painAreas: List<PainAreaCommand>
)

/**
 * 통증 부위 커맨드
 */
data class PainAreaCommand(
    val location: PainLocation,
    val intensity: Int,
    val description: String?
)
```

#### ☐ Task 3-2-2: Result DTO 생성
```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/application/dto/DailyAssessmentResult.kt

package com.beyondmedicine.application.dto

import java.time.LocalDate

/**
 * 일일 검사 생성 결과
 * - Service 반환 값
 */
data class DailyAssessmentResult(
    val assessmentId: Long,
    val prescriptionCode: String,
    val weekNumber: Int,
    val assessmentDate: LocalDate
)
```

#### ☐ Task 3-2-3: 주차별 추이 DTO 생성
```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/application/dto/WeeklyTrendQuery.kt

package com.beyondmedicine.application.dto

/**
 * 주차별 추이 분석 쿼리
 */
data class WeeklyTrendQuery(
    val prescriptionCode: String,
    val startWeek: Int,
    val endWeek: Int
)
```

```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/application/dto/WeeklyTrendData.kt

package com.beyondmedicine.application.dto

import com.beyondmedicine.domain.model.PainLocation

/**
 * 주차별 추이 분석 결과
 */
data class WeeklyTrendData(
    val prescriptionCode: String,
    val startWeek: Int,
    val endWeek: Int,
    val weeklyTrends: List<WeeklyData>,
    val topPainAreas: List<TopPainAreaData>
)

/**
 * 주차별 데이터
 */
data class WeeklyData(
    val weekNumber: Int,
    val averagePainScore: Double,
    val averageStressScore: Double,
    val averageJawFunctionScore: Double,
    val assessmentCount: Long
)

/**
 * 빈발 통증 부위 데이터
 */
data class TopPainAreaData(
    val location: PainLocation,
    val count: Long,
    val averageIntensity: Double
)
```

**커밋:**
```bash
git add src/main/kotlin/com/beyondmedicine/application/dto/
git commit -m "feat(dto): Command/Result DTO 정의

- CreateDailyAssessmentCommand
- DailyAssessmentResult
- WeeklyTrendQuery
- WeeklyTrendData (+ 내부 DTO들)"
```

---

### 3.3. AssessmentService 구현 (TDD)

#### ☐ Task 3-3-1: AssessmentService 테스트 작성
```kotlin
// 파일: src/test/kotlin/com/beyondmedicine/application/service/AssessmentServiceTest.kt

package com.beyondmedicine.application.service

import com.beyondmedicine.application.dto.CreateDailyAssessmentCommand
import com.beyondmedicine.application.dto.PainAreaCommand
import com.beyondmedicine.application.exception.*
import com.beyondmedicine.application.repository.DailyAssessmentRepository
import com.beyondmedicine.application.repository.PrescriptionRepository
import com.beyondmedicine.domain.entity.DailyAssessment
import com.beyondmedicine.domain.entity.Prescription
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
    @DisplayName("일일 검사 정상 등록")
    fun createAssessment_ValidCommand_Success() {
        // given
        val prescription = createActivePrescription()
        val command = createValidCommand()

        whenever(prescriptionRepository.findByCode(command.prescriptionCode))
            .thenReturn(prescription)
        whenever(dailyAssessmentRepository.existsByPrescriptionAndAssessmentDate(any(), any()))
            .thenReturn(false)
        whenever(dailyAssessmentRepository.save(any<DailyAssessment>()))
            .thenAnswer { it.arguments[0] as DailyAssessment }

        // when
        val result = assessmentService.createAssessment(command)

        // then
        assertNotNull(result)
        assertEquals(command.prescriptionCode, result.prescriptionCode)
        assertEquals(1, result.weekNumber)
        verify(dailyAssessmentRepository).save(any<DailyAssessment>())
    }

    @Test
    @DisplayName("처방이 없으면 PrescriptionNotFoundException")
    fun createAssessment_PrescriptionNotFound_ThrowsException() {
        // given
        val command = createValidCommand()
        whenever(prescriptionRepository.findByCode(command.prescriptionCode))
            .thenReturn(null)

        // when & then
        assertThrows<PrescriptionNotFoundException> {
            assessmentService.createAssessment(command)
        }
    }

    @Test
    @DisplayName("PENDING 상태 처방은 InvalidPrescriptionStatusException")
    fun createAssessment_PendingStatus_ThrowsException() {
        // given
        val pendingPrescription = createPendingPrescription()
        val command = createValidCommand()

        whenever(prescriptionRepository.findByCode(command.prescriptionCode))
            .thenReturn(pendingPrescription)

        // when & then
        assertThrows<InvalidPrescriptionStatusException> {
            assessmentService.createAssessment(command)
        }
    }

    @Test
    @DisplayName("중복 검사는 DuplicateAssessmentException")
    fun createAssessment_DuplicateAssessment_ThrowsException() {
        // given
        val prescription = createActivePrescription()
        val command = createValidCommand()

        whenever(prescriptionRepository.findByCode(command.prescriptionCode))
            .thenReturn(prescription)
        whenever(dailyAssessmentRepository.existsByPrescriptionAndAssessmentDate(any(), any()))
            .thenReturn(true)  // 중복

        // when & then
        assertThrows<DuplicateAssessmentException> {
            assessmentService.createAssessment(command)
        }
    }

    @Test
    @DisplayName("통증 부위 7개는 IllegalArgumentException")
    fun createAssessment_TooManyPainAreas_ThrowsException() {
        // given
        val prescription = createActivePrescription()
        val command = createValidCommand().copy(
            painAreas = List(7) {
                PainAreaCommand(com.beyondmedicine.domain.model.PainLocation.LEFT_JAW, 5, null)
            }
        )

        whenever(prescriptionRepository.findByCode(command.prescriptionCode))
            .thenReturn(prescription)
        whenever(dailyAssessmentRepository.existsByPrescriptionAndAssessmentDate(any(), any()))
            .thenReturn(false)

        // when & then
        assertThrows<IllegalArgumentException> {
            assessmentService.createAssessment(command)
        }
    }

    // 헬퍼 메서드
    private fun createActivePrescription(): Prescription {
        return Prescription(
            code = "ABCD1234",
            createdAt = LocalDateTime.now().minusDays(10),
            activatedAt = LocalDateTime.now().minusDays(5)
        )
    }

    private fun createPendingPrescription(): Prescription {
        return Prescription(
            code = "PEND1234",
            createdAt = LocalDateTime.now().minusDays(1),
            activatedAt = null
        )
    }

    private fun createValidCommand(): CreateDailyAssessmentCommand {
        return CreateDailyAssessmentCommand(
            prescriptionCode = "ABCD1234",
            assessmentDate = LocalDate.now(),
            painScore = 7,
            stressScore = 5,
            jawFunctionScore = 6,
            painAreas = emptyList()
        )
    }
}
```

**검증:**
```bash
# 테스트 실행 (실패해야 함)
./gradlew test --tests AssessmentServiceTest
```

#### ☐ Task 3-3-2: AssessmentService 구현
```kotlin
// 파일: src/main/kotlin/com/beyondmedicine/application/service/AssessmentService.kt

package com.beyondmedicine.application.service

import com.beyondmedicine.application.dto.*
import com.beyondmedicine.application.exception.*
import com.beyondmedicine.application.repository.DailyAssessmentRepository
import com.beyondmedicine.application.repository.PrescriptionRepository
import com.beyondmedicine.domain.entity.DailyAssessment
import com.beyondmedicine.domain.entity.PainArea
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime

/**
 * 일일 검사 서비스
 * - 참고: docs/service-layer-design.md
 * - 과제 PDF 6페이지
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
     * 검증 순서:
     * 1. 처방 존재 확인
     * 2. 처방 상태 검증 (ACTIVE만 가능)
     * 3. 중복 검사 검증 (하루 1회)
     * 4. 주차 계산 및 검증 (1~6주차)
     * 5. 통증 부위 검증 (최대 6개, 중복 불가)
     */
    fun createAssessment(command: CreateDailyAssessmentCommand): DailyAssessmentResult {
        // 1. 처방 조회
        val prescription = prescriptionRepository.findByCode(command.prescriptionCode)
            ?: throw PrescriptionNotFoundException(
                "처방을 찾을 수 없습니다. 처방 코드: ${command.prescriptionCode}"
            )

        // 2. 처방 상태 검증 (ACTIVE 상태만 검사 가능)
        val currentDateTime = command.assessmentDate.atTime(LocalTime.now())
        if (!prescription.canPerformAssessment(currentDateTime)) {
            val status = prescription.getStatus(currentDateTime)
            throw InvalidPrescriptionStatusException(
                "검사를 수행할 수 없는 처방 상태입니다. " +
                "현재 상태: $status, 처방 코드: ${prescription.code}"
            )
        }

        // 3. 중복 검사 검증 (하루 1회 제한)
        val exists = dailyAssessmentRepository.existsByPrescriptionAndAssessmentDate(
            prescription = prescription,
            assessmentDate = command.assessmentDate
        )
        if (exists) {
            throw DuplicateAssessmentException(
                "해당 날짜에 이미 검사가 등록되어 있습니다. " +
                "처방 코드: ${prescription.code}, 날짜: ${command.assessmentDate}"
            )
        }

        // 4. 주차 계산 및 검증 (1~6주차)
        val weekNumber = prescription.calculateWeekNumber(command.assessmentDate)
            ?: throw InvalidAssessmentDateException(
                "유효하지 않은 검사 일자입니다. " +
                "검사는 활성화 후 1~6주차(D+0 ~ D+41) 사이에만 가능합니다. " +
                "검사 일자: ${command.assessmentDate}"
            )

        require(weekNumber in 1..6) {
            "주차는 1~6 사이여야 합니다. 계산된 주차: $weekNumber"
        }

        // 5. 통증 부위 검증 (최대 6개, 중복 불가)
        validatePainAreas(command.painAreas)

        // 6. 엔티티 생성 및 저장
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
            assessment.addPainArea(painArea)
        }

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
}
```

**검증:**
```bash
# 테스트 실행 (모두 통과해야 함)
./gradlew test --tests AssessmentServiceTest

# 결과: 5 tests passed
```

**커밋:**
```bash
git add src/test/kotlin/com/beyondmedicine/application/service/AssessmentServiceTest.kt
git add src/main/kotlin/com/beyondmedicine/application/service/AssessmentService.kt
git commit -m "test(service): AssessmentService 테스트 추가

feat(service): AssessmentService 구현
- 일일 검사 등록 비즈니스 로직
- 처방 상태 검증 (ACTIVE만)
- 중복 검사 검증 (하루 1회)
- 주차 자동 계산
- 통증 부위 검증 (최대 6개, 중복 불가)"
```

2편에 이어서 계속
---
