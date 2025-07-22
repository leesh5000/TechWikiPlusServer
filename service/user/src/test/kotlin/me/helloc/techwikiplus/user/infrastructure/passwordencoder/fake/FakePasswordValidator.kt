package me.helloc.techwikiplus.user.infrastructure.passwordencoder.fake

import me.helloc.techwikiplus.user.domain.service.PasswordValidator

class FakePasswordValidator : PasswordValidator {
    override fun validate(password: String) {
        // 테스트용 간단한 검증 - 6자 이상
        if (password.length < 6) {
            throw IllegalArgumentException("비밀번호는 최소 6자 이상이어야 합니다.")
        }
    }
}
