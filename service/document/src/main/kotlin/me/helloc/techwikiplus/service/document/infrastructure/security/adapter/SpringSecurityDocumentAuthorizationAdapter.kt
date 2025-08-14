package me.helloc.techwikiplus.service.document.infrastructure.security.adapter

import me.helloc.techwikiplus.service.document.domain.exception.DocumentDomainException
import me.helloc.techwikiplus.service.document.domain.exception.DocumentErrorCode
import me.helloc.techwikiplus.service.document.domain.model.LoginUser
import me.helloc.techwikiplus.service.document.domain.service.port.DocumentAuthorizationPort
import org.springframework.stereotype.Component

@Component
class SpringSecurityDocumentAuthorizationAdapter : DocumentAuthorizationPort {
    override fun requireAuthenticated(): LoginUser {
        return getCurrentUserId() ?: throw DocumentDomainException(DocumentErrorCode.UNAUTHORIZED)
    }

    fun getCurrentUserId(): LoginUser? {
        // TODO: Integration with Spring Security context to get current user ID
        // For now, return a dummy user ID for compilation
        return LoginUser(1L)
    }
}
