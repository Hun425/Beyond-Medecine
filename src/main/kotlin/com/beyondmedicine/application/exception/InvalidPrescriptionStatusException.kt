package com.beyondmedicine.application.exception

/**
 * 유효하지 않은 처방 상태 (400)
 * - 예: PENDING, EXPIRED 상태에서 검사 시도
 */
class InvalidPrescriptionStatusException(
    message: String
) : DomainException(message)
