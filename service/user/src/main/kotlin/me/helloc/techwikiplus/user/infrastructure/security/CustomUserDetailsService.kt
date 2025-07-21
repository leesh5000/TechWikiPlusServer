package me.helloc.techwikiplus.user.infrastructure.security

import me.helloc.techwikiplus.user.domain.service.UserReader
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(
    private val userReader: UserReader
) : UserDetailsService {
    
    override fun loadUserByUsername(email: String): UserDetails {
        val user = userReader.findByEmail(email)
            ?: throw UsernameNotFoundException("User not found with email: $email")
        
        return CustomUserDetails(user)
    }
}