package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.VerificationCode
import me.helloc.techwikiplus.user.domain.service.UserReader
import me.helloc.techwikiplus.user.domain.service.UserWriter
import me.helloc.techwikiplus.user.domain.service.VerificationCodeStore
import org.springframework.stereotype.Component

@Component
class VerifyEmailUseCase(
    private val verificationCodeStore: VerificationCodeStore,
    private val userReader: UserReader,
    private val userWriter: UserWriter,
) {

    fun verifyEmail(email: String, code: String) {
        val verificationCode: VerificationCode = verificationCodeStore.retrieveOrThrows(email)
        verificationCode.equalsOrThrows(code)
        val user: User = userReader.readByEmailOrThrows(email)
        val verifiedUser = user.completeSignUp()
        userWriter.insertOrUpdate(verifiedUser)
    }
}
