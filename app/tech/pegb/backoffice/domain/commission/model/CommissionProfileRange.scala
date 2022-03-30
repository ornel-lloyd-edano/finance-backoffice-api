package tech.pegb.backoffice.domain.commission.model

import java.time.LocalDateTime

case class CommissionProfileRange(
    id: Int,
    commissionProfileId: Int,
    min: BigDecimal,
    max: Option[BigDecimal],
    flatAmount: Option[BigDecimal],
    percentageAmount: Option[BigDecimal],
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime)

