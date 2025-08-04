package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.service.user.domain.exception.UserAlreadyExistsException
import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.service.port.UserRepository

class UserWriter(
    private val repository: UserRepository,
) {
    fun update(user: User): User {
        // 사용자 업데이트
        return repository.save(user)
    }

    fun insert(user: User): User {
        // 이메일 중복 검증
        if (repository.exists(user.email)) {
            throw UserAlreadyExistsException.ForEmail(user.email)
        }
        // 닉네임 중복 검증
        if (repository.exists(user.nickname)) {
            throw UserAlreadyExistsException.ForNickname(user.nickname)
        }
        // 사용자 삽입
        return repository.save(user)
    }
}
