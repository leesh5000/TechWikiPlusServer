package me.helloc.techwikiplus.user.interfaces.http

import me.helloc.techwikiplus.user.domain.exception.authentication.InvalidCredentialsException
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody

/**
 * GlobalExceptionHandler 테스트를 위한 더미 컨트롤러
 */
@Controller
@RequestMapping("/test/exception-handler")
class TestControllerForExceptionHandler {
    @PostMapping("/login")
    @ResponseBody
    fun login(
        @RequestBody request: Map<String, String>,
    ): Map<String, String> {
        // 이메일 형식이 올바르지 않은 경우를 시뮬레이션
        if (request["email"] == "invalid-email-format") {
            throw InvalidCredentialsException()
        }
        return mapOf("result" to "success")
    }

    @GetMapping("/verify")
    @ResponseBody
    fun verify(
        @RequestParam email: String,
        @RequestParam code: String,
    ): Map<String, String> {
        // 파라미터 검증 로직은 Spring이 자동으로 처리
        return mapOf("result" to "success")
    }
}
