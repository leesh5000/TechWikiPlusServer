package me.helloc.techwikiplus.application

import me.helloc.techwikiplus.application.usecase.UserSignUpUseCase

class UserSignUpFacade: UserSignUpUseCase {

    override fun signup(email: String, password: String, name: String): Boolean {
        TODO("Not yet implemented")
    }
}
