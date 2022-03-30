package tech.pegb.backoffice.domain.customer.model

import java.time.LocalDateTime
import java.util.UUID

final case class PaymentOption(
    id: Int,
    customerId: UUID,
    `type`: Int,
    provider: Int,
    maskedNumber: String,
    additionalData: Option[String],
    addedAt: LocalDateTime)
