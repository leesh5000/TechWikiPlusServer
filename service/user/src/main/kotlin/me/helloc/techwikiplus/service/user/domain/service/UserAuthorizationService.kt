package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.exception.DomainException
import me.helloc.techwikiplus.service.user.domain.exception.ErrorCode
import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.value.UserId
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
            throw DomainException(ErrorCode.FORBIDDEN)
        }
    }

    fun requireRole(role: UserRole) {
        if (!authorizationPort.hasRole(role)) {
            throw DomainException(ErrorCode.FORBIDDEN)
        }
    }

    fun isAuthenticated(): Boolean {
        return authorizationPort.isAuthenticated()
    }
}
