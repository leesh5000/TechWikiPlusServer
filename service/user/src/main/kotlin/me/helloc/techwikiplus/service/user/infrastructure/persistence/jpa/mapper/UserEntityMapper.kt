package me.helloc.techwikiplus.service.user.infrastructure.persistence.jpa.mapper

import me.helloc.techwikiplus.service.user.domain.model.User
import me.helloc.techwikiplus.service.user.domain.model.type.UserRole
import me.helloc.techwikiplus.service.user.domain.model.type.UserStatus
import me.helloc.techwikiplus.service.user.domain.model.value.Email
import me.helloc.techwikiplus.service.user.domain.model.value.EncodedPassword
import me.helloc.techwikiplus.service.user.domain.model.value.Nickname
import me.helloc.techwikiplus.service.user.domain.model.value.UserId
import me.helloc.techwikiplus.service.user.infrastructure.persistence.jpa.entity.UserEntity
import org.springframework.stereotype.Component

@Component
class UserEntityMapper {
    fun toDomain(entity: UserEntity): User {
        return User(
            id = UserId(entity.id),
            email = Email(entity.email),
            nickname = Nickname(entity.nickname),
            encodedPassword = EncodedPassword(entity.password),
            status = UserStatus.valueOf(entity.status),
            role = UserRole.valueOf(entity.role),
            createdAt = entity.createdAt,
            modifiedAt = entity.modifiedAt,
        )
    }

    fun toEntity(user: User): UserEntity {
        return UserEntity(
            id = user.id.value,
            email = user.email.value,
            nickname = user.nickname.value,
            password = user.encodedPassword.value,
            status = user.status.name,
            role = user.role.name,
            createdAt = user.createdAt,
            modifiedAt = user.modifiedAt,
        )
    }
}
