package me.helloc.techwikiplus.service.user.domain.model.value

class Nickname(val value: String) {
    init {
        require(value.isNotBlank()) { "Nickname cannot be blank" }
        require(value.length >= MIN_LENGTH) { "Nickname must be at least $MIN_LENGTH characters" }
        require(value.length <= MAX_LENGTH) { "Nickname cannot exceed $MAX_LENGTH characters" }
        require(!value.contains(' ')) { "Nickname cannot contain spaces" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Nickname) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "Nickname(value=$value)"
    }

    companion object {
        private const val MIN_LENGTH = 2
        private const val MAX_LENGTH = 20
    }
}
