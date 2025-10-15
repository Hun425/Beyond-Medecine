# Git Commit Convention (TDD 기반)

## 📌 중요 원칙

- **커밋 메시지(제목)**: 영어로 작성
- **커밋 설명(body)**: 한국어로 작성
- **코드**: 영어 (변수명, 함수명, 클래스명)
- **주석**: 필요시 한국어 가능

---

## 커밋 메시지 기본 형식

```
<type>(<scope>): <subject in English>

[한국어로 상세 설명]
```

---

## TDD 사이클당 커밋 패턴

### 1️⃣ 🔴 RED: 실패하는 테스트 작성

```bash
git commit -m "test(calculator): add test for PENDING status calculation

처방 상태 계산 테스트 추가
- 조건: 생성 후 활성화되지 않음
- 기대값: PrescriptionStatus.PENDING
- 현재 상태: 컴파일 에러 (구현 안 됨)"
```

### 2️⃣ 🟢 GREEN: 테스트 통과시키는 최소 구현

```bash
git commit -m "feat(calculator): implement PENDING status calculation

PENDING 상태 계산 로직 구현
- PrescriptionStatus enum 추가
- calculateStatus() 메서드 구현 (하드코딩)
- 테스트 통과: 1개"
```

### 3️⃣ 🔵 REFACTOR: 코드 개선 (선택적)

```bash
git commit -m "refactor(calculator): extract private methods

가독성 개선을 위한 메서드 분리
- calculateActivatedStatus() 추출
- calculateNotActivatedStatus() 추출
- 모든 테스트 여전히 통과: 5개"
```

### 4️⃣ 📝 DOCS: 문서 업데이트 (주기적)

```bash
git commit -m "docs: update Phase 1 progress

Phase 1.1 PrescriptionCalculator 완료
- 구현 완료: PENDING, ACTIVE, COMPLETED, EXPIRED
- 테스트 통과: 5개
- 다음 단계: WeekCalculator 구현"
```

---

## Type 분류

### TDD 관련
- **test**: 테스트 코드 추가/수정 (RED 단계)
- **feat**: 기능 구현 (GREEN 단계)
- **refactor**: 리팩토링 (REFACTOR 단계, 동작 변경 없음)

### 기타
- **docs**: 문서 업데이트
- **chore**: 빌드 설정, 의존성 업데이트
- **fix**: 버그 수정
- **style**: 코드 포맷팅 (동작 변경 없음)

---

## Scope 가이드

| Scope | 의미 | 예시 파일 |
|-------|------|----------|
| `calculator` | 도메인 계산기 (순수 함수) | `PrescriptionCalculator.kt` |
| `entity` | JPA 엔티티 | `Prescription.kt` |
| `repository` | Repository 계층 | `PrescriptionRepository.kt` |
| `service` | 서비스 계층 | `AssessmentService.kt` |
| `api` | API 계층 | `AssessmentController.kt` |
| `dto` | DTO 클래스 | `CreateAssessmentRequest.kt` |
| `exception` | 예외 처리 | `PrescriptionNotFoundException.kt` |
| `config` | 설정 파일 | `application.yml` |

---

## 실제 커밋 예시

### Phase 1: Domain Calculator

#### Iteration 1.1: PENDING 상태

```bash
# 🔴 RED
git add src/test/kotlin/.../PrescriptionCalculatorTest.kt
git commit -m "test(calculator): add test for PENDING status calculation

PENDING 상태 계산 테스트 추가
- 생성 후 활성화하지 않은 경우 PENDING 반환
- Given: createdAt, activatedAt=null, currentTime
- Expected: PrescriptionStatus.PENDING
- 현재: 컴파일 에러 (PrescriptionCalculator 없음)"

# 🟢 GREEN
git add src/main/kotlin/.../PrescriptionStatus.kt
git add src/main/kotlin/.../PrescriptionCalculator.kt
git commit -m "feat(calculator): implement PENDING status calculation

PENDING 상태 계산 로직 구현
- PrescriptionStatus enum 추가 (PENDING, ACTIVE, COMPLETED, EXPIRED)
- PrescriptionCalculator 객체 생성
- calculateStatus() 메서드 구현 (하드코딩 PENDING 반환)
- 테스트 통과: 1개"
```

#### Iteration 1.2: ACTIVE 상태

