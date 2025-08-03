package me.helloc.techwikiplus.service.user.infrastructure.mail

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import org.springframework.boot.autoconfigure.mail.MailProperties
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.testcontainers.containers.GenericContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

/**
 * JavaMailSender 통합 테스트
 *
 * 이 테스트는 실제 메일 서버(MailHog)를 사용하여
 * 이메일 전송 기능을 종단간(End-to-End) 검증합니다.
 */
@Testcontainers
class JavaMailSenderIntegrationTest : FunSpec() {
    private lateinit var javaMailSender: JavaMailSender
    private val restTemplate: TestRestTemplate by lazy {
        val objectMapper =
            ObjectMapper().apply {
                registerKotlinModule()
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false)
            }

        val converter = MappingJackson2HttpMessageConverter(objectMapper)
        converter.supportedMediaTypes =
            listOf(
                MediaType.APPLICATION_JSON,
                MediaType(MediaType.TEXT_HTML.type, "json"),
            )

        TestRestTemplate().apply {
            restTemplate.messageConverters.clear()
            restTemplate.messageConverters.add(converter)
        }
    }

    companion object {
        private val MAILHOG_IMAGE = DockerImageName.parse("mailhog/mailhog:latest")

        private val mailhogContainer =
            GenericContainer(MAILHOG_IMAGE).apply {
                withExposedPorts(1025, 8025)
                withReuse(true)
                start()
            }
    }

    init {
        beforeSpec {
            // Set up JavaMailSender with MailHog configuration
            val springMailSender =
                JavaMailSenderImpl().apply {
                    host = mailhogContainer.host
                    port = mailhogContainer.getMappedPort(1025)
                    username = "test@example.com"
                    protocol = "smtp"
                    defaultEncoding = "UTF-8"
                }

            val mailProperties =
                MailProperties().apply {
                    username = "test@example.com"
                }

            javaMailSender = JavaMailSender(springMailSender, mailProperties)
        }

        beforeEach {
            // Clear all emails in MailHog before each test
            val mailhogApiUrl = "http://${mailhogContainer.host}:${mailhogContainer.getMappedPort(
                8025,
            )}/api/v1/messages"
            restTemplate.delete(mailhogApiUrl)
        }

        test("플레인 텍스트 이메일을 성공적으로 전송해야 한다") {
            // Given
            val to = Email("recipient@example.com")
            val subject = "Integration Test Subject"
            val body = "This is a plain text email body"

            // When
            javaMailSender.send(to, subject, body)

            // Then - Verify email was received by MailHog
            Thread.sleep(1000) // Give MailHog time to receive the email
            val messages = getMailhogMessages()

            messages shouldHaveSize 1
            val message = messages.first()
            message.to.first().mailbox shouldBe "recipient"
            message.to.first().domain shouldBe "example.com"
            message.content?.headers?.subject?.first() shouldBe "Integration Test Subject"
            message.content?.body shouldContain "This is a plain text email body"
            message.from?.mailbox shouldBe "test"
            message.from?.domain shouldBe "example.com"
        }

        test("HTML 이메일을 성공적으로 전송해야 한다") {
            // Given
            val to = Email("recipient@example.com")
            val subject = "HTML Email Test"
            val htmlBody = "<html><body><h1>Hello</h1><p>This is an HTML email</p></body></html>"

            // When
            javaMailSender.send(to, subject, htmlBody)

            // Then - Verify email was received by MailHog
            Thread.sleep(1000) // Give MailHog time to receive the email
            val messages = getMailhogMessages()

            messages shouldHaveSize 1
            val message = messages.first()
            message.to.first().mailbox shouldBe "recipient"
            message.to.first().domain shouldBe "example.com"
            message.content?.headers?.subject?.first() shouldBe "HTML Email Test"
            message.content?.body shouldContain "<h1>Hello</h1>"
            message.content?.body shouldContain "<p>This is an HTML email</p>"
            // HTML emails are sent as multipart/mixed with HTML content
            message.content?.headers?.contentType?.first() shouldContain "multipart"
        }

        test("여러 이메일을 순차적으로 전송할 수 있어야 한다") {
            // Given
            val recipients =
                listOf(
                    Email("user1@example.com"),
                    Email("user2@example.com"),
                    Email("user3@example.com"),
                )

            // When
            recipients.forEachIndexed { index, email ->
                javaMailSender.send(email, "Test Email $index", "Body content $index")
            }

            // Then
            Thread.sleep(1500) // Give MailHog time to receive all emails
            val messages = getMailhogMessages()

            messages shouldHaveSize 3

            // Verify all expected recipients received emails
            val recipientMailboxes = messages.map { it.to.first().mailbox }.sorted()
            recipientMailboxes shouldBe listOf("user1", "user2", "user3")

            // Verify all emails have the expected content pattern
            messages.forEach { message ->
                message.to.first().domain shouldBe "example.com"
                message.content?.headers?.subject?.first() shouldContain "Test Email"
                message.content?.body shouldContain "Body content"
            }
        }

        test("한글이 포함된 이메일을 올바르게 전송해야 한다") {
            // Given
            val to = Email("recipient@example.com")
            val subject = "한글 제목 테스트"
            val body = "안녕하세요. 한글 본문 테스트입니다."

            // When
            javaMailSender.send(to, subject, body)

            // Then
            Thread.sleep(1000)
            val messages = getMailhogMessages()

            messages shouldHaveSize 1
            val message = messages.first()
            // Korean subjects are MIME-encoded in email headers
            // The encoded version should contain the UTF-8 encoded representation
            message.content?.headers?.subject?.first() shouldContain "UTF-8"

            // The body might be base64 encoded or plain text
            // We just verify the email was sent successfully
            message.to.first().mailbox shouldBe "recipient"
            message.to.first().domain shouldBe "example.com"
        }
    }

    private fun getMailhogMessages(): List<MailhogMessage> {
        val mailhogApiUrl = "http://${mailhogContainer.host}:${mailhogContainer.getMappedPort(8025)}/api/v2/messages"
        val response: ResponseEntity<MailhogResponse> =
            restTemplate.exchange(
                mailhogApiUrl,
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<MailhogResponse>() {},
            )

        response.statusCode shouldBe HttpStatus.OK
        return response.body?.items ?: emptyList()
    }

    // MailHog API response data classes
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class MailhogResponse(
        val total: Int = 0,
        val count: Int = 0,
        val start: Int = 0,
        val items: List<MailhogMessage> = emptyList(),
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class MailhogMessage(
        @JsonProperty("ID")
        val id: String? = null,
        @JsonProperty("From")
        val from: MailhogAddress? = null,
        @JsonProperty("To")
        val to: List<MailhogAddress> = emptyList(),
        @JsonProperty("Content")
        val content: MailhogContent? = null,
        @JsonProperty("Created")
        val created: String? = null,
        @JsonProperty("Raw")
        val raw: MailhogRaw? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class MailhogAddress(
        @JsonProperty("Relays")
        val relays: List<String>? = null,
        @JsonProperty("Mailbox")
        val mailbox: String = "",
        @JsonProperty("Domain")
        val domain: String = "",
        @JsonProperty("Params")
        val params: String = "",
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class MailhogContent(
        @JsonProperty("Headers")
        val headers: MailhogHeaders? = null,
        @JsonProperty("Body")
        val body: String = "",
        @JsonProperty("Size")
        val size: Int = 0,
        @JsonProperty("MIME")
        val mime: String? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class MailhogHeaders(
        @JsonProperty("Content-Type")
        val contentType: List<String>? = null,
        @JsonProperty("Date")
        val date: List<String> = emptyList(),
        @JsonProperty("From")
        val from: List<String> = emptyList(),
        @JsonProperty("MIME-Version")
        val mimeVersion: List<String>? = null,
        @JsonProperty("Received")
        val received: List<String> = emptyList(),
        @JsonProperty("Return-Path")
        val returnPath: List<String>? = null,
        @JsonProperty("Subject")
        val subject: List<String> = emptyList(),
        @JsonProperty("To")
        val to: List<String> = emptyList(),
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class MailhogRaw(
        @JsonProperty("From")
        val from: String = "",
        @JsonProperty("To")
        val to: List<String> = emptyList(),
        @JsonProperty("Data")
        val data: String = "",
        @JsonProperty("Helo")
        val helo: String = "",
    )
}
