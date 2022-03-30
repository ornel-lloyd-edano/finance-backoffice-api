package tech.pegb.backoffice.dao.commission.entity

import java.time.LocalDateTime

import ai.x.play.json.Jsonx

case class CommissionProfile(
    id: Int,
    uuid: String,
    businessType: String,
    tier: String,
    subscriptionType: String,
    transactionType: String,
    currencyId: Int,
    currencyCode: String,
    channel: Option[String],
    instrument: Option[String],
    calculationMethod: String,
    maxCommission: Option[BigDecimal],
    minCommission: Option[BigDecimal],
    commissionAmount: Option[BigDecimal],
    commissionRatio: Option[BigDecimal],
    ranges: Option[Seq[CommissionProfileRange]],
    createdBy: String,
    updatedBy: String,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
    deletedAt: Option[LocalDateTime]) {

  def addRanges(rangesToAdd: Option[Seq[CommissionProfileRange]]): CommissionProfile = {
    this.copy(ranges = rangesToAdd)
  }

}

object CommissionProfile {
  implicit val f = Jsonx.formatCaseClass[CommissionProfile]

  val TableName = "commission_profiles"
  val TableAlias = "com_pro"

  val cId = "id"
  val cUuid = "uuid"
  val cBusinessType = "business_type"
  val cTier = "tier"
  val cSubscriptionType = "subscription_type"
  val cTransactionType = "transaction_type"
  val cCurrencyId = "currency_id"
  val cChannel = "channel"
  val cInstrument = "instrument"
  val cCalculationMethod = "calculation_method"
  val cMaxCommission = "max_commission"
  val cMinCommission = "min_commission"
  val cCommissionAmount = "commission_amount"
  val cCommissionRatio = "commission_ratio"
  val cCreatedBy = "created_by"
  val cUpdatedBy = "updated_by"
  val cCreatedAt = "created_at"
  val cUpdatedAt = "updated_at"
  val cDeletedAt = "deleted_at"

  val cCurrencyCode = "currency_code"
}
