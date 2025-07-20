package me.helloc.techwikiplus.user.domain.service

import me.helloc.techwikiplus.user.domain.repository.UserRepository
import org.springframework.stereotype.Service

@Transactional
@Service
class UserReader(
    private val userRepository: UserRepository
) {

}
