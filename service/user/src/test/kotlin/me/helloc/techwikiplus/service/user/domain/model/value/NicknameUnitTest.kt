package me.helloc.techwikiplus.service.user.domain.model.value

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.service.user.domain.exception.NicknameValidationException

class NicknameUnitTest : FunSpec({

    test("유효한 닉네임을 생성해야 한다") {
        val nickname = Nickname("user123")
        nickname.value shouldBe "user123"
    }

    test("null이거나 빈 닉네임을 거부해야 한다") {
        val exception1 =
            shouldThrow<NicknameValidationException> {
                Nickname("")
            }
        exception1.errorCode shouldBe NicknameValidationException.BLANK_NICKNAME
        exception1.field shouldBe "nickname"

        val exception2 =
            shouldThrow<NicknameValidationException> {
                Nickname("   ")
            }
        exception2.errorCode shouldBe NicknameValidationException.BLANK_NICKNAME
    }

    test("20자를 초과하는 닉네임을 거부해야 한다") {
        val exception =
            shouldThrow<NicknameValidationException> {
                Nickname("thisnicknameislongerthan20chars")
            }
        exception.errorCode shouldBe NicknameValidationException.TOO_LONG
        exception.field shouldBe "nickname"
    }

    test("정확히 20자인 닉네임을 허용해야 한다") {
        val nickname = Nickname("12345678901234567890")
        nickname.value shouldBe "12345678901234567890"
    }

    test("2자 미만의 닉네임을 거부해야 한다") {
        val exception =
            shouldThrow<NicknameValidationException> {
                Nickname("a")
            }
        exception.errorCode shouldBe NicknameValidationException.TOO_SHORT
        exception.message shouldBe "닉네임은 최소 2자 이상이어야 합니다"
    }

    test("정확히 2자인 닉네임을 허용해야 한다") {
        val nickname = Nickname("ab")
        nickname.value shouldBe "ab"
    }

    test("공백이 포함된 닉네임을 거부해야 한다") {
        val exception1 =
            shouldThrow<NicknameValidationException> {
                Nickname("user name")
            }
        exception1.errorCode shouldBe NicknameValidationException.CONTAINS_SPACE
        exception1.message shouldBe "닉네임에는 공백을 포함할 수 없습니다"

        val exception2 =
            shouldThrow<NicknameValidationException> {
                Nickname(" username")
            }
        exception2.errorCode shouldBe NicknameValidationException.CONTAINS_SPACE

        val exception3 =
            shouldThrow<NicknameValidationException> {
                Nickname("username ")
            }
        exception3.errorCode shouldBe NicknameValidationException.CONTAINS_SPACE

        val exception4 =
            shouldThrow<NicknameValidationException> {
                Nickname("user  name")
            }
        exception4.errorCode shouldBe NicknameValidationException.CONTAINS_SPACE
    }

    test("영숫자 문자가 포함된 닉네임을 허용해야 한다") {
        Nickname("abc123").value shouldBe "abc123"
        Nickname("ABC123").value shouldBe "ABC123"
        Nickname("User2024").value shouldBe "User2024"
    }

    test("언더스코어와 하이픈을 포함한 닉네임을 허용해야 한다") {
        Nickname("user_123").value shouldBe "user_123"
        Nickname("user-123").value shouldBe "user-123"
        Nickname("닉네임_123").value shouldBe "닉네임_123"
        Nickname("닉네임-123").value shouldBe "닉네임-123"
    }

    test("허용되지 않는 특수문자가 포함된 경우 예외를 던져야 한다") {
        shouldThrow<NicknameValidationException> {
            Nickname("user.123")
        }.apply {
            errorCode shouldBe NicknameValidationException.CONTAINS_SPECIAL_CHAR
            message shouldBe "닉네임은 한글, 영문, 숫자, 언더스코어(_), 하이픈(-)만 사용할 수 있습니다"
        }

        shouldThrow<NicknameValidationException> {
            Nickname("user@123")
        }.apply {
            errorCode shouldBe NicknameValidationException.CONTAINS_SPECIAL_CHAR
        }

        shouldThrow<NicknameValidationException> {
            Nickname("user!123")
        }.apply {
            errorCode shouldBe NicknameValidationException.CONTAINS_SPECIAL_CHAR
        }

        shouldThrow<NicknameValidationException> {
            Nickname("user#123")
        }.apply {
            errorCode shouldBe NicknameValidationException.CONTAINS_SPECIAL_CHAR
        }
    }

    test("불변 객체여야 한다") {
        val nickname = Nickname("user123")
        nickname.value shouldBe "user123"
        // Value should not be modifiable (enforced by val property)
    }

    test("equals를 올바르게 구현해야 한다") {
        val nickname1 = Nickname("user123")
        val nickname2 = Nickname("user123")
        val nickname3 = Nickname("user456")

        nickname1 shouldBe nickname2
        nickname1 shouldNotBe nickname3
        nickname1 shouldNotBe null
        nickname1 shouldNotBe "user123"
    }

    test("hashCode를 올바르게 구현해야 한다") {
        val nickname1 = Nickname("user123")
        val nickname2 = Nickname("user123")
        val nickname3 = Nickname("user456")

        nickname1.hashCode() shouldBe nickname2.hashCode()
        nickname1.hashCode() shouldNotBe nickname3.hashCode()
    }

    test("toString을 올바르게 구현해야 한다") {
        val nickname = Nickname("user123")
        nickname.toString() shouldBe "Nickname(value=user123)"
    }
})
