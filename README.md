# Beyond Medicine - TMJ 장애 DTx 서비스

비욘드메디슨 백엔드 개발자 채용 과제

## 📌 프로젝트 개요

TMJ (턱관절 장애) 환자의 일일 검사 데이터를 관리하고 주차별 추이를 분석하는 백엔드 서비스입니다.

## 🛠 기술 스택

- **Language**: Kotlin 2.1.0
- **Framework**: Spring Boot 3.4.2
- **Build Tool**: Gradle (Kotlin DSL)
- **Database**: H2 (dev/test), MySQL (optional)
- **ORM**: Spring Data JPA / Hibernate
- **Test**: JUnit 5, Mockito-Kotlin, Kotest

## 🏗 아키텍처

**FP + OOP 하이브리드 아키텍처**
- 핵심 도메인 계산 로직: 순수 함수 (Functional Programming)
- 경계 레이어 (I/O, 트랜잭션): 객체지향 (OOP)

```
┌─────────────────────────────────────┐
│  Presentation (Controller, DTO)     │  ← OOP
└─────────────────┬───────────────────┘
                  │
┌─────────────────▼───────────────────┐
│  Application (Service, Repository)  │  ← OOP (트랜잭션 경계)
└─────────────────┬───────────────────┘
                  │
┌─────────────────▼───────────────────┐
│  Domain (Entity, Calculator)        │  ← OOP (Entity) + FP (Calculator)
└─────────────────────────────────────┘
```

## 📂 프로젝트 구조

```
src/main/kotlin/com/beyondmedicine/
├── domain/
│   ├── model/         # JPA Entities
│   ├── calculator/    # Pure Functions (순수 함수)
│   └── exception/     # Domain Exceptions
├── application/
│   ├── service/       # Service Layer
│   ├── repository/    # Spring Data JPA
│   └── dto/           # Internal DTOs
└── presentation/
    ├── controller/    # REST Controllers
    ├── dto/           # API DTOs
    └── exception/     # Global Exception Handler
```

## 🚀 빌드 및 실행

### 빌드
```bash
./gradlew build
```

### 테스트 실행
```bash
./gradlew test
```

### 애플리케이션 실행
```bash
./gradlew bootRun
```

### H2 Console 접속
```
URL: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:beyondmedicine
Username: sa
Password: (비워두기)
```

## 📝 개발 방식

**TDD (Test-Driven Development)**
- Red-Green-Refactor 사이클 엄수
- 테스트 먼저 작성 후 구현
- 커밋 단위: 각 TDD 사이클마다 커밋

## 📄 문서

- [설계 의사결정](docs/design-decisions.md)
- [FP + OOP 하이브리드 설계](docs/fp-hybrid-design.md)
- [API 설계](docs/api-design.md)
- [비즈니스 로직 설계](docs/business-logic-design.md)
- [Service 계층 설계](docs/service-layer-design.md)
- [Repository 설계](docs/repository-design.md)
- [예외 처리 전략](docs/exception-handling-design.md)
- [테스트 전략](docs/test-strategy.md)
- [TDD 구현 계획](docs/tdd-implementation-plan.md)
- [Git 커밋 컨벤션](docs/git-commit-convention.md)
- [구현 체크리스트](docs/implementation-checklist.md)
- [FAQ](docs/faq.md)

## 📊 진행 상황

- [x] Phase 0: 프로젝트 환경 설정 (완료)
- [ ] Phase 1: Domain Calculator (TDD) (진행 중)
- [ ] Phase 2: Domain Entity (TDD)
- [ ] Phase 3: Application Service (TDD)
- [ ] Phase 4: Presentation API (TDD)
- [ ] Phase 5: 통합 테스트

## 🔗 API 엔드포인트 (예정)

### 일일 검사 등록
```
POST /api/v1/assessments/daily
```

### 주차별 추이 조회
```
GET /api/v1/assessments/weekly?prescriptionCode={code}&startWeek={start}&endWeek={end}
```

## 👨‍💻 개발자

채기훈 (Kihun Chae)
