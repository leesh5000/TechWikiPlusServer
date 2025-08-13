package me.helloc.techwikiplus.service.common.infrastructure.security.adapter

import me.helloc.techwikiplus.service.common.infrastructure.security.context.SecurityContextService
import me.helloc.techwikiplus.service.document.domain.model.LoginUser
import me.helloc.techwikiplus.service.document.domain.service.port.DocumentAuthorizationPort
import me.helloc.techwikiplus.service.user.domain.exception.UserDomainException
import me.helloc.techwikiplus.service.user.domain.exception.UserErrorCode
import org.springframework.stereotype.Component

@Component
class SpringSecurityDocumentAuthorizationAdapter(
    private val securityContextService: SecurityContextService,
) : DocumentAuthorizationPort {
    override fun requireAuthenticated(): LoginUser {
        return getCurrentUserId() ?: throw UserDomainException(UserErrorCode.UNAUTHORIZED)
    }

    fun getCurrentUserId(): LoginUser? {
        val currentUserId: Long = securityContextService.getCurrentUserId() ?: return null
        return LoginUser(currentUserId)
    }
}
