package me.helloc.techwikiplus.service.document.domain.service.port

import me.helloc.techwikiplus.service.document.domain.model.LoginUser

interface DocumentAuthorizationPort {
    fun requireAuthenticated(): LoginUser
}
