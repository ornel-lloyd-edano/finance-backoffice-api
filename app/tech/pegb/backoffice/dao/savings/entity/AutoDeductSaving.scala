package tech.pegb.backoffice.dao.savings.entity

import java.time.LocalDateTime

case class AutoDeductSaving(
    id: Int,
    uuid: String,
    userId: Int,
    userUuid: String,
    accountId: Int,
    accountUuid: String,
    currency: String,
    currentAmount: BigDecimal,
    savingPercentage: BigDecimal,
    minIncome: BigDecimal,
    statusUpdatedAt: Option[LocalDateTime],
    createdAt: LocalDateTime,
    isActive: Boolean,
    updatedAt: LocalDateTime) extends SavingOption {

}
