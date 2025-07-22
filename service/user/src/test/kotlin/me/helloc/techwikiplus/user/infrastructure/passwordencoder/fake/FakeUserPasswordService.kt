package me.helloc.techwikiplus.user.infrastructure.passwordencoder.fake

import me.helloc.techwikiplus.user.domain.service.UserPasswordService

class FakeUserPasswordService : UserPasswordService {
    override fun validate(password: String) {
        // 테스트용 간단한 검증 - 6자 이상
        if (password.length < 6) {
            throw IllegalArgumentException("비밀번호는 최소 6자 이상이어야 합니다.")
        }
    }

    override fun encode(password: String): String {
        // Simple encoding for testing - just add a prefix
        return "encoded_$password"
    }

    override fun matches(rawPassword: String, encodedPassword: String): Boolean {
        return encodedPassword == "encoded_$rawPassword"
    }
}