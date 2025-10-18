package com.beyondmedicine.common.validator

/**
 * 처방 코드 검증 유틸리티
 * - 영대문자 4자 + 숫자 4자 (순서 무관)
 * - 과제 PDF 6페이지 요구사항
 */
object PrescriptionCodeValidator {

    private const val EXPECTED_LENGTH = 8
    private const val EXPECTED_UPPER_COUNT = 4
    private const val EXPECTED_DIGIT_COUNT = 4

    /**
     * 처방 코드 유효성 검증
     * @return true: 유효함, false: 유효하지 않음
     */
    fun validate(code: String): Boolean {
        if (code.length != EXPECTED_LENGTH) return false

        val upperCount = code.count { it in 'A'..'Z' }
        val digitCount = code.count { it.isDigit() }

        return upperCount == EXPECTED_UPPER_COUNT && digitCount == EXPECTED_DIGIT_COUNT
    }

    /**
     * 처방 코드 검증 (예외 발생)
     * @throws IllegalArgumentException 유효하지 않은 코드
     */
    fun validateOrThrow(code: String) {
        require(validate(code)) {
            "처방 코드는 영대문자 ${EXPECTED_UPPER_COUNT}자 + " +
            "숫자 ${EXPECTED_DIGIT_COUNT}자 조합이어야 합니다. " +
            "입력값: $code"
        }
    }
}