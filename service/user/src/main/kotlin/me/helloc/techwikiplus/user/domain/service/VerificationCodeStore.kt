package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.VerificationCode
import java.time.Duration

interface VerificationCodeStore {
    fun storeUserWithExpiry(email: String, code: VerificationCode, ttl: Duration, user: User)
    fun retrieveUserOrThrows(email: String, verificationCode: VerificationCode): User
}
