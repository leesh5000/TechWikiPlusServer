package me.helloc.techwikiplus.service.user.infrastructure.security.jwt

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import me.helloc.techwikiplus.service.user.domain.exception.UserDomainException
import me.helloc.techwikiplus.service.user.domain.exception.UserErrorCode
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
import org.springframework.security.web.AuthenticationEntryPoint
import java.time.Instant

class JwtAuthenticationFilterTest : DescribeSpec({

    lateinit var jwtTokenManager: TokenManager
    lateinit var userRepository: UserRepository
    lateinit var authenticationEntryPoint: AuthenticationEntryPoint
    lateinit var filter: JwtAuthenticationFilter
    lateinit var request: MockHttpServletRequest
    lateinit var response: MockHttpServletResponse
    lateinit var filterChain: FilterChain

    beforeEach {
        jwtTokenManager = mockk()
        userRepository = mockk()
        authenticationEntryPoint = mockk(relaxed = true)
        filter = JwtAuthenticationFilter(jwtTokenManager, userRepository, authenticationEntryPoint)
        request = MockHttpServletRequest()
        response = MockHttpServletResponse()
        filterChain = mockk(relaxed = true)
        SecurityContextHolder.clearContext()
    }

    afterEach {
        SecurityContextHolder.clearContext()
    }

    describe("JwtAuthenticationFilter") {
        context("유효한 JWT 토큰과 ACTIVE 상태 사용자일 때") {
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
                verify(exactly = 0) { authenticationEntryPoint.commence(any(), any(), any()) }
            }
        }

        context("유효한 JWT 토큰이지만 DORMANT 상태 사용자일 때") {
            it("AuthenticationEntryPoint를 호출하고 필터 체인을 중단해야 함") {
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
                        status = UserStatus.DORMANT,
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
                authentication shouldBe null

                verify {
                    authenticationEntryPoint.commence(
                        request,
                        response,
                        match<UserStatusAuthenticationException> {
                            it.errorCode == UserErrorCode.USER_DORMANT
                        },
                    )
                }
                verify(exactly = 0) { filterChain.doFilter(request, response) }
            }
        }

        context("유효한 JWT 토큰이지만 BANNED 상태 사용자일 때") {
            it("AuthenticationEntryPoint를 호출하고 필터 체인을 중단해야 함") {
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
                        status = UserStatus.BANNED,
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
                authentication shouldBe null

                verify {
                    authenticationEntryPoint.commence(
                        request,
                        response,
                        match<UserStatusAuthenticationException> {
                            it.errorCode == UserErrorCode.USER_BANNED
                        },
                    )
                }
                verify(exactly = 0) { filterChain.doFilter(request, response) }
            }
        }

        context("유효한 JWT 토큰이지만 PENDING 상태 사용자일 때") {
            it("AuthenticationEntryPoint를 호출하고 필터 체인을 중단해야 함") {
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
                        status = UserStatus.PENDING,
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
                authentication shouldBe null

                verify {
                    authenticationEntryPoint.commence(
                        request,
                        response,
                        match<UserStatusAuthenticationException> {
                            it.errorCode == UserErrorCode.USER_PENDING
                        },
                    )
                }
                verify(exactly = 0) { filterChain.doFilter(request, response) }
            }
        }

        context("유효한 JWT 토큰이지만 DELETED 상태 사용자일 때") {
            it("AuthenticationEntryPoint를 호출하고 필터 체인을 중단해야 함") {
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
                        status = UserStatus.DELETED,
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
                authentication shouldBe null

                verify {
                    authenticationEntryPoint.commence(
                        request,
                        response,
                        match<UserStatusAuthenticationException> {
                            it.errorCode == UserErrorCode.USER_DELETED
                        },
                    )
                }
                verify(exactly = 0) { filterChain.doFilter(request, response) }
            }
        }

        context("유효한 JWT 토큰이지만 사용자가 존재하지 않을 때") {
            it("AuthenticationEntryPoint를 호출하고 필터 체인을 중단해야 함") {
                // given
                val token = "valid.jwt.token"
                val userId = UserId(123L)

                request.addHeader("Authorization", "Bearer $token")
                every { jwtTokenManager.validateAccessToken(token) } returns userId
                every { userRepository.findBy(userId) } returns null

                // when
                filter.doFilter(request, response, filterChain)

                // then
                val authentication = SecurityContextHolder.getContext().authentication
                authentication shouldBe null

                verify {
                    authenticationEntryPoint.commence(
                        request,
                        response,
                        match<UserStatusAuthenticationException> {
                            it.errorCode == UserErrorCode.USER_NOT_FOUND
                        },
                    )
                }
                verify(exactly = 0) { filterChain.doFilter(request, response) }
            }
        }

        context("토큰이 만료되었을 때") {
            it("AuthenticationEntryPoint를 호출하고 필터 체인을 중단해야 함") {
                // given
                val expiredToken = "expired.jwt.token"

                request.addHeader("Authorization", "Bearer $expiredToken")
                every { jwtTokenManager.validateAccessToken(expiredToken) } throws
                    UserDomainException(UserErrorCode.TOKEN_EXPIRED, arrayOf("Access token"))

                // when
                filter.doFilter(request, response, filterChain)

                // then
                val authentication = SecurityContextHolder.getContext().authentication
                authentication shouldBe null

                verify {
                    authenticationEntryPoint.commence(
                        request,
                        response,
                        match<UserStatusAuthenticationException> {
                            it.errorCode == UserErrorCode.TOKEN_EXPIRED
                        },
                    )
                }
                verify(exactly = 0) { filterChain.doFilter(request, response) }
            }
        }

        context("유효하지 않은 토큰일 때") {
            it("AuthenticationEntryPoint를 호출하고 필터 체인을 중단해야 함") {
                // given
                val invalidToken = "invalid.jwt.token"

                request.addHeader("Authorization", "Bearer $invalidToken")
                every { jwtTokenManager.validateAccessToken(invalidToken) } throws
                    UserDomainException(UserErrorCode.INVALID_TOKEN, arrayOf("Invalid token"))

                // when
                filter.doFilter(request, response, filterChain)

                // then
                val authentication = SecurityContextHolder.getContext().authentication
                authentication shouldBe null

                verify {
                    authenticationEntryPoint.commence(
                        request,
                        response,
                        match<UserStatusAuthenticationException> {
                            it.errorCode == UserErrorCode.INVALID_TOKEN
                        },
                    )
                }
                verify(exactly = 0) { filterChain.doFilter(request, response) }
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
                verify(exactly = 0) { authenticationEntryPoint.commence(any(), any(), any()) }
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
                verify(exactly = 0) { authenticationEntryPoint.commence(any(), any(), any()) }
            }
        }

        context("예상치 못한 예외가 발생할 때") {
            it("SecurityContext를 설정하지 않고 다음 필터로 진행해야 함") {
                // given
                val token = "valid.jwt.token"

                request.addHeader("Authorization", "Bearer $token")
                every { jwtTokenManager.validateAccessToken(token) } throws RuntimeException("Unexpected error")

                // when
                filter.doFilter(request, response, filterChain)

                // then
                val authentication = SecurityContextHolder.getContext().authentication
                authentication shouldBe null

                verify { filterChain.doFilter(request, response) }
                verify(exactly = 0) { authenticationEntryPoint.commence(any(), any(), any()) }
            }
        }
    }
})
