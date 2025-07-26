package me.helloc.techwikiplus.user.integration.http

import me.helloc.techwikiplus.user.domain.UserStatus
import me.helloc.techwikiplus.user.domain.port.outbound.MailSender
import me.helloc.techwikiplus.user.domain.port.outbound.UserRepository
import me.helloc.techwikiplus.user.domain.port.outbound.VerificationCodeStore
import me.helloc.techwikiplus.user.interfaces.http.dto.request.UserSignUpRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.bean.override.mockito.MockitoBean

class UserSignUpControllerIntegrationTest : ControllerIntegrationTestSupport() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @MockitoBean
    private lateinit var mailSender: MailSender

    @Autowired
    private lateinit var verificationCodeStore: VerificationCodeStore

    @BeforeEach
    fun setUp() {
        // 이메일 발송은 Mock 처리
        `when`(mailSender.sendVerificationEmail(anyString())).thenReturn(
            me.helloc.techwikiplus.user.domain.VerificationCode("123456"),
        )
    }

    @Test
    fun `회원가입 성공 시 202 응답을 반환한다`() {
        // given
        val request =
            UserSignUpRequest(
                email = "test@example.com",
                nickname = "testuser",
                password = "ValidPass123!",
            )

        // when
        val response: ResponseEntity<Void> =
            restTemplate.postForEntity(
                "/api/v1/users/signup",
                createJsonHttpEntity(request),
                Void::class.java,
            )

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.ACCEPTED)

        // 데이터베이스에 사용자가 저장되었는지 확인
        val savedUser = userRepository.findByEmail(request.email)
        assertThat(savedUser).isNotNull
        assertThat(savedUser!!.getEmailValue()).isEqualTo(request.email)
        assertThat(savedUser.nickname).isEqualTo(request.nickname)
        assertThat(savedUser.status).isEqualTo(UserStatus.PENDING)

        // 이메일이 발송되었는지 확인
        verify(mailSender).sendVerificationEmail(request.email)

        // 인증 코드가 저장되었는지 확인
        val verificationCode = verificationCodeStore.retrieveOrThrows(request.email)
        assertThat(verificationCode.value).isEqualTo("123456")
    }

    @Test
    fun `중복된 이메일로 회원가입 시 409 응답을 반환한다`() {
        // given
        val email = "duplicate@example.com"
        val existingUser = createTestUser(email = email)
        userRepository.insertOrUpdate(existingUser)

        val request =
            UserSignUpRequest(
                email = email,
                nickname = "newuser",
                password = "ValidPass123!",
            )

        // when
        val response: ResponseEntity<String> =
            restTemplate.postForEntity(
                "/api/v1/users/signup",
                createJsonHttpEntity(request),
                String::class.java,
            )

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)

        val errorResponse = parseErrorResponse(response.body!!)
        assertThat(errorResponse.errorCode).isEqualTo("CONFLICT")
        assertThat(errorResponse.message).contains("Email already exists")
    }

    @Test
    fun `중복된 닉네임으로 회원가입 시 409 응답을 반환한다`() {
        // given
        val nickname = "duplicatenick"
        val existingUser = createTestUser(nickname = nickname)
        userRepository.insertOrUpdate(existingUser)

        val request =
            UserSignUpRequest(
                email = "new@example.com",
                nickname = nickname,
                password = "ValidPass123!",
            )

        // when
        val response: ResponseEntity<String> =
            restTemplate.postForEntity(
                "/api/v1/users/signup",
                createJsonHttpEntity(request),
                String::class.java,
            )

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)

        val errorResponse = parseErrorResponse(response.body!!)
        assertThat(errorResponse.errorCode).isEqualTo("CONFLICT")
        assertThat(errorResponse.message).contains("Nickname already exists")
    }

    @Test
    fun `유효하지 않은 비밀번호로 회원가입 시 400 응답을 반환한다`() {
        // given
        val request =
            UserSignUpRequest(
                email = "test@example.com",
                nickname = "testuser",
                // 너무 짧은 비밀번호
                password = "weak",
            )

        // when
        val response: ResponseEntity<String> =
            restTemplate.postForEntity(
                "/api/v1/users/signup",
                createJsonHttpEntity(request),
                String::class.java,
            )

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)

        val errorResponse = parseErrorResponse(response.body!!)
        assertThat(errorResponse.errorCode).isEqualTo("VALIDATION_FAILED")
        assertThat(errorResponse.message).contains("Password must be")
    }

    @Test
    fun `유효하지 않은 이메일 형식으로 회원가입 시 400 응답을 반환한다`() {
        // given
        val request =
            UserSignUpRequest(
                email = "invalid-email",
                nickname = "testuser",
                password = "ValidPass123!",
            )

        // when
        val response: ResponseEntity<String> =
            restTemplate.postForEntity(
                "/api/v1/users/signup",
                createJsonHttpEntity(request),
                String::class.java,
            )

        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
    }

    private fun createTestUser(
        email: String = "test@example.com",
        nickname: String = "testuser",
    ): me.helloc.techwikiplus.user.domain.User {
        return me.helloc.techwikiplus.user.domain.User(
            id = System.currentTimeMillis(),
            email = me.helloc.techwikiplus.user.domain.UserEmail(email, true),
            nickname = nickname,
            password = "encoded_password",
            status = UserStatus.ACTIVE,
            createdAt = me.helloc.techwikiplus.user.domain.port.outbound.Clock.system.localDateTime(),
            updatedAt = me.helloc.techwikiplus.user.domain.port.outbound.Clock.system.localDateTime(),
        )
    }
}
