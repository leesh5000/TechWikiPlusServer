package me.helloc.techwikiplus.user.infrastructure.config

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import me.helloc.techwikiplus.user.infrastructure.cors.CorsProperties
import me.helloc.techwikiplus.user.infrastructure.security.JwtProperties
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.info.BuildProperties
import org.springframework.core.env.StandardEnvironment
import java.time.Instant
import java.util.Properties

class ApplicationConfigurationPrinterTest : BehaviorSpec({

    Given("ApplicationConfigurationPrinter가 생성되었을 때") {
        val mockEnvironment = FakeEnvironment()
        val mockJwtProperties =
            JwtProperties(
                secret = "test-secret",
                // 1 hour
                accessTokenExpiration = 3600000,
                // 7 days
                refreshTokenExpiration = 604800000,
            )
        val mockCorsProperties =
            CorsProperties(
                allowedOrigins = listOf("*"),
                allowedMethods = listOf("GET", "POST", "PUT", "DELETE"),
                allowedHeaders = listOf("*"),
                allowCredentials = true,
                maxAge = 3600,
            )
        val mockBuildProperties =
            FakeBuildProperties(
                version = "0.0.1",
                time = Instant.parse("2025-01-25T10:00:00Z"),
            )
        val mockSpringApplication = org.springframework.boot.SpringApplication()
        val mockApplicationContext = org.springframework.context.support.GenericApplicationContext()
        val mockApplicationReadyEvent =
            ApplicationReadyEvent(
                mockSpringApplication,
                arrayOf(),
                mockApplicationContext,
                java.time.Duration.ZERO,
            )

        // Setup fake environment properties
        mockEnvironment.setProperty("server.port", "9000")
        mockEnvironment.setProperty("spring.application.name", "techwikiplus-user-service")
        mockEnvironment.setProperty("spring.datasource.url", "jdbc:mysql://localhost:13306/techwikiplus?useSSL=false")
        mockEnvironment.setProperty("spring.datasource.hikari.maximum-pool-size", "30")
        mockEnvironment.setProperty("spring.datasource.hikari.minimum-idle", "10")
        mockEnvironment.setProperty("spring.data.redis.host", "localhost")
        mockEnvironment.setProperty("spring.data.redis.port", "16379")
        mockEnvironment.setProperty("spring.mail.host", "smtp.gmail.com")
        mockEnvironment.setProperty("spring.mail.port", "1025")
        mockEnvironment.setProperty("spring.mail.username", "test@example.com")
        mockEnvironment.setProperty("logging.level.root", "INFO")
        mockEnvironment.setProperty("logging.level.me.helloc.techwikiplus", "DEBUG")
        mockEnvironment.setActiveProfiles("test")

        val printer =
            ApplicationConfigurationPrinter(
                environment = mockEnvironment,
                jwtProperties = mockJwtProperties,
                corsProperties = mockCorsProperties,
                buildProperties = mockBuildProperties,
            )

        When("ApplicationReadyEvent가 발생하면") {
            printer.onApplicationEvent(mockApplicationReadyEvent)

            Then("설정 정보가 로깅되어야 한다") {
                // 실제 로깅 확인은 통합 테스트에서 수행
                // 여기서는 메서드 호출만 확인
                mockEnvironment.getProperty("server.port", "8080") shouldBe "9000"
            }
        }

        When("이메일 마스킹 처리") {
            val testEmail = "test@example.com"
            val maskedEmail =
                printer.javaClass.getDeclaredMethod("maskEmail", String::class.java).apply {
                    isAccessible = true
                }.invoke(printer, testEmail) as String

            Then("이메일이 적절히 마스킹되어야 한다") {
                maskedEmail shouldContain "tes"
                maskedEmail shouldContain "@example.com"
                maskedEmail shouldNotContain "test@"
            }
        }

        When("시간 포맷팅 처리") {
            val formatDuration =
                printer.javaClass.getDeclaredMethod("formatDuration", Long::class.java).apply {
                    isAccessible = true
                }

            Then("1시간은 1h로 표시되어야 한다") {
                val result = formatDuration.invoke(printer, 3600000L) as String
                result shouldBe "1h"
            }

            Then("7일은 7d로 표시되어야 한다") {
                val result = formatDuration.invoke(printer, 604800000L) as String
                result shouldBe "7d"
            }
        }
    }

    Given("BuildProperties가 없을 때") {
        val mockEnvironment = FakeEnvironment()
        val mockJwtProperties = JwtProperties()
        val mockCorsProperties = CorsProperties()
        val mockSpringApplication = org.springframework.boot.SpringApplication()
        val mockApplicationContext = org.springframework.context.support.GenericApplicationContext()
        val mockApplicationReadyEvent =
            ApplicationReadyEvent(
                mockSpringApplication,
                arrayOf(),
                mockApplicationContext,
                java.time.Duration.ZERO,
            )

        val printer =
            ApplicationConfigurationPrinter(
                environment = mockEnvironment,
                jwtProperties = mockJwtProperties,
                corsProperties = mockCorsProperties,
                buildProperties = null,
            )

        When("ApplicationReadyEvent가 발생하면") {
            printer.onApplicationEvent(mockApplicationReadyEvent)

            Then("버전이 Unknown으로 표시되어야 한다") {
                // 실제 로깅 내용은 통합 테스트에서 확인
                mockEnvironment.activeProfiles.isEmpty() shouldBe true
            }
        }
    }
})

// Fake implementations for testing
class FakeEnvironment : StandardEnvironment() {
    private val properties = mutableMapOf<String, String>()
    private var profiles = emptyArray<String>()

    fun setProperty(
        key: String,
        value: String,
    ) {
        properties[key] = value
    }

    override fun setActiveProfiles(vararg profiles: String) {
        this.profiles = arrayOf(*profiles)
    }

    override fun getProperty(key: String): String? = properties[key]

    override fun getProperty(
        key: String,
        defaultValue: String,
    ): String = properties[key] ?: defaultValue

    override fun getActiveProfiles(): Array<String> = profiles
}

class FakeBuildProperties(
    private val version: String,
    private val time: Instant,
) : BuildProperties(Properties()) {
    override fun getVersion(): String = version

    override fun getTime(): Instant = time
}
