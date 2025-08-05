package me.helloc.techwikiplus.service.user.domain.model.value

class UserId(value: String) {
    val value: String = value.trim()

    init {
        require(this.value.isNotBlank()) { "User ID cannot be blank" }
        require(this.value.length <= 64) { "User ID cannot exceed 64 characters" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserId) return false
        return value == other.value
    }

    override fun hashCode(): Int = value.hashCode()

    override fun toString(): String = value

    companion object {
        fun from(value: String): UserId = UserId(value)
    }
}