```bash
# 🔴 RED
git commit -m "test(calculator): add test for ACTIVE status calculation

ACTIVE 상태 계산 테스트 추가
- 활성화 후 42일 이내인 경우 ACTIVE 반환
- Given: activatedAt != null, currentTime within D+41
- Expected: PrescriptionStatus.ACTIVE
- 현재: 테스트 실패 (PENDING 반환됨)"

# 🟢 GREEN
git commit -m "feat(calculator): implement ACTIVE status logic

ACTIVE 상태 로직 구현
- activatedAt이 null이 아니면 ACTIVE 반환
- 간단한 분기 처리 추가
- 테스트 통과: 2개"
```

#### Iteration 1.3: COMPLETED 상태 (경계값)

```bash
# 🔴 RED
git commit -m "test(calculator): add boundary tests for ACTIVE/COMPLETED

ACTIVE/COMPLETED 경계값 테스트 추가
- D+41 23:59:59.999 → ACTIVE (경계값)
- D+42 00:00:00.000 → COMPLETED (경계값)
- 현재: 테스트 실패 (날짜 계산 없음)"

# 🟢 GREEN
git commit -m "feat(calculator): implement date-based status logic

날짜 기반 상태 계산 구현
- ACTIVE_PERIOD_DAYS 상수 추가 (42일)
- endOfActiveWeek 계산 (D+41 23:59:59.999)
- currentTime > endOfActiveWeek이면 COMPLETED
- 테스트 통과: 4개"

# 🔵 REFACTOR
git commit -m "refactor(calculator): extract status calculation methods

가독성 개선을 위한 메서드 분리
- calculateActivatedStatus() private 메서드 추출
- calculateEndOfActiveWeek() 헬퍼 메서드 추출
- when 표현식으로 main logic 단순화
- 모든 테스트 여전히 통과: 4개"

# 📝 DOCS
git commit -m "docs: update Phase 1.1 progress

Phase 1.1 진행 상황 업데이트
- PrescriptionCalculator 부분 완료
- 구현 완료: PENDING, ACTIVE, COMPLETED
- 테스트 통과: 4개
- 남은 작업: EXPIRED 상태 구현"
```

#### Iteration 1.4: EXPIRED 상태

```bash
# 🔴 RED
git commit -m "test(calculator): add test for EXPIRED status

EXPIRED 상태 테스트 추가
- 생성 후 6주 경과, 활성화 안 됨 → EXPIRED
- Given: createdAt, activatedAt=null, currentTime > 6주
- Expected: PrescriptionStatus.EXPIRED
- 현재: 테스트 실패 (PENDING 반환됨)"

# 🟢 GREEN
git commit -m "feat(calculator): implement EXPIRED status logic

EXPIRED 상태 로직 구현
- EXPIRATION_WEEKS 상수 추가 (6주)
- calculateNotActivatedStatus() 메서드 구현
- createdAt + 6주 경과 체크
- 테스트 통과: 5개"

# 📝 DOCS
git commit -m "docs: complete Phase 1.1 - PrescriptionCalculator

Phase 1.1 완료
- PrescriptionCalculator 모든 상태 구현 완료
- PENDING, ACTIVE, COMPLETED, EXPIRED 로직 구현
- 테스트 통과: 5개
- 다음 단계: Phase 1.2 WeekCalculator 시작"
```

---

### Phase 2: WeekCalculator

```bash
# 🔴 RED
git commit -m "test(calculator): add test for week 1 calculation

1주차 계산 테스트 추가
- 활성화 당일 = 1주차
- Given: activationDate, assessmentDate (동일)
- Expected: weekNumber = 1
- 현재: 컴파일 에러 (WeekCalculator 없음)"

# 🟢 GREEN
git commit -m "feat(calculator): implement basic week number calculation

기본 주차 계산 구현
- WeekCalculator 객체 생성
- calculateWeekNumber() 메서드 구현 (하드코딩 1)
- 테스트 통과: 1개"

# 🔴 RED
git commit -m "test(calculator): add test for week 3 calculation (PDF example)

3주차 계산 테스트 추가 (과제 예시)
- 09-01 활성화 → 09-15 검사 = 3주차
- (14일 / 7) + 1 = 3주차
- Given: 14일 차이
- Expected: weekNumber = 3
- 현재: 테스트 실패 (1 반환됨)"

# 🟢 GREEN
git commit -m "feat(calculator): implement date-based week calculation

날짜 기반 주차 계산 구현
- ChronoUnit.DAYS.between() 사용
- 공식: (daysSinceActivation / 7) + 1
- DAYS_PER_WEEK 상수 추가 (7)
- 테스트 통과: 2개"

# ... 계속 반복
```

---

### Phase 3: Service Layer

