package me.helloc.techwikiplus.service.user.infrastructure.mail

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import me.helloc.techwikiplus.service.user.domain.model.value.VerificationCode

class RegistrationEmailTemplateServiceTest : FunSpec({
    val emailTemplateService =
        RegistrationEmailTemplateService().apply {
            loadTemplate()
        }

    test("인증 이메일 제목이 올바르게 생성되어야 한다") {
        // Given
        val verificationCode = VerificationCode("123456")

        // When
        val emailContent = emailTemplateService.createVerificationEmailContent(verificationCode)

        // Then
        emailContent.subject shouldBe "TechWiki+ 회원가입 인증 코드"
    }

    test("인증 이메일이 HTML 형식으로 생성되어야 한다") {
        // Given
        val verificationCode = VerificationCode("123456")

        // When
        val emailContent = emailTemplateService.createVerificationEmailContent(verificationCode)

        // Then
        emailContent.isHtml shouldBe true
    }

    test("인증 이메일 본문에 필수 요소들이 포함되어야 한다") {
        // Given
        val verificationCode = VerificationCode("123456")

        // When
        val emailContent = emailTemplateService.createVerificationEmailContent(verificationCode)

        // Then
        emailContent.body shouldContain "<!DOCTYPE html>"
        emailContent.body shouldContain "<html lang=\"ko\">"
        emailContent.body shouldContain "TechWiki+"
        emailContent.body shouldContain "이메일 인증 코드"
        emailContent.body shouldContain "123456"
        emailContent.body shouldContain "10분간 유효"
        emailContent.body shouldContain "support@techwikiplus.com"
    }

    test("인증 코드가 눈에 띄게 표시되어야 한다") {
        // Given
        val verificationCode = VerificationCode("789012")

        // When
        val emailContent = emailTemplateService.createVerificationEmailContent(verificationCode)

        // Then
        emailContent.body shouldContain "font-size: 36px"
        emailContent.body shouldContain "font-weight: 700"
        emailContent.body shouldContain "color: #2563eb"
        emailContent.body shouldContain "789012"
    }

    test("반응형 디자인 요소들이 포함되어야 한다") {
        // Given
        val verificationCode = VerificationCode("123401")

        // When
        val emailContent = emailTemplateService.createVerificationEmailContent(verificationCode)

        // Then
        emailContent.body shouldContain "viewport"
        emailContent.body shouldContain "max-width: 100%"
        emailContent.body shouldContain "width: 600px"
    }

    test("보안 관련 경고 메시지가 포함되어야 한다") {
        // Given
        val verificationCode = VerificationCode("345678")

        // When
        val emailContent = emailTemplateService.createVerificationEmailContent(verificationCode)

        // Then
        emailContent.body shouldContain "타인과 공유하지 마세요"
        emailContent.body shouldContain "요청하지 않은 경우"
    }

    test("이메일 템플릿에 스크립트 태그가 포함되지 않아야 한다") {
        // Given
        val verificationCode = VerificationCode("456789")

        // When
        val emailContent = emailTemplateService.createVerificationEmailContent(verificationCode)

        // Then
        emailContent.body shouldNotContain "<script"
        emailContent.body shouldNotContain "</script>"
        emailContent.body shouldNotContain "javascript:"
    }
})
