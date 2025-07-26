package me.helloc.techwikiplus.user.documentation

import com.fasterxml.jackson.databind.ObjectMapper
import me.helloc.techwikiplus.user.domain.port.outbound.MailSender
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.restdocs.RestDocumentationExtension
import org.springframework.restdocs.headers.HeaderDocumentation.headerWithName
import org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders
import org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest
import org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse
import org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint
import org.springframework.restdocs.payload.FieldDescriptor
import org.springframework.restdocs.payload.JsonFieldType
import org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath
import org.springframework.restdocs.request.ParameterDescriptor
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions

/**
 * API 문서화 테스트를 위한 베이스 클래스
 *
 * Spring REST Docs와 restdocs-api-spec을 사용하여
 * 테스트 기반 API 문서를 생성합니다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@ExtendWith(RestDocumentationExtension::class)
@ActiveProfiles("test")
abstract class ApiDocumentationTestSupport {
    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @MockitoBean
    protected lateinit var mailSender: MailSender

    @BeforeEach
    fun setUpMocks() {
        // 이메일 발송은 Mock 처리
        `when`(mailSender.sendVerificationEmail(anyString())).thenReturn(
            me.helloc.techwikiplus.user.domain.VerificationCode("123456"),
        )
    }

    /**
     * REST Docs 문서화 헬퍼
     */
    protected fun documentApi(identifier: String): RestDocumentationResultHandler {
        return document(
            identifier,
            preprocessRequest(prettyPrint()),
            preprocessResponse(prettyPrint()),
        )
    }

    /**
     * 공통 응답 헤더
     */
    protected fun commonResponseHeaders() =
        responseHeaders(
            headerWithName("Content-Type").description("응답 컨텐츠 타입"),
        )

    /**
     * 인증 요청 헤더
     */
    protected fun authRequestHeaders() =
        requestHeaders(
            headerWithName("Authorization").description("Bearer JWT 토큰"),
        )

    /**
     * 에러 응답 필드
     */
    protected fun errorResponseFields() =
        listOf(
            fieldWithPath("errorCode").type(JsonFieldType.STRING).description("에러 코드"),
            fieldWithPath("message").type(JsonFieldType.STRING).description("에러 메시지"),
            fieldWithPath("timestamp").type(JsonFieldType.STRING).description("에러 발생 시간"),
            fieldWithPath("path").type(JsonFieldType.STRING).description("요청 경로"),
            fieldWithPath("localizedMessage").type(JsonFieldType.STRING).description("현지화된 메시지").optional(),
            fieldWithPath("details").type(JsonFieldType.OBJECT).description("추가 에러 정보").optional(),
        )

    /**
     * POST 요청 수행
     */
    protected fun performPost(
        url: String,
        content: Any? = null,
    ): ResultActions {
        val request =
            RestDocumentationRequestBuilders.post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)

        content?.let {
            request.content(objectMapper.writeValueAsString(it))
        }

        return mockMvc.perform(request)
    }

    /**
     * GET 요청 수행
     */
    protected fun performGet(
        url: String,
        token: String? = null,
    ): ResultActions {
        val request =
            RestDocumentationRequestBuilders.get(url)
                .accept(MediaType.APPLICATION_JSON)

        token?.let {
            request.header("Authorization", "Bearer $it")
        }

        return mockMvc.perform(request)
    }

    /**
     * 필드 설명 생성 헬퍼 메서드
     */
    protected fun field(
        path: String,
        description: String,
        type: JsonFieldType = JsonFieldType.STRING,
        optional: Boolean = false,
    ): FieldDescriptor {
        val field = fieldWithPath(path).type(type).description(description)
        return if (optional) field.optional() else field
    }

    /**
     * 파라미터 설명 생성 헬퍼 메서드
     */
    protected fun param(
        name: String,
        description: String,
        optional: Boolean = false,
    ): ParameterDescriptor {
        val param =
            org.springframework.restdocs.request.RequestDocumentation
                .parameterWithName(name)
                .description(description)
        return if (optional) param.optional() else param
    }
}