```bash
# 🔴 RED
git commit -m "test(service): add test for createAssessment happy path

일일 검사 등록 성공 케이스 테스트
- 유효한 command로 검사 등록 성공
- Mock: PrescriptionRepository, DailyAssessmentRepository
- Expected: DailyAssessmentResult 반환
- 현재: 컴파일 에러 (AssessmentService 없음)"

# 🟢 GREEN
git commit -m "feat(service): implement AssessmentService.createAssessment

AssessmentService 기본 구현
- CreateDailyAssessmentCommand DTO 추가
- DailyAssessmentResult DTO 추가
- createAssessment() 메서드 구현 (최소 로직)
- 테스트 통과: 1개"

# 🔴 RED
git commit -m "test(service): add test for prescription not found case

처방 없음 예외 케이스 테스트
- 존재하지 않는 처방 코드로 요청
- Expected: PrescriptionNotFoundException
- 현재: 테스트 실패 (예외 안 던짐)"

# 🟢 GREEN
git commit -m "feat(service): handle prescription not found case

처방 없음 예외 처리 구현
- PrescriptionNotFoundException 클래스 추가
- findByCode() null 체크 및 예외 throw
- 테스트 통과: 2개"
```

---

## 특수 상황 처리

### 1. 잘못된 테스트 수정

```bash
git commit -m "test(calculator): fix incorrect test expectation

잘못된 테스트 기댓값 수정
- 이전: PENDING 기대 (잘못됨)
- 수정: ACTIVE 기대 (올바름)
- 이유: 활성화된 처방이므로 ACTIVE가 맞음"
```

### 2. 버그 발견 및 수정

```bash
# 🔴 RED (버그 재현)
git commit -m "test(calculator): add failing test for boundary bug

경계값 버그 재현 테스트
- Bug: D+41 23:59:59에서 COMPLETED 반환됨
- Expected: ACTIVE
- Actual: COMPLETED
- 원인: >= 비교 연산자 사용"

# 🟢 GREEN (수정)
git commit -m "fix(calculator): correct boundary comparison logic

경계값 비교 로직 수정
- 변경: >= → >
- D+41 23:59:59.999까지 ACTIVE 반환
- D+42 00:00:00부터 COMPLETED 반환
- 테스트 통과"
```

---

## 문서 업데이트 주기

### Iteration 완료 시
```bash
git commit -m "docs: update Phase 1.1 progress

Phase 1.1 진행 상황 업데이트
- PrescriptionCalculator 완료
- 테스트 통과: 5개
- 체크리스트 업데이트"
```

### Phase 완료 시
```bash
git commit -m "docs: complete Phase 1 - Domain Calculator

Phase 1 완료 문서화
- PrescriptionCalculator: 5 tests
- WeekCalculator: 7 tests
- ChangeRateCalculator: 3 tests
- 전체 테스트: 15개 통과
- 다음 단계: Phase 2 Entity 구현 시작"
```

---

## ✅ 좋은 커밋 메시지 예시

```bash
# 명확한 의도
test(calculator): add test for PENDING status calculation

# 구체적인 구현 내용
feat(calculator): implement PENDING status logic
- Return PENDING when activatedAt is null
- Add PrescriptionStatus enum
- Test passes: 1/5

# 리팩토링 이유 명시
refactor(calculator): extract private methods for readability
- Extract calculateActivatedStatus()
- Extract calculateNotActivatedStatus()
- All tests still passing: 5/5
```

---

## ❌ 나쁜 커밋 메시지 예시

```bash
# 너무 모호함
test: add test
feat: implement

# 의미 없음
refactor: code refactoring
fix: fix bug

# 한글로 작성 (X)
test: 테스트 추가
feat: 기능 구현
```

---

## 체크리스트

### 커밋 전 확인사항
- [ ] 커밋 메시지(제목)가 영어인가?
- [ ] Type이 올바른가? (test/feat/refactor/docs)
- [ ] Scope가 명확한가?
- [ ] 설명(body)이 한국어로 상세히 작성되었는가?
- [ ] TDD 단계가 명확한가?

### 문서 업데이트
- [ ] Iteration 완료 시 docs 커밋 했는가?
- [ ] Phase 완료 시 전체 문서 리뷰했는가?

---

## 요약

### 기본 원칙
- **커밋 제목**: 영어 (국제 표준)
- **커밋 본문**: 한국어 (상세 설명)
- **코드**: 영어 (변수/함수/클래스명)

### TDD 사이클당 커밋
```
1️⃣ test(<scope>): ... (영어)
   [한국어 설명]

2️⃣ feat(<scope>): ... (영어)
   [한국어 설명]

3️⃣ refactor(<scope>): ... (영어)
   [한국어 설명]

4️⃣ docs: ... (영어)
   [한국어 설명]
```

**준비 완료! 이제 TDD를 시작합시다!** 🚀
