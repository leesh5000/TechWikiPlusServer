package me.helloc.techwikiplus.user.domain

import java.time.Duration

/**
 * 도메인 레이어에서 사용하는 상수를 중앙화하여 관리하는 클래스
 */
object DomainConstants {
    /**
     * 닉네임 관련 상수
     */
    object Nickname {
        const val MIN_LENGTH = 2
        const val MAX_LENGTH = 20
        val PATTERN = "^[a-zA-Z0-9가-힣]{$MIN_LENGTH,$MAX_LENGTH}$".toRegex()
    }

    /**
     * 비밀번호 관련 상수
     */
    object Password {
        const val MIN_LENGTH = 8
        const val MAX_LENGTH = 30

        /**
         * 비밀번호 검증 정규표현식
         * - 최소 8자, 최대 30자
         * - 대문자 최소 1개 포함 (?=.*[A-Z])
         * - 소문자 최소 1개 포함 (?=.*[a-z])
         * - 숫자 최소 1개 포함 (?=.*\d)
         * - 특수문자 최소 1개 포함 (?=.*[!@#$%^&*()_+\-=\[\]{};':"\\|,.<>/?])
         */
        val PATTERN =
            Regex(
                "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)" +
                    "(?=.*[!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?])" +
                    ".{$MIN_LENGTH,$MAX_LENGTH}$",
            )
    }

    /**
     * 이메일 인증 관련 상수
     */
    object EmailVerification {
        val CODE_TTL: Duration = Duration.ofMinutes(5)
    }

    /**
     * 토큰 관련 상수
     */
    object Token {
        val ACCESS_TYPE = TokenType.ACCESS.value
        val REFRESH_TYPE = TokenType.REFRESH.value
    }
}
