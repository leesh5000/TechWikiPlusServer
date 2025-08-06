package me.helloc.techwikiplus.service.user.domain.exception

class PasswordsDoNotMatchException :
    UserDomainException("비밀번호와 비밀번호 확인이 일치하지 않습니다.")
