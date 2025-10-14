# API 설계 문서

## API 목록

### 1. 일일 검사 등록 API
**POST /api/v1/assessments/daily**

### 2. 주차별 검사 추이 분석 API
**GET /api/v1/assessments/weekly-trend**

---

## 계층별 DTO 설계

### 설계 원칙
1. **계층 간 명확한 분리**: Presentation ↔ Application ↔ Domain
2. **변화 대응력**: API 스펙 변경이 내부 로직에 영향 최소화
3. **재사용성**: Command/Result 패턴으로 여러 진입점 지원
4. **검증 계층화**: Bean Validation (형식) + Custom Validator (비즈니스 규칙)

---

## 1. 일일 검사 등록 API

### Request DTO (Presentation Layer)

```kotlin
package com.beyondmedicine.presentation.dto.request

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate
import javax.validation.Valid
import javax.validation.constraints.*

/**
 * 일일 검사 등록 요청 DTO
 * - API 계층에서만 사용
 * - String 타입으로 느슨한 결합 유지
 */
data class CreateDailyAssessmentRequest(
    @field:NotBlank(message = "처방 코드는 필수입니다")
    @field:Size(min = 8, max = 8, message = "처방 코드는 8자리여야 합니다")
    val prescriptionCode: String,

    @field:NotNull(message = "검사 일자는 필수입니다")
    @JsonFormat(pattern = "yyyy-MM-dd")
    val assessmentDate: LocalDate,

    @field:NotNull(message = "통증 점수는 필수입니다")
    @field:Min(value = 0, message = "통증 점수는 0 이상이어야 합니다")
    @field:Max(value = 10, message = "통증 점수는 10 이하여야 합니다")
    val painScore: Int,

    @field:NotNull(message = "스트레스 점수는 필수입니다")
    @field:Min(value = 0, message = "스트레스 점수는 0 이상이어야 합니다")
    @field:Max(value = 10, message = "스트레스 점수는 10 이하여야 합니다")
    val stressScore: Int,

    @field:NotNull(message = "턱 기능 점수는 필수입니다")
    @field:Min(value = 0, message = "턱 기능 점수는 0 이상이어야 합니다")
    @field:Max(value = 10, message = "턱 기능 점수는 10 이하여야 합니다")
    val jawFunctionScore: Int,

    @field:Valid
    @field:Size(max = 6, message = "통증 부위는 최대 6개까지 등록 가능합니다")
    val painAreas: List<PainAreaRequest> = emptyList()
)

/**
 * 통증 부위 요청 DTO
 */
data class PainAreaRequest(
    @field:NotBlank(message = "통증 부위는 필수입니다")
    val location: String,  // Enum 대신 String (느슨한 결합)

    @field:NotNull(message = "통증 강도는 필수입니다")
    @field:Min(value = 0, message = "통증 강도는 0 이상이어야 합니다")
    @field:Max(value = 10, message = "통증 강도는 10 이하여야 합니다")
    val intensity: Int,

    @field:Size(max = 500, message = "부연 설명은 500자 이하여야 합니다")
    val description: String? = null
)
```

### Response DTO (Presentation Layer)

```kotlin
package com.beyondmedicine.presentation.dto.response

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

/**
 * 일일 검사 등록 응답 DTO
 */
data class CreateDailyAssessmentResponse(
    val assessmentId: String,
    val prescriptionCode: String,
    val weekNumber: Int,

    @JsonFormat(pattern = "yyyy-MM-dd")
    val assessmentDate: LocalDate
)
```

### Command (Application Layer)

```kotlin
package com.beyondmedicine.application.dto

import com.beyondmedicine.domain.model.PainLocation
import java.time.LocalDate

/**
 * 일일 검사 생성 커맨드
 * - Application 계층 진입점
 * - 여러 인터페이스(REST, gRPC, Batch)에서 재사용 가능
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
    val location: PainLocation,  // 여기서는 Domain Enum 사용
    val intensity: Int,
    val description: String?
)
```

### Result (Application Layer)

```kotlin
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

---

## 2. 주차별 검사 추이 분석 API

### Request (Query Parameters)

```kotlin
package com.beyondmedicine.presentation.dto.request

