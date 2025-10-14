# 설계 의사결정 문서

## 기술 스택 결정

### 확정된 기술 스택
- **언어**: Kotlin
- **프레임워크**: Spring MVC + JPA
- **빌드 도구**: Gradle
- **데이터베이스**: H2 (개발), MySQL (선택적)
- **타임존**: KST (UTC+9)

### 선택 이유
- Spring MVC + JPA: 5일 내 구현 안정성, 복잡한 집계 쿼리 작성 용이
- H2: 빠른 개발 및 테스트 환경 구축

## 아키텍처 결정

### Pragmatic Layered Architecture + FP Hybrid (실용적 Clean Architecture + 함수형 프로그래밍)
```
src/main/kotlin/
├── domain/              # 순수 도메인 계층
│   ├── model/          # 엔티티 (Prescription, DailyAssessment, PainArea) - OOP
│   ├── calculator/     # 순수 함수 모듈 - FP
│   │   ├── PrescriptionCalculator.kt
│   │   ├── WeekCalculator.kt
│   │   └── ChangeRateCalculator.kt
│   └── vo/             # Value Objects (선택적)
│
├── application/         # 애플리케이션 서비스 계층 - OOP
│   ├── service/        # 유즈케이스 구현 (트랜잭션 경계)
│   ├── repository/     # Repository 인터페이스 (Spring Data JPA)
│   └── dto/            # Command/Result DTO
│
└── presentation/        # 표현 계층 (API) - OOP
    ├── controller/     # REST 컨트롤러
    └── dto/            # API Request/Response DTO
        ├── request/
        └── response/
```

### 아키텍처 원칙
- **도메인 계층**:
  - **Entity (OOP)**: 상태 저장, JPA 영속성
  - **Calculator (FP)**: 순수 함수로 핵심 계산 로직 분리
- **의존성 방향**: Presentation → Application → Domain
- **Repository 위치 결정**: Application 계층에 위치
  - 이유: Spring Data JPA 활용 편의성, 빠른 개발, 실무 표준
  - 트레이드오프: DIP 완벽 구현보다 실용성 선택
- **FP + OOP 하이브리드**:
  - **순수 계산 로직 → FP**: 상태 계산, 주차 계산, 변화율 계산
  - **경계(I/O, 트랜잭션) → OOP**: Service, Repository, Entity
- **비즈니스 로직 캡슐화**:
  - 계산 로직은 순수 함수로 분리
  - Entity는 얇게 유지하고 순수 함수에 위임
- **단일 책임 원칙**: 각 레이어와 모듈은 명확한 책임을 가짐

---

## 도메인 모델 설계

### 1. 처방 (Prescription) 엔티티

#### 핵심 요구사항
- 처방 코드: 영대문자 4자 + 숫자 4자 조합 (순서 무관), 고유해야 함
- 상태: 대기, 활성, 완료, 만료 (계산 가능해야 함)
- 생성일, 활성화일 필요

#### 상태 계산 로직
- **대기(PENDING)**: 생성 후 활성화되지 않음 (`activatedAt == null && 6주 미경과`)
- **활성(ACTIVE)**: 활성화 후 42일(D+41)까지 (`activatedAt != null && 현재 <= D+41 23:59:59.999`)
- **완료(COMPLETED)**: 활성화 후 43일째(D+42)부터 (`activatedAt != null && 현재 >= D+42 00:00:00.000`)
- **만료(EXPIRED)**: 생성 후 활성화 없이 6주 경과 (`activatedAt == null && 6주 경과`)

#### 엔티티 설계
```kotlin
@Entity
@Table(name = "prescriptions")
class Prescription(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true, length = 8)
    val code: String,  // 처방 코드 (예: A1BC23D4)

    @Column(nullable = false)
    val createdAt: LocalDateTime,

    @Column(nullable = true)
    var activatedAt: LocalDateTime? = null
) {
    // 상태 계산 메서드 (현재 시점 기준)
    fun getStatus(currentTime: LocalDateTime = LocalDateTime.now()): PrescriptionStatus {
        // 활성화된 경우
        if (activatedAt != null) {
            val endOfWeek6 = activatedAt!!.plusDays(41)
                .withHour(23).withMinute(59).withSecond(59).withNano(999_999_999)

            return if (currentTime.isAfter(endOfWeek6)) {
                PrescriptionStatus.COMPLETED
            } else {
                PrescriptionStatus.ACTIVE
            }
        }

        // 활성화되지 않은 경우
        val expirationDate = createdAt.plusWeeks(6)
        return if (currentTime.isAfter(expirationDate)) {
            PrescriptionStatus.EXPIRED
        } else {
            PrescriptionStatus.PENDING
        }
    }

    // 현재 주차 계산 (활성화된 처방만)
    fun getCurrentWeek(currentDate: LocalDate = LocalDate.now()): Int? {
        if (activatedAt == null) return null

        val activationDate = activatedAt!!.toLocalDate()
        val daysSinceActivation = ChronoUnit.DAYS.between(activationDate, currentDate)

        return if (daysSinceActivation >= 0) {
            (daysSinceActivation / 7).toInt() + 1
        } else {
            null
        }
    }

    // 활성화 메서드
    fun activate(activationTime: LocalDateTime) {
        require(activatedAt == null) { "이미 활성화된 처방입니다" }
        require(getStatus(activationTime) == PrescriptionStatus.PENDING) {
            "활성화할 수 없는 상태입니다"
        }
        this.activatedAt = activationTime
    }
}

enum class PrescriptionStatus {
    PENDING,    // 대기
    ACTIVE,     // 활성
    COMPLETED,  // 완료
    EXPIRED     // 만료
}
```

