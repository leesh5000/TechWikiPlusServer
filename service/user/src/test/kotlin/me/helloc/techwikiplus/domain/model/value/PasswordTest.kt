package me.helloc.techwikiplus.domain.model.value

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.domain.model.value.Password
import me.helloc.techwikiplus.domain.service.port.PasswordEncoder

class PasswordTest : FunSpec({

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
        val password = Password.fromRawPassword("mySecretPassword123!", testPasswordEncoder)
        password.value shouldNotBe "mySecretPassword123!"
        password.value.length shouldBe 60
        password.value.startsWith("encoded:") shouldBe true
    }

    test("should reject null or empty raw password") {
        shouldThrow<IllegalArgumentException> {
            Password.fromRawPassword("", testPasswordEncoder)
        }

        shouldThrow<IllegalArgumentException> {
            Password.fromRawPassword("   ", testPasswordEncoder)
        }
    }

    test("should verify correct password") {
        val rawPassword = "mySecretPassword123!"
        val password = Password.fromRawPassword(rawPassword, testPasswordEncoder)

        password.matches(rawPassword, testPasswordEncoder) shouldBe true
    }

    test("should reject incorrect password") {
        val password = Password.fromRawPassword("mySecretPassword123!", testPasswordEncoder)

        password.matches("wrongPassword", testPasswordEncoder) shouldBe false
        password.matches("mySecretPassword123", testPasswordEncoder) shouldBe false
        password.matches("MySecretPassword123!", testPasswordEncoder) shouldBe false
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
        val password1 = Password.fromRawPassword(rawPassword, bcryptLikeEncoder)
        val password2 = Password.fromRawPassword(rawPassword, bcryptLikeEncoder)

        password1.value shouldNotBe password2.value
        password1.matches(rawPassword, bcryptLikeEncoder) shouldBe true
        password2.matches(rawPassword, bcryptLikeEncoder) shouldBe true
    }

    test("should accept encrypted password directly") {
        val encryptedValue = Password.fromRawPassword("test", testPasswordEncoder).value
        val password = Password(encryptedValue)

        password.value shouldBe encryptedValue
    }

    test("should ensure encrypted password fits in 255 characters") {
        val longPassword = "a".repeat(100)
        val password = Password.fromRawPassword(longPassword, testPasswordEncoder)

        // Our test encoder creates 60 character hashes, which fits in 255 chars
        password.value.length shouldBe 60
        (password.value.length <= 255) shouldBe true
    }

    test("should never expose raw password") {
        val password = Password.fromRawPassword("mySecretPassword123!", testPasswordEncoder)

        password.toString() shouldNotBe "Password(value=mySecretPassword123!)"
        password.toString() shouldBe "Password(****)"
    }

    test("should be immutable") {
        val password = Password.fromRawPassword("mySecretPassword123!", testPasswordEncoder)
        val originalValue = password.value
        // Value should not be modifiable (enforced by val property)
        password.value shouldBe originalValue
    }

    test("should implement equals correctly") {
        val rawPwd = "mySecretPassword123!"
        val encodedValue = testPasswordEncoder.encode(rawPwd)

        val password1 = Password(encodedValue)
        val password2 = Password(encodedValue)
        val password3 = Password(testPasswordEncoder.encode("differentPassword"))

        password1 shouldBe password2
        password1 shouldNotBe password3
        password1 shouldNotBe null
        password1 shouldNotBe "password"
    }

    test("should implement hashCode correctly") {
        val password1 = Password.fromRawPassword("mySecretPassword123!", testPasswordEncoder)
        val password2 = Password(password1.value)

        password1.hashCode() shouldBe password2.hashCode()
    }
})
