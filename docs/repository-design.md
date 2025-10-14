# Repository 설계

## Repository의 역할

### 핵심 책임
1. **데이터 접근 추상화**: SQL 쿼리를 도메인 친화적 메서드로 변환
2. **집계 쿼리**: 복잡한 통계 및 집계 처리
3. **성능 최적화**: N+1 문제 방지, 인덱스 활용

---

## Repository 구조

```
application/repository/
├── PrescriptionRepository.kt           # 처방 조회
├── DailyAssessmentRepository.kt        # 일일 검사 CRUD + 집계
└── PainAreaRepository.kt                # 통증 부위 (선택적)
```

---

## 1. PrescriptionRepository

### 인터페이스

```kotlin
package com.beyondmedicine.application.repository

import com.beyondmedicine.domain.model.Prescription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

/**
 * 처방 리포지토리
 * - 단순 조회 위주
 */
@Repository
interface PrescriptionRepository : JpaRepository<Prescription, Long> {

    /**
     * 처방 코드로 조회
     * - 가장 빈번하게 사용되는 조회 메서드
     * - 인덱스: code (unique)
     */
    fun findByCode(code: String): Prescription?

    /**
     * 처방 코드 존재 여부 확인
     * - 중복 처방 코드 검증용
     */
    fun existsByCode(code: String): Boolean
}
```

### 특징
- ✅ Spring Data JPA 기본 제공 메서드 활용
- ✅ 복잡한 쿼리 없음 (단순 조회만)
- ✅ `findByCode`가 핵심 메서드

---

## 2. DailyAssessmentRepository

### 인터페이스

```kotlin
package com.beyondmedicine.application.repository

import com.beyondmedicine.domain.model.DailyAssessment
import com.beyondmedicine.domain.model.Prescription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate

/**
 * 일일 검사 리포지토리
 * - CRUD + 복잡한 집계 쿼리
 */
@Repository
interface DailyAssessmentRepository : JpaRepository<DailyAssessment, Long> {

    // ========================================
    // 기본 조회
    // ========================================

    /**
     * 중복 검사 확인
     * - 하루 1회 제한 검증용
     * - 인덱스: (prescription_id, assessment_date) unique
     */
    fun existsByPrescriptionAndAssessmentDate(
        prescription: Prescription,
        assessmentDate: LocalDate
    ): Boolean

    /**
     * 처방별 검사 목록 조회
     * - 날짜 오름차순 정렬
     */
    fun findByPrescriptionOrderByAssessmentDateAsc(
        prescription: Prescription
    ): List<DailyAssessment>

    // ========================================
    // 주차별 집계 쿼리
    // ========================================

    /**
     * 주차별 평균 점수 집계
     * - 통증, 스트레스, 턱 기능 평균
     * - 주차별 검사 횟수
     *
     * @param prescription 처방
     * @param startWeek 시작 주차 (1~6)
     * @param endWeek 종료 주차 (1~6)
     * @return 주차별 집계 결과
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

    // ========================================
    // 통증 부위 집계 쿼리
    // ========================================

    /**
     * Top N 통증 부위 집계
     * - 기록 횟수 기준 내림차순
     * - 동일 횟수면 평균 강도 높은 순
     *
     * @param prescription 처방
     * @param startWeek 시작 주차
     * @param endWeek 종료 주차
     * @param limit Top N
     * @return 통증 부위 집계 결과
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
        @Param("endWeek") endWeek: Int,
        @Param("limit") limit: Int
    ): List<PainAreaAggregation>
}
```

---

## 3. 집계 결과 DTO

### WeeklyAggregation (주차별 집계)

```kotlin
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

### PainAreaAggregation (통증 부위 집계)

```kotlin
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

---

## 4. 쿼리 최적화 전략

### 인덱스 설계

```sql
-- 1. 처방 코드 unique 인덱스 (자동 생성)
CREATE UNIQUE INDEX idx_prescriptions_code ON prescriptions(code);

-- 2. 일일 검사 중복 방지 unique 인덱스
CREATE UNIQUE INDEX idx_daily_assessments_unique
ON daily_assessments(prescription_id, assessment_date);

-- 3. 주차별 집계용 복합 인덱스
CREATE INDEX idx_daily_assessments_prescription_week
ON daily_assessments(prescription_id, week_number);

-- 4. 통증 부위 조인용 인덱스
CREATE INDEX idx_pain_areas_assessment
ON pain_areas(daily_assessment_id);
```

### N+1 문제 방지

#### 문제 상황
```kotlin
// ❌ N+1 발생
val assessments = dailyAssessmentRepository.findByPrescription(prescription)
assessments.forEach { assessment ->
    assessment.painAreas.forEach { painArea ->  // N번의 추가 쿼리 발생
        println(painArea.location)
    }
}
```

#### 해결 방법 1: Fetch Join

```kotlin
@Query("""
    SELECT DISTINCT da
    FROM DailyAssessment da
    LEFT JOIN FETCH da.painAreas
    WHERE da.prescription = :prescription
    ORDER BY da.assessmentDate ASC
""")
fun findByPrescriptionWithPainAreas(
    @Param("prescription") prescription: Prescription
): List<DailyAssessment>
```

