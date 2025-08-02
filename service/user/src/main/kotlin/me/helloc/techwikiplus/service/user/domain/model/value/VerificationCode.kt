package me.helloc.techwikiplus.service.user.domain.model.value

import kotlin.random.Random

@JvmInline
value class VerificationCode(val value: String) {
    init {
        require(value.isNotBlank()) { "인증 코드는 비어있을 수 없습니다" }
        require(value.length == VERIFICATION_CODE_LENGTH) { "인증 코드는 정확히 ${VERIFICATION_CODE_LENGTH}자리여야 합니다" }
        require(value.all { it.isDigit() }) { "인증 코드는 숫자로만 구성되어야 합니다" }
    }

    override fun toString(): String = "VerificationCode(******)"

    companion object {
        private const val VERIFICATION_CODE_LENGTH = 6

        fun generate(): VerificationCode {
            val code =
                (0 until VERIFICATION_CODE_LENGTH)
                    .map { Random.nextInt(0, 10) }
                    .joinToString("")
            return VerificationCode(code)
        }
    }
}
