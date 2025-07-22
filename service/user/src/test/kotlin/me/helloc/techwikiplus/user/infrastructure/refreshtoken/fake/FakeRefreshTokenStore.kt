package me.helloc.techwikiplus.user.infrastructure.refreshtoken.fake

import me.helloc.techwikiplus.user.domain.service.RefreshTokenStore
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class FakeRefreshTokenStore : RefreshTokenStore {
    private val tokenStore = ConcurrentHashMap<String, Pair<Long, Instant>>()
    private val userTokenStore = ConcurrentHashMap<Long, String>()

    override fun store(
        userId: Long,
        refreshToken: String,
        ttl: Duration,
    ) {
        // Invalidate existing token for user
        invalidate(userId)

        val expiresAt = Instant.now().plus(ttl)
        tokenStore[refreshToken] = userId to expiresAt
        userTokenStore[userId] = refreshToken
    }

    override fun exists(refreshToken: String): Boolean {
        val entry = tokenStore[refreshToken] ?: return false
        val (_, expiresAt) = entry

        // Check if token has expired
        if (Instant.now().isAfter(expiresAt)) {
            tokenStore.remove(refreshToken)
            return false
        }

        return true
    }

    override fun invalidate(userId: Long) {
        val existingToken = userTokenStore[userId]
        existingToken?.let {
            tokenStore.remove(it)
        }
        userTokenStore.remove(userId)
    }

    override fun invalidateToken(refreshToken: String) {
        val entry = tokenStore.remove(refreshToken)
        entry?.let { (userId, _) ->
            userTokenStore.remove(userId)
        }
    }

    fun clear() {
        tokenStore.clear()
        userTokenStore.clear()
    }
}