package tech.pegb.backoffice.dao.commission.dto

import java.time.LocalDateTime

import cats.data.NonEmptyList

case class CommissionProfileToInsert(
    uuid: String,
    businessType: String,
    tier: String,
    subscriptionType: String,
    transactionType: String,
    currencyId: Int,
    channel: Option[String],
    instrument: Option[String],
    calculationMethod: String,
    maxCommission: Option[BigDecimal],
    minCommission: Option[BigDecimal],
    commissionAmount: Option[BigDecimal],
    commissionRatio: Option[BigDecimal],
    ranges: Option[NonEmptyList[CommissionProfileRangeToInsert]],
    createdBy: String,
    createdAt: LocalDateTime)
