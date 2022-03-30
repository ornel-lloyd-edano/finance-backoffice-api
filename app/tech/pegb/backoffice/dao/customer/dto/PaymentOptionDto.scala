package tech.pegb.backoffice.dao.customer.dto

import java.time.LocalDateTime
import java.util.UUID

final case class PaymentOptionDto(
    id: Int,
    customerId: UUID,
    `type`: Int,
    provider: Int,
    maskedNumber: String,
    additionalData: Option[String],
    addedAt: LocalDateTime)
