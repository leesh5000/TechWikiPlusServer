package me.helloc.techwikiplus.user.infrastructure.verificationcode.redis

import me.helloc.techwikiplus.user.domain.VerificationCode
import me.helloc.techwikiplus.user.domain.exception.CustomException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.time.Duration

class VerificationCodeRedisStoreTest {

    @Mock
    private lateinit var redisTemplate: StringRedisTemplate

    @Mock
    private lateinit var valueOperations: ValueOperations<String, String>

    private lateinit var verificationCodeStore: VerificationCodeRedisStore

    @BeforeEach
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        `when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
        verificationCodeStore = VerificationCodeRedisStore(redisTemplate)
    }

    @Test
    fun `should store verification code with expiry`() {
        val email = "test@example.com"
        val code = VerificationCode("123456")
        val ttl = Duration.ofMinutes(5)

        verificationCodeStore.storeWithExpiry(email, code, ttl)

        verify(valueOperations).set("user:verification:$email", code.value, ttl)
        verify(redisTemplate).delete("user:verification:attempt:$email")
    }

    @Test
    fun `should retrieve valid verification code`() {
        val email = "test@example.com"
        val code = VerificationCode("123456")

        `when`(valueOperations.get("user:verification:$email")).thenReturn(code.value)
        `when`(valueOperations.increment("user:verification:attempt:$email")).thenReturn(1L)

        verificationCodeStore.retrieveOrThrows(email, code)

        verify(redisTemplate).delete("user:verification:$email")
        verify(redisTemplate).delete("user:verification:attempt:$email")
    }

    @Test
    fun `should throw exception when verification code not found`() {
        val email = "test@example.com"
        val code = VerificationCode("123456")

        `when`(valueOperations.get("user:verification:$email")).thenReturn(null)
        `when`(valueOperations.increment("user:verification:attempt:$email")).thenReturn(1L)

        assertThatThrownBy {
            verificationCodeStore.retrieveOrThrows(email, code)
        }.isInstanceOf(CustomException.AuthenticationException.ExpiredEmailVerification::class.java)
    }

    @Test
    fun `should throw exception when verification code does not match`() {
        val email = "test@example.com"
        val code = VerificationCode("123456")
        val wrongCode = VerificationCode("654321")

        `when`(valueOperations.get("user:verification:$email")).thenReturn(code.value)
        `when`(valueOperations.increment("user:verification:attempt:$email")).thenReturn(1L)

        assertThatThrownBy {
            verificationCodeStore.retrieveOrThrows(email, wrongCode)
        }.isInstanceOf(CustomException.AuthenticationException.ExpiredEmailVerification::class.java)
    }

    @Test
    fun `should throw exception when max attempts exceeded`() {
        val email = "test@example.com"
        val code = VerificationCode("123456")

        `when`(valueOperations.increment("user:verification:attempt:$email")).thenReturn(6L)

        assertThatThrownBy {
            verificationCodeStore.retrieveOrThrows(email, code)
        }.isInstanceOf(CustomException.AuthenticationException.UnauthorizedAccess::class.java)
            .hasMessageContaining("Too many verification attempts")
    }
}
