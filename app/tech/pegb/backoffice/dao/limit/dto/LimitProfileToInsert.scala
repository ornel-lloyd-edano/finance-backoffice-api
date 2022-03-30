package tech.pegb.backoffice.dao.limit.dto

import java.time.LocalDateTime

case class LimitProfileToInsert(
    limitType: String,
    userType: Option[String], //todo confirm if user type is optional
    tier: Option[String],
    subscription: Option[String],
    transactionType: Option[String],
    channel: Option[String],
    provider: Option[String],
    instrument: Option[String],
    maxIntervalAmount: Option[BigDecimal],
    maxAmount: Option[BigDecimal],
    minAmount: Option[BigDecimal],
    maxCount: Option[Int],
    interval: Option[String],
    currencyId: Int,
    createdBy: String,
    createdAt: LocalDateTime)
