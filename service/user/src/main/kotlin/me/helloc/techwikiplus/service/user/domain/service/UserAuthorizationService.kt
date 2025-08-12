package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.exception.UserDomainException
import me.helloc.techwikiplus.service.user.domain.exception.UserErrorCode
import me.helloc.techwikiplus.service.user.domain.model.UserId
import me.helloc.techwikiplus.service.user.domain.model.UserRole
import me.helloc.techwikiplus.service.user.domain.service.port.AuthorizationPort
import org.springframework.stereotype.Service

@Service
class UserAuthorizationService(
    private val authorizationPort: AuthorizationPort,
) {
    fun getCurrentUserOrThrow(): UserId {
        return authorizationPort.requireAuthenticated()
    }

    fun getCurrentUserId(): UserId? {
        return authorizationPort.getCurrentUserId()
    }

    fun requireUserAccess(targetUserId: UserId) {
        if (!authorizationPort.canAccessUser(targetUserId)) {
            throw UserDomainException(UserErrorCode.FORBIDDEN)
        }
    }

    fun requireRole(role: UserRole) {
        if (!authorizationPort.hasRole(role)) {
            throw UserDomainException(UserErrorCode.FORBIDDEN)
        }
    }

    fun isAuthenticated(): Boolean {
        return authorizationPort.isAuthenticated()
    }
}
