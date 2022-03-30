package tech.pegb.backoffice.api.commission.dto

trait CommissionProfileRangeToCreateTrait {
  def max: Option[BigDecimal]
  def min: BigDecimal
  def commissionAmount: Option[BigDecimal]
  def commissionRatio: Option[BigDecimal]
}

case class CommissionProfileRangeToCreate(
    max: Option[BigDecimal],
    min: BigDecimal,
    commissionAmount: Option[BigDecimal],
    commissionRatio: Option[BigDecimal]) extends CommissionProfileRangeToCreateTrait

