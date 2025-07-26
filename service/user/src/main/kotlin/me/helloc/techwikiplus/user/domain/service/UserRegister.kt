package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.User
import me.helloc.techwikiplus.user.domain.UserEmail
import me.helloc.techwikiplus.user.domain.port.outbound.Clock
import me.helloc.techwikiplus.user.domain.port.outbound.IdGenerator
import me.helloc.techwikiplus.user.domain.port.outbound.PasswordEncoder
import me.helloc.techwikiplus.user.domain.port.outbound.PasswordValidator

class UserRegister(
    private val userWriter: UserWriter,
    private val userDuplicateChecker: UserDuplicateChecker,
    private val passwordValidator: PasswordValidator,
    private val passwordEncoder: PasswordEncoder,
    private val idGenerator: IdGenerator,
) {
    fun registerPendingUser(
        email: String,
        nickname: String,
        password: String,
    ): User {
        userDuplicateChecker.validateUserEmailDuplicate(email)
        userDuplicateChecker.validateUserNicknameDuplicate(nickname)

        passwordValidator.validate(password)
        val encodedPassword = passwordEncoder.encode(password)

        val user =
            User.withPendingUser(
                id = idGenerator.next(),
                email = UserEmail(email, false),
                nickname = nickname,
                password = encodedPassword,
                clock = Clock.system,
            )

        userWriter.insertOrUpdate(user)
        return user
    }
}
