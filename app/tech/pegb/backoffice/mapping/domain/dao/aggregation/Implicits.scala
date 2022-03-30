package tech.pegb.backoffice.mapping.domain.dao.aggregation

import java.time.LocalDateTime

import cats.implicits._
import tech.pegb.backoffice.api.aggregations.controllers.Constants.TxnType
import tech.pegb.backoffice.dao.DbConstants._
import tech.pegb.backoffice.dao.aggregations.abstraction.{AggFunction, ScalarFunctions}
import tech.pegb.backoffice.dao.aggregations.dto.{AggregationInput, Entity, GroupByInput, JoinColumn}
import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao
import tech.pegb.backoffice.dao.fee.dto.ThirdPartyFeeProfileCriteria
import tech.pegb.backoffice.dao.fee.sql.ThirdPartyFeeProfileSqlDao
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes}
import tech.pegb.backoffice.dao.transaction.dto.TransactionGroupings
import tech.pegb.backoffice.domain.aggregations.dto.{TransactionGrouping, TxnAggregationsCriteria}
import tech.pegb.backoffice.util.Implicits._

object Implicits {

  implicit class TransactionSimpleCriteriaDaoAdapter(val arg: TxnAggregationsCriteria) extends AnyVal {
    // needs full path to columns like : tableAlias.created_at
    def asGenericDao(txnGrouping: Option[TransactionGrouping]): Seq[CriteriaField[_]] = Seq(
       arg.currencyCode.map(CriteriaField[String](s"${currencyName}", _)),

      arg.institution match {
        case Some(value) ⇒
          CriteriaField[String](s"${txnProvider}", value).some
        case _ ⇒
          CriteriaField[String](s"${txnProviderId}", "", MatchTypes.IsNotNull).some
      },

      arg.transactionType.map{
        case value if value.equalsIgnoreCase(TxnType.EtcTxns) ⇒ CriteriaField(s"${txnType}", Set(TxnType.CashOut, TxnType.CashIn), MatchTypes.NotIn)
        case value ⇒ CriteriaField[String](s"${txnType}", value)
      },

      arg.accountType.map(CriteriaField[String](s"${accountType}", _)),
      arg.isAnyTheseAccountTypes.map(accountTypes ⇒ CriteriaField(s"${accountType}", accountTypes, MatchTypes.In)),

      arg.userType.map(CriteriaField[String](userType, _)),

      (arg.startDate, arg.endDate) match {
        case (Some(dateFrom), Some(dateTo))⇒
          CriteriaField[(LocalDateTime, LocalDateTime)](s"${txnCreatedAt}", (dateFrom, dateTo), MatchTypes.InclusiveBetween).toOption
        case (Some(dateFrom), None)⇒
          CriteriaField[LocalDateTime](s"${txnCreatedAt}", dateFrom, MatchTypes.GreaterOrEqual).toOption
        case (None, Some(dateTo))⇒
          CriteriaField[LocalDateTime](s"${txnCreatedAt}", dateTo, MatchTypes.LesserOrEqual).toOption
        case _⇒
          None
      },
      arg.notLikeThisAccountNumber.map(accNumber => CriteriaField[String](txnPrimaryAccountNumber, accNumber, MatchTypes.NotPartial))
    ).flatten

    def asGenericDaoWithoutTransaction: Seq[CriteriaField[_]] = Seq(
      arg.currencyCode.map(CriteriaField[String](s"${currencyName}", _)),

      arg.accountType.map(CriteriaField[String](s"${accountType}", _)),
      arg.isAnyTheseAccountTypes.map(accountTypes ⇒ CriteriaField(s"${accountType}", accountTypes, MatchTypes.In)),

      arg.userType.map(CriteriaField[String](userType, _)),

      (arg.startDate, arg.endDate) match {
        case (Some(dateFrom), Some(dateTo))⇒
          CriteriaField[(LocalDateTime, LocalDateTime)](s"${txnCreatedAt}", (dateFrom, dateTo), MatchTypes.InclusiveBetween).toOption
        case (Some(dateFrom), None)⇒
          CriteriaField[LocalDateTime](s"${txnCreatedAt}", dateFrom, MatchTypes.GreaterOrEqual).toOption
        case (None, Some(dateTo))⇒
          CriteriaField[LocalDateTime](s"${txnCreatedAt}", dateTo, MatchTypes.LesserOrEqual).toOption
        case _⇒
          None
      }).flatten
  }

  implicit class TxnGroupingAdapter(val arg: TransactionGrouping) extends AnyVal {
    def asDao: Seq[GroupByInput] = Seq(
      if(arg.currencyCode) GroupByInput(currencyName, None, None).toOption else None,
      if(arg.institution) GroupByInput(txnProvider, None, Some(txnProviderAlias)).toOption else None,
      if(arg.transactionType) GroupByInput(txnType, None, None).toOption else None,
      if(arg.primaryAccountNumber) GroupByInput(accountNumber, None, None).toOption else None,

      if(arg.daily) GroupByInput(txnCreatedAt, Some(ScalarFunctions.GetDate), Some("date")).toOption else None,
      if(arg.weekly) GroupByInput(txnCreatedAt, Some(ScalarFunctions.GetWeek), Some("week")).toOption else None,
      if(arg.monthly) GroupByInput(txnCreatedAt, Some(ScalarFunctions.GetMonth), Some("month")).toOption else None,
    ).flatten
  }

  implicit class EntityAdapter(val arg: (String, Option[String], Seq[Tuple2[String, String]])) extends AnyVal {
    def asDao = Entity(name = arg._1, alias = arg._2, joinColumns = arg._3.map {
      case (column1, column2)⇒ JoinColumn(column1, column2)
    })
  }

  implicit class AggregationInputAdapter(val arg: (String, AggFunction, Option[String])) extends AnyVal {
    def asDao = AggregationInput(columnOrExpression = arg._1, function = arg._2, alias = arg._3)
  }

  private type ProviderName = String
  private type CurrencyCode = Option[String]
  private type TxnType = Option[String]
  implicit class ThirdPartyFeeProfileCriteriaAdapter(val arg: (ProviderName, CurrencyCode, TxnType)) extends AnyVal {
    def asDao = ThirdPartyFeeProfileCriteria(
      provider = CriteriaField[String](ThirdPartyFeeProfileSqlDao.cOtherParty, arg._1).toOption,
      currencyCode = arg._2.map(CriteriaField[String](CurrencySqlDao.cName, _)),
      transactionType = arg._3.map(CriteriaField[String](ThirdPartyFeeProfileSqlDao.cTransactionType, _)))
  }
  //TODO apply type class implicit
  implicit class TransactionGroupingAdapter(val arg: TransactionGrouping) extends AnyVal {
    def asDao2 = TransactionGroupings(
      provider = arg.institution,
      currencyCode = arg.currencyCode,
      transactionType = arg.transactionType
    )
  }


}