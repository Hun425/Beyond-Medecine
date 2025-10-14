# 예외 처리 전략

## 예외 처리 원칙

### 핵심 원칙
1. **명확한 예외 메시지**: 무엇이 잘못되었는지 명확히 전달
2. **적절한 HTTP 상태 코드**: REST API 표준 준수
3. **일관된 에러 응답 형식**: 클라이언트 처리 용이성
4. **보안 고려**: 민감한 정보 노출 방지

---

## 예외 계층 구조

```
RuntimeException
    ↓
DomainException (sealed class)
    ↓
    ├── PrescriptionNotFoundException (404)
    ├── InvalidPrescriptionStatusException (400)
    ├── DuplicateAssessmentException (409)
    ├── InvalidAssessmentDateException (400)
    └── ValidationException (400)
```

---

## 1. Custom Exception 정의

### DomainException (Base)

```kotlin
package com.beyondmedicine.application.exception

/**
 * 도메인 예외 기본 클래스
 * - Sealed Class로 타입 안전성 확보
 * - 모든 도메인 예외는 이 클래스를 상속
 */
sealed class DomainException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) {

    /**
     * 에러 코드 (선택적)
     * - 클라이언트에서 에러 타입 구분용
     */
    abstract val errorCode: String

    /**
     * HTTP 상태 코드
     */
    abstract val httpStatus: Int
}
```

### 구체적인 예외 클래스

```kotlin
package com.beyondmedicine.application.exception

/**
 * 처방을 찾을 수 없음 (404)
 */
class PrescriptionNotFoundException(
    message: String
) : DomainException(message) {
    override val errorCode = "PRESCRIPTION_NOT_FOUND"
    override val httpStatus = 404
}

/**
 * 유효하지 않은 처방 상태 (400)
 * - ACTIVE 상태가 아닌 처방에 검사 시도
 */
class InvalidPrescriptionStatusException(
    message: String
) : DomainException(message) {
    override val errorCode = "INVALID_PRESCRIPTION_STATUS"
    override val httpStatus = 400
}

/**
 * 중복 검사 (409)
 * - 하루 1회 제한 위반
 */
class DuplicateAssessmentException(
    message: String
) : DomainException(message) {
    override val errorCode = "DUPLICATE_ASSESSMENT"
    override val httpStatus = 409
}

/**
 * 유효하지 않은 검사 일자 (400)
 * - 1~6주차 범위 밖
 */
class InvalidAssessmentDateException(
    message: String
) : DomainException(message) {
    override val errorCode = "INVALID_ASSESSMENT_DATE"
    override val httpStatus = 400
}

/**
 * 검증 실패 (400)
 * - Bean Validation 실패
 * - 비즈니스 규칙 위반
 */
class ValidationException(
    message: String,
    val errors: List<String> = emptyList()
) : DomainException(message) {
    override val errorCode = "VALIDATION_FAILED"
    override val httpStatus = 400
}
```

---

## 2. 에러 응답 DTO

### ErrorResponse (표준 에러 응답)

```kotlin
package com.beyondmedicine.presentation.dto.response

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

/**
 * 표준 에러 응답
 * - RFC 7807 Problem Details 스타일
 */
data class ErrorResponse(
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val timestamp: LocalDateTime = LocalDateTime.now(),

    val status: Int,                    // HTTP 상태 코드
    val error: String,                  // HTTP 상태 텍스트 (예: Bad Request)
    val errorCode: String,              // 에러 코드 (예: PRESCRIPTION_NOT_FOUND)
    val message: String,                // 사용자 친화적 에러 메시지
    val path: String,                   // 요청 경로
    val validationErrors: List<ValidationError>? = null  // 검증 에러 상세 (선택)
)

/**
 * 검증 에러 상세
 */
data class ValidationError(
    val field: String,              // 필드명
    val rejectedValue: Any?,        // 거부된 값
    val message: String             // 에러 메시지
)
```

### 에러 응답 예시

#### 1. 처방 없음 (404)
```json
{
  "timestamp": "2025-10-14T15:30:45",
  "status": 404,
  "error": "Not Found",
  "errorCode": "PRESCRIPTION_NOT_FOUND",
  "message": "처방을 찾을 수 없습니다. 처방 코드: ABCD1234",
  "path": "/api/v1/assessments/daily",
  "validationErrors": null
}
```

