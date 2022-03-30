package tech.pegb.backoffice.dao.provider.entity

import java.time.LocalDateTime

case class Provider(
    id: Int,
    userId: Int,
    serviceId: Option[Int],
    name: String,
    transactionType: String,
    icon: String,
    label: String,
    pgInstitutionId: Int,
    utilityPaymentType: Option[String],
    utilityMinPaymentAmount: Option[BigDecimal],
    utilityMaxPaymentAmount: Option[BigDecimal],
    isActive: Boolean,
    createdBy: String,
    createdAt: LocalDateTime,
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime]) {

}

object Provider {
  val cId = "id"
  val cUsrId = "user_id"
  val cServiceId = "service_id"
  val cName = "name"
  val cTransactionType = "transaction_type"
  val cIcon = "icon"
  val cLabel = "label"
  val cPgInstitutionId = "pg_institution_id"
  val cUtilPayType = "utility_payment_type"
  val cMinUtilPayType = "utility_min_payment_amount"
  val cMaxUtilPayType = "utility_max_payment_amount"
  val cIsActive = "is_active"
  val cCreatedAt = "created_at"
  val cUpdatedAt = "updated_at"
  val cCreatedBy = "created_by"
  val cUpdatedBy = "updated_by"
}
