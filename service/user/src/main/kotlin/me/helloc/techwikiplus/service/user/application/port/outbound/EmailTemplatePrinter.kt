package me.helloc.techwikiplus.service.user.application.port.outbound

import me.helloc.techwikiplus.service.user.domain.model.value.RegistrationCode

interface EmailTemplatePrinter {
    fun createVerificationEmailContent(registrationCode: RegistrationCode): EmailContent

    data class EmailContent(
        val subject: String,
        val body: String,
        val isHtml: Boolean = true,
    )
}
