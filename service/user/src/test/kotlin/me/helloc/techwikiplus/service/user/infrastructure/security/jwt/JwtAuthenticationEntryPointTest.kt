package me.helloc.techwikiplus.service.user.infrastructure.security.jwt

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import me.helloc.techwikiplus.service.user.interfaces.web.ErrorResponse
import me.helloc.techwikiplus.service.user.domain.exception.UserErrorCode
import me.helloc.techwikiplus.service.user.interfaces.web.UserErrorCodeMapper
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import java.io.PrintWriter

class JwtAuthenticationEntryPointTest : DescribeSpec({

    lateinit var entryPoint: JwtAuthenticationEntryPoint
    lateinit var objectMapper: ObjectMapper
    lateinit var userErrorCodeMapper: UserErrorCodeMapper
    lateinit var request: HttpServletRequest
    lateinit var response: HttpServletResponse
    lateinit var authException: AuthenticationException
    lateinit var writer: PrintWriter

    beforeEach {
        objectMapper = ObjectMapper().registerModule(JavaTimeModule())
        userErrorCodeMapper = UserErrorCodeMapper()
        entryPoint = JwtAuthenticationEntryPoint(objectMapper, userErrorCodeMapper)
        request = mockk()
        response = mockk(relaxed = true)
        authException = mockk()
        writer = mockk(relaxed = true)
    }

    describe("JwtAuthenticationEntryPoint") {
        context("UserStatusAuthenticationException이 전달될 때") {
            context("USER_DORMANT 에러일 때") {
                it("403 Forbidden과 함께 휴면 계정 메시지를 반환해야 함") {
                    // given
                    val userStatusException =
                        UserStatusAuthenticationException(
                            UserErrorCode.USER_DORMANT,
                            "User is dormant",
                        )
                    every { response.writer } returns writer

                    // when
                    entryPoint.commence(request, response, userStatusException)

                    // then
                    verify { response.status = HttpStatus.FORBIDDEN.value() }
                    verify { response.contentType = MediaType.APPLICATION_JSON_VALUE }
                    verify { response.characterEncoding = "UTF-8" }
                    verify {
                        writer.write(
                            match<String> { jsonString ->
                                val mapper =
                                    ObjectMapper()
                                        .registerModule(JavaTimeModule())
                                        .registerModule(KotlinModule.Builder().build())
                                val errorResponse = mapper.readValue(jsonString, ErrorResponse::class.java)
                                errorResponse.code == "USER_DORMANT" &&
                                    errorResponse.message == "휴면 계정입니다. 관리자에게 문의해주세요"
                            },
                        )
                    }
                }
            }

            context("USER_BANNED 에러일 때") {
                it("403 Forbidden과 함께 차단된 계정 메시지를 반환해야 함") {
                    // given
                    val userStatusException =
                        UserStatusAuthenticationException(
                            UserErrorCode.USER_BANNED,
                            "User is banned",
                        )
                    every { response.writer } returns writer

                    // when
                    entryPoint.commence(request, response, userStatusException)

                    // then
                    verify { response.status = HttpStatus.FORBIDDEN.value() }
                    verify { response.contentType = MediaType.APPLICATION_JSON_VALUE }
                    verify { response.characterEncoding = "UTF-8" }
                    verify {
                        writer.write(
                            match<String> { jsonString ->
                                val mapper =
                                    ObjectMapper()
                                        .registerModule(JavaTimeModule())
                                        .registerModule(KotlinModule.Builder().build())
                                val errorResponse = mapper.readValue(jsonString, ErrorResponse::class.java)
                                errorResponse.code == "USER_BANNED" &&
                                    errorResponse.message == "차단된 계정입니다. 관리자에게 문의해주세요"
                            },
                        )
                    }
                }
            }

            context("USER_PENDING 에러일 때") {
                it("403 Forbidden과 함께 인증 대기중 메시지를 반환해야 함") {
                    // given
                    val userStatusException =
                        UserStatusAuthenticationException(
                            UserErrorCode.USER_PENDING,
                            "User is pending",
                        )
                    every { response.writer } returns writer

                    // when
                    entryPoint.commence(request, response, userStatusException)

                    // then
                    verify { response.status = HttpStatus.FORBIDDEN.value() }
                    verify { response.contentType = MediaType.APPLICATION_JSON_VALUE }
                    verify { response.characterEncoding = "UTF-8" }
                    verify {
                        writer.write(
                            match<String> { jsonString ->
                                val mapper =
                                    ObjectMapper()
                                        .registerModule(JavaTimeModule())
                                        .registerModule(KotlinModule.Builder().build())
                                val errorResponse = mapper.readValue(jsonString, ErrorResponse::class.java)
                                errorResponse.code == "USER_PENDING" &&
                                    errorResponse.message == "인증 대기중인 계정입니다. 이메일 인증을 완료 후 다시 시도해주세요."
                            },
                        )
                    }
                }
            }

            context("USER_DELETED 에러일 때") {
                it("410 Gone과 함께 삭제된 계정 메시지를 반환해야 함") {
                    // given
                    val userStatusException =
                        UserStatusAuthenticationException(
                            UserErrorCode.USER_DELETED,
                            "User is deleted",
                        )
                    every { response.writer } returns writer

                    // when
                    entryPoint.commence(request, response, userStatusException)

                    // then
                    verify { response.status = HttpStatus.GONE.value() }
                    verify { response.contentType = MediaType.APPLICATION_JSON_VALUE }
                    verify { response.characterEncoding = "UTF-8" }
                    verify {
                        writer.write(
                            match<String> { jsonString ->
                                val mapper =
                                    ObjectMapper()
                                        .registerModule(JavaTimeModule())
                                        .registerModule(KotlinModule.Builder().build())
                                val errorResponse = mapper.readValue(jsonString, ErrorResponse::class.java)
                                errorResponse.code == "USER_DELETED" &&
                                    errorResponse.message == "이미 삭제된 계정입니다."
                            },
                        )
                    }
                }
            }

            context("USER_NOT_FOUND 에러일 때") {
                it("404 Not Found와 함께 사용자 없음 메시지를 반환해야 함") {
                    // given
                    val userStatusException =
                        UserStatusAuthenticationException(
                            UserErrorCode.USER_NOT_FOUND,
                            "User not found",
                        )
                    every { response.writer } returns writer

                    // when
                    entryPoint.commence(request, response, userStatusException)

                    // then
                    verify { response.status = HttpStatus.NOT_FOUND.value() }
                    verify { response.contentType = MediaType.APPLICATION_JSON_VALUE }
                    verify { response.characterEncoding = "UTF-8" }
                    verify {
                        writer.write(
                            match<String> { jsonString ->
                                val mapper =
                                    ObjectMapper()
                                        .registerModule(JavaTimeModule())
                                        .registerModule(KotlinModule.Builder().build())
                                val errorResponse = mapper.readValue(jsonString, ErrorResponse::class.java)
                                errorResponse.code == "USER_NOT_FOUND" &&
                                    errorResponse.message == "사용자를 찾을 수 없습니다"
                            },
                        )
                    }
                }
            }

            context("TOKEN_EXPIRED 에러일 때") {
                it("401 Unauthorized와 함께 토큰 만료 메시지를 반환해야 함") {
                    // given
                    val userStatusException =
                        UserStatusAuthenticationException(
                            UserErrorCode.TOKEN_EXPIRED,
                            "Token expired",
                        )
                    every { response.writer } returns writer

                    // when
                    entryPoint.commence(request, response, userStatusException)

                    // then
                    verify { response.status = HttpStatus.UNAUTHORIZED.value() }
                    verify { response.contentType = MediaType.APPLICATION_JSON_VALUE }
                    verify { response.characterEncoding = "UTF-8" }
                    verify {
                        writer.write(
                            match<String> { jsonString ->
                                val mapper =
                                    ObjectMapper()
                                        .registerModule(JavaTimeModule())
                                        .registerModule(KotlinModule.Builder().build())
                                val errorResponse = mapper.readValue(jsonString, ErrorResponse::class.java)
                                errorResponse.code == "TOKEN_EXPIRED" &&
                                    errorResponse.message == "만료된 토큰입니다"
                            },
                        )
                    }
                }
            }

            context("INVALID_TOKEN 에러일 때") {
                it("401 Unauthorized와 함께 유효하지 않은 토큰 메시지를 반환해야 함") {
                    // given
                    val userStatusException =
                        UserStatusAuthenticationException(
                            UserErrorCode.INVALID_TOKEN,
                            "Invalid token",
                        )
                    every { response.writer } returns writer

                    // when
                    entryPoint.commence(request, response, userStatusException)

                    // then
                    verify { response.status = HttpStatus.UNAUTHORIZED.value() }
                    verify { response.contentType = MediaType.APPLICATION_JSON_VALUE }
                    verify { response.characterEncoding = "UTF-8" }
                    verify {
                        writer.write(
                            match<String> { jsonString ->
                                val mapper =
                                    ObjectMapper()
                                        .registerModule(JavaTimeModule())
                                        .registerModule(KotlinModule.Builder().build())
                                val errorResponse = mapper.readValue(jsonString, ErrorResponse::class.java)
                                errorResponse.code == "INVALID_TOKEN" &&
                                    errorResponse.message == "유효하지 않은 토큰입니다"
                            },
                        )
                    }
                }
            }
        }

        context("일반 AuthenticationException이 전달될 때") {
            it("401 Unauthorized와 함께 기본 인증 필요 메시지를 반환해야 함") {
                // given
                every { authException.message } returns "Some authentication error"
                every { response.writer } returns writer

                // when
                entryPoint.commence(request, response, authException)

                // then
                verify { response.status = HttpStatus.UNAUTHORIZED.value() }
                verify { response.contentType = MediaType.APPLICATION_JSON_VALUE }
                verify { response.characterEncoding = "UTF-8" }
                verify {
                    writer.write(
                        match<String> { jsonString ->
                            val mapper =
                                ObjectMapper()
                                    .registerModule(JavaTimeModule())
                                    .registerModule(KotlinModule.Builder().build())
                            val errorResponse = mapper.readValue(jsonString, ErrorResponse::class.java)
                            errorResponse.code == "UNAUTHORIZED" &&
                                errorResponse.message == "Some authentication error"
                        },
                    )
                }
            }
        }

        context("예외가 null일 때") {
            it("401 Unauthorized와 함께 기본 메시지를 반환해야 함") {
                // given
                every { response.writer } returns writer

                // when
                entryPoint.commence(request, response, null)

                // then
                verify { response.status = HttpStatus.UNAUTHORIZED.value() }
                verify { response.contentType = MediaType.APPLICATION_JSON_VALUE }
                verify { response.characterEncoding = "UTF-8" }
                verify {
                    writer.write(
                        match<String> { jsonString ->
                            val mapper =
                                ObjectMapper()
                                    .registerModule(JavaTimeModule())
                                    .registerModule(KotlinModule.Builder().build())
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
                every { authException.message } returns null
                every { response.writer } returns writer

                // when
                entryPoint.commence(request, response, authException)

                // then
                verify { response.status = HttpStatus.UNAUTHORIZED.value() }
                verify { response.contentType = MediaType.APPLICATION_JSON_VALUE }
                verify { response.characterEncoding = "UTF-8" }
                verify {
                    writer.write(
                        match<String> { jsonString ->
                            val mapper =
                                ObjectMapper()
                                    .registerModule(JavaTimeModule())
                                    .registerModule(KotlinModule.Builder().build())
                            val errorResponse = mapper.readValue(jsonString, ErrorResponse::class.java)
                            errorResponse.code == "UNAUTHORIZED" &&
                                errorResponse.message == "인증이 필요합니다"
                        },
                    )
                }
            }
        }
    }
})
