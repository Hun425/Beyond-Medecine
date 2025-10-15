# Beyond Medicine - Implementation Checklist

## 프로젝트 개요
- **목표**: TMJ 장애 DTx 서비스 백엔드 구현
- **아키텍처**: FP + OOP 하이브리드 (핵심 계산은 FP, 경계는 OOP)
- **기술스택**:
  - Kotlin 2.1.0 (최신 안정 버전)
  - Spring Boot 3.4.2 (2025년 1월 최신)
  - Gradle (Kotlin DSL)
  - H2 Database (dev/test)
- **개발 방식**: TDD (Test-Driven Development)
- **기한**: 5일 (120시간)

---

## Phase 0: 프로젝트 초기 설정 ✅ 완료

### 0.1 Gradle 프로젝트 구조
- [x] Spring Boot 3.4.2 프로젝트 생성
- [x] build.gradle.kts 의존성 설정
  - Spring Boot Starter Web
  - Spring Boot Starter Data JPA
  - Spring Boot Starter Validation
  - H2 Database (runtime)
  - MySQL Connector (runtime, optional)
  - Kotlin 2.1.0, reflect, stdlib
  - JUnit 5, Mockito-Kotlin 5.4.0, Kotest 5.9.1
- [x] settings.gradle.kts 생성
- [x] gradle.properties 생성
- [x] application.yml 설정 (H2, JPA, 로깅)
- [x] application-test.yml 설정 (H2 in-memory)

### 0.2 패키지 구조 생성
- [x] 메인 소스 디렉토리 생성
  - domain/ (model, calculator, exception)
  - application/ (service, repository, dto)
  - presentation/ (controller, dto, exception)
- [x] 테스트 소스 디렉토리 생성
  - domain/calculator/
  - application/service/
  - application/repository/
  - presentation/controller/

```
src/main/kotlin/com/beyondmedicine/
├── domain/
│   ├── model/              # JPA Entities (OOP)
│   ├── calculator/         # Pure Functions (FP)
│   └── exception/          # Domain Exceptions
├── application/
│   ├── service/            # Service Layer (OOP boundary)
│   ├── repository/         # Spring Data JPA
│   └── dto/                # Internal DTOs
└── presentation/
    ├── controller/         # REST Controllers
    ├── dto/                # API Request/Response DTOs
    └── exception/          # GlobalExceptionHandler

src/test/kotlin/com/beyondmedicine/
├── domain/calculator/      # Pure Function Tests (no mocks)
├── application/service/    # Service Unit Tests (with mocks)
├── application/repository/ # Repository Integration Tests
└── presentation/controller/ # API Tests (MockMvc)
```

### 0.3 Application 클래스 및 설정
- [x] BeyondMedicineApplication.kt 생성
- [x] application.yml 설정 (H2, JPA, Logging)
- [x] application-test.yml 설정

### 0.4 첫 번째 스모크 테스트
- [x] BeyondMedicineApplicationTest.kt 작성
- [x] contextLoads() 테스트 통과

**커밋**: `6218d9b` - chore: setup Spring Boot project with Gradle

---

---

## Phase 1: Domain Calculator (순수 함수) - TDD 🔴🟢🔵 (진행 중)

> **TDD 방식**: Red (테스트 작성) → Green (구현) → Refactor (개선) → Docs (문서화)

### 1.1 PrescriptionCalculator 구현 (진행 중)

#### 완료된 Iteration
- [x] **Iteration 1.1**: PENDING 상태 (커밋: `f80ab4b`, `bb62bab`)
  - 🔴 RED: 테스트 추가
  - 🟢 GREEN: 하드코딩 구현

- [x] **Iteration 1.2**: ACTIVE 상태 (커밋: `b69503e`, `a01abc5`)
  - 🔴 RED: ACTIVE 테스트 추가
  - 🟢 GREEN: activatedAt 체크 로직

- [x] **Iteration 1.3**: COMPLETED 상태 + 경계값 (커밋: `52c453d`, `29f20fb`, `dc19e94`)
  - 🔴 RED: D+41/D+42 경계값 테스트
  - 🟢 GREEN: 날짜 계산 로직 구현
  - 🔵 REFACTOR: 함수 분리 (calculateActivatedStatus, calculateEndOfActiveWeek)

#### 구현된 코드
```kotlin
object PrescriptionCalculator {
    private const val ACTIVE_PERIOD_DAYS = 42L

    fun calculateStatus(...)  // ✅ when 표현식
    private fun calculateActivatedStatus(...)  // ✅ ACTIVE/COMPLETED 판단
    private fun calculateEndOfActiveWeek(...)  // ✅ D+41 23:59:59.999 계산
}
```

