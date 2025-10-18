package com.beyondmedicine.common.validator

import com.beyondmedicine.domain.model.PainLocation

/**
 * 통증 부위 검증 유틸리티
 * - 과제 PDF 7페이지 부록 참조
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
     * 통증 부위 검증 (예외 발생)
     * @throws IllegalArgumentException 유효하지 않은 통증 부위
     */
    fun validateOrThrow(location: String) {
        require(isValid(location)) {
            "유효하지 않은 통증 부위입니다. " +
            "입력값: $location, " +
            "허용값: ${PainLocation.values().joinToString { it.name }}"
        }
    }

    /**
     * 허용되는 모든 통증 부위 목록
     */
    fun getAllowedLocations(): List<String> {
        return PainLocation.values().map { it.name }
    }
}