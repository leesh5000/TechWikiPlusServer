package me.helloc.techwikiplus.service.common.infrastructure.security.jwt

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import me.helloc.techwikiplus.service.user.domain.model.Email
import me.helloc.techwikiplus.service.user.domain.model.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.Nickname
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.UserId
import me.helloc.techwikiplus.service.user.domain.model.UserRole
import me.helloc.techwikiplus.service.user.domain.model.UserStatus
import me.helloc.techwikiplus.service.user.domain.service.port.TokenManager
import me.helloc.techwikiplus.service.user.domain.service.port.UserRepository
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import java.time.Instant

class JwtAuthenticationFilterTest : DescribeSpec({

    lateinit var jwtTokenManager: TokenManager
    lateinit var userRepository: UserRepository
    lateinit var filter: JwtAuthenticationFilter
    lateinit var request: MockHttpServletRequest
    lateinit var response: MockHttpServletResponse
    lateinit var filterChain: FilterChain

    beforeEach {
        jwtTokenManager = mockk()
        userRepository = mockk()
        filter = JwtAuthenticationFilter(jwtTokenManager, userRepository)
        request = MockHttpServletRequest()
        response = MockHttpServletResponse()
        filterChain = mockk(relaxed = true)
        SecurityContextHolder.clearContext()
    }

    afterEach {
        SecurityContextHolder.clearContext()
    }

    describe("JwtAuthenticationFilter") {
        context("유효한 JWT 토큰이 있을 때") {
            it("SecurityContext에 인증 정보를 설정해야 함") {
                // given
                val token = "valid.jwt.token"
                val userId = UserId(123L)
                val user =
                    User(
                        id = userId,
                        email = Email("user@example.com"),
                        encodedPassword = EncodedPassword("encoded"),
                        nickname = Nickname("user"),
                        role = UserRole.USER,
                        status = UserStatus.ACTIVE,
                        createdAt = Instant.now(),
                        modifiedAt = Instant.now(),
                    )

                request.addHeader("Authorization", "Bearer $token")
                every { jwtTokenManager.validateAccessToken(token) } returns userId
                every { userRepository.findBy(userId) } returns user

                // when
                filter.doFilter(request, response, filterChain)

                // then
                val authentication = SecurityContextHolder.getContext().authentication
                authentication shouldNotBe null
                authentication?.principal shouldBe userId
                authentication?.isAuthenticated shouldBe true
                authentication?.authorities?.contains(SimpleGrantedAuthority("ROLE_USER")) shouldBe true

                verify { filterChain.doFilter(request, response) }
            }
        }

        context("Authorization 헤더가 없을 때") {
            it("SecurityContext를 설정하지 않고 다음 필터로 진행해야 함") {
                // given
                // 헤더를 추가하지 않음

                // when
                filter.doFilter(request, response, filterChain)

                // then
                val authentication = SecurityContextHolder.getContext().authentication
                authentication shouldBe null

                verify { filterChain.doFilter(request, response) }
            }
        }

        context("Bearer 접두사가 없을 때") {
            it("SecurityContext를 설정하지 않고 다음 필터로 진행해야 함") {
                // given
                request.addHeader("Authorization", "InvalidFormat token")

                // when
                filter.doFilter(request, response, filterChain)

                // then
                val authentication = SecurityContextHolder.getContext().authentication
                authentication shouldBe null

                verify { filterChain.doFilter(request, response) }
            }
        }

        context("유효하지 않은 JWT 토큰일 때") {
            it("SecurityContext를 설정하지 않고 다음 필터로 진행해야 함") {
                // given
                val invalidToken = "invalid.jwt.token"

                request.addHeader("Authorization", "Bearer $invalidToken")
                every { jwtTokenManager.validateAccessToken(invalidToken) } throws Exception("Invalid token")

                // when
                filter.doFilter(request, response, filterChain)

                // then
                val authentication = SecurityContextHolder.getContext().authentication
                authentication shouldBe null

                verify { filterChain.doFilter(request, response) }
            }
        }
    }
})
