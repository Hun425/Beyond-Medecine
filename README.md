# 비욘드 메디슨 백엔드 채용과제

턱관절 질환 디지털 치료제(DTx) 일일 검사 및 건강 추이 분석 시스템

## 📋 프로젝트 개요

본 프로젝트는 턱관절 질환 치료를 위한 디지털 치료제(DTx) 서비스의 핵심 기능을 구현한 백엔드 애플리케이션입니다.

### 주요 기능
- **처방 관리**: 4가지 상태(PENDING, ACTIVE, COMPLETED, EXPIRED)를 가진 처방 엔티티 관리
- **일일 검사 등록**: 통증, 스트레스, 턱 기능 점수 및 통증 부위 기록 (하루 1회 제한)
- **주차별 추이 분석**: 검사 데이터 집계 및 전주 대비 변화율 분석, 빈발 통증 부위 통계

### 구현 완료 현황
- ✅ 69개 테스트 100% 통과
- ✅ 3개 과제 모두 구현 완료
- ✅ TDD 방식으로 개발
- ✅ Swagger 문서화 완료

## 🏗 아키텍처 설명

### 계층형 아키텍처 (Layered Architecture)

```
┌─────────────────────────────────────────────────────────┐
│              Presentation Layer                         │
│  - Controller: HTTP 요청/응답 처리                         │
│  - DTO: Request/Response 객체                            │
│  - Mapper: DTO ↔ Command/Result 변환                     │
│  - ExceptionHandler: 전역 예외 처리                        │
└─────────────────────────────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────┐
│              Application Layer                          │
│  - Service: 비즈니스 로직 조율                              │
│  - DTO: Command/Result/Query 객체                        │
│  - Repository Interface: 데이터 접근 추상화                 │
│  - Custom Exception: 도메인 예외 정의                       │
└─────────────────────────────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────┐
│                 Domain Layer                            │
│  - Entity: JPA 엔티티 (Prescription, DailyAssessment)     │
│  - Calculator: 순수 함수 (주차 계산, 변화율 계산)              │
│  - Model: Enum, Value Object                            │
└─────────────────────────────────────────────────────────┘
```

### 설계 패턴

#### 1. FP + OOP 하이브리드 패턴
- **순수 함수 (Functional Programming)**
  - `PrescriptionCalculator`: 처방 상태 계산
  - `WeekCalculator`: 주차 번호 계산
  - `ChangeRateCalculator`: 변화율 계산
  - 불변성, 부수효과 없음, 테스트 용이

- **객체지향 (Object-Oriented Programming)**
  - `Prescription`, `DailyAssessment`, `PainArea`: 상태와 행위를 가진 엔티티
  - 캡슐화된 비즈니스 로직 (예: `canPerformAssessment()`)

#### 2. DDD (Domain-Driven Design) 원칙
- 도메인 로직을 외부 세계(프레임워크, DB)로부터 분리
- Repository를 통한 영속성 추상화
- Rich Domain Model (행위를 가진 엔티티)

#### 3. DTO 변환 계층 분리
```
Request DTO (Presentation)
    ↓ Mapper
Command (Application)
    ↓ Service
Result (Application)
    ↓ Mapper
Response DTO (Presentation)
```

## 🛠 기술 스택

### Core
- **Language**: Kotlin 2.1.0
- **Framework**: Spring Boot 3.4.2
- **Build Tool**: Gradle 8.14
- **JDK**: OpenJDK 21 (Temurin)

### Spring Ecosystem
- Spring Web MVC
- Spring Data JPA (Hibernate 6.6.5)
- Spring Validation (Bean Validation)

### Database
- H2 Database (In-Memory)
- 초기 데이터: `src/main/resources/data-init.sql`

### Documentation
- SpringDoc OpenAPI 3 (Swagger UI)

### Testing
- JUnit 5
- Mockito Kotlin
- Spring Boot Test
- @DataJpaTest (Repository 테스트)
- @WebMvcTest (Controller 테스트)
- @SpringBootTest (통합 테스트)

## 📁 프로젝트 구조