#### 2. 중복 검사 (409)
```json
{
  "timestamp": "2025-10-14T15:30:45",
  "status": 409,
  "error": "Conflict",
  "errorCode": "DUPLICATE_ASSESSMENT",
  "message": "해당 날짜에 이미 검사가 등록되어 있습니다. 처방 코드: ABCD1234, 날짜: 2025-10-14",
  "path": "/api/v1/assessments/daily",
  "validationErrors": null
}
```

#### 3. 검증 실패 (400)
```json
{
  "timestamp": "2025-10-14T15:30:45",
  "status": 400,
  "error": "Bad Request",
  "errorCode": "VALIDATION_FAILED",
  "message": "입력값 검증에 실패했습니다",
  "path": "/api/v1/assessments/daily",
  "validationErrors": [
    {
      "field": "painScore",
      "rejectedValue": 15,
      "message": "통증 점수는 10 이하여야 합니다"
    },
    {
      "field": "prescriptionCode",
      "rejectedValue": "ABC123",
      "message": "처방 코드는 8자리여야 합니다"
    }
  ]
}
```

---

## 3. Global Exception Handler

### RestControllerAdvice

```kotlin
package com.beyondmedicine.presentation.exception

import com.beyondmedicine.application.exception.*
import com.beyondmedicine.presentation.dto.response.ErrorResponse
import com.beyondmedicine.presentation.dto.response.ValidationError
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import javax.validation.ConstraintViolationException

/**
 * 전역 예외 처리기
 * - 모든 예외를 일관된 형식으로 변환
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    /**
     * 도메인 예외 처리
     */
    @ExceptionHandler(DomainException::class)
    fun handleDomainException(
        ex: DomainException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = ex.httpStatus,
            error = getHttpStatusText(ex.httpStatus),
            errorCode = ex.errorCode,
            message = ex.message ?: "알 수 없는 오류가 발생했습니다",
            path = extractPath(request),
            validationErrors = if (ex is ValidationException) {
                ex.errors.map { ValidationError("", null, it) }
            } else null
        )

        return ResponseEntity
            .status(ex.httpStatus)
            .body(errorResponse)
    }

    /**
     * Bean Validation 예외 처리 (@Valid 실패)
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(
        ex: MethodArgumentNotValidException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val validationErrors = ex.bindingResult.fieldErrors.map { error ->
            ValidationError(
                field = error.field,
                rejectedValue = error.rejectedValue,
                message = error.defaultMessage ?: "유효하지 않은 값입니다"
            )
        }

        val errorResponse = ErrorResponse(
            status = 400,
            error = "Bad Request",
            errorCode = "VALIDATION_FAILED",
            message = "입력값 검증에 실패했습니다",
            path = extractPath(request),
            validationErrors = validationErrors
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponse)
    }

    /**
     * BindException 처리 (Query Parameter 검증 실패)
     */
    @ExceptionHandler(BindException::class)
    fun handleBindException(
        ex: BindException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val validationErrors = ex.bindingResult.fieldErrors.map { error ->
            ValidationError(
                field = error.field,
                rejectedValue = error.rejectedValue,
                message = error.defaultMessage ?: "유효하지 않은 값입니다"
            )
        }

        val errorResponse = ErrorResponse(
            status = 400,
            error = "Bad Request",
            errorCode = "VALIDATION_FAILED",
            message = "쿼리 파라미터 검증에 실패했습니다",
            path = extractPath(request),
            validationErrors = validationErrors
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponse)
    }

    /**
     * ConstraintViolation 예외 처리
     */
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(
        ex: ConstraintViolationException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val validationErrors = ex.constraintViolations.map { violation ->
            ValidationError(
                field = violation.propertyPath.toString(),
                rejectedValue = violation.invalidValue,
                message = violation.message
            )
        }

        val errorResponse = ErrorResponse(
            status = 400,
            error = "Bad Request",
            errorCode = "VALIDATION_FAILED",
            message = "제약 조건 검증에 실패했습니다",
            path = extractPath(request),
            validationErrors = validationErrors
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponse)
    }

    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        ex: IllegalArgumentException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = 400,
            error = "Bad Request",
            errorCode = "ILLEGAL_ARGUMENT",
            message = ex.message ?: "잘못된 요청입니다",
            path = extractPath(request)
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponse)
    }

    /**
     * 예상치 못한 예외 처리 (500)
     */
    @ExceptionHandler(Exception::class)
    fun handleGeneralException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        // 프로덕션에서는 상세 에러 메시지 숨기기
        val errorResponse = ErrorResponse(
            status = 500,
            error = "Internal Server Error",
            errorCode = "INTERNAL_SERVER_ERROR",
            message = "서버 내부 오류가 발생했습니다",
            path = extractPath(request)
        )

        // 로그에는 상세 정보 기록
        // logger.error("Unexpected error occurred", ex)

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorResponse)
    }

    /**
     * HTTP 상태 코드 → 텍스트 변환
     */
    private fun getHttpStatusText(statusCode: Int): String {
        return when (statusCode) {
            400 -> "Bad Request"
            404 -> "Not Found"
            409 -> "Conflict"
            500 -> "Internal Server Error"
            else -> "Unknown Error"
        }
    }

    /**
     * WebRequest에서 경로 추출
     */
    private fun extractPath(request: WebRequest): String {
        return request.getDescription(false).removePrefix("uri=")
    }
}
```

