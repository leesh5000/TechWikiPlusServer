package me.helloc.techwikiplus.service.user.domain.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.service.user.domain.exception.PasswordValidationException
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword
import me.helloc.techwikiplus.service.user.infrastructure.security.FakePasswordEncoder

class UserPasswordServiceTest : FunSpec({

    test("패스워드 인코더를 사용하여 원시 패스워드를 인코딩해야 한다") {
        // Given
        val passwordEncoder = FakePasswordEncoder()
        val userPasswordService = UserPasswordService(passwordEncoder)
        val rawPassword = RawPassword("mySecretPassword123!")

        // When
        val encodedPassword = userPasswordService.encode(rawPassword)

        // Then
        encodedPassword.value shouldBe "FAKE_ENCODED:mySecretPassword123!"
    }

    test("동일한 경우 원시 패스워드와 인코딩된 패스워드가 일치해야 한다") {
        // Given
        val passwordEncoder = FakePasswordEncoder()
        val userPasswordService = UserPasswordService(passwordEncoder)
        val rawPassword = RawPassword("CorrectPassword123!")
        val encodedPassword = EncodedPassword("FAKE_ENCODED:CorrectPassword123!")

        // When
        val matches = userPasswordService.matches(rawPassword, encodedPassword)

        // Then
        matches shouldBe true
    }

    test("다른 경우 원시 패스워드와 인코딩된 패스워드가 일치하지 않아야 한다") {
        // Given
        val passwordEncoder = FakePasswordEncoder()
        val userPasswordService = UserPasswordService(passwordEncoder)
        val rawPassword = RawPassword("WrongPassword123!")
        val encodedPassword = EncodedPassword("FAKE_ENCODED:CorrectPassword123!")

        // When
        val matches = userPasswordService.matches(rawPassword, encodedPassword)

        // Then
        matches shouldBe false
    }

    test("빈 패스워드 인코딩 시 예외가 발생해야 한다") {
        // Given
        val passwordEncoder = FakePasswordEncoder()
        val userPasswordService = UserPasswordService(passwordEncoder)
        // When & Then
        shouldThrow<PasswordValidationException> {
            RawPassword("")
        }
    }

    test("특수문자가 있는 유효한 패스워드를 인코딩해야 한다") {
        // Given
        val passwordEncoder = FakePasswordEncoder()
        val userPasswordService = UserPasswordService(passwordEncoder)
        val rawPassword = RawPassword("P@ssw0rd!") // 대문자, 소문자, 특수문자 포함

        // When
        val encodedPassword = userPasswordService.encode(rawPassword)

        // Then
        encodedPassword.value shouldBe "FAKE_ENCODED:P@ssw0rd!"
    }

    test("31자 이상의 패스워드 인코딩 시 예외가 발생해야 한다") {
        // Given
        val passwordEncoder = FakePasswordEncoder()
        val userPasswordService = UserPasswordService(passwordEncoder)
        // When & Then
        shouldThrow<PasswordValidationException> {
            RawPassword("VeryLongPassword1234567890123!@") // 31자
        }
    }

    test("유효한 패스워드의 공백을 포함하여 인코딩해야 한다") {
        // Given
        val passwordEncoder = FakePasswordEncoder()
        val userPasswordService = UserPasswordService(passwordEncoder)
        val rawPassword = RawPassword("  PassWord123!  ") // 앞뒤 공백 포함, 대소문자와 특수문자 포함

        // When
        val encodedPassword = userPasswordService.encode(rawPassword)

        // Then
        encodedPassword.value shouldBe "FAKE_ENCODED:  PassWord123!  "
    }

    test("인코딩된 패스워드가 잘못된 형식일 때 일치하지 않아야 한다") {
        // Given
        val passwordEncoder = FakePasswordEncoder()
        val userPasswordService = UserPasswordService(passwordEncoder)
        val rawPassword = RawPassword("Password123!")
        val malformedEncodedPassword = EncodedPassword("WRONG_PREFIX:Password123!")

        // When
        val matches = userPasswordService.matches(rawPassword, malformedEncodedPassword)

        // Then
        matches shouldBe false
    }
})
