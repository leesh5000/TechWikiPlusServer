package me.helloc.techwikiplus.service.user.domain.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import me.helloc.techwikiplus.service.user.domain.exception.PasswordValidationException
import me.helloc.techwikiplus.service.user.domain.model.value.RawPassword
import me.helloc.techwikiplus.service.user.infrastructure.security.FakePasswordCrypter

class UserPasswordServiceUnitTest : FunSpec({

    test("패스워드 인코더를 사용하여 원시 패스워드를 인코딩해야 한다") {
        // Given
        val passwordEncoder = FakePasswordCrypter()
        val userPasswordEncoder = UserPasswordEncoder(passwordEncoder)
        val rawPassword = RawPassword("mySecretPassword123!")

        // When
        val encodedPassword = userPasswordEncoder.encode(rawPassword)

        // Then
        encodedPassword.value shouldBe "FAKE_ENCODED:mySecretPassword123!"
    }

    test("빈 패스워드 인코딩 시 예외가 발생해야 한다") {
        // Given
        // When & Then
        shouldThrow<PasswordValidationException> {
            RawPassword("")
        }
    }

    test("특수문자가 있는 유효한 패스워드를 인코딩해야 한다") {
        // Given
        val passwordEncoder = FakePasswordCrypter()
        val userPasswordEncoder = UserPasswordEncoder(passwordEncoder)
        val rawPassword = RawPassword("P@ssw0rd!") // 대문자, 소문자, 특수문자 포함

        // When
        val encodedPassword = userPasswordEncoder.encode(rawPassword)

        // Then
        encodedPassword.value shouldBe "FAKE_ENCODED:P@ssw0rd!"
    }

    test("31자 이상의 패스워드 인코딩 시 예외가 발생해야 한다") {
        // Given
        // When & Then
        shouldThrow<PasswordValidationException> {
            RawPassword("VeryLongPassword1234567890123!@") // 31자
        }
    }

    test("유효한 패스워드의 공백을 포함하여 인코딩해야 한다") {
        // Given
        val passwordEncoder = FakePasswordCrypter()
        val userPasswordEncoder = UserPasswordEncoder(passwordEncoder)
        val rawPassword = RawPassword("  PassWord123!  ") // 앞뒤 공백 포함, 대소문자와 특수문자 포함

        // When
        val encodedPassword = userPasswordEncoder.encode(rawPassword)

        // Then
        encodedPassword.value shouldBe "FAKE_ENCODED:  PassWord123!  "
    }
})
