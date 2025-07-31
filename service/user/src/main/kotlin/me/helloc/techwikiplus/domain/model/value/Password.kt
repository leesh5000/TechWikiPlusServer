package me.helloc.techwikiplus.domain.model.value

import me.helloc.techwikiplus.domain.service.port.PasswordEncoder

class Password(val value: String) {
    init {
        require(value.isNotBlank()) { "Password value cannot be blank" }
    }

    fun matches(
        rawPassword: String,
        encoder: PasswordEncoder,
    ): Boolean {
        return encoder.matches(rawPassword, value)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Password) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "Password(****)"
    }

    companion object {
        fun fromRawPassword(
            rawPassword: String,
            encoder: PasswordEncoder,
        ): Password {
            require(rawPassword.isNotBlank()) { "Raw password cannot be blank" }
            val encodedPassword = encoder.encode(rawPassword)
            return Password(encodedPassword)
        }
    }
}
