package tech.pegb.backoffice.dao.commission.entity

import java.time.LocalDateTime

import ai.x.play.json.Jsonx

case class CommissionProfileRange(
    id: Int,
    commissionProfileId: Int,
    min: BigDecimal,
    max: Option[BigDecimal],
    commissionAmount: Option[BigDecimal],
    commissionRatio: Option[BigDecimal],
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime)

object CommissionProfileRange {
  implicit val f = Jsonx.formatCaseClass[CommissionProfileRange]

  val TableName = "commission_profile_ranges"
  val TableAlias = "com_pro_ranges"

  val cId = "id"
  val cCommissionProfileId = "commission_profile_id"
  val cMin = "min"
  val cMax = "max"
  val cCommissionAmount = "commission_amount"
  val cCommissionRatio = "commission_ratio"
  val cCreatedAt = "created_at"
  val cUpdatedAt = "updated_at"
}
