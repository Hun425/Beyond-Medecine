# Git Commit Strategy for TDD

## TDD 커밋 전략 개요

TDD의 Red-Green-Refactor 사이클을 Git 커밋 히스토리로 명확히 기록합니다.
각 사이클마다 3~4번의 커밋이 발생하며, 이를 통해 TDD 과정을 추적 가능하게 만듭니다.

---

## 커밋 메시지 컨벤션

### 기본 형식
```
<type>(<scope>): <subject>

[optional body]
```

### Type 분류

#### 1. TDD 사이클 관련
- **`test`**: 테스트 코드 추가 또는 수정 (주로 RED 단계)
- **`feat`**: 프로덕션 코드 구현 (주로 GREEN 단계)
- **`refactor`**: 코드 리팩토링 (REFACTOR 단계, 기능 변경 없음)

#### 2. 기타
- **`docs`**: 문서 업데이트 (README, 설계 문서 등)
- **`chore`**: 빌드 설정, 의존성 업데이트 등
- **`fix`**: 버그 수정
- **`style`**: 코드 포맷팅 (세미콜론, 공백 등)

---

## TDD Iteration별 커밋 흐름

### 기본 사이클 (한 기능당 3~4 커밋)

```
┌─────────────────────────────────────────────────────────┐
│ Iteration N: [기능명]                                    │
└─────────────────────────────────────────────────────────┘

1️⃣ 🔴 RED Phase
   → test(<scope>): add test for [기능명]

2️⃣ 🟢 GREEN Phase
   → feat(<scope>): implement [기능명] to pass test

3️⃣ 🔵 REFACTOR Phase (선택적)
   → refactor(<scope>): improve [구체적 개선 내용]

4️⃣ 📝 DOCS Phase (주기적)
   → docs: update implementation progress for [Phase/Iteration]
```

---

## 구체적인 커밋 예시

### Phase 1: Domain Calculator (순수 함수)

#### Iteration 1.1: PrescriptionCalculator - PENDING 상태

```bash
# 🔴 RED
git add src/test/kotlin/com/beyondmedicine/domain/calculator/PrescriptionCalculatorTest.kt
git commit -m "test(calculator): add test for PENDING status calculation

- Test: calculateStatus returns PENDING when not activated
- Given: createdAt, activatedAt=null, currentTime
- Expected: PrescriptionStatus.PENDING"

# 🟢 GREEN
git add src/main/kotlin/com/beyondmedicine/domain/model/PrescriptionStatus.kt
git add src/main/kotlin/com/beyondmedicine/domain/calculator/PrescriptionCalculator.kt
git commit -m "feat(calculator): implement PENDING status calculation

- Add PrescriptionStatus enum
- Implement calculateStatus() returning hardcoded PENDING
- Test passes: 1 passing"

# 🔵 REFACTOR (이 단계에서는 불필요, 스킵)

# 📝 DOCS (나중에 일괄 업데이트)
```

---

#### Iteration 1.2: PrescriptionCalculator - ACTIVE 상태

```bash
# 🔴 RED
git add src/test/kotlin/com/beyondmedicine/domain/calculator/PrescriptionCalculatorTest.kt
git commit -m "test(calculator): add test for ACTIVE status calculation

- Test: calculateStatus returns ACTIVE when activated within 42 days
- Given: activatedAt, currentTime within D+41
- Expected: PrescriptionStatus.ACTIVE"

# 🟢 GREEN
git add src/main/kotlin/com/beyondmedicine/domain/calculator/PrescriptionCalculator.kt
git commit -m "feat(calculator): implement ACTIVE status logic

- Add logic to return ACTIVE when activatedAt is not null
- Test passes: 2 passing"

# 🔵 REFACTOR (아직 단순하므로 스킵)
```

---

#### Iteration 1.3: PrescriptionCalculator - COMPLETED 상태 (경계값)

