package tech.pegb.backoffice.mapping.api.domain.aggregation

import tech.pegb.backoffice.api.aggregations.controllers.Constants
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo}
import tech.pegb.backoffice.domain.aggregations.dto.{TransactionGrouping, TxnAggregationsCriteria}
import tech.pegb.backoffice.util.Implicits._

object Implicits {

  private type CurrencyCode = String
  private type Institution = Option[String]
  private type TransactionType = Option[String]
  private type AccountType = Option[String]
  private type UserType = Option[String]
  private type DateFrom = Option[LocalDateTimeFrom]
  private type DateTo = Option[LocalDateTimeTo]
  private type AccountNumber = Option[String]
  //private type OtherParty = Option[String]

  implicit class TransactionSimpleCriteriaAdapter(val arg: (CurrencyCode, Institution, TransactionType, AccountType, UserType, DateFrom, DateTo, AccountNumber)) extends AnyVal {
    def asDomain = TxnAggregationsCriteria(
      currencyCode = Option(arg._1),
      institution = arg._2,
      transactionType = arg._3,
      isAnyTheseAccountTypes = arg._4 match {
        case Some(value) if value.equals(s"${Constants.AccType.UserBalanceSaving},${Constants.AccType.UserBalanceWallet}") ⇒ Option(Set(Constants.AccType.UserBalanceSaving, Constants.AccType.UserBalanceWallet))
        case Some(value) ⇒ Option(Set(value))
        case None ⇒ None
      },
      userType = arg._5,
      startDate = arg._6.map(_.localDateTime),
      endDate = arg._7.map(_.localDateTime),
      notLikeThisAccountNumber = arg._8)
  }

  //  implicit class TransactionAccountNumberCriteriaAdapter(val arg: (CurrencyCode, Institution, TransactionType, AccountType, UserType, DateFrom, DateTo, AccountNumber)) extends AnyVal {
  //    // new domain implicit for turnover with accountNumber filters
  //    def asDomain = TxnAggregationsCriteria(
  //      currencyCode = Option(arg._1),
  //      institution = arg._2,
  //      transactionType = arg._3,
  //      isAnyTheseAccountTypes = arg._4 match {
  //        case Some(value) if value.equals(s"${Constants.AccType.UserBalanceSaving},${Constants.AccType.UserBalanceWallet}") ⇒ Option(Set(Constants.AccType.UserBalanceSaving, Constants.AccType.UserBalanceWallet))
  //        case Some(value) ⇒ Option(Set(value))
  //        case None ⇒ None
  //      },
  //      userType = arg._5,
  //      dateTimeFrom = arg._6.map(_.localDateTime),
  //      dateTimeTo = arg._7.map(_.localDateTime),
  //      accountNumber = arg._8)
  //  }

  implicit class TransactionGroupingAdapter(val arg: String) extends AnyVal {
    def asDomain(frequency: Option[String] = None) = {
      val items: Seq[String] = arg.toSeqByComma.map(_.toLowerCase)
      TransactionGrouping(
        currencyCode = items.lenientContains(Constants.Grouping.CurrencyCode),
        institution = items.lenientContains(Constants.Grouping.Institution),
        transactionType = items.lenientContains(Constants.Grouping.TxnType),
        primaryAccountNumber = false,
        daily = items.lenientContains(Constants.Grouping.TimePeriod) && (frequency.isEmpty || frequency.lenientContains(Constants.Frequencies.Daily)),
        weekly = items.lenientContains(Constants.Grouping.TimePeriod) && frequency.lenientContains(Constants.Frequencies.Weekly),
        monthly = items.lenientContains(Constants.Grouping.TimePeriod) && frequency.lenientContains(Constants.Frequencies.Monthly))
    }
  }

}
