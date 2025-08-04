package me.helloc.techwikiplus.service.user.domain.service.port

import me.helloc.techwikiplus.service.user.domain.model.value.VerificationCode

interface EmailTemplateService {
    fun createVerificationEmailContent(verificationCode: VerificationCode): EmailContent

    data class EmailContent(
        val subject: String,
        val body: String,
        val isHtml: Boolean = true,
    )
}