```bash
# 🔴 RED
git add src/test/kotlin/com/beyondmedicine/domain/calculator/PrescriptionCalculatorTest.kt
git commit -m "test(calculator): add boundary tests for ACTIVE/COMPLETED

- Test: D+41 23:59:59.999 returns ACTIVE (boundary)
- Test: D+42 00:00:00.000 returns COMPLETED (boundary)
- Expected: Proper boundary handling"

# 🟢 GREEN
git add src/main/kotlin/com/beyondmedicine/domain/calculator/PrescriptionCalculator.kt
git commit -m "feat(calculator): implement date-based ACTIVE/COMPLETED logic

- Add ACTIVE_PERIOD_DAYS constant (42)
- Calculate endOfActiveWeek (D+41 23:59:59.999)
- Return COMPLETED if currentTime > endOfActiveWeek
- Test passes: 4 passing"

# 🔵 REFACTOR
git add src/main/kotlin/com/beyondmedicine/domain/calculator/PrescriptionCalculator.kt
git commit -m "refactor(calculator): extract status calculation methods

- Extract calculateActivatedStatus() private method
- Extract calculateEndOfActiveWeek() helper method
- Improve code readability
- All tests still passing: 4 passing"

# 📝 DOCS
git add docs/implementation-checklist.md
git commit -m "docs: update Phase 1 progress (Iteration 1.1-1.3 complete)

- Mark PrescriptionCalculator PENDING/ACTIVE/COMPLETED as done
- Update test coverage: 4/8 test cases complete"
```

---

#### Iteration 1.4: PrescriptionCalculator - EXPIRED 상태

```bash
# 🔴 RED
git add src/test/kotlin/com/beyondmedicine/domain/calculator/PrescriptionCalculatorTest.kt
git commit -m "test(calculator): add test for EXPIRED status

- Test: not activated after 6 weeks returns EXPIRED
- Given: createdAt, activatedAt=null, currentTime > 6 weeks
- Expected: PrescriptionStatus.EXPIRED"

# 🟢 GREEN
git add src/main/kotlin/com/beyondmedicine/domain/calculator/PrescriptionCalculator.kt
git commit -m "feat(calculator): implement EXPIRED status logic

- Add EXPIRATION_WEEKS constant (6)
- Implement calculateNotActivatedStatus() method
- Return EXPIRED if currentTime > createdAt + 6 weeks
- Test passes: 5 passing"

# 🔵 REFACTOR (이미 깔끔하므로 스킵)

# 📝 DOCS
git add docs/implementation-checklist.md
git commit -m "docs: complete Phase 1.1 - PrescriptionCalculator

- All 5 test cases passing
- PENDING, ACTIVE, COMPLETED, EXPIRED logic complete
- Ready for Phase 1.2: WeekCalculator"
```

---

### Phase 2: WeekCalculator

```bash
# 🔴 RED
git add src/test/kotlin/com/beyondmedicine/domain/calculator/WeekCalculatorTest.kt
git commit -m "test(calculator): add test for week number calculation

- Test: same day as activation returns week 1
- Given: activationDate, assessmentDate (same day)
- Expected: weekNumber = 1"

# 🟢 GREEN
git add src/main/kotlin/com/beyondmedicine/domain/calculator/WeekCalculator.kt
git commit -m "feat(calculator): implement basic week number calculation

- Add WeekCalculator object
- Implement calculateWeekNumber() returning hardcoded 1
- Test passes: 1 passing"

# ... 이후 반복
```

---

### Phase 3: Service Layer (Outside-In TDD)

```bash
# 🔴 RED
git add src/test/kotlin/com/beyondmedicine/application/service/AssessmentServiceTest.kt
git commit -m "test(service): add test for createAssessment happy path

- Test: createAssessment with valid command succeeds
- Mock: PrescriptionRepository, DailyAssessmentRepository
- Expected: DailyAssessmentResult with correct data"

# 🟢 GREEN
git add src/main/kotlin/com/beyondmedicine/application/service/AssessmentService.kt
git add src/main/kotlin/com/beyondmedicine/application/dto/CreateDailyAssessmentCommand.kt
git add src/main/kotlin/com/beyondmedicine/application/dto/DailyAssessmentResult.kt
git commit -m "feat(service): implement AssessmentService.createAssessment

- Add CreateDailyAssessmentCommand DTO
- Add DailyAssessmentResult DTO
- Implement basic createAssessment logic
- Test passes: 1 passing"

# 🔴 RED (예외 케이스)
git add src/test/kotlin/com/beyondmedicine/application/service/AssessmentServiceTest.kt
git commit -m "test(service): add test for prescription not found

- Test: createAssessment throws exception when prescription not found
- Expected: PrescriptionNotFoundException"

# 🟢 GREEN
git add src/main/kotlin/com/beyondmedicine/application/exception/PrescriptionNotFoundException.kt
git add src/main/kotlin/com/beyondmedicine/application/service/AssessmentService.kt
git commit -m "feat(service): handle prescription not found case

- Add PrescriptionNotFoundException
- Throw exception when prescription is null
- Test passes: 2 passing"
```

