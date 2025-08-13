package me.helloc.techwikiplus.service.user.domain.service.port

import me.helloc.techwikiplus.service.user.domain.model.UserId
import me.helloc.techwikiplus.service.user.domain.model.UserRole

interface UserAuthorizationPort {
    fun getCurrentUserId(): UserId?

    fun requireAuthenticated(): UserId

    fun isAuthenticated(): Boolean

    fun hasRole(role: UserRole): Boolean

    fun canAccessUser(targetUserId: UserId): Boolean
}
