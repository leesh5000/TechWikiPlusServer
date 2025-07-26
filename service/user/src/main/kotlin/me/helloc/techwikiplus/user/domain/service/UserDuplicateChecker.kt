package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.exception.CustomException.ConflictException.DuplicateEmail
import me.helloc.techwikiplus.user.domain.exception.CustomException.ConflictException.DuplicateNickname
import me.helloc.techwikiplus.user.domain.port.outbound.UserRepository

class UserDuplicateChecker(
    private val repository: UserRepository,
) {
    fun validateUserEmailDuplicate(email: String) {
        if (repository.existsByEmail(email)) {
            throw DuplicateEmail(email)
        }
    }

    fun validateUserNicknameDuplicate(nickname: String) {
        if (repository.existsByNickname(nickname)) {
            throw DuplicateNickname(nickname)
        }
    }
}
