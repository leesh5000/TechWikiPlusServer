package me.helloc.techwikiplus.user.infrastructure.refreshtoken

import me.helloc.techwikiplus.user.infrastructure.exception.ExternalServiceException
import me.helloc.techwikiplus.user.infrastructure.refreshtoken.redis.RefreshTokenRedisStore
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

@DisplayName("RefreshTokenRedisStore 예외 변환 테스트")
class RefreshTokenRedisStoreExceptionTest {
    private lateinit var redisTemplate: StringRedisTemplate
    private lateinit var valueOperations: ValueOperations<String, String>
    private lateinit var store: RefreshTokenRedisStore

    @BeforeEach
    fun setUp() {
        redisTemplate = mock()
        valueOperations = mock()
        store = RefreshTokenRedisStore(redisTemplate)

        `when`(redisTemplate.opsForValue()).thenReturn(valueOperations)
    }

    @Test
    @DisplayName("store에서 Redis 연결 실패 시 ExternalServiceException으로 변환")
    fun `converts Redis connection failure to ExternalServiceException on store`() {
        // given
        val userId = 1L
        val refreshToken = "test-token"
        val ttl = Duration.ofDays(7)
        val redisException = RedisConnectionFailureException("Connection refused")

        `when`(valueOperations.get(any(String::class.java))).thenThrow(redisException)

        // when & then
        assertThatThrownBy { store.store(userId, refreshToken, ttl) }
            .isInstanceOf(ExternalServiceException::class.java)
            .hasMessageContaining("Redis")
            .hasCause(redisException)
            .extracting("retryable")
            .isEqualTo(true)
    }

    @Test
    @DisplayName("exists에서 Redis 연결 실패 시 ExternalServiceException으로 변환")
    fun `converts Redis connection failure to ExternalServiceException on exists`() {
        // given
        val refreshToken = "test-token"
        val redisException = RedisConnectionFailureException("Connection timeout")

        `when`(redisTemplate.hasKey(any())).thenThrow(redisException)

        // when & then
        assertThatThrownBy { store.exists(refreshToken) }
            .isInstanceOf(ExternalServiceException::class.java)
            .hasMessageContaining("Redis")
            .hasCause(redisException)
    }

    @Test
    @DisplayName("invalidate에서 Redis 연결 실패 시 ExternalServiceException으로 변환")
    fun `converts Redis connection failure to ExternalServiceException on invalidate`() {
        // given
        val userId = 1L
        val redisException = RedisConnectionFailureException("Connection lost")

        `when`(valueOperations.get(any(String::class.java))).thenThrow(redisException)

        // when & then
        assertThatThrownBy { store.invalidate(userId, null) }
            .isInstanceOf(ExternalServiceException::class.java)
            .hasMessageContaining("Redis")
            .hasCause(redisException)
    }

    @Test
    @DisplayName("Redis 일반 예외도 ExternalServiceException으로 변환")
    fun `converts general Redis exception to ExternalServiceException`() {
        // given
        val refreshToken = "test-token"
        val redisException = RuntimeException("Redis serialization error")

        `when`(redisTemplate.hasKey(any())).thenThrow(redisException)

        // when & then
        assertThatThrownBy { store.exists(refreshToken) }
            .isInstanceOf(ExternalServiceException::class.java)
            .hasMessageContaining("Redis")
            .hasCause(redisException)
    }
}