---

### Phase 4: API Layer

```bash
# 🔴 RED
git add src/test/kotlin/com/beyondmedicine/presentation/controller/AssessmentControllerTest.kt
git commit -m "test(api): add test for POST /api/v1/assessments/daily

- Test: valid request returns 201 Created
- Mock: AssessmentService, DtoMapper
- Expected: CreateDailyAssessmentResponse with correct data"

# 🟢 GREEN
git add src/main/kotlin/com/beyondmedicine/presentation/controller/AssessmentController.kt
git add src/main/kotlin/com/beyondmedicine/presentation/dto/request/CreateDailyAssessmentRequest.kt
git add src/main/kotlin/com/beyondmedicine/presentation/dto/response/CreateDailyAssessmentResponse.kt
git add src/main/kotlin/com/beyondmedicine/presentation/dto/mapper/AssessmentDtoMapper.kt
git commit -m "feat(api): implement POST /api/v1/assessments/daily endpoint

- Add AssessmentController
- Add Request/Response DTOs
- Add DtoMapper
- Return 201 Created on success
- Test passes: 1 passing"
```

---

## 커밋 Scope 가이드

### Scope 네이밍 규칙
- `calculator`: Domain Calculator (순수 함수)
- `entity`: Domain Entity (JPA)
- `repository`: Repository 인터페이스 및 구현
- `service`: Application Service
- `api`: Presentation Layer (Controller, DTO)
- `exception`: 예외 처리
- `config`: 설정 파일
- `test`: 테스트 인프라 (Fixture, Helper 등)

### 예시
```bash
test(calculator): ...
feat(entity): ...
refactor(service): ...
docs: ...
chore(config): ...
```

---

## 문서 업데이트 주기

### 주기적 문서 커밋
- **매 Iteration 완료 시**: implementation-checklist.md 업데이트
- **매 Phase 완료 시**:
  - implementation-checklist.md 체크
  - 필요시 설계 문서 최신화
  - README.md 업데이트

### 예시
```bash
# Iteration 완료 시
docs: update Phase 1.1 progress (PrescriptionCalculator complete)

# Phase 완료 시
docs: complete Phase 1 - Domain Calculator layer

- All Calculator tests passing (15/15)
- PrescriptionCalculator: 5 tests
- WeekCalculator: 7 tests
- ChangeRateCalculator: 3 tests
- Ready to proceed to Phase 2: Domain Entity
```

---

## 커밋 메시지 작성 가이드

### ✅ 좋은 커밋 메시지
```bash
# 명확한 의도와 결과
test(calculator): add test for PENDING status calculation

# 구체적인 구현 내용
feat(calculator): implement PENDING status logic
- Return PENDING when activatedAt is null
- Test passes: 1 passing

# 리팩토링 이유 명시
refactor(calculator): extract private methods for better readability
- Extract calculateActivatedStatus()
- Extract calculateNotActivatedStatus()
- All tests still passing
```

### ❌ 나쁜 커밋 메시지
```bash
# 모호함
test: add test

# 너무 짧음
feat: fix

# 의미 없음
refactor: refactoring
```

---

## 특수 상황 처리

### 1. 테스트 실패 후 수정
```bash
# 원래 RED 커밋이 잘못된 경우
git add src/test/...
git commit -m "test(calculator): fix incorrect test expectation

- Previous test expected wrong value
- Correct expectation: ACTIVE instead of PENDING"
```

