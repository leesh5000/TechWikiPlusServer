package me.helloc.techwikiplus.domain.model.value

class Nickname(val value: String) {
    init {
        require(value.isNotBlank()) { "Nickname cannot be blank" }
        require(value.length <= MAX_LENGTH) { "Nickname cannot exceed $MAX_LENGTH characters" }
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
        private const val MAX_LENGTH = 20
    }
}