```
src/main/kotlin/com/beyondmedicine/
├── BeyondMedicineApplication.kt         # Main 클래스
├── domain/                              # 도메인 계층
│   ├── entity/                          # JPA 엔티티
│   │   ├── Prescription.kt              # 처방 (Primary Key: code)
│   │   ├── DailyAssessment.kt           # 일일 검사
│   │   └── PainArea.kt                  # 통증 부위
│   ├── calculator/                      # 순수 함수
│   │   ├── PrescriptionCalculator.kt    # 처방 상태 계산
│   │   ├── WeekCalculator.kt            # 주차 계산
│   │   └── ChangeRateCalculator.kt      # 변화율 계산
│   └── model/                           # 도메인 모델
│       ├── PrescriptionStatus.kt        # 처방 상태 Enum
│       └── PainLocation.kt              # 통증 부위 Enum
├── application/                         # 애플리케이션 계층
│   ├── service/                         # 비즈니스 로직
│   │   ├── AssessmentService.kt         # 검사 등록 서비스
│   │   └── AssessmentAnalysisService.kt # 추이 분석 서비스
│   ├── repository/                      # 데이터 접근
│   │   ├── PrescriptionRepository.kt
│   │   └── DailyAssessmentRepository.kt # JPQL 집계 쿼리 포함
│   ├── dto/                             # 애플리케이션 DTO
│   │   ├── CreateDailyAssessmentCommand.kt
│   │   ├── DailyAssessmentResult.kt
│   │   ├── WeeklyTrendQuery.kt
│   │   ├── WeeklyTrendData.kt
│   │   ├── WeeklyAggregation.kt         # JPQL 집계용 DTO
│   │   └── PainAreaAggregation.kt
│   └── exception/                       # 도메인 예외
│       ├── DomainException.kt           # 기본 예외 클래스
│       ├── PrescriptionNotFoundException.kt
│       ├── InvalidPrescriptionStatusException.kt
│       ├── DuplicateAssessmentException.kt
│       └── InvalidAssessmentDateException.kt
├── presentation/                        # 표현 계층
│   ├── controller/
│   │   └── AssessmentController.kt      # REST API 엔드포인트
│   ├── dto/                             # HTTP DTO
│   │   ├── CreateDailyAssessmentRequest.kt
│   │   ├── CreateDailyAssessmentResponse.kt
│   │   └── WeeklyTrendResponse.kt
│   ├── mapper/
│   │   └── AssessmentDtoMapper.kt       # DTO 변환
│   └── exception/
│       └── GlobalExceptionHandler.kt    # 전역 예외 처리
└── common/                              # 공통 유틸리티
    └── validator/
        ├── PrescriptionCodeValidator.kt # 처방 코드 검증
        └── PainLocationValidator.kt     # 통증 부위 검증

src/test/kotlin/com/beyondmedicine/
├── domain/                              # 도메인 단위 테스트
│   ├── calculator/                      # 18 tests
│   └── entity/                          # 14 tests
├── application/                         # 애플리케이션 계층 테스트
│   ├── repository/                      # 8 tests (@DataJpaTest)
│   └── service/                         # 10 tests (Mockito)
├── presentation/
│   └── mapper/                          # 4 tests
└── integration/
    └── AssessmentIntegrationTest.kt     # 14 tests (E2E)

src/main/resources/
├── application.yml                      # Spring Boot 설정
└── data-init.sql                        # 초기 데이터 (9개 처방 + 검사 데이터)
```

## 🚀 로컬 실행 방법

### 1. 사전 요구사항
- JDK 17 이상 (권장: JDK 21)
- Git

### 2. 프로젝트 클론
```bash
git clone <repository-url>
cd beyond-medicine
```

### 3. 애플리케이션 실행
```bash
# Gradle Wrapper를 사용한 실행
./gradlew bootRun

# 또는 빌드 후 실행
./gradlew build
java -jar build/libs/beyond-medicine-0.0.1-SNAPSHOT.jar
```

### 4. 실행 확인
- 애플리케이션: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 Console: http://localhost:8080/h2-console
  - JDBC URL: `jdbc:h2:mem:beyondmedicine`
  - Username: `sa`
  - Password: (공백)

## 📊 데이터베이스 설정

### H2 In-Memory Database
- **자동 초기화**: 애플리케이션 시작 시 스키마 생성 및 데이터 삽입
- **초기 데이터**: `src/main/resources/data-init.sql`
  - 9개의 처방 데이터 (다양한 상태)
  - 11개의 일일 검사 데이터 (ABCD1234 처방)
  - 통증 부위 데이터

### 테스트 데이터 예시
```sql
-- ACTIVE 처방 (테스트용)
ABCD1234: 2025-09-01 활성화 (현재 6주차, COMPLETED)
TEST0001: 2025-10-01 활성화 (현재 3주차, ACTIVE)
EXAM5678: 2025-10-10 활성화 (현재 1주차, ACTIVE)

-- PENDING 처방
WAIT9999, PEND7777

-- COMPLETED 처방
DONE5555, COMP3333

-- EXPIRED 처방
EXPR1111, EXPR2222
```

## 🧪 테스트 실행

