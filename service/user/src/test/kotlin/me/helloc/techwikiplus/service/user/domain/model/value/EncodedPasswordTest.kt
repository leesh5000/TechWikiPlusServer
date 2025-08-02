package me.helloc.techwikiplus.service.user.domain.model.value

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.service.user.domain.exception.PasswordValidationException
import me.helloc.techwikiplus.service.user.domain.service.port.PasswordEncoder

class EncodedPasswordTest : FunSpec({

    val testPasswordEncoder =
        object : PasswordEncoder {
            override fun encode(rawPassword: String): String {
                // Simple test encoder that creates a fixed 60 char hash
                val hash = rawPassword.hashCode().toString()
                return "encoded:$hash".padEnd(60, '*')
            }

            override fun matches(
                rawPassword: String,
                encodedPassword: String,
            ): Boolean {
                return encodedPassword == encode(rawPassword)
            }
        }

    test("should create encrypted password from raw password") {
        // validateRawPasswordPolicy only validates, doesn't return EncodedPassword
        EncodedPassword.validateRawPasswordPolicy("mySecretPassword123!")

        // Create EncodedPassword with encoded value
        val encodedValue = testPasswordEncoder.encode("mySecretPassword123!")
        val encodedPassword = EncodedPassword(encodedValue)
        encodedPassword.value shouldNotBe "mySecretPassword123!"
        encodedPassword.value.length shouldBe 60
        encodedPassword.value.startsWith("encoded:") shouldBe true
    }

    test("should reject null or empty raw password") {
        val exception1 =
            shouldThrow<PasswordValidationException> {
                EncodedPassword.validateRawPasswordPolicy("")
            }
        exception1.errorCode shouldBe PasswordValidationException.BLANK_PASSWORD
        exception1.field shouldBe "password"

        val exception2 =
            shouldThrow<PasswordValidationException> {
                EncodedPassword.validateRawPasswordPolicy("   ")
            }
        exception2.errorCode shouldBe PasswordValidationException.BLANK_PASSWORD
    }

    test("should reject password shorter than 8 characters") {
        val exception =
            shouldThrow<PasswordValidationException> {
                EncodedPassword.validateRawPasswordPolicy("Short1!")
            }
        exception.errorCode shouldBe PasswordValidationException.TOO_SHORT
        exception.message shouldBe "비밀번호는 최소 8자 이상이어야 합니다"
    }

    test("should reject password longer than 30 characters") {
        val exception =
            shouldThrow<PasswordValidationException> {
                EncodedPassword.validateRawPasswordPolicy("VeryLongPassword1234567890123!@")
            }
        exception.errorCode shouldBe PasswordValidationException.TOO_LONG
        exception.message shouldBe "비밀번호는 최대 30자 이하여야 합니다"
    }

    test("should reject password without uppercase letter") {
        val exception =
            shouldThrow<PasswordValidationException> {
                EncodedPassword.validateRawPasswordPolicy("lowercase123!")
            }
        exception.errorCode shouldBe PasswordValidationException.NO_UPPERCASE
        exception.message shouldBe "비밀번호는 대문자를 포함해야 합니다"
    }

    test("should reject password without lowercase letter") {
        val exception =
            shouldThrow<PasswordValidationException> {
                EncodedPassword.validateRawPasswordPolicy("UPPERCASE123!")
            }
        exception.errorCode shouldBe PasswordValidationException.NO_LOWERCASE
        exception.message shouldBe "비밀번호는 소문자를 포함해야 합니다"
    }

    test("should reject password without special character") {
        val exception =
            shouldThrow<PasswordValidationException> {
                EncodedPassword.validateRawPasswordPolicy("Password123")
            }
        exception.errorCode shouldBe PasswordValidationException.NO_SPECIAL_CHAR
        exception.message shouldBe "비밀번호는 특수문자를 포함해야 합니다"
    }

    test("should accept valid password with all requirements") {
        val validPasswords =
            listOf(
                "Password123!",
                "MySecret@123",
                "Qwerty!234",
                "AbCdEf@123",
                "Test1234$",
            )

        validPasswords.forEach { rawPassword ->
            // This should not throw
            EncodedPassword.validateRawPasswordPolicy(rawPassword)

            val encodedValue = testPasswordEncoder.encode(rawPassword)
            val encodedPassword = EncodedPassword(encodedValue)
            encodedPassword.value shouldNotBe rawPassword
        }
    }

    test("should verify correct password") {
        val rawPassword = "mySecretPassword123!"
        EncodedPassword.validateRawPasswordPolicy(rawPassword)

        val encodedValue = testPasswordEncoder.encode(rawPassword)
        val encodedPassword = EncodedPassword(encodedValue)

        // Test password matching using encoder directly
        testPasswordEncoder.matches(rawPassword, encodedPassword.value) shouldBe true
    }

    test("should reject incorrect password") {
        val rawPassword = "mySecretPassword123!"
        EncodedPassword.validateRawPasswordPolicy(rawPassword)

        val encodedValue = testPasswordEncoder.encode(rawPassword)
        val encodedPassword = EncodedPassword(encodedValue)

        testPasswordEncoder.matches("wrongPassword", encodedPassword.value) shouldBe false
        testPasswordEncoder.matches("mySecretPassword123", encodedPassword.value) shouldBe false
        testPasswordEncoder.matches("MySecretPassword123!", encodedPassword.value) shouldBe false
    }

    test("should create different hashes for same password with real BCrypt-like behavior") {
        // For this test, we'll use a more realistic encoder that simulates BCrypt's random salt
        val bcryptLikeEncoder =
            object : PasswordEncoder {
                private var counter = 0

                override fun encode(rawPassword: String): String {
                    // Simulate different salts by adding counter
                    return "bcrypt:${counter++}:$rawPassword".padEnd(60, '*')
                }

                override fun matches(
                    rawPassword: String,
                    encodedPassword: String,
                ): Boolean {
                    // Extract the original password from encoded format
                    val parts = encodedPassword.trim('*').split(":")
                    return parts.size >= 3 && parts[2] == rawPassword
                }
            }

        val rawPassword = "mySecretPassword123!"
        EncodedPassword.validateRawPasswordPolicy(rawPassword)

        val encodedValue1 = bcryptLikeEncoder.encode(rawPassword)
        val encodedValue2 = bcryptLikeEncoder.encode(rawPassword)
        val encodedPassword1 = EncodedPassword(encodedValue1)
        val encodedPassword2 = EncodedPassword(encodedValue2)

        encodedPassword1.value shouldNotBe encodedPassword2.value
        bcryptLikeEncoder.matches(rawPassword, encodedPassword1.value) shouldBe true
        bcryptLikeEncoder.matches(rawPassword, encodedPassword2.value) shouldBe true
    }

    test("should never expose raw password") {
        val rawPassword = "mySecretPassword123!"
        EncodedPassword.validateRawPasswordPolicy(rawPassword)

        val encodedValue = testPasswordEncoder.encode(rawPassword)
        val encodedPassword = EncodedPassword(encodedValue)

        encodedPassword.toString() shouldNotBe "Password(value=mySecretPassword123!)"
        encodedPassword.toString() shouldBe "EncodedPassword(****)"
    }

    test("should be immutable") {
        val rawPassword = "mySecretPassword123!"
        EncodedPassword.validateRawPasswordPolicy(rawPassword)

        val encodedValue = testPasswordEncoder.encode(rawPassword)
        val encodedPassword = EncodedPassword(encodedValue)
        val originalValue = encodedPassword.value
        // Value should not be modifiable (enforced by val property)
        encodedPassword.value shouldBe originalValue
    }

    test("should implement equals correctly") {
        val rawPwd = "mySecretPassword123!"
        val encodedValue = testPasswordEncoder.encode(rawPwd)

        val encodedPassword1 = EncodedPassword(encodedValue)
        val encodedPassword2 = EncodedPassword(encodedValue)
        val encodedPassword3 = EncodedPassword(testPasswordEncoder.encode("differentPassword"))

        encodedPassword1 shouldBe encodedPassword2
        encodedPassword1 shouldNotBe encodedPassword3
        encodedPassword1 shouldNotBe null
        encodedPassword1 shouldNotBe "password"
    }

    test("should implement hashCode correctly") {
        val rawPassword = "mySecretPassword123!"
        EncodedPassword.validateRawPasswordPolicy(rawPassword)

        val encodedValue = testPasswordEncoder.encode(rawPassword)
        val encodedPassword1 = EncodedPassword(encodedValue)
        val encodedPassword2 = EncodedPassword(encodedValue)

        encodedPassword1.hashCode() shouldBe encodedPassword2.hashCode()
    }
})
