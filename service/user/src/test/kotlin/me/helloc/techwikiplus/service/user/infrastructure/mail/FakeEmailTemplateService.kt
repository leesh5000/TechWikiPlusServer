package me.helloc.techwikiplus.service.user.infrastructure.mail

import me.helloc.techwikiplus.service.user.domain.model.value.VerificationCode
import me.helloc.techwikiplus.service.user.domain.service.port.EmailTemplateService
import me.helloc.techwikiplus.service.user.domain.service.port.EmailTemplateService.EmailContent

class FakeEmailTemplateService : EmailTemplateService {
    var createVerificationEmailContentCalled = false
    var lastVerificationCode: VerificationCode? = null

    override fun createVerificationEmailContent(verificationCode: VerificationCode): EmailContent {
        createVerificationEmailContentCalled = true
        lastVerificationCode = verificationCode
        return EmailContent(
            subject = "TechWiki+ 이메일 인증 코드",
            body = "인증 코드는 $verificationCode 입니다.",
            isHtml = false,
        )
    }
}
