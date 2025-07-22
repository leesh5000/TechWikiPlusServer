package me.helloc.techwikiplus.user.infrastructure.passwordencoder.spring

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.kotest.matchers.string.shouldStartWith
import me.helloc.techwikiplus.user.infrastructure.passwordencoder.SpringPasswordService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

class SpringPasswordServiceUnitTest : FunSpec({

    val passwordEncoder = BCryptPasswordEncoder()
    val passwordService = SpringPasswordService(passwordEncoder)

    context("패스워드 암호화") {
        test("평문 패스워드를 BCrypt로 암호화한다") {
            val rawPassword = "mySecretPassword123!"

            val encodedPassword = passwordService.encode(rawPassword)

            encodedPassword shouldNotBe rawPassword
            encodedPassword.shouldNotBeBlank()
            // BCrypt 해시는 $2a$ 또는 $2b$로 시작
            encodedPassword shouldStartWith "$2"
        }

        test("같은 패스워드를 암호화해도 매번 다른 해시값이 생성된다") {
            val rawPassword = "samePassword123!"

            val encoded1 = passwordService.encode(rawPassword)
            val encoded2 = passwordService.encode(rawPassword)
            val encoded3 = passwordService.encode(rawPassword)

            // Salt가 다르므로 해시값도 다름
            encoded1 shouldNotBe encoded2
            encoded2 shouldNotBe encoded3
            encoded1 shouldNotBe encoded3
        }

        test("빈 패스워드도 암호화 가능하다") {
            val emptyPassword = ""

            val encodedPassword = passwordService.encode(emptyPassword)

            encodedPassword.shouldNotBeBlank()
            encodedPassword shouldStartWith "$2"
        }

        test("매우 긴 패스워드도 암호화 가능하다") {
            // BCrypt는 72바이트까지만 처리 가능
            val longPassword = "a".repeat(50) + "1234567890!@#$%^&*()"  // 총 70바이트

            val encodedPassword = passwordService.encode(longPassword)

            encodedPassword.shouldNotBeBlank()
            encodedPassword shouldStartWith "$2"
        }

        test("특수문자가 포함된 패스워드를 암호화한다") {
            val specialPassword = "P@ssw0rd!#$%^&*()_+-=[]{}|;':\",./<>?"

            val encodedPassword = passwordService.encode(specialPassword)

            encodedPassword.shouldNotBeBlank()
            encodedPassword shouldStartWith "$2"
        }
    }

    context("패스워드 매칭") {
        test("올바른 패스워드는 true를 반환한다") {
            val rawPassword = "correctPassword123!"
            val encodedPassword = passwordService.encode(rawPassword)

            val matches = passwordService.matches(rawPassword, encodedPassword)

            matches shouldBe true
        }

        test("틀린 패스워드는 false를 반환한다") {
            val rawPassword = "correctPassword123!"
            val wrongPassword = "wrongPassword123!"
            val encodedPassword = passwordService.encode(rawPassword)

            val matches = passwordService.matches(wrongPassword, encodedPassword)

            matches shouldBe false
        }

        test("대소문자를 구분한다") {
            val rawPassword = "MyPassword123!"
            val wrongCasePassword = "mypassword123!"
            val encodedPassword = passwordService.encode(rawPassword)

            val correctMatches = passwordService.matches(rawPassword, encodedPassword)
            val wrongMatches = passwordService.matches(wrongCasePassword, encodedPassword)

            correctMatches shouldBe true
            wrongMatches shouldBe false
        }

        test("공백 문자를 구분한다") {
            val passwordWithSpace = "my password 123"
            val passwordWithoutSpace = "mypassword123"
            val encodedPassword = passwordService.encode(passwordWithSpace)

            val correctMatches = passwordService.matches(passwordWithSpace, encodedPassword)
            val wrongMatches = passwordService.matches(passwordWithoutSpace, encodedPassword)

            correctMatches shouldBe true
            wrongMatches shouldBe false
        }

        test("빈 패스워드도 매칭 가능하다") {
            val emptyPassword = ""
            val encodedPassword = passwordService.encode(emptyPassword)

            val matches = passwordService.matches(emptyPassword, encodedPassword)

            matches shouldBe true
        }

        test("잘못된 형식의 해시값은 false를 반환한다") {
            val rawPassword = "testPassword123!"
            val invalidHash = "not-a-bcrypt-hash"

            val matches = passwordService.matches(rawPassword, invalidHash)

            matches shouldBe false
        }

        test("null이 아닌 빈 해시값은 false를 반환한다") {
            val rawPassword = "testPassword123!"
            val emptyHash = ""

            val matches = passwordService.matches(rawPassword, emptyHash)

            matches shouldBe false
        }
    }

    context("여러 라운드의 암호화와 매칭") {
        test("동일한 패스워드로 생성된 여러 해시값 모두 매칭된다") {
            val rawPassword = "multiHashTest123!"

            val hash1 = passwordService.encode(rawPassword)
            val hash2 = passwordService.encode(rawPassword)
            val hash3 = passwordService.encode(rawPassword)

            // 모든 해시값이 원본 패스워드와 매칭
            passwordService.matches(rawPassword, hash1) shouldBe true
            passwordService.matches(rawPassword, hash2) shouldBe true
            passwordService.matches(rawPassword, hash3) shouldBe true

            // 해시값들은 서로 다름
            hash1 shouldNotBe hash2
            hash2 shouldNotBe hash3
            hash1 shouldNotBe hash3
        }
    }

    context("보안 강도 확인") {
        test("BCrypt 해시는 60자 길이를 가진다") {
            val password = "testPassword123!"

            val encodedPassword = passwordService.encode(password)

            encodedPassword.length shouldBe 60
        }

        test("해시값에는 알고리즘 버전, cost factor, salt, 해시가 포함된다") {
            val password = "testPassword123!"

            val encodedPassword = passwordService.encode(password)

            // BCrypt 형식: $2a$10$salt22characters.hash31characters
            val parts = encodedPassword.split("$")
            parts.size shouldBe 4 // 빈 문자열, 2a, 10, salt+hash
            parts[1] shouldBe "2a" // 알고리즘 버전
            parts[2] shouldBe "10" // 기본 cost factor
        }
    }
})