### 2. 버그 발견 및 수정
```bash
# 🔴 RED (버그 재현 테스트)
git commit -m "test(calculator): add failing test for edge case bug

- Bug: calculateStatus fails for exact boundary time
- Expected: ACTIVE, Actual: COMPLETED"

# 🟢 GREEN (버그 수정)
git commit -m "fix(calculator): correct boundary comparison logic

- Change from >= to > for boundary check
- Now correctly handles D+41 23:59:59.999
- Test passes"
```

### 3. 여러 파일 동시 커밋 (GREEN 단계)
```bash
# 여러 파일이 함께 필요한 경우
git add src/main/kotlin/com/beyondmedicine/domain/model/PrescriptionStatus.kt
git add src/main/kotlin/com/beyondmedicine/domain/calculator/PrescriptionCalculator.kt
git commit -m "feat(calculator): implement status calculation with enum

- Add PrescriptionStatus enum (PENDING, ACTIVE, COMPLETED, EXPIRED)
- Implement PrescriptionCalculator.calculateStatus()
- Test passes: 1 passing"
```

---

## Git Log 예상 히스토리

```bash
$ git log --oneline --graph

* abc1234 docs: complete Phase 1 - Domain Calculator layer
* def5678 refactor(calculator): improve ChangeRateCalculator structure
* ghi9012 feat(calculator): implement change rate calculation logic
* jkl3456 test(calculator): add tests for ChangeRateCalculator
* mno7890 docs: update Phase 1.2 progress (WeekCalculator complete)
* pqr1234 feat(calculator): implement week number boundary validation
* stu5678 test(calculator): add boundary tests for week calculation
* vwx9012 feat(calculator): implement basic week number calculation
* yza3456 test(calculator): add test for week number calculation
* bcd7890 docs: complete Phase 1.1 - PrescriptionCalculator
* efg1234 feat(calculator): implement EXPIRED status logic
* hij5678 test(calculator): add test for EXPIRED status
* klm9012 refactor(calculator): extract status calculation methods
* nop3456 feat(calculator): implement date-based ACTIVE/COMPLETED logic
* qrs7890 test(calculator): add boundary tests for ACTIVE/COMPLETED
* tuv1234 feat(calculator): implement ACTIVE status logic
* wxy5678 test(calculator): add test for ACTIVE status calculation
* zab9012 feat(calculator): implement PENDING status calculation
* cde3456 test(calculator): add test for PENDING status calculation
* fgh7890 chore: setup Spring Boot project with Gradle
* ijk1234 docs: add TDD implementation plan
* lmn5678 docs: add initial design documents
* opq9012 Initial commit
```

---

## 체크리스트

### 커밋 전 확인 사항
- [ ] 커밋 메시지가 명확한가?
- [ ] Type과 Scope가 올바른가?
- [ ] TDD 단계(RED/GREEN/REFACTOR)가 명확한가?
- [ ] 테스트 통과 여부가 메시지에 포함되었는가?
- [ ] 관련 파일이 모두 포함되었는가?

### 문서 업데이트 체크
- [ ] Iteration 완료 시 체크리스트 업데이트했는가?
- [ ] Phase 완료 시 전체 문서 리뷰했는가?
- [ ] README가 최신 상태인가?

---

## 참고 자료

- [Conventional Commits](https://www.conventionalcommits.org/)
- [Angular Commit Message Guidelines](https://github.com/angular/angular/blob/main/CONTRIBUTING.md#commit)
- Kent Beck - "Test Driven Development: By Example"

---

## 요약

### TDD 사이클당 커밋 패턴
```
1️⃣ test(<scope>): add test for ...
2️⃣ feat(<scope>): implement ...
3️⃣ refactor(<scope>): improve ... (선택적)
4️⃣ docs: update ... (주기적)
```

### 핵심 원칙
- **명확성**: 커밋 메시지만 봐도 무엇을 했는지 이해 가능
- **추적성**: TDD 사이클이 Git 히스토리에 명확히 기록
- **일관성**: 모든 커밋이 동일한 컨벤션 준수
- **의미성**: 각 커밋이 의미 있는 단위로 구성

**이제 TDD 여정을 시작할 준비가 완료되었습니다!** 🚀
