package me.helloc.techwikiplus.user.application

import me.helloc.techwikiplus.user.domain.UserStatus
import me.helloc.techwikiplus.user.domain.exception.CustomException
import me.helloc.techwikiplus.user.domain.service.UserPasswordService
import me.helloc.techwikiplus.user.domain.service.UserReader
import me.helloc.techwikiplus.user.infrastructure.security.JwtTokenProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
open class UserLoginFacade(
    private val userReader: UserReader,
    private val userPasswordService: UserPasswordService,
    private val jwtTokenProvider: JwtTokenProvider
) {

    fun login(email: String, password: String): LoginResult {
        val user = userReader.readByEmailOrThrows(email)
        if (!userPasswordService.matches(password, user.password)) {
            throw CustomException.AuthenticationException.InvalidCredentials()
        }

        if (user.status != UserStatus.ACTIVE) {
            when (user.status) {
                UserStatus.PENDING -> throw CustomException.AuthenticationException.EmailNotVerified()
                UserStatus.BANNED -> throw CustomException.AuthenticationException.AccountBanned()
                UserStatus.DORMANT -> throw CustomException.AuthenticationException.AccountDormant()
                UserStatus.DELETED -> throw CustomException.AuthenticationException.AccountDeleted()
                else -> throw CustomException.AuthenticationException.InvalidCredentials()
            }
        }

        val accessToken = jwtTokenProvider.createAccessToken(user.email.value, user.id)
        val refreshToken = jwtTokenProvider.createRefreshToken(user.email.value, user.id)

        return LoginResult(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = user.id,
            email = user.email.value,
            nickname = user.nickname
        )
    }

    data class LoginResult(
        val accessToken: String,
        val refreshToken: String,
        val userId: Long,
        val email: String,
        val nickname: String
    )
}

