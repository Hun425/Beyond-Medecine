package com.beyondmedicine.application.exception

/**
 * 유효하지 않은 검사 일자 (400)
 * - 1~6주차 범위 밖
 */
class InvalidAssessmentDateException(
    message: String
) : DomainException(message)
