package tech.pegb.backoffice.domain.customer.model

import java.time.{LocalDate, LocalDateTime}
import java.util.{Currency, UUID}

case class GenericSavingOption(
    id: UUID,
    customerId: UUID,
    savingType: SavingOptionType,
    savingGoalName: Option[String] = None,
    amount: Option[BigDecimal],
    currentAmount: BigDecimal,
    currency: Currency,
    reason: Option[String],
    createdAt: LocalDateTime,
    dueDate: Option[LocalDate],
    updatedAt: LocalDateTime) {

  //do not add model validations here, backoffice api does not own the tables
}
