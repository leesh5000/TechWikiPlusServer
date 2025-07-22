package me.helloc.techwikiplus.user.integration.http

import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.UserEmail
import me.helloc.techwikiplus.user.domain.UserStatus
import me.helloc.techwikiplus.user.domain.VerificationCode
import me.helloc.techwikiplus.user.domain.service.UserRepository
import me.helloc.techwikiplus.user.domain.service.Clock
import me.helloc.techwikiplus.user.domain.service.MailSender
import me.helloc.techwikiplus.user.domain.service.VerificationCodeStore
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class ResendVerificationCodeControllerIntegrationTest : ControllerIntegrationTestSupport() {
    
    @Autowired
    private lateinit var userRepository: UserRepository
    
    @MockBean
    private lateinit var mailSender: MailSender
    
    @Autowired
    private lateinit var verificationCodeStore: VerificationCodeStore
    
    @BeforeEach
    fun setUp() {
        // 이메일 발송은 Mock 처리
        `when`(mailSender.sendVerificationEmail(anyString())).thenReturn(
            VerificationCode("654321")
        )
    }
    
    @Test
    fun `PENDING 상태 사용자의 이메일로 재전송 요청 시 200 응답을 반환한다`() {
        // given
        val pendingEmail = "pending@example.com"
        val pendingUser = User(
            id = System.currentTimeMillis(),
            email = UserEmail(pendingEmail, false),
            nickname = "pendinguser",
            password = "encoded_password",
            status = UserStatus.PENDING,
            createdAt = Clock.system.localDateTime(),
            updatedAt = Clock.system.localDateTime()
        )
        userRepository.insertOrUpdate(pendingUser)
        
        // when
        val response: ResponseEntity<Void> = restTemplate.getForEntity(
            "/api/v1/users/signup/verify/resend?email=$pendingEmail",
            Void::class.java
        )
        
        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.ACCEPTED)
        
        // 이메일이 발송되었는지 확인
        verify(mailSender, times(1)).sendVerificationEmail(pendingEmail)
        
        // 새로운 인증 코드가 저장되었는지 확인
        val storedCode = verificationCodeStore.retrieveOrThrows(pendingEmail)
        assertThat(storedCode.value).isEqualTo("654321")
    }
    
    @Test
    fun `ACTIVE 상태 사용자의 이메일로 재전송 요청 시 401 응답을 반환한다`() {
        // given
        val activeEmail = "active@example.com"
        val activeUser = User(
            id = System.currentTimeMillis(),
            email = UserEmail(activeEmail, true),
            nickname = "activeuser",
            password = "encoded_password",
            status = UserStatus.ACTIVE,
            createdAt = Clock.system.localDateTime(),
            updatedAt = Clock.system.localDateTime()
        )
        userRepository.insertOrUpdate(activeUser)
        
        // when
        val response: ResponseEntity<String> = restTemplate.getForEntity(
            "/api/v1/users/signup/verify/resend?email=$activeEmail",
            String::class.java
        )
        
        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        
        val errorResponse = parseErrorResponse(response.body!!)
        assertThat(errorResponse.errorCode).isEqualTo("AUTHENTICATION_FAILED")
        assertThat(errorResponse.message).contains("Pending user not found")
        
        // 이메일이 발송되지 않았는지 확인
        verify(mailSender, times(0)).sendVerificationEmail(anyString())
    }
    
    @Test
    fun `존재하지 않는 이메일로 재전송 요청 시 401 응답을 반환한다`() {
        // given
        val nonExistentEmail = "nonexistent@example.com"
        
        // when
        val response: ResponseEntity<String> = restTemplate.getForEntity(
            "/api/v1/users/signup/verify/resend?email=$nonExistentEmail",
            String::class.java
        )
        
        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        
        val errorResponse = parseErrorResponse(response.body!!)
        assertThat(errorResponse.errorCode).isEqualTo("AUTHENTICATION_FAILED")
        assertThat(errorResponse.message).contains("Pending user not found")
        
        // 이메일이 발송되지 않았는지 확인
        verify(mailSender, times(0)).sendVerificationEmail(anyString())
    }
    
    @Test
    fun `BANNED 상태 사용자의 이메일로 재전송 요청 시 401 응답을 반환한다`() {
        // given
        val bannedEmail = "banned@example.com"
        val bannedUser = User(
            id = System.currentTimeMillis(),
            email = UserEmail(bannedEmail, true),
            nickname = "banneduser",
            password = "encoded_password",
            status = UserStatus.BANNED,
            createdAt = Clock.system.localDateTime(),
            updatedAt = Clock.system.localDateTime()
        )
        userRepository.insertOrUpdate(bannedUser)
        
        // when
        val response: ResponseEntity<String> = restTemplate.getForEntity(
            "/api/v1/users/signup/verify/resend?email=$bannedEmail",
            String::class.java
        )
        
        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        
        val errorResponse = parseErrorResponse(response.body!!)
        assertThat(errorResponse.errorCode).isEqualTo("AUTHENTICATION_FAILED")
        assertThat(errorResponse.message).contains("Pending user not found")
        
        // 이메일이 발송되지 않았는지 확인
        verify(mailSender, times(0)).sendVerificationEmail(anyString())
    }
    
    @Test
    fun `이메일 파라미터 없이 재전송 요청 시 400 응답을 반환한다`() {
        // when
        val response: ResponseEntity<String> = restTemplate.getForEntity(
            "/api/v1/users/signup/verify/resend",
            String::class.java
        )
        
        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        
        val errorResponse = parseErrorResponse(response.body!!)
        assertThat(errorResponse.errorCode).isEqualTo("MISSING_PARAMETER")
        assertThat(errorResponse.message).contains("Required parameter 'email' is missing")
    }
    
    @Test
    fun `이전 인증 코드가 있는 경우 새로운 코드로 덮어쓴다`() {
        // given
        val pendingEmail = "pending-with-old-code@example.com"
        val pendingUser = User(
            id = System.currentTimeMillis(),
            email = UserEmail(pendingEmail, false),
            nickname = "pendinguser2",
            password = "encoded_password",
            status = UserStatus.PENDING,
            createdAt = Clock.system.localDateTime(),
            updatedAt = Clock.system.localDateTime()
        )
        userRepository.insertOrUpdate(pendingUser)
        
        // 이전 인증 코드 저장
        verificationCodeStore.storeWithExpiry(
            pendingEmail,
            VerificationCode("OLD123"),
            java.time.Duration.ofMinutes(5)
        )
        
        // when
        val response: ResponseEntity<Void> = restTemplate.getForEntity(
            "/api/v1/users/signup/verify/resend?email=$pendingEmail",
            Void::class.java
        )
        
        // then
        assertThat(response.statusCode).isEqualTo(HttpStatus.ACCEPTED)
        
        // 새로운 인증 코드로 덮어쓰여졌는지 확인
        val storedCode = verificationCodeStore.retrieveOrThrows(pendingEmail)
        assertThat(storedCode.value).isEqualTo("654321")
        assertThat(storedCode.value).isNotEqualTo("OLD123")
    }
}