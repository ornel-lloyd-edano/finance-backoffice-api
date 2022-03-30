package tech.pegb.backoffice.dao.limit.entity

import java.time.LocalDateTime

case class LimitProfileHistory(
    id: Int,
    limitProfileId: Int,
    oldMaxIntervalAmount: Option[BigDecimal],
    oldMaxAmount: Option[BigDecimal],
    oldMinAmount: Option[BigDecimal],
    oldMaxCount: Option[Int],
    newMaxIntervalAmount: Option[BigDecimal],
    newMaxAmount: Option[BigDecimal],
    newMinAmount: Option[BigDecimal],
    newMaxCount: Option[Int],
    updatedBy: String,
    updatedAt: LocalDateTime,
    reason: Option[String])
