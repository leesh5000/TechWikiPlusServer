package me.helloc.techwikiplus.service.user.adapter.outbound.mail

import me.helloc.techwikiplus.service.user.application.port.outbound.EmailTemplatePrinter
import me.helloc.techwikiplus.service.user.application.port.outbound.EmailTemplatePrinter.EmailContent
import me.helloc.techwikiplus.service.user.domain.model.value.RegistrationCode

class FakeEmailTemplatePrinter : EmailTemplatePrinter {
    var createVerificationEmailContentCalled = false
    var lastRegistrationCode: RegistrationCode? = null

    override fun createVerificationEmailContent(registrationCode: RegistrationCode): EmailContent {
        createVerificationEmailContentCalled = true
        lastRegistrationCode = registrationCode
        return EmailContent(
            subject = "TechWiki+ 이메일 인증 코드",
            body = "인증 코드는 $registrationCode 입니다.",
            isHtml = false,
        )
    }
}
