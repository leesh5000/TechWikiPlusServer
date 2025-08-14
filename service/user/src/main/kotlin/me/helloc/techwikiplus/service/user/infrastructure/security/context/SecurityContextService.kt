package me.helloc.techwikiplus.service.user.infrastructure.security.context

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

@Service
class SecurityContextService {
    fun getCurrentUserId(): Long? {
        val authentication = SecurityContextHolder.getContext().authentication

        return when (val principal = authentication?.principal) {
            is Long -> principal
            is String -> principal.toLong()
            else -> null
        }
    }

    fun isAuthenticated(): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication != null && authentication.isAuthenticated
    }

    fun hasRole(role: String): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.authorities?.any {
            it.authority == "ROLE_$role"
        } ?: false
    }
}