#### 다음 Iteration
- [ ] **Iteration 1.4**: EXPIRED 상태
  - EXPIRED: 활성화 없이 6주 경과
  - `calculateNotActivatedStatus()` private 함수
  - 상수: EXPIRATION_WEEKS = 6L

#### WeekCalculator.kt
- [ ] `calculateWeekNumber()` 구현
  - 공식: (daysSinceActivation / 7) + 1
  - PDF 검증 예시: 09-01 → 09-15 = (14 / 7) + 1 = 3주차 ✓
  - assessmentDate < activationDate → null
  - daysSinceActivation > 41 → null
- [ ] `isValidWeekNumber()` 유틸 함수 (1~6주 검증)
- [ ] 상수: MAX_ACTIVE_DAYS = 41, DAYS_PER_WEEK = 7

#### ChangeRateCalculator.kt
- [ ] `calculate()` 구현
  - 공식: ((current - previous) / previous) * 100
  - previous == 0.0 → null
  - MetricType에 따라 부호 조정
  - 소수점 첫째자리 반올림
- [ ] `calculateAll()` 편의 함수 (ScoreSet 처리)
- [ ] `roundToOneDecimal()` private 함수

#### MetricType.kt (Sealed Class)
- [ ] `sealed class MetricType`
- [ ] `object LOWER_IS_BETTER` (pain, stress는 감소가 개선 → 부호 반전)
- [ ] `object HIGHER_IS_BETTER` (jawFunction은 증가가 개선)
- [ ] `abstract fun adjustRate(rawRate: Double): Double`

### 2.2 Calculator 단위 테스트 (Pure Function Tests - No Mocks!)

#### PrescriptionCalculatorTest.kt
- [ ] `calculateStatus_NotActivated_ReturnsPending()`
- [ ] `calculateStatus_JustActivated_ReturnsActive()`
- [ ] `calculateStatus_EndOfActiveWeek_ReturnsActive()` (경계값: D+41 23:59:59)
- [ ] `calculateStatus_StartOfCompletedWeek_ReturnsCompleted()` (경계값: D+42 00:00:00)
- [ ] `calculateStatus_NotActivatedExpired_ReturnsExpired()` (D+7 경과)

#### WeekCalculatorTest.kt
- [ ] `calculateWeekNumber_FirstDay_ReturnsWeek1()` (D+0)
- [ ] `calculateWeekNumber_LastDayOfWeek1_ReturnsWeek1()` (D+6)
- [ ] `calculateWeekNumber_FirstDayOfWeek2_ReturnsWeek2()` (D+7)
- [ ] `calculateWeekNumber_Week3Example_ReturnsWeek3()` (PDF 예시: 09-01 → 09-15)
- [ ] `calculateWeekNumber_LastValidDay_ReturnsWeek6()` (D+41)
- [ ] `calculateWeekNumber_AfterActiveWeek_ReturnsNull()` (D+42)
- [ ] `calculateWeekNumber_BeforeActivation_ReturnsNull()`

#### ChangeRateCalculatorTest.kt
- [ ] `calculate_LowerIsBetter_ReturnsNegativeRate()` (10 → 8 = -20%)
- [ ] `calculate_HigherIsBetter_ReturnsPositiveRate()` (60 → 75 = +25%)
- [ ] `calculate_PreviousIsZero_ReturnsNull()`
- [ ] `calculate_RoundingToOneDecimal()` (14.285714... → 14.3)
- [ ] `calculateAll_AllScores_ReturnsAllRates()`

---

## Phase 3: Domain Layer - Entities (Thin OOP) ✅ 설계 완료

### 3.1 Enum Classes
- [ ] `PrescriptionStatus.kt` (PENDING, ACTIVE, COMPLETED, EXPIRED)
- [ ] `PainLocation.kt` (LEFT, RIGHT, BOTH)

### 3.2 Entity Classes

#### Prescription.kt
- [ ] 필드: id, prescriptionCode, createdAt, activatedAt
- [ ] `getStatus()` 메서드 → `PrescriptionCalculator.calculateStatus()` 위임
- [ ] `calculateWeekNumber()` 메서드 → `WeekCalculator.calculateWeekNumber()` 위임
- [ ] `activate()` 메서드 (activatedAt 설정, 멱등성 보장)
- [ ] `@Table(indexes = [...])` 설정

