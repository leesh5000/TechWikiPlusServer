package me.helloc.techwikiplus.user.infrastructure.passwordencoder

import me.helloc.techwikiplus.user.domain.DomainConstants
import me.helloc.techwikiplus.user.domain.exception.validation.InvalidPasswordException
import me.helloc.techwikiplus.user.domain.port.outbound.PasswordValidator
import org.springframework.stereotype.Component

@Component
class RegexPasswordValidator : PasswordValidator {
    override fun validate(password: String) {
        // ReDoS 방어를 위한 사전 길이 검증
        if (password.length < DomainConstants.Password.MIN_LENGTH ||
            password.length > DomainConstants.Password.MAX_LENGTH
        ) {
            throw InvalidPasswordException(password)
        }

        // 정규표현식을 통한 상세 검증
        if (!DomainConstants.Password.PATTERN.matches(password)) {
            throw InvalidPasswordException(password)
        }
    }
}
