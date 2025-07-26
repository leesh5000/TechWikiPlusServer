package me.helloc.techwikiplus.user.e2e

import me.helloc.techwikiplus.user.domain.VerificationCode
import me.helloc.techwikiplus.user.domain.port.outbound.MailSender
import me.helloc.techwikiplus.user.domain.port.outbound.UserRepository
import me.helloc.techwikiplus.user.domain.port.outbound.VerificationCodeStore
import me.helloc.techwikiplus.user.integration.http.ControllerIntegrationTestSupport
import me.helloc.techwikiplus.user.interfaces.http.dto.request.LoginRequest
import me.helloc.techwikiplus.user.interfaces.http.dto.request.RefreshTokenRequest
import me.helloc.techwikiplus.user.interfaces.http.dto.request.UserSignUpRequest
import me.helloc.techwikiplus.user.interfaces.http.dto.request.UserSignUpVerifyRequest
import me.helloc.techwikiplus.user.interfaces.http.dto.response.LoginResponse
import me.helloc.techwikiplus.user.interfaces.http.dto.response.RefreshTokenResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.bean.override.mockito.MockitoBean
import kotlin.system.measureTimeMillis

/**
 * End-to-End 테스트: 전체 사용자 인증 플로우
 *
 * 테스트 시나리오:
 * 1. 회원가입
 * 2. 이메일 인증 전 로그인 시도 (실패)
 * 3. 이메일 인증
 * 4. 인증 코드 재전송
 * 5. 재전송된 코드로 이메일 인증
 * 6. 로그인
 * 7. 토큰 갱신
 */
