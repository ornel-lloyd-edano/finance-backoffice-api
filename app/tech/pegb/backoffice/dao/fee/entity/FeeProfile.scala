package tech.pegb.backoffice.dao.fee.entity

import java.time.LocalDateTime
import java.util.UUID

import ai.x.play.json.Jsonx

case class FeeProfile(
    id: Int,
    uuid: UUID,
    feeType: String,
    userType: String,
    tier: String,
    subscription: String,
    transactionType: String,
    channel: Option[String],
    provider: Option[String],
    instrument: Option[String],
    calculationMethod: String,
    maxFee: Option[BigDecimal],
    minFee: Option[BigDecimal],
    feeAmount: Option[BigDecimal],
    feeRatio: Option[BigDecimal],
    feeMethod: String,
    taxIncluded: String,
    ranges: Option[Seq[FeeProfileRange]],
    currencyCode: String,
    createdAt: LocalDateTime,
    createdBy: String,
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String],
    deletedAt: Option[LocalDateTime])

object FeeProfile {
  implicit val f = Jsonx.formatCaseClass[FeeProfile]
}