#### 해결 방법 2: EntityGraph

```kotlin
@EntityGraph(attributePaths = ["painAreas"])
fun findByPrescriptionOrderByAssessmentDateAsc(
    prescription: Prescription
): List<DailyAssessment>
```

---

## 5. 쿼리 성능 고려사항

### 집계 쿼리 최적화

#### 1. 주차별 집계
```sql
-- 실행 쿼리 (예상)
SELECT
    da.week_number,
    AVG(da.pain_score) as avg_pain,
    AVG(da.stress_score) as avg_stress,
    AVG(da.jaw_function_score) as avg_jaw,
    COUNT(da.id) as cnt
FROM daily_assessments da
WHERE da.prescription_id = ?
  AND da.week_number BETWEEN ? AND ?
GROUP BY da.week_number
ORDER BY da.week_number;

-- 인덱스 활용: idx_daily_assessments_prescription_week
-- 예상 rows: 6개 (최대 6주차)
-- 성능: 매우 빠름 (인덱스 스캔 + 그룹화)
```

#### 2. Top 3 통증 부위 집계
```sql
-- 실행 쿼리 (예상)
SELECT
    pa.location,
    COUNT(pa.id) as cnt,
    AVG(pa.intensity) as avg_intensity
FROM daily_assessments da
INNER JOIN pain_areas pa ON pa.daily_assessment_id = da.id
WHERE da.prescription_id = ?
  AND da.week_number BETWEEN ? AND ?
GROUP BY pa.location
ORDER BY COUNT(pa.id) DESC, AVG(pa.intensity) DESC
LIMIT 3;

-- 인덱스 활용:
--   1. idx_daily_assessments_prescription_week (DailyAssessment 필터링)
--   2. idx_pain_areas_assessment (조인)
-- 예상 rows: 최대 6개 (통증 부위 종류)
-- 성능: 빠름 (작은 데이터셋 + 인덱스)
```

### 데이터 볼륨 추정

```
처방 1개당:
- 최대 42개 일일 검사 (6주 * 7일)
- 최대 252개 통증 부위 (42 * 6)

전체 시스템:
- 처방: 수천~수만 건
- 일일 검사: 수십만 건
- 통증 부위: 수백만 건

→ 인덱스 필수, 집계 쿼리 최적화 중요
```

---

## 6. Repository 테스트 전략

### 단위 테스트 (Spring Data JPA Test)

```kotlin
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DailyAssessmentRepositoryTest {

    @Autowired
    private lateinit var dailyAssessmentRepository: DailyAssessmentRepository

    @Autowired
    private lateinit var prescriptionRepository: PrescriptionRepository

    @Test
    fun `처방과 날짜로 중복 검사를 확인한다`() {
        // given
        val prescription = createAndSavePrescription()
        val assessment = createAndSaveAssessment(prescription, LocalDate.now())

        // when
        val exists = dailyAssessmentRepository.existsByPrescriptionAndAssessmentDate(
            prescription, LocalDate.now()
        )

        // then
        assertTrue(exists)
    }

    @Test
    fun `주차별 평균을 집계한다`() {
        // given
        val prescription = createAndSavePrescription()
        createAssessmentsForWeeks(prescription, listOf(1, 1, 2, 2, 3))

        // when
        val aggregations = dailyAssessmentRepository.findWeeklyAggregations(
            prescription = prescription,
            startWeek = 1,
            endWeek = 3
        )

        // then
        assertEquals(3, aggregations.size)
        assertEquals(1, aggregations[0].weekNumber)
        assertEquals(2, aggregations[0].assessmentCount)
        assertEquals(2, aggregations[1].weekNumber)
        assertEquals(2, aggregations[1].assessmentCount)
    }

    @Test
    fun `Top 3 통증 부위를 집계한다`() {
        // given
        val prescription = createAndSavePrescription()
        val assessment1 = createAndSaveAssessment(prescription, LocalDate.now())
        val assessment2 = createAndSaveAssessment(prescription, LocalDate.now().plusDays(1))

        // LEFT_JAW: 2회
        addPainArea(assessment1, PainLocation.LEFT_JAW, 8)
        addPainArea(assessment2, PainLocation.LEFT_JAW, 7)

        // RIGHT_TEMPLE: 1회
        addPainArea(assessment1, PainLocation.RIGHT_TEMPLE, 6)

        // when
        val topPainAreas = dailyAssessmentRepository.findTopPainAreas(
            prescription = prescription,
            startWeek = 1,
            endWeek = 6,
            limit = 3
        )

        // then
        assertEquals(2, topPainAreas.size)
        assertEquals(PainLocation.LEFT_JAW, topPainAreas[0].location)
        assertEquals(2, topPainAreas[0].count)
        assertEquals(7.5, topPainAreas[0].averageIntensity, 0.1)
    }

    // 헬퍼 메서드
    private fun createAndSavePrescription(): Prescription {
        val prescription = Prescription(
            code = "TEST1234",
            createdAt = LocalDateTime.now().minusDays(10),
            activatedAt = LocalDateTime.now().minusDays(5)
        )
        return prescriptionRepository.save(prescription)
    }

    private fun createAndSaveAssessment(
        prescription: Prescription,
        assessmentDate: LocalDate
    ): DailyAssessment {
        val weekNumber = prescription.calculateWeekNumber(assessmentDate) ?: 1
        val assessment = DailyAssessment(
            prescription = prescription,
            assessmentDate = assessmentDate,
            weekNumber = weekNumber,
            painScore = 7,
            stressScore = 5,
            jawFunctionScore = 6
        )
        return dailyAssessmentRepository.save(assessment)
    }
}
```

