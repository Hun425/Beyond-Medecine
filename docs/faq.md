# 예상 질문 및 답변 (FAQ)

## Q1. Repository 인터페이스를 Domain이 아닌 Application 계층에 둔 이유는?

**답변:**
엄격한 Clean Architecture에서는 Domain 계층에 Repository 인터페이스를 두고 Infrastructure에서 구현하는 것이 원칙입니다 (DIP - Dependency Inversion Principle). 하지만 이번 프로젝트에서는 **실용적 접근(Pragmatic Clean Architecture)**을 선택했습니다.

**선택 이유:**
1. **Spring Data JPA와의 자연스러운 통합**: `JpaRepository`를 상속받아 자동 구현 기능 활용
2. **개발 속도**: 5일 제한 시간 내 핵심 비즈니스 로직 구현에 집중
3. **실무 표준**: 대부분의 Spring Boot 프로젝트에서 사용하는 방식
4. **복잡한 쿼리 작성 편의**: 주차별 집계 쿼리 등을 `@Query`로 쉽게 작성 가능

**트레이드오프 인지:**
- 완벽한 DIP 구현은 포기했지만, **비즈니스 로직은 Domain 계층(엔티티)에 완전히 캡슐화**
- Repository는 단순 데이터 접근 계층으로 제한하여 외부 변화 영향 최소화

---

## Q2. 처방 상태(PrescriptionStatus)를 DB 컬럼으로 저장하지 않고 계산하는 이유는?

**답변:**
처방 상태는 시간에 따라 자동으로 변하는 **파생 데이터(Derived Data)**이므로 저장하지 않고 계산 메서드로 구현했습니다.

**장점:**
1. **데이터 정합성**: 시간에 따라 상태가 자동 변경되므로 불일치 발생 불가
2. **단일 진실 공급원**: `createdAt`, `activatedAt`만 관리하면 됨
3. **비즈니스 로직 캡슐화**: `getStatus(currentTime)` 메서드에 상태 계산 로직이 명확히 드러남

**단점 및 대응:**
- 조회 성능 이슈 가능 → 필요시 인덱스 추가 또는 조회 쿼리 최적화
- 대용량 데이터에서는 상태별 필터링이 어려움 → 요구사항에서는 처방 단건 조회가 주요 시나리오

**향후 개선 방안:**
- 성능 이슈 발생시 상태 컬럼 추가 + 스케줄러로 주기적 업데이트 고려

---

## Q3. weekNumber를 DailyAssessment에 저장하는 이유는? (중복 데이터 아닌가?)

**답변:**
맞습니다. `weekNumber`는 `prescription.activatedAt`과 `assessmentDate`로 계산 가능하므로 **중복 데이터**입니다. 하지만 **조회 성능 최적화**를 위해 저장했습니다.

**저장하는 이유:**
1. **주차별 집계 쿼리 성능**: `GROUP BY weekNumber`로 간단하고 빠른 집계 가능
2. **계산 비용 절감**: 매번 날짜 계산 없이 바로 사용
3. **쿼리 가독성**: SQL이 단순해짐

**일관성 보장 방법:**
- DailyAssessment 생성 시 서비스 레이어에서 `prescription.calculateWeekNumber(assessmentDate)` 호출하여 자동 계산
- 검증 로직으로 잘못된 주차 저장 방지

**트레이드오프:**
- 저장 공간 약간 증가 vs 조회 성능 대폭 향상 → 조회가 훨씬 빈번하므로 합리적

---

## Q4. PainArea를 별도 엔티티로 분리한 이유는? Embedded로 하면 안 되나?

**답변:**
`@Embeddable`로 구현할 수도 있지만, **별도 엔티티**로 분리한 이유는 다음과 같습니다:

