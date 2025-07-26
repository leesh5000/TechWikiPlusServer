package me.helloc.techwikiplus.user.documentation.controller

import com.epages.restdocs.apispec.MockMvcRestDocumentationWrapper
import me.helloc.techwikiplus.user.documentation.ApiDocumentationTestSupport
import me.helloc.techwikiplus.user.interfaces.http.dto.request.UserSignUpRequest
import org.junit.jupiter.api.Test
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.payload.PayloadDocumentation.requestFields
import org.springframework.restdocs.payload.PayloadDocumentation.responseFields
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 회원가입 API 문서화 테스트
 */
class SimpleUserSignUpDocumentationTest : ApiDocumentationTestSupport() {
    @Test
    fun `회원가입 성공`() {
        // given
        val request =
            UserSignUpRequest(
                email = "test@example.com",
                nickname = "testuser",
                password = "ValidPass123!",
            )

        // when & then
        performPost("/api/v1/users/signup", request)
            .andExpect(status().isAccepted)
            .andExpect(header().exists("Location"))
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    "user-signup-success",
                    "새로운 사용자를 등록합니다. 등록 후 이메일 인증이 필요합니다.",
                    false,
                    requestFields(
                        fieldWithPath("email").type(JsonFieldType.STRING).description("이메일 주소"),
                        fieldWithPath("nickname").type(JsonFieldType.STRING).description("사용자 닉네임 (2-20자)"),
                        fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호 (8자 이상, 영문/숫자/특수문자 포함)"),
                    ),
                    responseHeaders(
                        headerWithName("Location").description("이메일 인증 엔드포인트 경로"),
                    ),
                ),
            )
    }

    @Test
    fun `회원가입 실패 - 잘못된 이메일 형식`() {
        // given
        val request =
            UserSignUpRequest(
                email = "invalid-email",
                nickname = "testuser",
                password = "ValidPass123!",
            )

        // when & then
        performPost("/api/v1/users/signup", request)
            .andExpect(status().isBadRequest)
            .andDo(
                MockMvcRestDocumentationWrapper.document(
                    "user-signup-invalid-email",
                    "잘못된 이메일 형식으로 회원가입 시도 시 발생하는 에러",
                    false,
                    requestFields(
                        fieldWithPath("email").type(JsonFieldType.STRING).description("이메일 주소"),
                        fieldWithPath("nickname").type(JsonFieldType.STRING).description("사용자 닉네임"),
                        fieldWithPath("password").type(JsonFieldType.STRING).description("비밀번호"),
                    ),
                    responseFields(
                        fieldWithPath("errorCode").type(JsonFieldType.STRING).description("에러 코드"),
                        fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
                        fieldWithPath("timestamp").type(JsonFieldType.STRING).description("에러 발생 시간"),
                        fieldWithPath("path").type(JsonFieldType.STRING).description("요청 경로"),
                        fieldWithPath("localizedMessage").type(JsonFieldType.STRING).description("현지화된 메시지").optional(),
                        fieldWithPath("details").type(JsonFieldType.OBJECT).description("추가 에러 정보").optional(),
                    ),
                ),
            )
    }
}