### 쿼리 성능 테스트

```kotlin
@SpringBootTest
class RepositoryPerformanceTest {

    @Autowired
    private lateinit var dailyAssessmentRepository: DailyAssessmentRepository

    @Test
    fun `주차별 집계 쿼리 성능 측정`() {
        // given: 대량 데이터 생성
        val prescription = createPrescriptionWithManyAssessments(count = 42)

        // when: 집계 쿼리 실행
        val startTime = System.currentTimeMillis()
        val aggregations = dailyAssessmentRepository.findWeeklyAggregations(
            prescription = prescription,
            startWeek = 1,
            endWeek = 6
        )
        val endTime = System.currentTimeMillis()

        // then: 성능 검증
        val executionTime = endTime - startTime
        assertTrue(executionTime < 100, "집계 쿼리는 100ms 이내에 완료되어야 함")
        assertEquals(6, aggregations.size)
    }
}
```

---

## 7. 쿼리 작성 가이드

### JPQL vs Native Query

#### JPQL 사용 (권장)
```kotlin
// ✅ JPQL: 타입 안전, 엔티티 기반
@Query("""
    SELECT da
    FROM DailyAssessment da
    WHERE da.prescription.code = :code
      AND da.weekNumber = :week
""")
fun findByPrescriptionCodeAndWeek(
    @Param("code") code: String,
    @Param("week") week: Int
): List<DailyAssessment>
```

#### Native Query 사용 (복잡한 집계만)
```kotlin
// ⚠️ Native Query: DB 종속적, 타입 안전성 낮음
@Query(value = """
    SELECT week_number,
           AVG(pain_score) as avg_pain
    FROM daily_assessments
    WHERE prescription_id = :prescriptionId
    GROUP BY week_number
    HAVING AVG(pain_score) > 5.0
""", nativeQuery = true)
fun findHighPainWeeks(
    @Param("prescriptionId") prescriptionId: Long
): List<Array<Any>>
```

**원칙**: 가능하면 JPQL 사용, 정말 복잡한 경우만 Native Query

---

## 8. Spring Data JPA 메서드 네이밍 전략

### 메서드 이름으로 쿼리 생성

```kotlin
// Spring Data JPA가 자동으로 쿼리 생성
fun findByPrescriptionAndAssessmentDate(
    prescription: Prescription,
    assessmentDate: LocalDate
): DailyAssessment?

// SQL 변환:
// SELECT * FROM daily_assessments
// WHERE prescription_id = ? AND assessment_date = ?

fun existsByPrescriptionAndAssessmentDate(
    prescription: Prescription,
    assessmentDate: LocalDate
): Boolean

// SQL 변환:
// SELECT COUNT(*) > 0 FROM daily_assessments
// WHERE prescription_id = ? AND assessment_date = ?
```

### 복잡한 쿼리는 @Query

```kotlin
// ✅ 복잡한 집계는 명시적 @Query
@Query("SELECT AVG(da.painScore) FROM DailyAssessment da WHERE ...")
fun calculateAveragePainScore(...): Double

// ❌ 메서드 이름으로는 한계
// findByPrescriptionAndWeekNumberBetweenGroupByWeekNumber(...) ← 불가능
```

---

## 요구사항 추적표

### Repository 쿼리 구현 (과제 요구사항 기반)

| 기능 | Repository 메서드 | 상태 |
|-----|------------------|------|
| 처방 코드로 조회 | `findByCode()` | ✅ |
| 중복 검사 확인 | `existsByPrescriptionAndAssessmentDate()` | ✅ |
| 주차별 평균 집계 | `findWeeklyAggregations()` | ✅ |
| Top 3 통증 부위 집계 | `findTopPainAreas()` | ✅ |

### 성능 최적화 (실무 고려사항)

| 최적화 항목 | 구현 방법 | 상태 |
|-----------|----------|------|
| 인덱스 설계 | Unique + 복합 인덱스 | ✅ |
| N+1 문제 방지 | Fetch Join / EntityGraph | ✅ |
| 집계 쿼리 최적화 | JPQL GROUP BY | ✅ |
| 쿼리 성능 측정 | 성능 테스트 코드 | ✅ |

---

## 참고 문서
- [설계 의사결정 문서](./design-decisions.md)
- [FP + OOP 하이브리드 설계](./fp-hybrid-design.md)
- [Service 계층 설계](./service-layer-design.md)
- [비즈니스 로직 설계](./business-logic-design.md)
