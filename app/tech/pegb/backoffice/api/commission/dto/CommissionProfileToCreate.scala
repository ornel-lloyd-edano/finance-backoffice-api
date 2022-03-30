package tech.pegb.backoffice.api.commission.dto

trait CommissionProfileToCreateTrait {
  def businessType: String
  def tier: String
  def subscriptionType: String
  def transactionType: String
  def currencyCode: String
  def channel: Option[String]
  def instrument: Option[String]
  def calculationMethod: String
  def maxCommission: Option[BigDecimal]
  def minCommission: Option[BigDecimal]
  def commissionAmount: Option[BigDecimal]
  def commissionRatio: Option[BigDecimal]
  def ranges: Option[Seq[CommissionProfileRangeToCreateTrait]]
}

case class CommissionProfileToCreate(
    businessType: String,
    tier: String,
    subscriptionType: String,
    transactionType: String,
    currencyCode: String,
    channel: Option[String],
    instrument: Option[String],
    calculationMethod: String,
    maxCommission: Option[BigDecimal],
    minCommission: Option[BigDecimal],
    commissionAmount: Option[BigDecimal],
    commissionRatio: Option[BigDecimal],
    ranges: Option[Seq[CommissionProfileRangeToCreate]]) extends CommissionProfileToCreateTrait
