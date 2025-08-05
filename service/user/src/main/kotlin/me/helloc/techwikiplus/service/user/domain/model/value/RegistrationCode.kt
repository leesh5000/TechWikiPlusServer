package me.helloc.techwikiplus.service.user.domain.model.value

import kotlin.random.Random

/**
 * 이메일 인증 코드를 표현하는 값 객체(Value Object)
 * 
 * value class를 사용한 이유:
 * 1. 타입 안전성: String이 아닌 RegistrationCode 타입으로 명확히 구분하여 
 *    실수로 다른 String과 혼용되는 것을 컴파일 타임에 방지
 *    예) fun verify(code: String) 대신 fun verify(code: RegistrationCode)
 * 
 * 2. 성능 최적화: value class는 런타임에 원시 타입(String)으로 최적화되어
 *    일반 클래스와 달리 객체 생성 오버헤드가 없음
 * 
 * 3. 도메인 규칙 캡슐화: 인증 코드는 반드시 6자리 숫자여야 한다는 
 *    비즈니스 규칙을 객체 생성 시점에 검증하여 무효한 상태 방지
 * 
 * 4. 불변성 보장: value class는 val 프로퍼티만 가질 수 있어 
 *    생성 후 값이 변경될 수 없는 불변 객체를 보장
 * 
 * @property value 6자리 숫자로 구성된 인증 코드
 */
@JvmInline
value class RegistrationCode(val value: String) {
    init {
        require(value.isNotBlank()) { "인증 코드는 비어있을 수 없습니다" }
        require(value.length == VERIFICATION_CODE_LENGTH) { "인증 코드는 정확히 ${VERIFICATION_CODE_LENGTH}자리여야 합니다" }
        require(value.all { it.isDigit() }) { "인증 코드는 숫자로만 구성되어야 합니다" }
    }

    override fun toString(): String = "VerificationCode(******)"

    companion object {
        private const val VERIFICATION_CODE_LENGTH = 6

        fun generate(): RegistrationCode {
            val code =
                (0 until VERIFICATION_CODE_LENGTH)
                    .map { Random.nextInt(0, 10) }
                    .joinToString("")
            return RegistrationCode(code)
        }
    }
}
