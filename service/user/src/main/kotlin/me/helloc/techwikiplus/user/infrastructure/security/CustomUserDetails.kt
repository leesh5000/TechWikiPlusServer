package me.helloc.techwikiplus.user.infrastructure.security

import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.UserStatus
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class CustomUserDetails(
    private val user: User
) : UserDetails {
    
    override fun getAuthorities(): Collection<GrantedAuthority> {
        return listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
    }

    override fun getPassword(): String = user.password

    override fun getUsername(): String = user.email.value

    override fun isAccountNonExpired(): Boolean = user.status != UserStatus.DELETED

    override fun isAccountNonLocked(): Boolean = user.status != UserStatus.BANNED

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = user.status == UserStatus.ACTIVE

    fun getUserId(): Long = user.id
    
    fun getNickname(): String = user.nickname
}