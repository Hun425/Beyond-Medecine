package com.beyondmedicine.presentation.exception

import com.beyondmedicine.application.exception.*
import com.beyondmedicine.presentation.dto.response.ErrorResponse
import com.beyondmedicine.presentation.dto.response.ValidationError
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import java.time.LocalDateTime

/**
 * 전역 예외 처리 핸들러
 * - 참고: docs/exception-handling-design.md
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    /**
     * 처방 없음 (404)
     */
    @ExceptionHandler(PrescriptionNotFoundException::class)
    fun handlePrescriptionNotFound(
        ex: PrescriptionNotFoundException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.NOT_FOUND.value(),
            error = "Not Found",
            message = ex.message ?: "처방을 찾을 수 없습니다",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }

    /**
     * 처방 상태 오류 (400)
     */
    @ExceptionHandler(InvalidPrescriptionStatusException::class)
    fun handleInvalidPrescriptionStatus(
        ex: InvalidPrescriptionStatusException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = ex.message ?: "유효하지 않은 처방 상태입니다",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    /**
     * 중복 검사 (409)
     */
    @ExceptionHandler(DuplicateAssessmentException::class)
    fun handleDuplicateAssessment(
        ex: DuplicateAssessmentException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.CONFLICT.value(),
            error = "Conflict",
            message = ex.message ?: "중복된 검사입니다",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse)
    }

    /**
     * 검사 일자 오류 (400)
     */
    @ExceptionHandler(InvalidAssessmentDateException::class)
    fun handleInvalidAssessmentDate(
        ex: InvalidAssessmentDateException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = ex.message ?: "유효하지 않은 검사 일자입니다",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    /**
     * Bean Validation 오류 (400)
     */
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationErrors(
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
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = "입력 값 검증 실패",
            path = request.getDescription(false).removePrefix("uri="),
            validationErrors = validationErrors
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    /**
     * IllegalArgumentException (400)
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        ex: IllegalArgumentException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = ex.message ?: "잘못된 요청입니다",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    /**
     * IllegalStateException (400)
     */
    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(
        ex: IllegalStateException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.BAD_REQUEST.value(),
            error = "Bad Request",
            message = ex.message ?: "잘못된 상태입니다",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.badRequest().body(errorResponse)
    }

    /**
     * 기타 예외 (500)
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            timestamp = LocalDateTime.now(),
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = "Internal Server Error",
            message = "서버 내부 오류가 발생했습니다",
            path = request.getDescription(false).removePrefix("uri=")
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
}
