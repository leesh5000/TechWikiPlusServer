package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.exception.conflict.DuplicateEmailException
import me.helloc.techwikiplus.user.domain.exception.conflict.DuplicateNicknameException
import me.helloc.techwikiplus.user.domain.port.outbound.UserRepository

class UserDuplicateChecker(
    private val repository: UserRepository,
) {
    fun validateUserEmailDuplicate(email: String) {
        if (repository.existsByEmail(email)) {
            throw DuplicateEmailException(email)
        }
    }

    fun validateUserNicknameDuplicate(nickname: String) {
        if (repository.existsByNickname(nickname)) {
            throw DuplicateNicknameException(nickname)
        }
    }
}