#### DailyAssessment.kt
- [ ] 필드: id, prescription (ManyToOne), assessmentDate, weekNumber, painScore, stressScore, jawFunctionScore, painLocation, memo
- [ ] `@Table(uniqueConstraints = [...])` 설정
- [ ] `@ManyToOne(fetch = LAZY)` 설정

### 3.3 Entity 단위 테스트
- [ ] `PrescriptionTest.kt`
  - `getStatus_DelegatesToCalculator()`
  - `activate_SetsActivatedAt()`
  - `activate_Idempotent()`
- [ ] `DailyAssessmentTest.kt`
  - 필드 검증 테스트

---

## Phase 4: Application Layer - Repository ✅ 설계 완료

### 4.1 Repository Interfaces

#### PrescriptionRepository.kt
- [ ] `fun findByPrescriptionCode(code: String): Prescription?`
- [ ] Spring Data JPA 인터페이스 상속

#### DailyAssessmentRepository.kt
- [ ] `fun existsByPrescriptionAndAssessmentDate(prescription: Prescription, date: LocalDate): Boolean`
- [ ] `@Query` - `findWeeklyAggregations()` (주차별 평균 집계)
  - GROUP BY weekNumber
  - BETWEEN :startWeek AND :endWeek
  - ORDER BY weekNumber ASC
- [ ] 인덱스 설정 확인 (Entity 레벨)

### 4.2 Repository 통합 테스트 (@DataJpaTest)
- [ ] `PrescriptionRepositoryTest.kt`
  - `findByPrescriptionCode_Exists_ReturnsEntity()`
  - `findByPrescriptionCode_NotExists_ReturnsNull()`
- [ ] `DailyAssessmentRepositoryTest.kt`
  - `existsByPrescriptionAndAssessmentDate_Exists_ReturnsTrue()`
  - `findWeeklyAggregations_MultipleWeeks_ReturnsAggregations()`
  - `findWeeklyAggregations_EmptyRange_ReturnsEmpty()`

---

## Phase 5: Application Layer - Service ✅ 설계 완료

### 5.1 Exception Classes (Domain)
- [ ] `DomainException.kt` (sealed class, httpStatus, errorCode 포함)
- [ ] `PrescriptionNotFoundException.kt` (404, PRESCRIPTION_NOT_FOUND)
- [ ] `PrescriptionNotActivatedException.kt` (400, PRESCRIPTION_NOT_ACTIVATED)
- [ ] `InvalidAssessmentDateException.kt` (400, INVALID_ASSESSMENT_DATE)
- [ ] `DuplicateAssessmentException.kt` (409, DUPLICATE_ASSESSMENT)
- [ ] `NoAssessmentDataException.kt` (404, NO_ASSESSMENT_DATA)

### 5.2 Internal DTOs
- [ ] `WeeklyAggregation.kt` (JPQL 생성자 프로젝션용)
- [ ] `ScoreSet.kt` (Calculator 입력용)
- [ ] `ChangeRates.kt` (Calculator 출력용)

### 5.3 Service Classes

#### AssessmentService.kt
- [ ] `@Service`, `@Transactional` 설정
- [ ] **createDailyAssessment()** 구현
  1. Prescription 조회 (없으면 PrescriptionNotFoundException)
  2. 상태 검증 (ACTIVE 아니면 PrescriptionNotActivatedException)
  3. 중복 검증 (existsByPrescriptionAndAssessmentDate)
  4. weekNumber 계산 (WeekCalculator 사용, null이면 InvalidAssessmentDateException)
  5. DailyAssessment 엔티티 생성 및 저장
  6. Result DTO 반환

- [ ] **getWeeklyTrend()** 구현
  1. Prescription 조회
  2. 상태 검증 (ACTIVE 아니면 PrescriptionNotActivatedException)
  3. Repository에서 주차별 집계 조회 (1~현재 주차)
  4. 데이터 없으면 NoAssessmentDataException
  5. ChangeRateCalculator로 전주 대비 변화율 계산
  6. Result DTO 반환

- [ ] private 헬퍼 메서드들
  - `calculateAndValidateWeekNumber()`
  - `getCurrentWeekNumber()`
  - `mapToWeeklyDataList()` (ChangeRateCalculator 사용)

