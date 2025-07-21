package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.exception.CustomException.ConflictException.DuplicateEmail
import me.helloc.techwikiplus.user.domain.exception.CustomException.ConflictException.DuplicateNickname
import me.helloc.techwikiplus.user.domain.service.UserRepository
import org.springframework.stereotype.Service

@Service
class UserDuplicateChecker(
    private val repository: UserRepository
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
