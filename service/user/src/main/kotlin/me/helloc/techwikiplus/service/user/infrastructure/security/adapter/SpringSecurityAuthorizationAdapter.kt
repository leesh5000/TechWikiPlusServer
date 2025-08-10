package me.helloc.techwikiplus.service.user.infrastructure.security.adapter

import me.helloc.techwikiplus.service.user.domain.exception.DomainException
import me.helloc.techwikiplus.service.user.domain.exception.ErrorCode
import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.value.UserId
import me.helloc.techwikiplus.service.user.domain.port.AuthorizationPort
import me.helloc.techwikiplus.service.user.infrastructure.security.context.SecurityContextService
import org.springframework.stereotype.Component

@Component
class SpringSecurityAuthorizationAdapter(
    private val securityContextService: SecurityContextService,
) : AuthorizationPort {
    override fun getCurrentUserId(): UserId? {
        return securityContextService.getCurrentUserId()
    }

    override fun requireAuthenticated(): UserId {
        return getCurrentUserId() ?: throw DomainException(ErrorCode.UNAUTHORIZED)
    }

    override fun isAuthenticated(): Boolean {
        return securityContextService.isAuthenticated()
    }

    override fun hasRole(role: UserRole): Boolean {
        return securityContextService.hasRole(role.name)
    }

    override fun canAccessUser(targetUserId: UserId): Boolean {
        val currentUserId = getCurrentUserId() ?: return false

        return when {
            hasRole(UserRole.ADMIN) -> true
            currentUserId == targetUserId -> true
            else -> false
        }
    }
}