---

### 2. 일일 검사 (DailyAssessment) 엔티티

#### 핵심 요구사항
- 하루 1회만 등록 가능 (처방 + 검사일 조합 unique)
- 통증, 스트레스, 턱 기능 점수: 0~10
- 주차 번호: 자동 계산
- 통증 부위: 0~6개

#### 엔티티 설계
```kotlin
@Entity
@Table(
    name = "daily_assessments",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["prescription_id", "assessment_date"])
    ]
)
class DailyAssessment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prescription_id", nullable = false)
    val prescription: Prescription,

    @Column(nullable = false)
    val assessmentDate: LocalDate,  // 검사 일자

    @Column(nullable = false)
    val weekNumber: Int,  // 주차 번호

    @Column(nullable = false)
    val painScore: Int,  // 통증 점수 (0~10)

    @Column(nullable = false)
    val stressScore: Int,  // 스트레스 점수 (0~10)

    @Column(nullable = false)
    val jawFunctionScore: Int,  // 턱 기능 점수 (0~10)

    @OneToMany(mappedBy = "dailyAssessment", cascade = [CascadeType.ALL], orphanRemoval = true)
    val painAreas: MutableList<PainArea> = mutableListOf(),

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    init {
        require(painScore in 0..10) { "통증 점수는 0~10 사이여야 합니다" }
        require(stressScore in 0..10) { "스트레스 점수는 0~10 사이여야 합니다" }
        require(jawFunctionScore in 0..10) { "턱 기능 점수는 0~10 사이여야 합니다" }
        require(weekNumber in 1..6) { "주차는 1~6 사이여야 합니다" }
    }

    fun addPainArea(painArea: PainArea) {
        require(painAreas.size < 6) { "통증 부위는 최대 6개까지 등록 가능합니다" }
        painAreas.add(painArea)
    }
}
```

---

### 3. 통증 부위 (PainArea) 엔티티

#### 핵심 요구사항
- 통증 부위 타입: LEFT_JAW, RIGHT_JAW, LEFT_TEMPLE, RIGHT_TEMPLE, NECK, CHIN
- 강도: 0~10
- 부연 설명: nullable

#### 엔티티 설계
```kotlin
@Entity
@Table(name = "pain_areas")
class PainArea(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "daily_assessment_id", nullable = false)
    val dailyAssessment: DailyAssessment,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val location: PainLocation,  // 통증 부위

    @Column(nullable = false)
    val intensity: Int,  // 강도 (0~10)

    @Column(length = 500)
    val description: String? = null  // 부연 설명
) {
    init {
        require(intensity in 0..10) { "통증 강도는 0~10 사이여야 합니다" }
    }
}

enum class PainLocation(val koreanName: String) {
    LEFT_JAW("좌측 턱 관절"),
    RIGHT_JAW("우측 턱 관절"),
    LEFT_TEMPLE("좌측 관자놀이"),
    RIGHT_TEMPLE("우측 관자놀이"),
    NECK("목"),
    CHIN("턱 끝")
}
```

---

### 4. Value Object 설계

#### 처방 코드 (PrescriptionCode)
```kotlin
@Embeddable
data class PrescriptionCode(
    @Column(nullable = false, unique = true, length = 8)
    val value: String
) {
    init {
        require(isValid(value)) {
            "처방 코드는 영대문자 4자 + 숫자 4자 조합이어야 합니다"
        }
    }

    companion object {
        private val PATTERN = "^(?=.*[A-Z]{4})(?=.*\\d{4})[A-Z\\d]{8}$".toRegex()

        fun isValid(code: String): Boolean {
            if (code.length != 8) return false

            val upperCount = code.count { it.isUpperCase() }
            val digitCount = code.count { it.isDigit() }

            return upperCount == 4 && digitCount == 4
        }
    }
}
```

---

## 다음 단계
- [x] API 요청/응답 DTO 설계
- [ ] 비즈니스 로직 상세 설계 (주차 계산, 변화율 계산)
- [ ] Service 계층 설계
- [ ] Repository 쿼리 설계
- [ ] 테스트 코드 작성 전략 상세화
- [ ] 예외 처리 전략 수립

---

## 참고 문서
- [FP + OOP 하이브리드 설계](./fp-hybrid-design.md)
- [API 설계 문서](./api-design.md)
- [비즈니스 로직 설계](./business-logic-design.md)
- [예상 질문 및 답변 (FAQ)](./faq.md)
