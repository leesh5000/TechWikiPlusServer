package me.helloc.techwikiplus.user.infrastructure.mail

import me.helloc.techwikiplus.user.domain.service.MailSender
import me.helloc.techwikiplus.user.infrastructure.mail.console.ConsoleMailSender
import me.helloc.techwikiplus.user.infrastructure.mail.java.SmtpMailSender
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.mail.type=smtp"])
class JavaMailSenderConfigTest {
    @Autowired
    private lateinit var mailSender: MailSender

    @Test
    @DisplayName("spring.mail.type이 smtp일 때 SmtpMailSender가 주입된다")
    fun shouldInjectSmtpMailSenderWhenTypeIsSmtp() {
        assertThat(mailSender).isInstanceOf(SmtpMailSender::class.java)
    }
}

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.mail.type=console"])
class ConsoleMailSenderConfigTest {
    @Autowired
    private lateinit var mailSender: MailSender

    @Test
    @DisplayName("spring.mail.type이 console일 때 ConsoleMailSender가 주입된다")
    fun shouldInjectConsoleMailSenderWhenTypeIsConsole() {
        assertThat(mailSender).isInstanceOf(ConsoleMailSender::class.java)
    }
}

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = ["spring.mail.type=smtp"])
class DefaultMailSenderConfigTest {
    @Autowired
    private lateinit var mailSender: MailSender

    @Test
    @DisplayName("spring.mail.type이 설정되지 않으면 기본적으로 SmtpMailSender가 주입된다")
    fun shouldInjectSmtpMailSenderByDefault() {
        assertThat(mailSender).isInstanceOf(SmtpMailSender::class.java)
    }
}

@SpringBootTest
@ActiveProfiles("test")
class TestProfileMailSenderConfigTest {
    @Autowired
    private lateinit var mailSender: MailSender

    @Test
    @DisplayName("test 프로필에서는 기본적으로 ConsoleMailSender가 주입된다")
    fun shouldInjectConsoleMailSenderInTestProfile() {
        assertThat(mailSender).isInstanceOf(ConsoleMailSender::class.java)
    }
}
