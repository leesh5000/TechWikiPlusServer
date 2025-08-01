package me.helloc.techwikiplus.service.user.domain.service

import me.helloc.techwikiplus.domain.service.port.MailSender

class VerificationCodeMailSender(
    private val mailSender: MailSender,
)
