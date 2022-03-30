package tech.pegb.backoffice.domain.customer.model

trait SavingOptionType {
  val underlying: String
  override def toString = underlying
}

object SavingOptionTypes {
  case object SavingGoals extends SavingOptionType {
    val underlying = "saving_goals"
  }

  case object RoundUp extends SavingOptionType {
    val underlying = "roundup_savings"
  }

  case object AutoDeduct extends SavingOptionType {
    val underlying = "auto_deduct_savings"
  }
}
