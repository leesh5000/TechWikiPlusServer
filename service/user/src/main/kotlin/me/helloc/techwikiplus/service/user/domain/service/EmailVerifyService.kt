package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.exception.DomainException
import me.helloc.techwikiplus.service.user.domain.exception.ErrorCode
import me.helloc.techwikiplus.service.user.domain.model.MailContent
import me.helloc.techwikiplus.service.user.domain.model.RegistrationMailTemplate
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.RegistrationCode
import me.helloc.techwikiplus.service.user.domain.port.CacheStore
import me.helloc.techwikiplus.service.user.domain.port.MailSender
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class EmailVerifyService(
    private val mailSender: MailSender,
    private val cacheStore: CacheStore,
) {
    companion object {
        private fun getRegistrationCodeKey(email: Email): String = "registration_code::$email"

        private val REGISTRATION_CODE_TTL_DURATION = Duration.ofMinutes(5)
    }

    fun startVerification(user: User) {
        val registrationCode: RegistrationCode = RegistrationCode.generate()
        val mail: MailContent = RegistrationMailTemplate.of(registrationCode)
        mailSender.send(user.email, mail)
        store(registrationCode, user.email)
    }

    @Throws(DomainException::class)
    fun verify(
        email: Email,
        registrationCode: RegistrationCode,
    ) {
        val registrationCodeKey = getRegistrationCodeKey(email)
        val code: String =
            cacheStore.get(registrationCodeKey)
                ?: throw DomainException(ErrorCode.REGISTRATION_NOT_FOUND, arrayOf(email.value))
        if (code != registrationCode.value) {
            throw DomainException(ErrorCode.CODE_MISMATCH)
        }
        cacheStore.delete(registrationCodeKey)
    }

    private fun store(
        registrationCode: RegistrationCode,
        email: Email,
    ) {
        val registrationCodeKey = getRegistrationCodeKey(email)
        cacheStore.put(registrationCodeKey, registrationCode.value, REGISTRATION_CODE_TTL_DURATION)
    }
}
