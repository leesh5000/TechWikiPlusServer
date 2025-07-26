package me.helloc.techwikiplus.user.infrastructure.refreshtoken.fake

import me.helloc.techwikiplus.user.domain.port.outbound.RefreshTokenStore
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
        invalidate(userId = userId)

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

    override fun invalidate(
        userId: Long?,
        refreshToken: String?,
    ) {
        when {
            userId != null && refreshToken == null -> {
                val existingToken = userTokenStore[userId]
                existingToken?.let {
                    tokenStore.remove(it)
                }
                userTokenStore.remove(userId)
            }
            refreshToken != null -> {
                val entry = tokenStore.remove(refreshToken)
                entry?.let { (uid, _) ->
                    val currentToken = userTokenStore[uid]
                    if (currentToken == refreshToken) {
                        userTokenStore.remove(uid)
                    }
                }
            }
            else -> {
                throw IllegalArgumentException("Either userId or refreshToken must be provided")
            }
        }
    }

    fun clear() {
        tokenStore.clear()
        userTokenStore.clear()
    }
}
