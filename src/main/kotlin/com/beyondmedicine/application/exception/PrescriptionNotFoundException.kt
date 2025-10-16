package com.beyondmedicine.application.exception

/**
 * 처방을 찾을 수 없음 (404)
 */
class PrescriptionNotFoundException(
    message: String
) : DomainException(message)