---

## 4. 예외 처리 흐름

### 전체 흐름도

```
Client Request
    ↓
Controller (@Valid 검증)
    ↓ (검증 실패)
    MethodArgumentNotValidException
    ↓ (검증 성공)
Service (비즈니스 검증)
    ↓ (비즈니스 규칙 위반)
    DomainException
    ↓ (통과)
Repository (데이터 조회)
    ↓ (데이터 없음)
    PrescriptionNotFoundException
    ↓ (성공)
Response
    ↓
GlobalExceptionHandler (모든 예외 캐치)
    ↓
ErrorResponse (일관된 형식)
    ↓
Client
```

### 계층별 예외 발생 지점

```kotlin
// Controller: Bean Validation
@PostMapping("/api/v1/assessments/daily")
fun createAssessment(
    @Valid @RequestBody request: CreateDailyAssessmentRequest  // MethodArgumentNotValidException
): CreateDailyAssessmentResponse

// Service: 비즈니스 검증
fun createAssessment(command: CreateDailyAssessmentCommand) {
    val prescription = prescriptionRepository.findByCode(command.prescriptionCode)
        ?: throw PrescriptionNotFoundException("처방을 찾을 수 없습니다")  // 404

    if (!prescription.canPerformAssessment()) {
        throw InvalidPrescriptionStatusException("검사 불가능한 상태")  // 400
    }

    if (dailyAssessmentRepository.existsByPrescriptionAndAssessmentDate(...)) {
        throw DuplicateAssessmentException("중복 검사")  // 409
    }
}

// Domain: 도메인 규칙 검증
class DailyAssessment(...) {
    init {
        require(painScore in 0..10) {  // IllegalArgumentException → 400
            "통증 점수는 0~10 사이여야 합니다"
        }
    }
}
```

---

## 5. 예외 처리 Best Practices

### ✅ 좋은 예외 처리

```kotlin
// 1. 명확한 예외 메시지
throw PrescriptionNotFoundException(
    "처방을 찾을 수 없습니다. 처방 코드: $prescriptionCode"
)

// 2. 적절한 예외 타입
if (exists) {
    throw DuplicateAssessmentException(
        "해당 날짜에 이미 검사가 등록되어 있습니다. " +
        "처방 코드: ${prescription.code}, 날짜: $assessmentDate"
    )
}

// 3. 도메인 규칙은 require 활용
require(weekNumber in 1..6) {
    "주차는 1~6 사이여야 합니다. 입력: $weekNumber"
}
```

### ❌ 나쁜 예외 처리

