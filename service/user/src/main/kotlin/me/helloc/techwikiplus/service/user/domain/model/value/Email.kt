package me.helloc.techwikiplus.service.user.domain.model.value

class Email(val value: String) {
    init {
        require(value.isNotBlank()) { "Email cannot be blank" }
        require(EMAIL_REGEX.matches(value)) { "Invalid email format: $value" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Email) return false
        return value == other.value
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun toString(): String {
        return "Email(value=$value)"
    }

    companion object {
        private val EMAIL_REGEX = """^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\.[A-Za-z]{2,})$""".toRegex()
    }
}
