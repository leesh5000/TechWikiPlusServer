package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.exception.UserDomainException
import me.helloc.techwikiplus.service.user.domain.exception.UserErrorCode
import me.helloc.techwikiplus.service.user.domain.model.UserId
import me.helloc.techwikiplus.service.user.domain.model.UserRole
import me.helloc.techwikiplus.service.user.domain.service.port.UserAuthorizationPort
import org.springframework.stereotype.Service

@Service
class UserAuthorizationService(
    private val userAuthorizationPort: UserAuthorizationPort,
) {
    fun getCurrentUserOrThrow(): UserId {
        return userAuthorizationPort.requireAuthenticated()
    }

    fun getCurrentUserId(): UserId? {
        return userAuthorizationPort.getCurrentUserId()
    }

    fun requireUserAccess(targetUserId: UserId) {
        if (!userAuthorizationPort.canAccessUser(targetUserId)) {
            throw UserDomainException(UserErrorCode.FORBIDDEN)
        }
    }

    fun requireRole(role: UserRole) {
        if (!userAuthorizationPort.hasRole(role)) {
            throw UserDomainException(UserErrorCode.FORBIDDEN)
        }
    }

    fun isAuthenticated(): Boolean {
        return userAuthorizationPort.isAuthenticated()
    }
}
