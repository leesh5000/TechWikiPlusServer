package me.helloc.techwikiplus.service.user.infrastructure.mail

import jakarta.annotation.PostConstruct
import me.helloc.techwikiplus.service.user.domain.model.value.VerificationCode
import me.helloc.techwikiplus.service.user.domain.service.port.EmailTemplateService
import me.helloc.techwikiplus.service.user.domain.service.port.EmailTemplateService.EmailContent
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

@Component
class RegistrationEmailTemplateService : EmailTemplateService {
    private val logger = LoggerFactory.getLogger(javaClass)
    private lateinit var templateContent: String

    companion object {
        private const val TEMPLATE_PATH = "email-templates/registration-verification.html"
        private const val CODE_PLACEHOLDER = "{{code}}"
    }

    @PostConstruct
    fun loadTemplate() {
        templateContent = loadTemplateFromClasspath()
    }

    override fun createVerificationEmailContent(verificationCode: VerificationCode): EmailContent {
        val subject = "TechWiki+ 회원가입 인증 코드"
        val body = templateContent.replace(CODE_PLACEHOLDER, verificationCode.value)
        return EmailContent(subject, body, true)
    }

    private fun loadTemplateFromClasspath(): String {
        return try {
            val resource = ClassPathResource(TEMPLATE_PATH)
            BufferedReader(InputStreamReader(resource.inputStream, StandardCharsets.UTF_8)).use { reader ->
                reader.readText()
            }
        } catch (e: IOException) {
            logger.error("Failed to load email template from $TEMPLATE_PATH", e)
            // Fallback to inline template if file loading fails
            createFallbackTemplate()
        }
    }

    private fun createFallbackTemplate(): String {
        logger.warn("Using fallback email template")
        return """
            <!DOCTYPE html>
            <html lang="ko">
            <head>
                <meta charset="UTF-8">
                <title>TechWiki+ 이메일 인증</title>
            </head>
            <body style="font-family: Arial, sans-serif; background-color: #f3f4f6; padding: 20px;">
                <div style="max-width: 600px; margin: 0 auto; background-color: #ffffff; padding: 40px; border-radius: 8px;">
                    <h1 style="color: #2563eb; text-align: center;">TechWiki+</h1>
                    <h2 style="color: #111827; text-align: center;">이메일 인증 코드</h2>
                    <p style="text-align: center;">안녕하세요! TechWiki+ 회원가입을 위해 아래 인증 코드를 입력해주세요.</p>
                    <div style="background-color: #f3f4f6; padding: 20px; text-align: center; margin: 20px 0; border-radius: 4px;">
                        <div style="font-size: 36px; font-weight: bold; color: #2563eb; letter-spacing: 4px;">{{code}}</div>
                    </div>
                    <p style="color: #6b7280; text-align: center; font-size: 14px;">이 코드는 10분간 유효합니다.</p>
                </div>
            </body>
            </html>
            """.trimIndent()
    }
}
