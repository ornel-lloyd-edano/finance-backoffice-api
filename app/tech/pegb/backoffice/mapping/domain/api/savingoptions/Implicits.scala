package tech.pegb.backoffice.mapping.domain.api.savingoptions

import tech.pegb.backoffice.api.customer.dto.SavingOptionsToReadI
import tech.pegb.backoffice.api.swagger.model.SavingOptionsToRead
import tech.pegb.backoffice.domain.customer.model.GenericSavingOption
import tech.pegb.backoffice.util.Implicits._
object Implicits {

  implicit class SavingOptionsReadAdapter(val arg: GenericSavingOption) extends AnyVal {
    def asApi: SavingOptionsToReadI = SavingOptionsToRead(
      id = arg.id,
      customerId = arg.customerId,
      `type` = arg.savingType.underlying,
      name = arg.savingGoalName,
      amount = arg.amount,
      currentAmount = arg.currentAmount,
      currency = arg.currency.getCurrencyCode,
      reason = arg.reason,
      createdAt = arg.createdAt.toZonedDateTimeUTC,
      dueDate = arg.dueDate,
      updatedAt = arg.updatedAt.toZonedDateTimeUTC)
  }
}
