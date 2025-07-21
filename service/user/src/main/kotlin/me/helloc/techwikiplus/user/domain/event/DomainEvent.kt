package me.helloc.techwikiplus.user.domain.event

import java.time.LocalDateTime

abstract class DomainEvent(
    val eventId: String,
    val occurredOn: LocalDateTime
)