```kotlin
// 1. 모호한 메시지
throw Exception("오류 발생")  // ❌ 무슨 오류?

// 2. 예외 무시
try {
    // ...
} catch (e: Exception) {
    // 아무것도 안 함  // ❌ 문제 추적 불가
}

// 3. 과도한 예외 처리
try {
    prescription.calculateWeekNumber(date)
} catch (e: Exception) {
    // 순수 함수는 예외 안 던짐  // ❌ 불필요한 try-catch
}
```

---

## 6. 로깅 전략 (선택적)

### 예외 로깅

```kotlin
import org.slf4j.LoggerFactory

@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    @ExceptionHandler(DomainException::class)
    fun handleDomainException(ex: DomainException, request: WebRequest): ResponseEntity<ErrorResponse> {
        // 비즈니스 예외는 WARN 레벨 (예상된 예외)
        logger.warn("Domain exception occurred: {}, path: {}",
            ex.message, extractPath(request))

        // ... 응답 생성
    }

    @ExceptionHandler(Exception::class)
    fun handleGeneralException(ex: Exception, request: WebRequest): ResponseEntity<ErrorResponse> {
        // 예상치 못한 예외는 ERROR 레벨 (버그 가능성)
        logger.error("Unexpected exception occurred: path: {}", extractPath(request), ex)

        // ... 응답 생성
    }
}
```

---

## 7. 테스트 전략

### Controller 테스트 (예외 응답 검증)

```kotlin
@WebMvcTest(AssessmentController::class)
class AssessmentControllerExceptionTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var assessmentService: AssessmentService

    @MockBean
    private lateinit var mapper: AssessmentDtoMapper

    @Test
    fun `처방이 없으면 404를 반환한다`() {
        // given
        given(assessmentService.createAssessment(any()))
            .willThrow(PrescriptionNotFoundException("처방을 찾을 수 없습니다"))

        // when & then
        mockMvc.perform(
            post("/api/v1/assessments/daily")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson())
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.errorCode").value("PRESCRIPTION_NOT_FOUND"))
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    fun `중복 검사는 409를 반환한다`() {
        // given
        given(assessmentService.createAssessment(any()))
            .willThrow(DuplicateAssessmentException("중복 검사"))

        // when & then
        mockMvc.perform(
            post("/api/v1/assessments/daily")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson())
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.errorCode").value("DUPLICATE_ASSESSMENT"))
    }

    @Test
    fun `잘못된 입력값은 400과 검증 에러를 반환한다`() {
        // when & then
        mockMvc.perform(
            post("/api/v1/assessments/daily")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "prescriptionCode": "ABC",
                        "assessmentDate": "2025-10-14",
                        "painScore": 15,
                        "stressScore": 5,
                        "jawFunctionScore": 6
                    }
                """)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.validationErrors").isArray)
            .andExpect(jsonPath("$.validationErrors[0].field").exists())
            .andExpect(jsonPath("$.validationErrors[0].message").exists())
    }
}
```

---

## 요구사항 추적표

### 예외 처리 구현 (실무 표준 기반)

| 기능 | 구현 위치 | 상태 |
|-----|----------|------|
| 처방 없음 예외 (404) | `PrescriptionNotFoundException` | ✅ |
| 처방 상태 오류 (400) | `InvalidPrescriptionStatusException` | ✅ |
| 중복 검사 (409) | `DuplicateAssessmentException` | ✅ |
| 검사 일자 오류 (400) | `InvalidAssessmentDateException` | ✅ |
| 검증 실패 (400) | `ValidationException` | ✅ |
| Bean Validation 실패 | `MethodArgumentNotValidException` 처리 | ✅ |
| 전역 예외 처리 | `@RestControllerAdvice` | ✅ |
| 일관된 에러 응답 | `ErrorResponse` DTO | ✅ |

### HTTP 상태 코드 매핑

| HTTP 상태 | 예외 타입 | 사용 시나리오 |
|----------|----------|--------------|
| 400 | Bad Request | 검증 실패, 잘못된 입력 |
| 404 | Not Found | 리소스 없음 |
| 409 | Conflict | 중복 데이터 |
| 500 | Internal Server Error | 예상치 못한 오류 |

---

## 참고 문서
- [설계 의사결정 문서](./design-decisions.md)
- [API 설계 문서](./api-design.md)
- [Service 계층 설계](./service-layer-design.md)