**별도 엔티티 선택 이유:**
1. **컬렉션 크기 가변**: 통증 부위는 0~6개로 개수가 동적
2. **쿼리 편의성**: "가장 많이 기록된 통증 부위 Top 3" 집계시 별도 테이블이 유리
3. **확장성**: 향후 통증 부위에 타임스탬프, 이미지 등 추가 정보 가능

**Embedded 방식의 문제:**
- `@ElementCollection` 사용시 별도 테이블 생성됨 (결국 유사)
- JSON 컬럼 저장시 쿼리 어려움

---

## Q5. PrescriptionCode를 Value Object로 분리하지 않은 이유는?

**답변:**
초기 설계에서는 `PrescriptionCode` VO를 고려했으나, **String으로 단순화**했습니다.

**단순화 이유:**
1. **검증 로직 위치**: 서비스 레이어 또는 엔티티 생성자에서 검증 가능
2. **과도한 추상화 방지**: 단순한 형식 검증에 별도 클래스는 오버엔지니어링
3. **JPA 편의성**: String이 매핑과 쿼리 작성이 더 간단

**검증은 어디서?**
- API 레이어: `@Pattern` 어노테이션으로 1차 검증
- 서비스 레이어: 정규식으로 2차 검증
- 필요시 유틸 클래스(`PrescriptionCodeValidator`) 분리 가능

**VO로 분리하는 것이 더 나은 경우:**
- 처방 코드에 복잡한 비즈니스 로직이 추가될 경우 (ex. 지역코드, 병원코드 파싱)

---

## Q6. 변화율 계산 로직은 어디에 위치시킬 예정인가?

**답변:**
변화율 계산은 **Application Service 계층**에 위치시킬 예정입니다.

**이유:**
1. **집계 성격의 로직**: 여러 DailyAssessment를 조회하여 계산하는 유즈케이스 로직
2. **도메인 순수성 유지**: 단일 엔티티의 책임 범위를 넘어서는 계산
3. **응답 DTO 생성**: API 응답 구조에 맞춘 데이터 가공

**구조:**
```kotlin
// Application Service
class AssessmentAnalysisService {
    fun calculateWeeklyTrend(prescriptionCode: String,
                            startWeek: Int?,
                            endWeek: Int?): WeeklyTrendResponse {
        // 1. Repository에서 주차별 데이터 조회
        // 2. 평균 계산
        // 3. 변화율 계산
        // 4. Response DTO 생성
    }

    private fun calculateChangeRate(prev: Double,
                                    curr: Double,
                                    isHigherBetter: Boolean): Double {
        // 변화율 계산 로직
    }
}
```

---

## Q7. 테스트 전략은 어떻게 가져갈 예정인가?

**답변:**
각 계층별로 적절한 테스트 전략을 수립했습니다.

**1. Domain 계층 테스트 (Unit Test)**
- 엔티티 비즈니스 로직 테스트 (상태 계산, 검증 등)
- 빠른 실행, 외부 의존성 없음
- 예: `Prescription.getStatus()`, `Prescription.getCurrentWeek()` 테스트

**2. Application 계층 테스트 (Integration Test)**
- Service 로직 테스트
- Repository는 Mock 또는 `@DataJpaTest` 활용
- 예: 일일 검사 등록 로직, 주차별 집계 로직

**3. Presentation 계층 테스트 (API Test)**
- `@WebMvcTest` 또는 `@SpringBootTest` + `MockMvc`
- 요청/응답 형식 검증
- 예: POST /api/v1/assessments/daily 정상 케이스, 예외 케이스

**4. 테스트 데이터 관리**
- 픽스처 패턴 활용하여 테스트 데이터 재사용
- `@Sql` 또는 `@DataJpaTest`로 DB 상태 초기화

**멱등성 보장:**
- 각 테스트는 독립적으로 실행 가능
- `@Transactional` + `@Rollback`으로 데이터 격리

---

## 추가 예정 질문

구현 과정에서 발생하는 추가 설계 결정 사항들을 여기에 계속 업데이트할 예정입니다.
