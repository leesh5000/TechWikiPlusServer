package me.helloc.techwikiplus.service.user.interfaces.web

import me.helloc.techwikiplus.service.user.domain.model.value.UserId
import me.helloc.techwikiplus.service.user.domain.port.UserRepository
import me.helloc.techwikiplus.service.user.infrastructure.security.context.SecurityContextService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/v1/users")
class UserProfileController(
    private val userRepository: UserRepository,
    private val securityContextService: SecurityContextService
) {
    
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    fun getMyProfile(): ResponseEntity<ProfileResponse> {
        val userId = securityContextService.getCurrentUserId()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        
        val user = userRepository.findBy(userId)
            ?: return ResponseEntity.notFound().build()
        
        return ResponseEntity.ok(ProfileResponse.from(user))
    }
    
    @GetMapping("/profile/{userId}")
    @PreAuthorize("isAuthenticated() and (#userId == authentication.principal.value or hasRole('ADMIN'))")
    fun getUserProfile(@PathVariable userId: String): ResponseEntity<ProfileResponse> {
        val user = userRepository.findBy(UserId(userId))
            ?: return ResponseEntity.notFound().build()
        
        return ResponseEntity.ok(ProfileResponse.from(user))
    }
    
    data class ProfileResponse(
        val userId: String,
        val email: String,
        val nickname: String,
        val role: String,
        val status: String,
        val createdAt: Instant,
        val modifiedAt: Instant
    ) {
        companion object {
            fun from(user: me.helloc.techwikiplus.service.user.domain.model.User): ProfileResponse {
                return ProfileResponse(
                    userId = user.id.value,
                    email = user.email.value,
                    nickname = user.nickname.value,
                    role = user.role.name,
                    status = user.status.name,
                    createdAt = user.createdAt,
                    modifiedAt = user.modifiedAt
                )
            }
        }
    }
}