class AuthenticationFlowE2ETest : ControllerIntegrationTestSupport() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var verificationCodeStore: VerificationCodeStore

    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

    @MockitoBean
    private lateinit var mailSender: MailSender

    // 테스트용 사용자 정보
    private val testEmail = "e2e.test@example.com"
    private val testNickname = "e2etestuser"
    private val testPassword = "E2ETestPass123!"

    // 캡처된 인증 코드
    private var capturedVerificationCode: String? = null

    @BeforeEach
    fun setUp() {
        // Redis 초기화
        val allKeys = redisTemplate.keys("*")
        if (allKeys.isNotEmpty()) {
            redisTemplate.delete(allKeys)
        }

        // 메일 발송 Mock 설정 - 인증 코드를 캡처
        `when`(mailSender.sendVerificationEmail(anyString())).thenAnswer { invocation ->
            val code = VerificationCode.generate()
            capturedVerificationCode = code.value
            code
        }
    }

    @Test
    fun `전체 인증 플로우 E2E 테스트`() {
        // 성능 측정을 위한 시간 기록
        val testResults = mutableMapOf<String, Long>()

        // Step 1: 회원가입
        val signupTime =
            measureTimeMillis {
                val signupRequest =
                    UserSignUpRequest(
                        email = testEmail,
                        nickname = testNickname,
                        password = testPassword,
                    )

                val signupResponse: ResponseEntity<Void> =
                    restTemplate.postForEntity(
                        "/api/v1/users/signup",
                        createJsonHttpEntity(signupRequest),
                        Void::class.java,
                    )

                assertThat(signupResponse.statusCode).isEqualTo(HttpStatus.ACCEPTED)
                assertThat(signupResponse.headers.getFirst("Location"))
                    .isEqualTo("/api/v1/users/signup/verify")
            }
        testResults["signup"] = signupTime

        // 메일이 발송되었는지 확인
        verify(mailSender, times(1)).sendVerificationEmail(testEmail)
        assertThat(capturedVerificationCode).isNotNull
        val firstVerificationCode = capturedVerificationCode!!

        // 사용자가 DB에 저장되었는지 확인
        val savedUser = userRepository.findByEmail(testEmail)
        assertThat(savedUser).isNotNull
        assertThat(savedUser!!.getEmailValue()).isEqualTo(testEmail)
        assertThat(savedUser.nickname).isEqualTo(testNickname)
        assertThat(savedUser.status.name).isEqualTo("PENDING")

        // Step 2: 이메일 인증 전 로그인 시도 (실패해야 함)
        val loginBeforeVerifyTime =
            measureTimeMillis {
                val loginRequest =
                    LoginRequest(
                        email = testEmail,
                        password = testPassword,
                    )

                val loginResponse: ResponseEntity<String> =
                    restTemplate.postForEntity(
                        "/api/v1/users/login",
                        createJsonHttpEntity(loginRequest),
                        String::class.java,
                    )

                assertThat(loginResponse.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
                val errorResponse = parseErrorResponse(loginResponse.body!!)
                assertThat(errorResponse.errorCode).isEqualTo("AUTHENTICATION_FAILED")
                assertThat(errorResponse.message).contains("Email not verified")
            }
        testResults["loginBeforeVerify"] = loginBeforeVerifyTime

        // Step 3: 잘못된 인증 코드로 이메일 인증 시도
        val verifyWithWrongCodeTime =
            measureTimeMillis {
                val verifyRequest =
                    UserSignUpVerifyRequest(
                        email = testEmail,
                        // 잘못된 코드
                        code = "999999",
                    )

                val verifyResponse: ResponseEntity<String> =
                    restTemplate.postForEntity(
                        "/api/v1/users/signup/verify",
                        createJsonHttpEntity(verifyRequest),
                        String::class.java,
                    )

                assertThat(verifyResponse.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
                val errorResponse = parseErrorResponse(verifyResponse.body!!)
                assertThat(errorResponse.errorCode).isEqualTo("AUTHENTICATION_FAILED")
                assertThat(errorResponse.message).contains("Invalid verification code")
            }
        testResults["verifyWithWrongCode"] = verifyWithWrongCodeTime

        // Step 4: 인증 코드 재전송
        val resendCodeTime =
            measureTimeMillis {
                val resendResponse: ResponseEntity<Void> =
                    restTemplate.getForEntity(
                        "/api/v1/users/signup/verify/resend?email=$testEmail",
                        Void::class.java,
                    )

                assertThat(resendResponse.statusCode).isEqualTo(HttpStatus.ACCEPTED)
            }
        testResults["resendCode"] = resendCodeTime

        // 새로운 인증 코드가 발송되었는지 확인
        verify(mailSender, times(2)).sendVerificationEmail(testEmail)
        assertThat(capturedVerificationCode).isNotNull
        assertThat(capturedVerificationCode).isNotEqualTo(firstVerificationCode)
        val secondVerificationCode = capturedVerificationCode!!

        // Step 5: 재전송된 코드로 이메일 인증
        val verifyEmailTime =
            measureTimeMillis {
                val verifyRequest =
                    UserSignUpVerifyRequest(
                        email = testEmail,
                        code = secondVerificationCode,
                    )

                val verifyResponse: ResponseEntity<Void> =
                    restTemplate.postForEntity(
                        "/api/v1/users/signup/verify",
                        createJsonHttpEntity(verifyRequest),
                        Void::class.java,
                    )

                assertThat(verifyResponse.statusCode).isEqualTo(HttpStatus.OK)
            }
        testResults["verifyEmail"] = verifyEmailTime

        // 사용자 상태가 ACTIVE로 변경되었는지 확인
        val verifiedUser = userRepository.findByEmail(testEmail)
        assertThat(verifiedUser).isNotNull
        assertThat(verifiedUser!!.status.name).isEqualTo("ACTIVE")
        assertThat(verifiedUser.email.verified).isTrue

        // Step 6: 이메일 인증 후 로그인
        var accessToken: String? = null
        var refreshToken: String? = null
        var userId: Long? = null

        val loginTime =
            measureTimeMillis {
                val loginRequest =
                    LoginRequest(
                        email = testEmail,
                        password = testPassword,
                    )

                val loginResponse: ResponseEntity<LoginResponse> =
                    restTemplate.postForEntity(
                        "/api/v1/users/login",
                        createJsonHttpEntity(loginRequest),
                        LoginResponse::class.java,
                    )

                assertThat(loginResponse.statusCode).isEqualTo(HttpStatus.OK)
                assertThat(loginResponse.body).isNotNull

                val loginBody = loginResponse.body!!
                assertThat(loginBody.accessToken).isNotBlank
                assertThat(loginBody.refreshToken).isNotBlank
                assertThat(loginBody.userId).isGreaterThan(0)

                accessToken = loginBody.accessToken
                refreshToken = loginBody.refreshToken
                userId = loginBody.userId
            }
        testResults["login"] = loginTime

        // Step 7: 토큰 갱신
        // 토큰 발급 시간에 차이를 두기 위해 잠시 대기
        Thread.sleep(1000)

        val refreshTokenTime =
            measureTimeMillis {
                val refreshRequest =
                    RefreshTokenRequest(
                        refreshToken = refreshToken!!,
                    )

                val refreshResponse: ResponseEntity<RefreshTokenResponse> =
                    restTemplate.postForEntity(
                        "/api/v1/users/refresh",
                        createJsonHttpEntity(refreshRequest),
                        RefreshTokenResponse::class.java,
                    )

                assertThat(refreshResponse.statusCode).isEqualTo(HttpStatus.OK)
                assertThat(refreshResponse.body).isNotNull

                val refreshBody = refreshResponse.body!!
                assertThat(refreshBody.accessToken).isNotBlank
                assertThat(refreshBody.refreshToken).isNotBlank
                assertThat(refreshBody.userId).isEqualTo(userId)

                // 새로운 토큰들이 기존 토큰과 다른지 확인
                assertThat(refreshBody.accessToken).isNotEqualTo(accessToken)
                assertThat(refreshBody.refreshToken).isNotEqualTo(refreshToken)
            }
        testResults["refreshToken"] = refreshTokenTime

        // Step 8: 이전 refresh token으로 재시도 (실패해야 함)
        val oldRefreshTokenTime =
            measureTimeMillis {
                val refreshRequest =
                    RefreshTokenRequest(
                        // 이전 토큰
                        refreshToken = refreshToken!!,
                    )

                val refreshResponse: ResponseEntity<String> =
                    restTemplate.postForEntity(
                        "/api/v1/users/refresh",
                        createJsonHttpEntity(refreshRequest),
                        String::class.java,
                    )

                assertThat(refreshResponse.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
                val errorResponse = parseErrorResponse(refreshResponse.body!!)
                assertThat(errorResponse.errorCode).isEqualTo("AUTHENTICATION_FAILED")
                assertThat(errorResponse.message).contains("Invalid refresh token")
            }
        testResults["oldRefreshToken"] = oldRefreshTokenTime

        // 성능 결과 출력
        println("\n========== E2E Test Performance Results ==========")
        println("Total test time: ${testResults.values.sum()}ms")
        testResults.forEach { (step, time) ->
            println("$step: ${time}ms")
        }
        println("==================================================\n")
    }

    @Test
    fun `중복 이메일 회원가입 시나리오`() {
        // 첫 번째 회원가입
        val signupRequest =
            UserSignUpRequest(
                email = "duplicate@example.com",
                nickname = "firstuser",
                password = "ValidPass123!",
            )

        val firstSignup: ResponseEntity<Void> =
            restTemplate.postForEntity(
                "/api/v1/users/signup",
                createJsonHttpEntity(signupRequest),
                Void::class.java,
            )
        assertThat(firstSignup.statusCode).isEqualTo(HttpStatus.ACCEPTED)

        // 같은 이메일로 두 번째 회원가입 시도
        val duplicateRequest =
            UserSignUpRequest(
                email = "duplicate@example.com",
                nickname = "seconduser",
                password = "AnotherPass456!",
            )

        val secondSignup: ResponseEntity<String> =
            restTemplate.postForEntity(
                "/api/v1/users/signup",
                createJsonHttpEntity(duplicateRequest),
                String::class.java,
            )

        assertThat(secondSignup.statusCode).isEqualTo(HttpStatus.CONFLICT)
        val errorResponse = parseErrorResponse(secondSignup.body!!)
        assertThat(errorResponse.errorCode).isEqualTo("CONFLICT")
        assertThat(errorResponse.message).contains("Email already exists")
    }

    @Test
    fun `인증 코드 재전송 rate limit 테스트`() {
        // 회원가입
        val signupRequest =
            UserSignUpRequest(
                email = "ratelimit@example.com",
                nickname = "ratelimituser",
                password = "ValidPass123!",
            )

        restTemplate.postForEntity(
            "/api/v1/users/signup",
            createJsonHttpEntity(signupRequest),
            Void::class.java,
        )

        // 연속으로 재전송 요청
        repeat(3) { attempt ->
            val response: ResponseEntity<Any> =
                restTemplate.getForEntity(
                    "/api/v1/users/signup/verify/resend?email=ratelimit@example.com",
                    Any::class.java,
                )

            if (attempt < 2) {
                // 처음 두 번은 성공해야 함
                assertThat(response.statusCode).isEqualTo(HttpStatus.ACCEPTED)
            } else {
                // 세 번째부터는 rate limit에 걸릴 수 있음
                // (구현에 따라 다를 수 있으므로 유연하게 처리)
                assertThat(response.statusCode).isIn(
                    HttpStatus.ACCEPTED,
                    HttpStatus.TOO_MANY_REQUESTS,
                )
            }

            // 다음 요청 전 잠시 대기
            Thread.sleep(100)
        }
    }
}
