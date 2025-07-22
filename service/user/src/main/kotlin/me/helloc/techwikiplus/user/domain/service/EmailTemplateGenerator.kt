package me.helloc.techwikiplus.user.domain.service

interface EmailTemplateGenerator {
    fun generateVerificationEmail(code: String): EmailTemplateDetails

    fun generatePasswordResetEmail(code: String): EmailTemplateDetails

    data class EmailTemplateDetails(
        val subject: String,
        val body: String,
    )
}
