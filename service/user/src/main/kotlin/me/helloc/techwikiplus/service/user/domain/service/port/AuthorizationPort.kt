package me.helloc.techwikiplus.service.user.domain.service.port

import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.value.UserId

interface AuthorizationPort {
    fun getCurrentUserId(): UserId?

    fun requireAuthenticated(): UserId

    fun isAuthenticated(): Boolean

    fun hasRole(role: UserRole): Boolean

    fun canAccessUser(targetUserId: UserId): Boolean
}
