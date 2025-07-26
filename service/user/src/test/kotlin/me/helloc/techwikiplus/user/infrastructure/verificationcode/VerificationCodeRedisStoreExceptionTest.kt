package me.helloc.techwikiplus.user.infrastructure.verificationcode

import me.helloc.techwikiplus.user.domain.VerificationCode
import me.helloc.techwikiplus.user.infrastructure.exception.ExternalServiceException
import me.helloc.techwikiplus.user.infrastructure.verificationcode.redis.VerificationCodeRedisStore
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration

@DisplayName("VerificationCodeRedisStore 예외 변환 테스트")
class VerificationCodeRedisStoreExceptionTest {
    private lateinit var redisTemplate: StringRedisTemplate
    private lateinit var valueOperations: ValueOperations<String, String>
    private lateinit var store: VerificationCodeRedisStore

    @BeforeEach
    fun setUp() {
        redisTemplate = mock()
        valueOperations = mock()
        store = VerificationCodeRedisStore(redisTemplate)

        `when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
    }

    @Test
    @DisplayName("storeWithExpiry에서 Redis 연결 실패 시 ExternalServiceException으로 변환")
    fun `converts Redis connection failure to ExternalServiceException on storeWithExpiry`() {
        // given
        val email = "user@example.com"
        val code = VerificationCode("123456")
        val ttl = Duration.ofMinutes(5)
        val redisException = RedisConnectionFailureException("Connection refused")

        `when`(
            valueOperations.set(any(String::class.java), any(String::class.java), any(Duration::class.java)),
        ).thenThrow(redisException)

        // when & then
        assertThatThrownBy { store.storeWithExpiry(email, code, ttl) }
            .isInstanceOf(ExternalServiceException::class.java)
            .hasMessageContaining("Redis")
            .hasCause(redisException)
            .extracting("retryable")
            .isEqualTo(true)
    }

    @Test
    @DisplayName("retrieveOrThrows에서 Redis 연결 실패 시 ExternalServiceException으로 변환")
    fun `converts Redis connection failure to ExternalServiceException on retrieveOrThrows`() {
        // given
        val email = "user@example.com"
        val redisException = RedisConnectionFailureException("Connection timeout")

        `when`(valueOperations.get(any())).thenThrow(redisException)

        // when & then
        assertThatThrownBy { store.retrieveOrThrows(email) }
            .isInstanceOf(ExternalServiceException::class.java)
            .hasMessageContaining("Redis")
            .hasCause(redisException)
    }

    @Test
    @DisplayName("Redis 일반 예외도 ExternalServiceException으로 변환")
    fun `converts general Redis exception to ExternalServiceException`() {
        // given
        val email = "user@example.com"
        val code = VerificationCode("654321")
        val ttl = Duration.ofMinutes(10)
        val redisException = RuntimeException("Redis serialization error")

        `when`(
            valueOperations.set(any(String::class.java), any(String::class.java), any(Duration::class.java)),
        ).thenThrow(redisException)

        // when & then
        assertThatThrownBy { store.storeWithExpiry(email, code, ttl) }
            .isInstanceOf(ExternalServiceException::class.java)
            .hasMessageContaining("Redis")
            .hasCause(redisException)
    }
}
