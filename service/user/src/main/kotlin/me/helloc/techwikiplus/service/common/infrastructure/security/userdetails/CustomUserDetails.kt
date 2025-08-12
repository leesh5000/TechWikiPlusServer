package me.helloc.techwikiplus.service.common.infrastructure.security.userdetails

import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.UserRole
import me.helloc.techwikiplus.service.user.domain.model.UserStatus
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class CustomUserDetails(
    private val user: User,
) : UserDetails {
    override fun getAuthorities(): Collection<GrantedAuthority> {
        return listOf(SimpleGrantedAuthority("ROLE_${user.role.name}"))
    }

    override fun getPassword(): String {
        return user.encodedPassword.value
    }

    override fun getUsername(): String {
        return user.id.value.toString()
    }

    override fun isAccountNonExpired(): Boolean {
        return user.status != UserStatus.DELETED
    }

    override fun isAccountNonLocked(): Boolean {
        return user.status != UserStatus.BANNED
    }

    override fun isCredentialsNonExpired(): Boolean {
        return true
    }

    override fun isEnabled(): Boolean {
        return user.status == UserStatus.ACTIVE
    }

    fun getUserId(): String = user.id.value.toString()

    fun getEmail(): String = user.email.value

    fun getNickname(): String = user.nickname.value

    fun getRole(): UserRole = user.role

    fun getStatus(): UserStatus = user.status
}
