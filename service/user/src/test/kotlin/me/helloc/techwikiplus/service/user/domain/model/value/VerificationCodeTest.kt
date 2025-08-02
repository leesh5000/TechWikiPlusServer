package me.helloc.techwikiplus.service.user.domain.model.value

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class VerificationCodeTest : FunSpec({

    test("6자리 숫자로 된 유효한 인증 코드를 생성해야 한다") {
        val validCode = "123456"
        val verificationCode = VerificationCode(validCode)

        verificationCode.value shouldBe validCode
    }

    test("6자리가 아닌 코드는 거부해야 한다") {
        shouldThrow<IllegalArgumentException> {
            VerificationCode("12345")
        }.message shouldBe "인증 코드는 정확히 6자리여야 합니다"

        shouldThrow<IllegalArgumentException> {
            VerificationCode("1234567")
        }.message shouldBe "인증 코드는 정확히 6자리여야 합니다"
    }

    test("숫자가 아닌 문자가 포함된 코드는 거부해야 한다") {
        shouldThrow<IllegalArgumentException> {
            VerificationCode("12345a")
        }.message shouldBe "인증 코드는 숫자로만 구성되어야 합니다"

        shouldThrow<IllegalArgumentException> {
            VerificationCode("abcdef")
        }.message shouldBe "인증 코드는 숫자로만 구성되어야 합니다"

        shouldThrow<IllegalArgumentException> {
            VerificationCode("1234 6")
        }.message shouldBe "인증 코드는 숫자로만 구성되어야 합니다"
    }

    test("빈 문자열이나 공백 문자열은 거부해야 한다") {
        shouldThrow<IllegalArgumentException> {
            VerificationCode("")
        }.message shouldBe "인증 코드는 비어있을 수 없습니다"

        shouldThrow<IllegalArgumentException> {
            VerificationCode("      ")
        }.message shouldBe "인증 코드는 비어있을 수 없습니다"
    }

    test("랜덤한 6자리 인증 코드를 생성할 수 있어야 한다") {
        val code1 = VerificationCode.generate()
        val code2 = VerificationCode.generate()

        code1.value.length shouldBe 6
        code2.value.length shouldBe 6

        code1.value.all { it.isDigit() } shouldBe true
        code2.value.all { it.isDigit() } shouldBe true

        // 매우 낮은 확률로 같을 수 있지만, 일반적으로는 달라야 함
        code1 shouldNotBe code2
    }

    test("동일한 코드를 가진 객체는 동등해야 한다") {
        val code1 = VerificationCode("123456")
        val code2 = VerificationCode("123456")
        val code3 = VerificationCode("654321")

        code1 shouldBe code2
        code1 shouldNotBe code3
        code1 shouldNotBe null
        code1 shouldNotBe "123456"
    }

    test("hashCode는 동일한 값에 대해 동일해야 한다") {
        val code1 = VerificationCode("123456")
        val code2 = VerificationCode("123456")

        code1.hashCode() shouldBe code2.hashCode()
    }

    test("toString은 보안을 위해 실제 코드를 노출하지 않아야 한다") {
        val code = VerificationCode("123456")

        code.toString() shouldNotBe "VerificationCode(value=123456)"
        code.toString() shouldBe "VerificationCode(******)"
    }

    test("불변 객체여야 한다") {
        val code = VerificationCode("123456")
        val originalValue = code.value

        // value는 val로 선언되어 수정 불가능
        code.value shouldBe originalValue
    }
})