### 전체 테스트
```bash
./gradlew test

# 결과: 69개 테스트 100% 통과
# - Domain: 32 tests
# - Application: 18 tests
# - Presentation: 4 tests
# - Integration: 14 tests
# - Context Load: 1 test
```

### 테스트 리포트
```bash
open build/reports/tests/test/index.html
```

### 계층별 테스트
```bash
# 도메인 계층만
./gradlew test --tests "com.beyondmedicine.domain.*"

# 통합 테스트만
./gradlew test --tests "com.beyondmedicine.integration.*"
```

## 📡 API 명세

### Swagger UI
http://localhost:8080/swagger-ui.html

### 1. 일일 검사 등록
```http
POST /api/v1/assessments/daily
Content-Type: application/json

{
  "prescriptionCode": "EXAM5678",
  "assessmentDate": "2025-10-18",
  "painScore": 6,
  "stressScore": 4,
  "jawFunctionScore": 7,
  "painAreas": [
    {
      "location": "LEFT_JAW",
      "intensity": 7,
      "description": "아침 통증"
    }
  ]
}
```

**응답 (201 Created)**
```json
{
  "assessmentId": "12",
  "prescriptionCode": "EXAM5678",
  "weekNumber": 2
}
```

**검증 규칙**
- `prescriptionCode`: 영대문자 4자 + 숫자 4자 조합 (순서 무관)
- `assessmentDate`: 처방의 1~6주차 범위 내
- `painScore`, `stressScore`, `jawFunctionScore`: 0~10 정수
- `painAreas`: 최대 6개, 중복 불가
- `intensity`: 0~10 정수
- `description`: 최대 500자

**에러 응답**
- `404`: 처방을 찾을 수 없음
- `400`: 유효하지 않은 처방 상태 (ACTIVE가 아님)
- `409`: 중복 검사 (하루 1회 제한)
- `400`: Bean Validation 실패

### 2. 주차별 추이 분석
```http
GET /api/v1/assessments/weekly-trend?prescriptionCode=ABCD1234&startWeek=1&endWeek=3
```

**파라미터**
- `prescriptionCode` (필수): 처방 코드
- `startWeek` (선택): 시작 주차 (기본값: 1)
- `endWeek` (선택): 종료 주차 (기본값: 현재 주차)

**응답 (200 OK)**
```json
{
  "prescriptionCode": "ABCD1234",
  "period": {
    "startWeek": 1,
    "endWeek": 3
  },
  "weeklyTrends": [
    {
      "weekNumber": 1,
      "averagePainScore": 7.0,
      "averageStressScore": 6.67,
      "averageJawFunctionScore": 5.33,
      "changeRates": null
    },
    {
      "weekNumber": 2,
      "averagePainScore": 6.0,
      "averageStressScore": 4.67,
      "averageJawFunctionScore": 6.33,
      "changeRates": {
        "pain": 14.3,
        "stress": 30.0,
        "jawFunction": 18.8
      }
    }
  ],
  "topPainAreas": [
    {
      "location": "LEFT_JAW",
      "count": 6,
      "averageIntensity": 6.5
    },
    {
      "location": "RIGHT_TEMPLE",
      "count": 2,
      "averageIntensity": 5.5
    }
  ]
}
```

**변화율 계산 규칙**
- 통증/스트레스: 낮을수록 좋음 → 감소 시 양수(호전), 증가 시 음수(악화)
- 턱 기능: 높을수록 좋음 → 증가 시 양수(호전), 감소 시 음수(악화)
- 소수점 첫째 자리 반올림
- 첫 주차는 변화율 null

## 🧩 구현 세부 사항

### 1. 주차 계산 로직
```kotlin
// WeekCalculator.kt
fun calculateWeekNumber(activationDate: LocalDate, assessmentDate: LocalDate): Int {
    val daysSinceActivation = ChronoUnit.DAYS.between(activationDate, assessmentDate)
    return (daysSinceActivation / 7).toInt() + 1
}

// 예: 활성화 2025-09-01, 검사 2025-09-15
// 경과 일수: 14일
// 주차: (14 / 7) + 1 = 3주차
```

### 2. 변화율 계산 로직
```kotlin
// ChangeRateCalculator.kt
fun calculate(previous: Double, current: Double, isHigherBetter: Boolean): Double {
    if (previous == 0.0) return 0.0

    val rawChangeRate = ((current - previous) / previous) * 100

    // 낮을수록 좋은 지표(통증, 스트레스)는 부호 반전
    val adjustedChangeRate = if (isHigherBetter) rawChangeRate else -rawChangeRate

    return String.format("%.1f", adjustedChangeRate).toDouble()
}

// 예: 통증 7.0 → 6.0
// rawChangeRate = ((6.0 - 7.0) / 7.0) * 100 = -14.3%
// adjustedChangeRate = -(-14.3) = 14.3% (호전)
```