import javax.validation.constraints.*

/**
 * 주차별 추이 분석 요청
 */
data class WeeklyTrendRequest(
    @field:NotBlank(message = "처방 코드는 필수입니다")
    @field:Size(min = 8, max = 8, message = "처방 코드는 8자리여야 합니다")
    val prescriptionCode: String,

    @field:Min(value = 1, message = "시작 주차는 1 이상이어야 합니다")
    @field:Max(value = 6, message = "시작 주차는 6 이하여야 합니다")
    val startWeek: Int? = null,  // null인 경우 1로 처리

    @field:Min(value = 1, message = "종료 주차는 1 이상이어야 합니다")
    @field:Max(value = 6, message = "종료 주차는 6 이하여야 합니다")
    val endWeek: Int? = null  // null인 경우 현재 주차로 처리
)
```

### Response DTO (Presentation Layer)

```kotlin
package com.beyondmedicine.presentation.dto.response

/**
 * 주차별 추이 분석 응답
 */
data class WeeklyTrendResponse(
    val prescriptionCode: String,
    val period: PeriodInfo,
    val weeklyTrends: List<WeeklyTrendInfo>,
    val topPainAreas: List<TopPainAreaInfo>
)

/**
 * 기간 정보
 */
data class PeriodInfo(
    val startWeek: Int,
    val endWeek: Int
)

/**
 * 주차별 추이 정보
 */
data class WeeklyTrendInfo(
    val weekNumber: Int,
    val averagePainScore: Double,
    val averageStressScore: Double,
    val averageJawFunctionScore: Double,
    val changeRates: ChangeRateInfo?  // 첫 번째 주차는 null
)

/**
 * 변화율 정보
 * - 양수: 호전
 * - 음수: 악화
 */
data class ChangeRateInfo(
    val pain: Double,           // 통증 변화율 (%)
    val stress: Double,         // 스트레스 변화율 (%)
    val jawFunction: Double     // 턱 기능 변화율 (%)
)

/**
 * 빈발 통증 부위 정보
 */
data class TopPainAreaInfo(
    val location: String,           // Enum name (예: LEFT_JAW)
    val locationName: String,       // 한글 이름 (예: 좌측 턱 관절)
    val count: Int,                 // 기록 횟수
    val averageIntensity: Double    // 평균 강도
)
```

### Query (Application Layer)

```kotlin
package com.beyondmedicine.application.dto

/**
 * 주차별 추이 분석 쿼리
 * - CQRS 패턴의 Query 객체
 */
data class WeeklyTrendQuery(
    val prescriptionCode: String,
    val startWeek: Int,  // 기본값 처리 완료된 상태
    val endWeek: Int     // 기본값 처리 완료된 상태
)
```

### Result (Application Layer)

```kotlin
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
    val assessmentCount: Int  // 해당 주차 검사 횟수
)

/**
 * 빈발 통증 부위 데이터
 */
data class TopPainAreaData(
    val location: PainLocation,  // Domain Enum
    val count: Int,
    val averageIntensity: Double
)
```

---

## 3. 에러 응답 표준화

```kotlin
package com.beyondmedicine.presentation.dto.response

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDateTime

/**
 * 표준 에러 응답
 */
