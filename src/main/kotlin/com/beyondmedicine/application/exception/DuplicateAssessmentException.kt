package com.beyondmedicine.application.exception

/**
 * 중복 검사 시도 (409 Conflict)
 * - 하루 1회 제한 위반
 */
class DuplicateAssessmentException(
    message: String
) : DomainException(message)
