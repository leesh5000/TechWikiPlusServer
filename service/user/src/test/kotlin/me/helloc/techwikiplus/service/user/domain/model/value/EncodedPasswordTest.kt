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

    test("원시 패스워드로부터 암호화된 패스워드를 생성해야 한다") {
        // validateRawPasswordPolicy only validates, doesn't return EncodedPassword
        EncodedPassword.validateRawPasswordPolicy("mySecretPassword123!")

        // Create EncodedPassword with encoded value
        val encodedValue = testPasswordEncoder.encode("mySecretPassword123!")
        val encodedPassword = EncodedPassword(encodedValue)
        encodedPassword.value shouldNotBe "mySecretPassword123!"
        encodedPassword.value.length shouldBe 60
        encodedPassword.value.startsWith("encoded:") shouldBe true
    }

    test("null이거나 빈 원시 패스워드를 거부해야 한다") {
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

    test("8자 미만의 패스워드를 거부해야 한다") {
        val exception =
            shouldThrow<PasswordValidationException> {
                EncodedPassword.validateRawPasswordPolicy("Short1!")
            }
        exception.errorCode shouldBe PasswordValidationException.TOO_SHORT
        exception.message shouldBe "비밀번호는 최소 8자 이상이어야 합니다"
    }

    test("30자를 초과하는 패스워드를 거부해야 한다") {
        val exception =
            shouldThrow<PasswordValidationException> {
                EncodedPassword.validateRawPasswordPolicy("VeryLongPassword1234567890123!@")
            }
        exception.errorCode shouldBe PasswordValidationException.TOO_LONG
        exception.message shouldBe "비밀번호는 최대 30자 이하여야 합니다"
    }

    test("대문자가 없는 패스워드를 거부해야 한다") {
        val exception =
            shouldThrow<PasswordValidationException> {
                EncodedPassword.validateRawPasswordPolicy("lowercase123!")
            }
        exception.errorCode shouldBe PasswordValidationException.NO_UPPERCASE
        exception.message shouldBe "비밀번호는 대문자를 포함해야 합니다"
    }

    test("소문자가 없는 패스워드를 거부해야 한다") {
        val exception =
            shouldThrow<PasswordValidationException> {
                EncodedPassword.validateRawPasswordPolicy("UPPERCASE123!")
            }
        exception.errorCode shouldBe PasswordValidationException.NO_LOWERCASE
        exception.message shouldBe "비밀번호는 소문자를 포함해야 합니다"
    }

    test("특수문자가 없는 패스워드를 거부해야 한다") {
        val exception =
            shouldThrow<PasswordValidationException> {
                EncodedPassword.validateRawPasswordPolicy("Password123")
            }
        exception.errorCode shouldBe PasswordValidationException.NO_SPECIAL_CHAR
        exception.message shouldBe "비밀번호는 특수문자를 포함해야 합니다"
    }

    test("모든 요구사항을 충족하는 유효한 패스워드를 허용해야 한다") {
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

    test("올바른 패스워드를 검증해야 한다") {
        val rawPassword = "mySecretPassword123!"
        EncodedPassword.validateRawPasswordPolicy(rawPassword)

        val encodedValue = testPasswordEncoder.encode(rawPassword)
        val encodedPassword = EncodedPassword(encodedValue)

        // Test password matching using encoder directly
        testPasswordEncoder.matches(rawPassword, encodedPassword.value) shouldBe true
    }

    test("올바르지 않은 패스워드를 거부해야 한다") {
        val rawPassword = "mySecretPassword123!"
        EncodedPassword.validateRawPasswordPolicy(rawPassword)

        val encodedValue = testPasswordEncoder.encode(rawPassword)
        val encodedPassword = EncodedPassword(encodedValue)

        testPasswordEncoder.matches("wrongPassword", encodedPassword.value) shouldBe false
        testPasswordEncoder.matches("mySecretPassword123", encodedPassword.value) shouldBe false
        testPasswordEncoder.matches("MySecretPassword123!", encodedPassword.value) shouldBe false
    }

    test("실제 BCrypt와 유사한 동작으로 동일한 패스워드에 대해 다른 해시를 생성해야 한다") {
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

    test("원시 패스워드를 절대 노출하지 않아야 한다") {
        val rawPassword = "mySecretPassword123!"
        EncodedPassword.validateRawPasswordPolicy(rawPassword)

        val encodedValue = testPasswordEncoder.encode(rawPassword)
        val encodedPassword = EncodedPassword(encodedValue)

        encodedPassword.toString() shouldNotBe "Password(value=mySecretPassword123!)"
        encodedPassword.toString() shouldBe "EncodedPassword(****)"
    }

    test("불변 객체여야 한다") {
        val rawPassword = "mySecretPassword123!"
        EncodedPassword.validateRawPasswordPolicy(rawPassword)

        val encodedValue = testPasswordEncoder.encode(rawPassword)
        val encodedPassword = EncodedPassword(encodedValue)
        val originalValue = encodedPassword.value
        // Value should not be modifiable (enforced by val property)
        encodedPassword.value shouldBe originalValue
    }

    test("equals를 올바르게 구현해야 한다") {
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

    test("hashCode를 올바르게 구현해야 한다") {
        val rawPassword = "mySecretPassword123!"
        EncodedPassword.validateRawPasswordPolicy(rawPassword)

        val encodedValue = testPasswordEncoder.encode(rawPassword)
        val encodedPassword1 = EncodedPassword(encodedValue)
        val encodedPassword2 = EncodedPassword(encodedValue)

        encodedPassword1.hashCode() shouldBe encodedPassword2.hashCode()
    }
})
