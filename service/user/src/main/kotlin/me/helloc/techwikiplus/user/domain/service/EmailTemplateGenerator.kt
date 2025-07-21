package me.helloc.techwikiplus.user.domain.service

interface EmailTemplateGenerator {
    fun generateVerificationEmail(code: String): EmailTemplate
    fun generatePasswordResetEmail(code: String): EmailTemplate
}

data class EmailTemplate(
    val subject: String,
    val body: String
)
