package me.helloc.techwikiplus.user.domain

/**
 * JWT 토큰의 타입을 나타내는 열거형
 */
enum class TokenType(val value: String) {
    ACCESS("access"),
    REFRESH("refresh"),
    ;

    companion object {
        fun from(value: String): TokenType {
            return values().find { it.value == value }
                ?: throw IllegalArgumentException("Unknown token type: $value")
        }
    }
}
