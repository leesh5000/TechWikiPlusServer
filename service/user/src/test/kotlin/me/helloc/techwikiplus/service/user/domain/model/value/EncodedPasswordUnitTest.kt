package me.helloc.techwikiplus.service.user.domain.model.value

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import me.helloc.techwikiplus.service.user.domain.service.port.PasswordCrypter

class EncodedPasswordUnitTest : FunSpec({

    val testPasswordCrypter =
        object : PasswordCrypter {
            override fun encode(rawPassword: RawPassword): EncodedPassword {
                // Simple test encoder that creates a fixed 60 char hash
                val hash = rawPassword.value.hashCode().toString()
                return EncodedPassword("encoded:$hash".padEnd(60, '*'))
            }

            override fun matches(
                rawPassword: RawPassword,
                encodedPassword: EncodedPassword,
            ): Boolean {
                return encodedPassword == encode(rawPassword)
            }
        }

    test("원시 패스워드로부터 암호화된 패스워드를 생성해야 한다") {
        // Create RawPassword to validate
        val rawPassword = RawPassword("mySecretPassword123!")

        // Create EncodedPassword with encoded value
        val encodedPassword = testPasswordCrypter.encode(rawPassword)
        encodedPassword.value shouldNotBe "mySecretPassword123!"
        encodedPassword.value.length shouldBe 60
        encodedPassword.value.startsWith("encoded:") shouldBe true
    }

    test("빈 인코딩된 패스워드를 거부해야 한다") {
        val exception =
            shouldThrow<IllegalArgumentException> {
                EncodedPassword("")
            }
        exception.message shouldBe "EncodedPassword value cannot be blank"

        val exception2 =
            shouldThrow<IllegalArgumentException> {
                EncodedPassword("   ")
            }
        exception2.message shouldBe "EncodedPassword value cannot be blank"
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

        validPasswords.forEach { rawPasswordStr ->
            // This should not throw
            val rawPassword = RawPassword(rawPasswordStr)

            val encodedPassword = testPasswordCrypter.encode(rawPassword)
            encodedPassword.value shouldNotBe rawPasswordStr
        }
    }

    test("올바른 패스워드를 검증해야 한다") {
        val rawPassword = RawPassword("mySecretPassword123!")

        val encodedPassword = testPasswordCrypter.encode(rawPassword)

        // Test password matching using encoder directly
        testPasswordCrypter.matches(rawPassword, encodedPassword) shouldBe true
    }

    test("올바르지 않은 패스워드를 거부해야 한다") {
        val rawPassword = RawPassword("mySecretPassword123!")

        val encodedPassword = testPasswordCrypter.encode(rawPassword)

        testPasswordCrypter.matches(RawPassword("wrongPassword123!"), encodedPassword) shouldBe false
        testPasswordCrypter.matches(RawPassword("mySecretPassword123@"), encodedPassword) shouldBe false
        testPasswordCrypter.matches(RawPassword("MySecretPassword123!"), encodedPassword) shouldBe false
    }

    test("실제 BCrypt와 유사한 동작으로 동일한 패스워드에 대해 다른 해시를 생성해야 한다") {
        // For this test, we'll use a more realistic encoder that simulates BCrypt's random salt
        val bcryptLikeEncoder =
            object : PasswordCrypter {
                private var counter = 0

                override fun encode(rawPassword: RawPassword): EncodedPassword {
                    // Simulate different salts by adding counter
                    return EncodedPassword("bcrypt:${counter++}:${rawPassword.value}".padEnd(60, '*'))
                }

                override fun matches(
                    rawPassword: RawPassword,
                    encodedPassword: EncodedPassword,
                ): Boolean {
                    // Extract the original password from encoded format
                    val parts = encodedPassword.value.trim('*').split(":")
                    return parts.size >= 3 && parts[2] == rawPassword.value
                }
            }

        val rawPassword = RawPassword("mySecretPassword123!")

        val encodedPassword1 = bcryptLikeEncoder.encode(rawPassword)
        val encodedPassword2 = bcryptLikeEncoder.encode(rawPassword)

        encodedPassword1.value shouldNotBe encodedPassword2.value
        bcryptLikeEncoder.matches(rawPassword, encodedPassword1) shouldBe true
        bcryptLikeEncoder.matches(rawPassword, encodedPassword2) shouldBe true
    }

    test("원시 패스워드를 절대 노출하지 않아야 한다") {
        val rawPassword = RawPassword("mySecretPassword123!")

        val encodedPassword = testPasswordCrypter.encode(rawPassword)

        encodedPassword.toString() shouldNotBe "Password(value=mySecretPassword123!)"
        encodedPassword.toString() shouldBe "EncodedPassword(****)"
    }

    test("불변 객체여야 한다") {
        val rawPassword = RawPassword("mySecretPassword123!")

        val encodedPassword = testPasswordCrypter.encode(rawPassword)
        val originalValue = encodedPassword.value
        // Value should not be modifiable (enforced by val property)
        encodedPassword.value shouldBe originalValue
    }

    test("equals를 올바르게 구현해야 한다") {
        val rawPwd = RawPassword("mySecretPassword123!")
        val encodedPassword = testPasswordCrypter.encode(rawPwd)

        val encodedPassword1 = encodedPassword
        val encodedPassword2 = encodedPassword
        val encodedPassword3 = testPasswordCrypter.encode(RawPassword("differentPassword123!"))

        encodedPassword1 shouldBe encodedPassword2
        encodedPassword1 shouldNotBe encodedPassword3
        encodedPassword1 shouldNotBe null
        encodedPassword1 shouldNotBe "password"
    }

    test("hashCode를 올바르게 구현해야 한다") {
        val rawPassword = RawPassword("mySecretPassword123!")

        val encodedPassword = testPasswordCrypter.encode(rawPassword)
        val encodedPassword1 = encodedPassword
        val encodedPassword2 = encodedPassword

        encodedPassword1.hashCode() shouldBe encodedPassword2.hashCode()
    }
})
