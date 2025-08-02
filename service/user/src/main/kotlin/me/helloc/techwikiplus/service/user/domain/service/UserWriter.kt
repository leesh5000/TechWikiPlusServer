package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.exception.UserAlreadyExistsException
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.service.port.UserRepository

class UserWriter(
    private val repository: UserRepository,
) {
    fun save(user: User): User {
        // 이메일 중복 검증
        if (repository.exists(user.email)) {
            throw UserAlreadyExistsException(user.email.value)
        }
        // 사용자 저장
        return repository.save(user)
    }
}