data class ErrorResponse(
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    val timestamp: LocalDateTime,

    val status: Int,                // HTTP 상태 코드
    val error: String,              // 에러 타입 (예: Bad Request)
    val message: String,            // 에러 메시지
    val path: String,               // 요청 경로
    val validationErrors: List<ValidationError>? = null  // 검증 에러 상세
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

---

## 4. DTO Mapper

```kotlin
package com.beyondmedicine.presentation.dto.mapper

import com.beyondmedicine.application.dto.*
import com.beyondmedicine.domain.model.PainLocation
import com.beyondmedicine.presentation.dto.request.*
import com.beyondmedicine.presentation.dto.response.*
import com.beyondmedicine.common.validator.PainLocationValidator
import com.beyondmedicine.common.validator.PrescriptionCodeValidator
import org.springframework.stereotype.Component

/**
 * DTO 변환 책임 분리
 * - API DTO ↔ Service Command/Result 변환
 */
@Component
class AssessmentDtoMapper {

    /**
     * Request DTO → Command 변환
     */
    fun toServiceCommand(request: CreateDailyAssessmentRequest): CreateDailyAssessmentCommand {
        // 추가 검증
        PrescriptionCodeValidator.validateOrThrow(request.prescriptionCode)

        return CreateDailyAssessmentCommand(
            prescriptionCode = request.prescriptionCode,
            assessmentDate = request.assessmentDate,
            painScore = request.painScore,
            stressScore = request.stressScore,
            jawFunctionScore = request.jawFunctionScore,
            painAreas = request.painAreas.map { toPainAreaCommand(it) }
        )
    }

    /**
     * PainAreaRequest → PainAreaCommand 변환
     */
    private fun toPainAreaCommand(request: PainAreaRequest): PainAreaCommand {
        // 통증 부위 검증 및 Enum 변환
        PainLocationValidator.validateOrThrow(request.location)

        return PainAreaCommand(
            location = PainLocation.valueOf(request.location),
            intensity = request.intensity,
            description = request.description
        )
    }

    /**
     * Result → Response DTO 변환
     */
    fun toResponse(result: DailyAssessmentResult): CreateDailyAssessmentResponse {
        return CreateDailyAssessmentResponse(
            assessmentId = result.assessmentId.toString(),
            prescriptionCode = result.prescriptionCode,
            weekNumber = result.weekNumber,
            assessmentDate = result.assessmentDate
        )
    }

    /**
     * Request → Query 변환 (기본값 처리 포함)
     */
    fun toQuery(request: WeeklyTrendRequest, currentWeek: Int): WeeklyTrendQuery {
        PrescriptionCodeValidator.validateOrThrow(request.prescriptionCode)

        return WeeklyTrendQuery(
            prescriptionCode = request.prescriptionCode,
            startWeek = request.startWeek ?: 1,
            endWeek = request.endWeek ?: currentWeek
        )
    }

    /**
     * WeeklyTrendData → Response DTO 변환
     */
    fun toWeeklyTrendResponse(data: WeeklyTrendData): WeeklyTrendResponse {
        return WeeklyTrendResponse(
            prescriptionCode = data.prescriptionCode,
            period = PeriodInfo(
                startWeek = data.startWeek,
                endWeek = data.endWeek
            ),
            weeklyTrends = toWeeklyTrendInfoList(data.weeklyTrends),
            topPainAreas = data.topPainAreas.map { toTopPainAreaInfo(it) }
        )
    }

    /**
     * 주차별 데이터 변환 (변화율 계산 포함)
     */
    private fun toWeeklyTrendInfoList(weeklyDataList: List<WeeklyData>): List<WeeklyTrendInfo> {
        return weeklyDataList.mapIndexed { index, current ->
            WeeklyTrendInfo(
                weekNumber = current.weekNumber,
                averagePainScore = current.averagePainScore,
                averageStressScore = current.averageStressScore,
                averageJawFunctionScore = current.averageJawFunctionScore,
                changeRates = if (index > 0) {
                    val previous = weeklyDataList[index - 1]
                    calculateChangeRates(previous, current)
                } else null
            )
        }
    }

    /**
     * 변화율 계산
     * - 통증/스트레스: 낮을수록 좋음 (감소 = 호전 = 양수)
     * - 턱 기능: 높을수록 좋음 (증가 = 호전 = 양수)
     */
    private fun calculateChangeRates(previous: WeeklyData, current: WeeklyData): ChangeRateInfo {
        return ChangeRateInfo(
            pain = calculateChangeRate(
                previous = previous.averagePainScore,
                current = current.averagePainScore,
                isHigherBetter = false
            ),
            stress = calculateChangeRate(
                previous = previous.averageStressScore,
                current = current.averageStressScore,
                isHigherBetter = false
            ),
            jawFunction = calculateChangeRate(
                previous = previous.averageJawFunctionScore,
                current = current.averageJawFunctionScore,
                isHigherBetter = true
            )
        )
    }

    /**
     * 변화율 계산 헬퍼 메서드
     * @param isHigherBetter true: 높을수록 좋음, false: 낮을수록 좋음
     * @return 양수: 호전, 음수: 악화
     */
    private fun calculateChangeRate(
        previous: Double,
        current: Double,
        isHigherBetter: Boolean
    ): Double {
        if (previous == 0.0) return 0.0

        val changeRate = ((current - previous) / previous) * 100

        // 낮을수록 좋은 지표는 부호 반전
        return if (isHigherBetter) {
            String.format("%.1f", changeRate).toDouble()
        } else {
            String.format("%.1f", -changeRate).toDouble()
        }
    }

    /**
     * TopPainAreaData → TopPainAreaInfo 변환
     */
    private fun toTopPainAreaInfo(data: TopPainAreaData): TopPainAreaInfo {
        return TopPainAreaInfo(
            location = data.location.name,
            locationName = data.location.koreanName,
            count = data.count,
            averageIntensity = data.averageIntensity
        )
    }
}
```

---

## 5. Custom Validator

```kotlin
package com.beyondmedicine.common.validator

import com.beyondmedicine.domain.model.PainLocation

/**
 * 처방 코드 검증 유틸리티
 * - 영대문자 4자 + 숫자 4자 조합 검증
 * - 순서 무관
 */
object PrescriptionCodeValidator {

    private const val EXPECTED_LENGTH = 8
    private const val EXPECTED_UPPER_COUNT = 4
    private const val EXPECTED_DIGIT_COUNT = 4

    /**
     * 처방 코드 검증
     */
    fun validate(code: String): Boolean {
        if (code.length != EXPECTED_LENGTH) return false

        val upperCount = code.count { it in 'A'..'Z' }
        val digitCount = code.count { it.isDigit() }

        return upperCount == EXPECTED_UPPER_COUNT && digitCount == EXPECTED_DIGIT_COUNT
    }

    /**
     * 검증 실패 시 예외 발생
     */
    fun validateOrThrow(code: String) {
        require(validate(code)) {
            "처방 코드는 영대문자 ${EXPECTED_UPPER_COUNT}자 + " +
            "숫자 ${EXPECTED_DIGIT_COUNT}자 조합이어야 합니다. " +
            "입력값: $code"
        }
    }

    /**
     * 검증 메시지 반환
     */
    fun getValidationMessage(): String {
        return "처방 코드는 영대문자 ${EXPECTED_UPPER_COUNT}자 + 숫자 ${EXPECTED_DIGIT_COUNT}자 조합"
    }
}

/**
 * 통증 부위 검증 유틸리티
 */
object PainLocationValidator {

    /**
     * 통증 부위 유효성 검증
     */
    fun isValid(location: String): Boolean {
        return try {
            PainLocation.valueOf(location)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    /**
     * 검증 실패 시 예외 발생
     */
    fun validateOrThrow(location: String) {
        require(isValid(location)) {
            "유효하지 않은 통증 부위입니다. " +
            "입력값: $location, " +
            "허용값: ${PainLocation.values().joinToString { it.name }}"
        }
    }

    /**
     * 허용되는 통증 부위 목록 반환
     */
    fun getAllowedLocations(): List<String> {
        return PainLocation.values().map { it.name }
    }
}
```

---

## 설계 의도 정리

### 1. 계층 간 명확한 분리
```
┌─────────────────────────┐
│  API DTO (String 기반)  │  ← Presentation Layer
└───────────┬─────────────┘
            │ Mapper (변환 + 검증)
            ↓
┌─────────────────────────┐
│  Command/Result         │  ← Application Layer Interface
└───────────┬─────────────┘
            │ Service (비즈니스 로직)
            ↓
┌─────────────────────────┐
│  Domain Entity          │  ← Domain Layer
└─────────────────────────┘
```

### 2. 변화 대응력
- API 스펙 변경 → Mapper만 수정
- 도메인 Enum 변경 → Service 이하만 영향
- 검증 규칙 변경 → Validator만 수정

### 3. 재사용성
- Command/Result: REST, gRPC, Batch 모두 재사용
- Validator: API, Service, Domain 모두 재사용

### 4. 테스트 용이성
- 각 계층 독립적으로 테스트 가능
- Mapper, Validator 단독 테스트 가능

---

## 참고 문서
- [설계 의사결정 문서](./design-decisions.md)
- [FAQ](./faq.md)
