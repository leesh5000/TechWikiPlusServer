package me.helloc.techwikiplus.service.user.domain.model.value

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.service.user.domain.exception.NicknameValidationException

class NicknameTest : FunSpec({

    test("should create valid nickname") {
        val nickname = Nickname("user123")
        nickname.value shouldBe "user123"
    }

    test("should reject null or empty nickname") {
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

    test("should reject nickname longer than 20 characters") {
        val exception =
            shouldThrow<NicknameValidationException> {
                Nickname("thisnicknameislongerthan20chars")
            }
        exception.errorCode shouldBe NicknameValidationException.TOO_LONG
        exception.field shouldBe "nickname"
    }

    test("should accept nickname with exactly 20 characters") {
        val nickname = Nickname("12345678901234567890")
        nickname.value shouldBe "12345678901234567890"
    }

    test("should reject nickname shorter than 2 characters") {
        val exception =
            shouldThrow<NicknameValidationException> {
                Nickname("a")
            }
        exception.errorCode shouldBe NicknameValidationException.TOO_SHORT
        exception.message shouldBe "닉네임은 최소 2자 이상이어야 합니다"
    }

    test("should accept nickname with exactly 2 characters") {
        val nickname = Nickname("ab")
        nickname.value shouldBe "ab"
    }

    test("should reject nickname with spaces") {
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

    test("should accept nickname with alphanumeric characters") {
        Nickname("abc123").value shouldBe "abc123"
        Nickname("ABC123").value shouldBe "ABC123"
        Nickname("User2024").value shouldBe "User2024"
    }

    test("should accept nickname with special characters") {
        Nickname("user_123").value shouldBe "user_123"
        Nickname("user-123").value shouldBe "user-123"
        Nickname("user.123").value shouldBe "user.123"
        Nickname("user@123").value shouldBe "user@123"
        Nickname("user!123").value shouldBe "user!123"
        Nickname("닉네임123").value shouldBe "닉네임123"
    }

    test("should be immutable") {
        val nickname = Nickname("user123")
        nickname.value shouldBe "user123"
        // Value should not be modifiable (enforced by val property)
    }

    test("should implement equals correctly") {
        val nickname1 = Nickname("user123")
        val nickname2 = Nickname("user123")
        val nickname3 = Nickname("user456")

        nickname1 shouldBe nickname2
        nickname1 shouldNotBe nickname3
        nickname1 shouldNotBe null
        nickname1 shouldNotBe "user123"
    }

    test("should implement hashCode correctly") {
        val nickname1 = Nickname("user123")
        val nickname2 = Nickname("user123")
        val nickname3 = Nickname("user456")

        nickname1.hashCode() shouldBe nickname2.hashCode()
        nickname1.hashCode() shouldNotBe nickname3.hashCode()
    }

    test("should implement toString correctly") {
        val nickname = Nickname("user123")
        nickname.toString() shouldBe "Nickname(value=user123)"
    }
})
