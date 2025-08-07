package me.helloc.techwikiplus.service.user.infrastructure.security.jwt

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.helloc.techwikiplus.service.user.domain.model.value.UserId
import org.springframework.security.core.context.SecurityContextHolder

class JwtAuthenticationFilterTest : DescribeSpec({

    lateinit var jwtTokenManager: JwtTokenManager
    lateinit var filter: JwtAuthenticationFilter
    lateinit var request: HttpServletRequest
    lateinit var response: HttpServletResponse
    lateinit var filterChain: FilterChain

    beforeEach {
        jwtTokenManager = mockk()
        filter = JwtAuthenticationFilter(jwtTokenManager)
        request = mockk()
        response = mockk()
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
                val userId = UserId("user123")

                every { request.getHeader("Authorization") } returns "Bearer $token"
                every { jwtTokenManager.validateAccessToken(token) } returns userId

                // when
                filter.doFilter(request, response, filterChain)

                // then
                val authentication = SecurityContextHolder.getContext().authentication
                authentication shouldNotBe null
                authentication?.principal shouldBe userId
                authentication?.isAuthenticated shouldBe true

                verify { filterChain.doFilter(request, response) }
            }
        }

        context("Authorization 헤더가 없을 때") {
            it("SecurityContext를 설정하지 않고 다음 필터로 진행해야 함") {
                // given
                every { request.getHeader("Authorization") } returns null

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
                every { request.getHeader("Authorization") } returns "InvalidFormat token"

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

                every { request.getHeader("Authorization") } returns "Bearer $invalidToken"
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
