package me.helloc.techwikiplus.user.integration.infrastructure.refreshtoken.redis

import me.helloc.techwikiplus.user.infrastructure.config.IntegrationTestSupport
import me.helloc.techwikiplus.user.infrastructure.refreshtoken.redis.RefreshTokenRedisStore
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.redis.core.StringRedisTemplate
import java.time.Duration

class RefreshTokenRedisStoreIntegrationTest : IntegrationTestSupport() {
    @Autowired
    private lateinit var refreshTokenStore: RefreshTokenRedisStore

    @Autowired
    private lateinit var redisTemplate: StringRedisTemplate

    @BeforeEach
    fun setUp() {
        // Redis 초기화
        redisTemplate.connectionFactory?.connection?.flushAll()
    }

    @Test
    fun `refresh token을 저장하고 조회할 수 있다`() {
        // given
        val userId = 123L
        val refreshToken = "test-refresh-token"
        val ttl = Duration.ofMinutes(30)

        // when
        refreshTokenStore.store(userId, refreshToken, ttl)

        // then
        assertThat(refreshTokenStore.exists(refreshToken)).isTrue
    }

    @Test
    fun `존재하지 않는 refresh token은 false를 반환한다`() {
        // given
        val nonExistentToken = "non-existent-token"

        // when
        val exists = refreshTokenStore.exists(nonExistentToken)

        // then
        assertThat(exists).isFalse
    }

    @Test
    fun `사용자의 기존 refresh token을 무효화할 수 있다`() {
        // given
        val userId = 123L
        val oldToken = "old-refresh-token"
        val newToken = "new-refresh-token"
        val ttl = Duration.ofMinutes(30)

        refreshTokenStore.store(userId, oldToken, ttl)
        assertThat(refreshTokenStore.exists(oldToken)).isTrue

        // when
        refreshTokenStore.store(userId, newToken, ttl)

        // then
        assertThat(refreshTokenStore.exists(oldToken)).isFalse
        assertThat(refreshTokenStore.exists(newToken)).isTrue
    }

    @Test
    fun `특정 refresh token을 무효화할 수 있다`() {
        // given
        val userId = 123L
        val refreshToken = "test-refresh-token"
        val ttl = Duration.ofMinutes(30)

        refreshTokenStore.store(userId, refreshToken, ttl)
        assertThat(refreshTokenStore.exists(refreshToken)).isTrue

        // when
        refreshTokenStore.invalidateToken(refreshToken)

        // then
        assertThat(refreshTokenStore.exists(refreshToken)).isFalse
    }

    @Test
    fun `사용자별로 다른 refresh token을 관리할 수 있다`() {
        // given
        val user1Id = 123L
        val user2Id = 456L
        val token1 = "user1-refresh-token"
        val token2 = "user2-refresh-token"
        val ttl = Duration.ofMinutes(30)

        // when
        refreshTokenStore.store(user1Id, token1, ttl)
        refreshTokenStore.store(user2Id, token2, ttl)

        // then
        assertThat(refreshTokenStore.exists(token1)).isTrue
        assertThat(refreshTokenStore.exists(token2)).isTrue

        // user1의 토큰 무효화
        refreshTokenStore.invalidate(user1Id)

        // user1의 토큰만 무효화되고 user2의 토큰은 유지
        assertThat(refreshTokenStore.exists(token1)).isFalse
        assertThat(refreshTokenStore.exists(token2)).isTrue
    }

    @Test
    fun `만료된 refresh token은 자동으로 삭제된다`() {
        // given
        val userId = 123L
        val refreshToken = "expiring-token"
        val ttl = Duration.ofSeconds(1)

        // when
        refreshTokenStore.store(userId, refreshToken, ttl)
        assertThat(refreshTokenStore.exists(refreshToken)).isTrue

        // 2초 대기
        Thread.sleep(2000)

        // then
        assertThat(refreshTokenStore.exists(refreshToken)).isFalse
    }

    @Test
    fun `여러 번 갱신해도 올바르게 동작한다`() {
        // given
        val userId = 123L
        val tokens = listOf("token1", "token2", "token3", "token4")
        val ttl = Duration.ofMinutes(30)

        // when
        tokens.forEach { token ->
            refreshTokenStore.store(userId, token, ttl)
        }

        // then
        // 마지막 토큰만 유효해야 함
        assertThat(refreshTokenStore.exists(tokens[0])).isFalse
        assertThat(refreshTokenStore.exists(tokens[1])).isFalse
        assertThat(refreshTokenStore.exists(tokens[2])).isFalse
        assertThat(refreshTokenStore.exists(tokens[3])).isTrue
    }
}
