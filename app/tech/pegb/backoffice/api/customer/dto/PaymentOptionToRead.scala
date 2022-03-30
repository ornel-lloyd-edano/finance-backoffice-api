package tech.pegb.backoffice.api.customer.dto

import java.time.LocalDateTime
import java.util.UUID

final case class PaymentOptionToRead(
    id: Int,
    customerId: UUID,
    `type`: Int,
    provider: Int,
    maskedNumber: String,
    additionalData: Option[String],
    addedAt: LocalDateTime)
