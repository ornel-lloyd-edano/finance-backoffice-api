package tech.pegb.backoffice.domain.limit.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.domain.limit.model.LimitProfile
import tech.pegb.backoffice.util.LastUpdatedAt

case class LimitProfileToUpdate(
    maxIntervalAmount: Option[BigDecimal],
    maxAmount: Option[BigDecimal],
    minAmount: Option[BigDecimal],
    maxCount: Option[Int],
    maxBalanceAmount: Option[BigDecimal],
    updatedBy: String,
    updatedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime]) extends LastUpdatedAt {

  LimitProfile.assertLimitProfileAmounts(maxIntervalAmount, maxAmount, minAmount, maxCount)
}
