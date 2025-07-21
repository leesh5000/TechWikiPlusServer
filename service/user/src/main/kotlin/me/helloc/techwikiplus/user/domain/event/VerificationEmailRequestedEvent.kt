package me.helloc.techwikiplus.user.domain.event

import java.time.LocalDateTime
import java.util.UUID

data class VerificationEmailRequestedEvent(
    val email: String,
    val verificationCode: String
) : DomainEvent(
    eventId = UUID.randomUUID().toString(),
    occurredOn = LocalDateTime.now()
)