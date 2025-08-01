package me.helloc.techwikiplus.service.user.domain.model.value

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class NicknameTest : FunSpec({

    test("should create valid nickname") {
        val nickname = Nickname("user123")
        nickname.value shouldBe "user123"
    }

    test("should reject null or empty nickname") {
        shouldThrow<IllegalArgumentException> {
            Nickname("")
        }

        shouldThrow<IllegalArgumentException> {
            Nickname("   ")
        }
    }

    test("should reject nickname longer than 20 characters") {
        shouldThrow<IllegalArgumentException> {
            Nickname("thisnicknameislongerthan20chars")
        }
    }

    test("should accept nickname with exactly 20 characters") {
        val nickname = Nickname("12345678901234567890")
        nickname.value shouldBe "12345678901234567890"
    }

    test("should reject nickname shorter than 2 characters") {
        shouldThrow<IllegalArgumentException> {
            Nickname("a")
        }.message shouldBe "Nickname must be at least 2 characters"
    }

    test("should accept nickname with exactly 2 characters") {
        val nickname = Nickname("ab")
        nickname.value shouldBe "ab"
    }

    test("should reject nickname with spaces") {
        shouldThrow<IllegalArgumentException> {
            Nickname("user name")
        }.message shouldBe "Nickname cannot contain spaces"

        shouldThrow<IllegalArgumentException> {
            Nickname(" username")
        }.message shouldBe "Nickname cannot contain spaces"

        shouldThrow<IllegalArgumentException> {
            Nickname("username ")
        }.message shouldBe "Nickname cannot contain spaces"

        shouldThrow<IllegalArgumentException> {
            Nickname("user  name")
        }.message shouldBe "Nickname cannot contain spaces"
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
