package me.helloc.techwikiplus.service.user.infrastructure.security.jwt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.helloc.techwikiplus.service.user.interfaces.web.ErrorResponse
import org.springframework.security.core.AuthenticationException
import java.io.PrintWriter

class JwtAuthenticationEntryPointTest : DescribeSpec({

    lateinit var entryPoint: JwtAuthenticationEntryPoint
    lateinit var objectMapper: ObjectMapper
    lateinit var request: HttpServletRequest
    lateinit var response: HttpServletResponse
    lateinit var authException: AuthenticationException
    lateinit var writer: PrintWriter

    beforeEach {
        objectMapper = ObjectMapper().registerModule(JavaTimeModule())
        entryPoint = JwtAuthenticationEntryPoint(objectMapper)
        request = mockk()
        response = mockk(relaxed = true)
        authException = mockk()
        writer = mockk(relaxed = true)
    }

    describe("JwtAuthenticationEntryPoint") {
        context("인증 실패 시") {
            it("401 Unauthorized 응답을 반환해야 함") {
                // given
                every { request.requestURI } returns "/api/v1/users/profile"
                every { authException.message } returns "JWT token is expired"
                every { response.writer } returns writer

                // when
                entryPoint.commence(request, response, authException)

                // then
                verify { response.status = HttpServletResponse.SC_UNAUTHORIZED }
                verify { response.contentType = "application/json;charset=UTF-8" }
            }

            it("에러 응답 본문에 적절한 에러 정보를 포함해야 함") {
                // given
                val requestUri = "/api/v1/users/profile"
                val errorMessage = "JWT token is invalid"

                every { request.requestURI } returns requestUri
                every { authException.message } returns errorMessage
                every { response.writer } returns writer

                // when
                entryPoint.commence(request, response, authException)

                // then
                verify {
                    writer.write(
                        match<String> { jsonString ->
                            val mapper = ObjectMapper().registerModule(JavaTimeModule())
                            val errorResponse = mapper.readValue(jsonString, ErrorResponse::class.java)
                            errorResponse.code == "UNAUTHORIZED" &&
                                errorResponse.message == "인증이 필요합니다"
                        },
                    )
                }
            }
        }

        context("예외 메시지가 없을 때") {
            it("기본 메시지를 사용해야 함") {
                // given
                every { request.requestURI } returns "/api/v1/protected"
                every { authException.message } returns null
                every { response.writer } returns writer

                // when
                entryPoint.commence(request, response, authException)

                // then
                verify {
                    writer.write(
                        match<String> { jsonString ->
                            val mapper = ObjectMapper().registerModule(JavaTimeModule())
                            val errorResponse = mapper.readValue(jsonString, ErrorResponse::class.java)
                            errorResponse.message == "인증이 필요합니다"
                        },
                    )
                }
            }
        }
    }
})
