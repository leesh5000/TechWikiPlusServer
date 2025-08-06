package me.helloc.techwikiplus.service.user.infrastructure.security.userdetails

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.model.value.UserId
import me.helloc.techwikiplus.service.user.domain.port.UserRepository
import org.springframework.security.core.userdetails.UsernameNotFoundException
import java.time.Instant

class CustomUserDetailsServiceTest : DescribeSpec({
    
    lateinit var userRepository: UserRepository
    lateinit var userDetailsService: CustomUserDetailsService
    
    beforeEach {
        userRepository = mockk()
        userDetailsService = CustomUserDetailsService(userRepository)
    }
    
    describe("CustomUserDetailsService") {
        context("사용자 ID로 조회할 때") {
            it("존재하는 사용자의 UserDetails를 반환해야 함") {
                // given
                val userId = "user123"
                val user = User(
                    id = UserId(userId),
                    email = Email("test@example.com"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("TestUser"),
                    role = UserRole.USER,
                    status = UserStatus.ACTIVE,
                    createdAt = Instant.now(),
                    modifiedAt = Instant.now()
                )
                
                every { userRepository.findBy(UserId(userId)) } returns user
                
                // when
                val userDetails = userDetailsService.loadUserByUsername(userId)
                
                // then
                userDetails shouldNotBe null
                userDetails.username shouldBe userId
                userDetails.password shouldBe "encoded_password"
                userDetails.isEnabled shouldBe true
                userDetails.isAccountNonExpired shouldBe true
                userDetails.isAccountNonLocked shouldBe true
                userDetails.isCredentialsNonExpired shouldBe true
                userDetails.authorities.size shouldBe 1
                userDetails.authorities.first().authority shouldBe "ROLE_USER"
            }
            
            it("ADMIN 권한을 가진 사용자의 경우 ROLE_ADMIN을 반환해야 함") {
                // given
                val userId = "admin123"
                val admin = User(
                    id = UserId(userId),
                    email = Email("admin@example.com"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("Admin"),
                    role = UserRole.ADMIN,
                    status = UserStatus.ACTIVE,
                    createdAt = Instant.now(),
                    modifiedAt = Instant.now()
                )
                
                every { userRepository.findBy(UserId(userId)) } returns admin
                
                // when
                val userDetails = userDetailsService.loadUserByUsername(userId)
                
                // then
                userDetails.authorities.first().authority shouldBe "ROLE_ADMIN"
            }
            
            it("DELETED 상태의 사용자는 비활성화되어야 함") {
                // given
                val userId = "deleted123"
                val deletedUser = User(
                    id = UserId(userId),
                    email = Email("deleted@example.com"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("Deleted"),
                    role = UserRole.USER,
                    status = UserStatus.DELETED,
                    createdAt = Instant.now(),
                    modifiedAt = Instant.now()
                )
                
                every { userRepository.findBy(UserId(userId)) } returns deletedUser
                
                // when
                val userDetails = userDetailsService.loadUserByUsername(userId)
                
                // then
                userDetails.isEnabled shouldBe false
            }
            
            it("BANNED 상태의 사용자는 계정이 잠겨있어야 함") {
                // given
                val userId = "banned123"
                val bannedUser = User(
                    id = UserId(userId),
                    email = Email("banned@example.com"),
                    encodedPassword = EncodedPassword("encoded_password"),
                    nickname = Nickname("Banned"),
                    role = UserRole.USER,
                    status = UserStatus.BANNED,
                    createdAt = Instant.now(),
                    modifiedAt = Instant.now()
                )
                
                every { userRepository.findBy(UserId(userId)) } returns bannedUser
                
                // when
                val userDetails = userDetailsService.loadUserByUsername(userId)
                
                // then
                userDetails.isAccountNonLocked shouldBe false
            }
            
            it("존재하지 않는 사용자의 경우 UsernameNotFoundException을 던져야 함") {
                // given
                val userId = "nonexistent"
                
                every { userRepository.findBy(UserId(userId)) } returns null
                
                // when & then
                shouldThrow<UsernameNotFoundException> {
                    userDetailsService.loadUserByUsername(userId)
                }
            }
        }
    }
})