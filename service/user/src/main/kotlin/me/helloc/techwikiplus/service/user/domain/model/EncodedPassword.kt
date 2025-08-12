package me.helloc.techwikiplus.service.user.domain.model.value

class EncodedPassword(val value: String) {
    init {
        require(value.isNotBlank()) { "EncodedPassword value cannot be blank" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncodedPassword) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "EncodedPassword(****)"
    }
}
