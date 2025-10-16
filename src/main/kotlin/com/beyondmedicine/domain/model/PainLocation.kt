package com.beyondmedicine.domain.model

/**
 * 통증 부위 Enum
 * - 과제 PDF 7페이지 부록 참조
 */
enum class PainLocation(val koreanName: String) {
    LEFT_JAW("좌측 턱 관절"),
    RIGHT_JAW("우측 턱 관절"),
    LEFT_TEMPLE("좌측 관자놀이"),
    RIGHT_TEMPLE("우측 관자놀이"),
    NECK("목"),
    CHIN("턱 끝")
}