### 5.4 Service 단위 테스트 (Mockito)
- [ ] `AssessmentServiceTest.kt`
  - `createDailyAssessment_Success_SavesAndReturns()`
  - `createDailyAssessment_PrescriptionNotFound_ThrowsException()`
  - `createDailyAssessment_NotActive_ThrowsException()`
  - `createDailyAssessment_Duplicate_ThrowsException()`
  - `createDailyAssessment_InvalidDate_ThrowsException()`
  - `getWeeklyTrend_Success_ReturnsAggregations()`
  - `getWeeklyTrend_NoData_ThrowsException()`

---

## Phase 6: Presentation Layer - API ✅ 설계 완료

### 6.1 Custom Validators
- [ ] `PrescriptionCodeValidator.kt` (정규식: `^BM-TMJ-\\d{4}$`)
- [ ] `PainLocationValidator.kt` (Enum 검증)

### 6.2 API DTOs

#### Request
- [ ] `CreateDailyAssessmentRequest.kt`
  - Bean Validation 어노테이션 (@field:NotBlank, @field:ValidPrescriptionCode, etc.)
  - 모든 필드 nullable=false (필수값)

#### Response
- [ ] `DailyAssessmentResponse.kt`
- [ ] `WeeklyTrendResponse.kt`
  - prescriptionInfo: PrescriptionInfoDto
  - weeklyData: List<WeeklyDataDto>
- [ ] `PrescriptionInfoDto.kt`
- [ ] `WeeklyDataDto.kt` (changeRates 포함)
- [ ] `ChangeRateInfoDto.kt`
- [ ] `ErrorResponse.kt` (status, error, errorCode, message, path, timestamp, validationErrors)

### 6.3 DTO Mapper
- [ ] `AssessmentDtoMapper.kt`
  - `toResponse(result: CreateDailyAssessmentResult)`
  - `toResponse(result: WeeklyTrendResult)` (ChangeRateCalculator 사용)
  - private: `calculateChangeRatesFromPrevious()` (ChangeRateCalculator 위임)

### 6.4 Controller
- [ ] `AssessmentController.kt`
  - `@RestController`, `@RequestMapping("/api/v1/assessments")`
  - **POST /daily** - createDailyAssessment()
    - `@Valid @RequestBody`
    - Service 호출 → Mapper 변환 → 201 Created 응답
  - **GET /weekly** - getWeeklyTrend()
    - `@RequestParam prescriptionCode`
    - Service 호출 → Mapper 변환 → 200 OK 응답

### 6.5 Global Exception Handler
- [ ] `GlobalExceptionHandler.kt`
  - `@RestControllerAdvice`
  - `@ExceptionHandler(DomainException::class)` (httpStatus, errorCode 사용)
  - `@ExceptionHandler(MethodArgumentNotValidException::class)` (Bean Validation 실패 → 400)
  - `@ExceptionHandler(Exception::class)` (예상치 못한 오류 → 500)

### 6.6 Controller API 테스트 (@WebMvcTest + MockMvc)
- [ ] `AssessmentControllerTest.kt`
  - `createDailyAssessment_ValidRequest_Returns201()`
  - `createDailyAssessment_InvalidPrescriptionCode_Returns400()`
  - `createDailyAssessment_MissingRequiredField_Returns400()`
  - `createDailyAssessment_PrescriptionNotFound_Returns404()`
  - `createDailyAssessment_Duplicate_Returns409()`
  - `getWeeklyTrend_ValidRequest_Returns200()`
  - `getWeeklyTrend_PrescriptionNotFound_Returns404()`
  - `getWeeklyTrend_NoData_Returns404()`

---

## Phase 7: Infrastructure - Data & Exception ✅ 설계 완료

### 7.1 Test Data (data.sql)
- [ ] Prescription 10건 INSERT
  - 5건: ACTIVE 상태 (activatedAt 설정, 현재 날짜 기준 D+0 ~ D+35)
  - 3건: PENDING 상태 (activatedAt NULL, createdAt 최근)
  - 2건: EXPIRED 상태 (activatedAt NULL, createdAt D-8)
- [ ] DailyAssessment 30건 INSERT
  - ACTIVE Prescription들에 대해 각 주차별 데이터
  - painScore, stressScore, jawFunctionScore 다양하게 (변화율 계산 가능하도록)
  - painLocation: LEFT, RIGHT, BOTH 섞어서

### 7.2 Application Configuration
- [ ] `application.yml` (dev profile)
  - H2 Console 활성화
  - JPA ddl-auto: create-drop (또는 validate)
  - SQL 로깅 활성화
- [ ] `application-test.yml` (test profile)
  - H2 in-memory
  - ddl-auto: create-drop