### 3. 처방 상태 계산
```kotlin
// PrescriptionCalculator.kt
fun calculateStatus(createdAt: LocalDateTime, activatedAt: LocalDateTime?, currentTime: LocalDateTime): PrescriptionStatus {
    val treatmentPeriod = Period.ofWeeks(6)

    return when {
        activatedAt == null && currentTime >= createdAt.plus(treatmentPeriod) -> EXPIRED
        activatedAt == null -> PENDING
        currentTime < activatedAt.plus(treatmentPeriod) -> ACTIVE
        else -> COMPLETED
    }
}
```

### 4. JPQL 집계 쿼리
```kotlin
// DailyAssessmentRepository.kt
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
fun findWeeklyAggregations(...): List<WeeklyAggregation>
```

## 🔍 기타 구현 설명

### 엣지 케이스 처리
1. **중복 검사 방지**: Unique Index (`prescription_code`, `assessment_date`)
2. **데이터 없는 주차**: 응답에서 제외
3. **처방 코드 검증**: 문자 개수 검증 (성능 최적화)
4. **변화율 0으로 나누기**: 이전 값이 0이면 0.0 반환
5. **통증 부위 중복**: `groupBy`로 중복 검증
6. **주차 범위 초과**: 1~6주차 외 검사 시도 시 예외 발생

### 성능 최적화
1. **인덱스 설계**
   - `idx_prescription_code`: 처방 조회
   - `idx_daily_assessment_unique`: 중복 검사 확인
   - `idx_daily_assessment_week`: 주차별 조회
   - `idx_pain_area_assessment`: 통증 부위 조회

2. **JPQL 집계**: N+1 문제 방지
3. **Lazy Loading**: 연관관계 지연 로딩

### 테스트 전략
1. **단위 테스트**: 순수 함수, 엔티티 로직
2. **통합 테스트**: Service + Repository
3. **슬라이스 테스트**: @DataJpaTest, @WebMvcTest
4. **멱등성**: 각 테스트는 독립적으로 실행 가능

## 📝 보완할 점 및 아쉬운 점

### 1. 테스트 커버리지
- **현재**: 주요 시나리오 중심 (69개 테스트)
- **개선 가능**: 엣지 케이스 추가

### 2. 인증/인가
- **현재**: 미구현 (과제 요구사항)
- **실제 운영 시**: JWT 또는 Session 기반 인증 필요

### 3. 동시성 제어
- **현재**: 낙관적 잠금 미적용
- **개선 가능**: `@Version`을 통한 동시성 제어

### 4. 데이터베이스
- **현재**: H2 In-Memory (개발/테스트용)
- **실제 운영 시**: MySQL/PostgreSQL 전환 필요

### 5. API 응답 캐싱
- **개선 가능**: 주차별 추이 분석은 캐싱 적용 가능

## ❓ 우려되는 부분 및 의문점

### 1. 주차 계산 경계값
- **현재 구현**: `(경과일수 / 7) + 1` (과제 예시 기준)
- **확인 필요**: 실제 의료 도메인에서 D+0도 1주차로 보는지?

### 2. 변화율 표시 정밀도
- **현재**: 소수점 첫째 자리 반올림
- **우려**: 반올림 정책에 대한 도메인 규칙 확인 필요

### 3. 통증 부위 Top 3 정렬
- **현재**: 횟수 → 평균 강도 순
- **의문**: 평균 강도 높지만 횟수 적은 부위의 중요도는?

### 4. 검사 일자 미래 날짜
- **현재**: 미래 날짜도 허용
- **확인 필요**: 미래 날짜 검사를 막아야 하는지?

### 5. 처방 만료 후 데이터 조회
- **현재**: COMPLETED 처방도 추이 조회 가능
- **확인 필요**: 만료 후 조회 제한 필요 여부

## 📚 참고 문서

- [API 설계 문서](docs/api-design.md)
- [비즈니스 로직 설계](docs/business-logic-design.md)
- [TDD 구현 가이드](docs/tdd-implementation-guide-1.md)
- [과제 명세서](docs/비욘드_메디슨_백엔드_개발자_채용_과제.pdf)

---

**개발 기간**: 2025-10-13 ~ 2025-10-18
**테스트 통과**: 69/69 (100%)
**Kotlin**: 2.1.0 | **Spring Boot**: 3.4.2 | **JDK**: 21
