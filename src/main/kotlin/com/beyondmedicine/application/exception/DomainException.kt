package com.beyondmedicine.application.exception

/**
 * 도메인 예외 기본 클래스
 * - 참고: docs/exception-handling-design.md
 */
sealed class DomainException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
