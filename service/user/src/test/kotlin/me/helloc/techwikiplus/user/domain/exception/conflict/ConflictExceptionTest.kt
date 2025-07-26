package me.helloc.techwikiplus.user.domain.exception.conflict

import me.helloc.techwikiplus.user.domain.exception.ErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConflictExceptionTest {
    @Test
    fun `should create DuplicateEmailException with correct error code and message`() {
        // given
        val email = "duplicate@example.com"

        // when
        val exception = DuplicateEmailException(email)

        // then
        assertThat(exception.errorCode).isEqualTo(ErrorCode.DUPLICATE_EMAIL)
        assertThat(exception.code).isEqualTo("USER_201")
        assertThat(exception.message).isEqualTo("Email already exists. Details: Your input: duplicate@example.com")
    }

    @Test
    fun `should create DuplicateNicknameException with correct error code and message`() {
        // given
        val nickname = "existingUser"

        // when
        val exception = DuplicateNicknameException(nickname)

        // then
        assertThat(exception.errorCode).isEqualTo(ErrorCode.DUPLICATE_NICKNAME)
        assertThat(exception.code).isEqualTo("USER_202")
        assertThat(exception.message).isEqualTo("Nickname already exists. Details: Your input: existingUser")
    }
}
