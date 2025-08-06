package me.helloc.techwikiplus.service.user.interfaces.web.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class XssPreventionTest {
    private val sanitizer = InputSanitizer()

    @ParameterizedTest
    @DisplayName("XSS 페이로드가 URI에서 적절히 살균되는지 확인")
    @ValueSource(
        strings = [
            "<script>alert('XSS')</script>",
            "javascript:alert('XSS')",
            "<img src=x onerror=alert('XSS')>",
            "<iframe src='evil.com'></iframe>",
            "';alert('XSS');//",
            "<svg onload=alert('XSS')>",
            "<body onload=alert('XSS')>",
            "<style>body{background:url('javascript:alert(1)')}</style>",
            "<<SCRIPT>alert('XSS');//<</SCRIPT>",
            "<IMG SRC=javascript:alert('XSS')>",
            "<IMG SRC=JaVaScRiPt:alert('XSS')>",
            "<IMG SRC=`javascript:alert('XSS')`>",
            "<SCRIPT/XSS SRC='http://evil.com/xss.js'></SCRIPT>",
            "<BODY ONLOAD=alert('XSS')>",
            "<INPUT TYPE='IMAGE' SRC='javascript:alert('XSS');'>",
            "<LINK REL='stylesheet' HREF='javascript:alert('XSS');'>",
            "eval(String.fromCharCode(97,108,101,114,116,40,39,88,83,83,39,41))",
            "document.cookie",
            "localStorage.getItem('token')",
            "window.location='http://evil.com'",
        ],
    )
    fun `should sanitize XSS payloads in URI`(maliciousInput: String) {
        val sanitized = sanitizer.sanitizeUri(maliciousInput)

        assertThat(sanitized).isNotNull()
        // HTML 이스케이프가 적용되었는지 확인
        assertThat(sanitized).doesNotContain("<script")
        assertThat(sanitized).doesNotContain("</script")
        assertThat(sanitized).doesNotContain("javascript:")
        assertThat(sanitized).doesNotContain("onerror=")
        assertThat(sanitized).doesNotContain("onload=")
        assertThat(sanitized).doesNotContain("<iframe")
        assertThat(sanitized).doesNotContain("<svg")
        assertThat(sanitized).doesNotContain("eval(")
        assertThat(sanitized).doesNotContain("document.")
        assertThat(sanitized).doesNotContain("window.")
        assertThat(sanitized).doesNotContain("localStorage")

        // 기본 HTML 문자가 이스케이프되었는지 확인
        if (maliciousInput.contains("<")) {
            assertThat(sanitized).contains("&lt;")
        }
        if (maliciousInput.contains(">")) {
            assertThat(sanitized).contains("&gt;")
        }
    }

    @Test
    @DisplayName("과도하게 긴 입력이 적절히 절단되는지 확인")
    fun `should truncate excessively long inputs`() {
        val longUri = "a".repeat(3000)
        val longMessage = "b".repeat(1000)
        val longField = "c".repeat(200)

        val sanitizedUri = sanitizer.sanitizeUri(longUri)
        val sanitizedMessage = sanitizer.sanitizeMessage(longMessage)
        val sanitizedField = sanitizer.sanitizeFieldName(longField)

        assertThat(sanitizedUri).isNotNull()
        assertThat(sanitizedUri!!.length).isLessThanOrEqualTo(2000)

        assertThat(sanitizedMessage).isNotNull()
        assertThat(sanitizedMessage!!.length).isLessThanOrEqualTo(500)

        assertThat(sanitizedField).isNotNull()
        assertThat(sanitizedField!!.length).isLessThanOrEqualTo(100)
    }

    @Test
    @DisplayName("제어 문자가 제거되는지 확인")
    fun `should remove control characters`() {
        val inputWithControlChars = "Hello\x00World\x1F\x7FTest\x0D\x0A"
        val sanitized = sanitizer.sanitizeMessage(inputWithControlChars)

        assertThat(sanitized).isNotNull()
        assertThat(sanitized).doesNotContain("\x00")
        assertThat(sanitized).doesNotContain("\x1F")
        assertThat(sanitized).doesNotContain("\x7F")
        assertThat(sanitized).doesNotContain("\x0D")
        assertThat(sanitized).doesNotContain("\x0A")
        assertThat(sanitized).isEqualTo("HelloWorldTest")
    }

    @Test
    @DisplayName("안전한 입력은 기본 HTML 이스케이프만 적용되는지 확인")
    fun `should only apply basic HTML escape for safe input`() {
        val safeInput = "/api/users/123"
        val sanitized = sanitizer.sanitizeUri(safeInput)

        assertThat(sanitized).isNotNull()
        // 슬래시가 이스케이프되는지 확인
        assertThat(sanitized).contains("&#x2F;")
        assertThat(sanitized).doesNotContain("/")
    }

    @Test
    @DisplayName("특수 HTML 문자가 적절히 이스케이프되는지 확인")
    fun `should escape special HTML characters`() {
        val input = "Test & <tag> \"quotes\" 'apostrophe' /slash"
        val sanitized = sanitizer.sanitizeMessage(input)

        assertThat(sanitized).isNotNull()
        assertThat(sanitized).contains("&amp;")
        assertThat(sanitized).contains("&lt;")
        assertThat(sanitized).contains("&gt;")
        assertThat(sanitized).contains("&quot;")
        assertThat(sanitized).contains("&#x27;")
        assertThat(sanitized).contains("&#x2F;")
    }

    @Test
    @DisplayName("빈 입력이 올바르게 처리되는지 확인")
    fun `should handle empty input correctly`() {
        assertThat(sanitizer.sanitizeUri(null)).isNull()
        assertThat(sanitizer.sanitizeUri("")).isEqualTo("")

        assertThat(sanitizer.sanitizeMessage(null)).isNull()
        assertThat(sanitizer.sanitizeMessage("")).isEqualTo("")

        assertThat(sanitizer.sanitizeFieldName(null)).isNull()
        assertThat(sanitizer.sanitizeFieldName("")).isEqualTo("")
    }

    @Test
    @DisplayName("맵 형태의 상세 정보가 올바르게 살균되는지 확인")
    fun `should sanitize details map correctly`() {
        val details =
            mapOf(
                "field1" to "<script>alert('XSS')</script>",
                "field2" to "normal value",
                "field3" to 123,
                "field4" to "javascript:alert('XSS')",
            )

        val sanitized = sanitizer.sanitizeDetails(details)

        assertThat(sanitized).isNotNull()
        assertThat(sanitized!!["field1"] as String).contains("&lt;script&gt;")
        assertThat(sanitized["field2"] as String).isEqualTo("normal value")
        assertThat(sanitized["field3"] as Int).isEqualTo(123)
        assertThat(sanitized["field4"] as String).contains("javascript")
        assertThat(sanitized["field4"] as String).doesNotContain("javascript:")
    }

    @Test
    @DisplayName("대소문자 혼합 XSS 패턴이 감지되는지 확인")
    fun `should detect mixed case XSS patterns`() {
        val mixedCasePayloads =
            listOf(
                "JaVaScRiPt:alert('XSS')",
                "<ScRiPt>alert('XSS')</ScRiPt>",
                "OnClIcK=alert('XSS')",
                "<IfRaMe src='evil.com'></IfRaMe>",
            )

        mixedCasePayloads.forEach { payload ->
            val sanitized = sanitizer.sanitizeMessage(payload)
            assertThat(sanitized).isNotNull()
            // 위험 패턴이 감지되어 HTML 이스케이프가 적용되었는지 확인
            assertThat(sanitized).contains("&lt;")
            assertThat(sanitized).contains("&gt;")
        }
    }

    @Test
    @DisplayName("인코딩된 XSS 페이로드가 감지되는지 확인")
    fun `should handle encoded XSS payloads`() {
        val encodedPayloads =
            listOf(
                "%3Cscript%3Ealert('XSS')%3C/script%3E",
                "&#60;script&#62;alert('XSS')&#60;/script&#62;",
                "\\x3cscript\\x3ealert('XSS')\\x3c/script\\x3e",
            )

        encodedPayloads.forEach { payload ->
            val sanitized = sanitizer.sanitizeUri(payload)
            assertThat(sanitized).isNotNull()
            // 인코딩된 문자들이 그대로 유지되면서 추가 이스케이프가 적용되는지 확인
            assertThat(sanitized).doesNotContain("<script")
        }
    }

    @Test
    @DisplayName("SQL Injection 시도가 기본 이스케이프로 방어되는지 확인")
    fun `should provide basic protection against SQL injection`() {
        val sqlPayloads =
            listOf(
                "' OR '1'='1",
                "'; DROP TABLE users; --",
                "1' UNION SELECT * FROM users --",
            )

        sqlPayloads.forEach { payload ->
            val sanitized = sanitizer.sanitizeFieldName(payload)
            assertThat(sanitized).isNotNull()
            // 작은따옴표가 이스케이프되는지 확인
            assertThat(sanitized).contains("&#x27;")
            assertThat(sanitized).doesNotContain("'")
        }
    }
}