---

## Phase 8: 통합 및 검증 ✅ 설계 완료

### 8.1 전체 테스트 실행
- [ ] `./gradlew test` - 모든 단위/통합 테스트 통과
- [ ] 테스트 커버리지 확인 (순수 함수 100%, Service 80%+)

### 8.2 API 수동 테스트 (H2 Console + curl/Postman)
- [ ] H2 Console 접속 (http://localhost:8080/h2-console)
- [ ] data.sql 데이터 확인
- [ ] **POST /api/v1/assessments/daily** 테스트
  - 정상 케이스 (201 Created)
  - 처방전 없음 (404)
  - 중복 평가 (409)
  - 유효하지 않은 날짜 (400)
  - Bean Validation 실패 (400)
- [ ] **GET /api/v1/assessments/weekly?prescriptionCode=XXX** 테스트
  - 정상 케이스 (200 OK, 주차별 평균 및 변화율 확인)
  - 처방전 없음 (404)
  - 평가 데이터 없음 (404)

### 8.3 비즈니스 로직 검증
- [ ] 주차 계산 정확성 (PDF 예시: 09-01 → 09-15 = 3주차)
- [ ] 변화율 계산 및 부호 조정 (LOWER_IS_BETTER는 부호 반전)
- [ ] 상태 전환 타이밍 (D+41 23:59:59 vs D+42 00:00:00)

---

## Phase 9: 문서화 및 제출 준비

### 9.1 README.md 작성
- [ ] 프로젝트 개요
- [ ] 기술 스택
- [ ] 아키텍처 설명 (FP + OOP 하이브리드)
- [ ] 빌드 및 실행 방법
- [ ] API 명세 (Request/Response 예시)
- [ ] 테스트 실행 방법
- [ ] 설계 문서 목록 (docs/ 폴더)

### 9.2 API 명세서 (선택)
- [ ] Swagger/SpringDoc 통합 (선택사항)
- [ ] 또는 Markdown 기반 API 문서

### 9.3 최종 코드 리뷰
- [ ] 코드 스타일 일관성 (Kotlin Convention)
- [ ] 불필요한 주석 제거
- [ ] TODO 제거
- [ ] 로그 레벨 확인 (프로덕션 준비)

---

## 요구사항 추적표 (Traceability Matrix)

| PDF 요구사항 | 구현 위치 | 체크리스트 Phase |
|------------|---------|----------------|
| **1. 처방전 (Prescription) 엔티티** | | |
| - 처방전 코드 (prescriptionCode) | Prescription.kt | Phase 3.2 |
| - 처방전 생성일 (createdAt) | Prescription.kt | Phase 3.2 |
| - 처방전 활성화일 (activatedAt) | Prescription.kt | Phase 3.2 |
| - 테스트 데이터 10건 생성 | data.sql | Phase 7.1 |
| **2. 일일 평가 등록 API** | | |
| - POST /api/v1/assessments/daily | AssessmentController.kt | Phase 6.4 |
| - 처방전 코드로 처방전 조회 | AssessmentService.kt | Phase 5.3 |
| - 처방전 상태 검증 (ACTIVE) | AssessmentService.kt | Phase 5.3 |
| - 중복 평가 방지 (날짜 기준) | AssessmentService.kt, DailyAssessmentRepository.kt | Phase 4.1, 5.3 |
| - 주차 계산 및 저장 | WeekCalculator.kt, AssessmentService.kt | Phase 2.1, 5.3 |
| - 통증 점수 (painScore) 1~10 | CreateDailyAssessmentRequest.kt, DailyAssessment.kt | Phase 3.2, 6.2 |
| - 스트레스 점수 (stressScore) 1~10 | CreateDailyAssessmentRequest.kt, DailyAssessment.kt | Phase 3.2, 6.2 |
| - 턱 기능 점수 (jawFunctionScore) 0~100 | CreateDailyAssessmentRequest.kt, DailyAssessment.kt | Phase 3.2, 6.2 |
| - 통증 부위 (painLocation) LEFT/RIGHT/BOTH | PainLocation.kt, DailyAssessment.kt | Phase 3.1, 3.2 |
| - 메모 (memo, 선택) | DailyAssessment.kt | Phase 3.2 |
| **3. 주간 추이 조회 API** | | |
| - GET /api/v1/assessments/weekly | AssessmentController.kt | Phase 6.4 |
| - 처방전 코드로 조회 | AssessmentService.kt | Phase 5.3 |
| - 1주차 ~ 현재 주차 데이터 반환 | AssessmentService.kt | Phase 5.3 |
| - 주차별 평균 점수 계산 | DailyAssessmentRepository.findWeeklyAggregations() | Phase 4.1 |
| - 전주 대비 변화율 계산 | ChangeRateCalculator.kt, AssessmentService.kt | Phase 2.1, 5.3 |
| - 변화율 부호 조정 (LOWER_IS_BETTER) | MetricType.kt, ChangeRateCalculator.kt | Phase 2.1 |
| - 소수점 첫째자리 반올림 | ChangeRateCalculator.kt | Phase 2.1 |
| **주차 계산 로직 (PDF 예시 검증)** | | |
| - 09-01 활성화 → 09-15 평가 = 3주차 | WeekCalculator.kt | Phase 2.1 |
| - (14 / 7) + 1 = 3 ✓ | WeekCalculatorTest.kt | Phase 2.2 |
| **예외 처리** | | |
| - 처방전 없음 (404) | PrescriptionNotFoundException | Phase 5.1 |
| - 처방전 미활성화 (400) | PrescriptionNotActivatedException | Phase 5.1 |
| - 중복 평가 (409) | DuplicateAssessmentException | Phase 5.1 |
| - 유효하지 않은 날짜 (400) | InvalidAssessmentDateException | Phase 5.1 |
| - 평가 데이터 없음 (404) | NoAssessmentDataException | Phase 5.1 |
| - Bean Validation 실패 (400) | GlobalExceptionHandler | Phase 6.5 |
| **테스트** | | |
| - 순수 함수 단위 테스트 (no mocks) | PrescriptionCalculatorTest, WeekCalculatorTest, ChangeRateCalculatorTest | Phase 2.2 |
| - Service 단위 테스트 (with mocks) | AssessmentServiceTest | Phase 5.4 |
| - Repository 통합 테스트 | PrescriptionRepositoryTest, DailyAssessmentRepositoryTest | Phase 4.2 |
| - Controller API 테스트 | AssessmentControllerTest | Phase 6.6 |

---

## 구현 우선순위 및 예상 소요 시간

| Phase | 작업 내용 | 예상 시간 | 누적 시간 |
|-------|---------|---------|---------|
| Phase 1 | 프로젝트 초기 설정 | 2h | 2h |
| Phase 2 | Pure Functions + Tests | 6h | 8h |
| Phase 3 | Entities + Tests | 3h | 11h |
| Phase 4 | Repositories + Tests | 4h | 15h |
| Phase 5 | Services + Tests | 8h | 23h |
| Phase 6 | API Layer + Tests | 8h | 31h |
| Phase 7 | Data & Configuration | 2h | 33h |
| Phase 8 | 통합 테스트 및 검증 | 4h | 37h |
| Phase 9 | 문서화 및 제출 준비 | 3h | 40h |
| **Total** | | **40h** | |

**여유 시간**: 120h - 40h = **80h** (버퍼, 리팩토링, 추가 기능)

---

## 핵심 설계 원칙 요약

1. **FP + OOP 하이브리드**
   - 핵심 계산 로직(Calculator)은 순수 함수로 작성 → 테스트 용이, 부수효과 없음
   - 경계 레이어(Service, Controller)는 OOP로 캡슐화 → 트랜잭션, I/O 관리

2. **Thin Entity Pattern**
   - JPA Entity는 최소한의 역할만 수행
   - 비즈니스 로직은 Calculator에 위임

3. **명확한 레이어 분리**
   - Domain: Calculator + Entity (비즈니스 로직)
   - Application: Service + Repository (트랜잭션 경계)
   - Presentation: Controller + DTO + Mapper (API 경계)

4. **테스트 전략**
   - Pure Functions: 모킹 불필요, 빠르고 신뢰성 높은 테스트
   - Service: Mockito로 의존성 모킹
   - Repository: @DataJpaTest로 실제 DB 쿼리 검증
   - Controller: @WebMvcTest + MockMvc로 API 계약 검증

5. **예외 처리**
   - Sealed Class 기반 DomainException 계층
   - GlobalExceptionHandler로 일관된 응답 포맷

---

## 다음 단계

설계 문서가 모두 완료되었으므로, 이제 **Phase 1부터 순차적으로 구현**을 시작할 수 있습니다.

**추천 시작점**: Phase 2 (Pure Functions) → 가장 독립적이고 테스트하기 쉬운 부분부터 시작하여 빠른 피드백 사이클 확보
