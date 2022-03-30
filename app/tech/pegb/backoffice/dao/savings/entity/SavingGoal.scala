package tech.pegb.backoffice.dao.savings.entity

import java.time.{LocalDate, LocalDateTime}

case class SavingGoal(
    id: Int,
    uuid: String,
    userId: Int,
    userUuid: String,
    accountId: Int,
    accountUuid: String,
    currency: String,
    dueDate: LocalDate,
    statusUpdatedAt: Option[LocalDateTime],
    createdAt: LocalDateTime,
    name: String,
    reason: Option[String],
    status: String,
    paymentType: String,
    goalAmount: BigDecimal,
    currentAmount: BigDecimal,
    initialAmount: BigDecimal,
    emiAmount: BigDecimal,
    updatedAt: LocalDateTime) extends SavingOption {

}
