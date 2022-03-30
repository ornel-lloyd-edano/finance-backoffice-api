package tech.pegb.backoffice.mapping.api.domain.transaction

import java.time.{LocalDate, LocalDateTime, ZonedDateTime}
import java.util.{Currency, UUID}

import tech.pegb.backoffice.api.transaction.dto.{ManualTransactionFxDetailsT, ManualTxnToCreate, TxnToUpdateForCancellation, TxnToUpdateForReversal}
import tech.pegb.backoffice.domain.transaction.dto.{ManualTxnCriteria, ManualTxnFxDetails, SettlementFxHistoryCriteria, TransactionCriteria, TxnCancellation, TxnConfigCriteria, TxnReversal, ManualTxnToCreate ⇒ DomainManualTxnToCreate, TxnConfigToCreate ⇒ DomainTxnConfigToCreate, TxnConfigToUpdate ⇒ DomainTxnConfigToUpdate}
import tech.pegb.backoffice.domain.transaction.model.{DirectionTypes, ManualTransactionLines, TransactionStatus}
import DirectionTypes._
import tech.pegb.backoffice.api.customer.dto.{CustomerTxnConfigToCreate, TxnConfigToCreate, TxnConfigToUpdate}
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo}
import tech.pegb.backoffice.mapping.api.domain.account.Implicits.{CustomerId, ExternalAccountId}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.UUIDLike

import scala.math.BigDecimal.RoundingMode
import scala.util.Try

object Implicits {

  private type Id = Option[String]
  private type AnyCustomerName = Option[String]
  private type CustomerUUIDLike = Option[UUIDLike]
  private type AccountId = Option[UUIDLike]
  private type DateFrom = Option[LocalDateTimeFrom]
  private type DateTo = Option[LocalDateTimeTo]
  private type TxnType = Option[String]
  private type Channel = Option[String]
  private type Status = Option[String]
  private type CurrencyCode = Option[String]
  private type PartialMatchFields = Set[String]
  //private type OtherParty = Option[String]

  private type UserType = Option[String]
  private type AccountNumber = Option[String]

  implicit class QueryParamTxnCriteriaAdapter(val arg: (Id, AnyCustomerName, CustomerUUIDLike, AccountId, DateFrom, DateTo, TxnType, Channel, Status, CurrencyCode, PartialMatchFields)) extends AnyVal {
    def asDomain = Try(TransactionCriteria(
      id = arg._1.map(_.sanitize),
      anyCustomerName = arg._2.map(_.sanitize),
      customerId = arg._3,
      accountId = arg._4,
      startDate = arg._5.map(_.localDateTime),
      endDate = arg._6.map(_.localDateTime),
      transactionType = arg._7.map(_.sanitize),
      channel = arg._8.map(_.sanitize),
      status = arg._9.map(s ⇒ TransactionStatus(s.sanitize)),
      currencyCode = arg._10,
      partialMatchFields = arg._11))
  }

  implicit class TxnCriteriaAdapterForFloatMgmt(val arg: (Seq[String], LocalDate, LocalDate)) extends AnyVal {
    def asDomain: TransactionCriteria = {
      val (accountNumbers, dateFrom, dateTo) = arg
      TransactionCriteria(
        accountNumbers = accountNumbers,
        startDate = dateFrom.atTime(0, 0, 0).toOption,
        endDate = dateTo.atTime(23, 59, 59).toOption)
    }
  }

  implicit class TxnToUpdateForCancelAdapter(val arg: TxnToUpdateForCancellation) extends AnyVal {
    def asDomain(id: String, doneBy: String, doneAt: ZonedDateTime) = TxnCancellation(
      txnId = id,
      reason = arg.reason,
      cancelledBy = doneBy,
      cancelledAt = doneAt.toLocalDateTimeUTC,
      lastUpdatedAt = arg.lastUpdatedAt.map(_.toLocalDateTime))
  }

  implicit class TxnToUpdateForReverseAdapter(val arg: TxnToUpdateForReversal) extends AnyVal {
    def asDomain(id: String, doneBy: String, doneAt: ZonedDateTime) = TxnReversal(
      txnId = id,
      isFeeReversed = arg.isFeeReversed.getOrElse(false),
      reason = arg.reason.toOption,
      reversedBy = doneBy,
      reversedAt = doneAt.toLocalDateTimeUTC,
      lastUpdatedAt = arg.lastUpdatedAt.map(_.toLocalDateTimeUTC))
  }

  implicit class ManualTxnCriteriaApiAdapter(
      val arg: (Option[UUIDLike], Option[LocalDateTimeFrom], Option[LocalDateTimeTo])) extends AnyVal {
    def asDomain: ManualTxnCriteria = {
      ManualTxnCriteria(
        id = arg._1,
        startCreatedAt = arg._2.map(_.localDateTime.toLocalDate),
        endCreatedAt = arg._3.map(_.localDateTime.toLocalDate))
    }
  }

  implicit class SettlementFxHistoryCriteriaApiAdapter(val arg: (Option[String], Option[String], Option[String], Option[LocalDateTimeFrom], Option[LocalDateTimeTo])) extends AnyVal {
    def asDomain: SettlementFxHistoryCriteria = {
      SettlementFxHistoryCriteria(
        fxProvider = arg._1.map(_.sanitize),
        fromCurrency = arg._2.map(_.sanitize),
        toCurrency = arg._3.map(_.sanitize),
        createdAtFrom = arg._4.map(_.localDateTime),
        createdAtTo = arg._5.map(_.localDateTime))
    }
  }

  implicit class ManualTxnToCreateAdapter(val arg: ManualTxnToCreate) extends AnyVal {
    def asDomain(createdByP: String, createdAtP: ZonedDateTime, maybeId: Option[UUID] = None) = Try(
      DomainManualTxnToCreate(
        uuid = maybeId.getOrElse(UUID.randomUUID()), //this uuid should be coming from front end
        reason = arg.transactionReason,
        createdBy = createdByP,
        createdAt = createdAtP.toLocalDateTimeUTC,
        manualTxnFxDetails = arg.fxDetails.map(_.asDomain),
        manualTxnLines = arg.manualTxnLines.foldLeft(Seq.empty[Seq[ManualTransactionLines]]) {
          (domainManualTxnLines, currentManualTxn) ⇒
            domainManualTxnLines :+ List(
              ManualTransactionLines(
                lineId = (domainManualTxnLines.size * 2) + 1,
                accountNumber = currentManualTxn.primaryAccountNumber,
                currency = Currency.getInstance(currentManualTxn.primaryCurrency),
                direction = currentManualTxn.primaryDirection.toTxnDirection,
                amount = currentManualTxn.amount,
                explanation = currentManualTxn.primaryExplanation),
              ManualTransactionLines(
                lineId = (domainManualTxnLines.size * 2) + 2,
                currency = Currency.getInstance(currentManualTxn.secondaryCurrency),
                accountNumber = currentManualTxn.secondaryAccountNumber,
                direction = currentManualTxn.primaryDirection.toOppositeTxnDirection,
                amount = arg.fxDetails.fold(currentManualTxn.amount)(fxDetail ⇒ currentManualTxn.secondaryAmount.getOrElse((fxDetail.fxRate * currentManualTxn.amount).setScale(2, RoundingMode.HALF_UP))),
                explanation = currentManualTxn.secondaryExplanation))
        }.flatten))
  }

  implicit class ManualTxnFxInfoAdapter(val arg: ManualTransactionFxDetailsT) extends AnyVal {
    def asDomain = ManualTxnFxDetails(
      fxProvider = arg.fxProvider,
      fromCurrency = arg.fromCurrency,
      toCurrency = arg.toCurrency,
      fxRate = arg.fxRate)
  }

  implicit class TxnCriteriaAdapter(arg: (Seq[String], LocalDateTime, LocalDateTime)) {
    def asDomain: TransactionCriteria = TransactionCriteria(
      accountNumbers = arg._1,
      startDate = arg._2.toOption,
      endDate = arg._3.toOption)

  }

  implicit class CustomerTxnConfigToCreateDomainAdapter(val arg: CustomerTxnConfigToCreate) extends AnyVal {
    def asDomain(requestId: UUID, customerId: UUID, doneBy: String, doneAt: ZonedDateTime) = DomainTxnConfigToCreate(
      id = requestId,
      customerId = customerId,
      transactionType = arg.transactionType.sanitize,
      currency = arg.currency.sanitize,
      createdBy = doneBy.sanitize,
      createdAt = doneAt.toLocalDateTimeUTC)
  }

  implicit class TxnConfigToCreateDomainAdapter(val arg: TxnConfigToCreate) extends AnyVal {
    def asDomain(requestId: UUID, doneBy: String, doneAt: ZonedDateTime) = DomainTxnConfigToCreate(
      id = requestId,
      customerId = arg.customerId,
      transactionType = arg.transactionType.sanitize,
      currency = arg.currency.sanitize,
      createdBy = doneBy.sanitize,
      createdAt = doneAt.toLocalDateTimeUTC)
  }

  implicit class TxnConfigToUpdateDomainAdapter(val arg: TxnConfigToUpdate) extends AnyVal {
    def asDomain(doneBy: String, doneAt: ZonedDateTime) = DomainTxnConfigToUpdate(
      transactionType = arg.transactionType.map(_.sanitize),
      currency = arg.currency.map(_.sanitize),
      updatedBy = doneBy.sanitize,
      updatedAt = doneAt.toLocalDateTimeUTC,
      lastUpdatedAt = arg.lastUpdatedAt.map(_.toLocalDateTimeUTC))
  }

  private type TransactionType = Option[String]
  private type TxnConfigUUIDLike = Option[UUIDLike]
  implicit class TxnConfigQueryCriteriaDomainAdapter(val arg: (TxnConfigUUIDLike, CustomerUUIDLike, AnyCustomerName, TransactionType, CurrencyCode, PartialMatchFields)) extends AnyVal {
    def asDomain = TxnConfigCriteria(
      id = arg._1,
      customerId = arg._2,
      customerName = arg._3.map(_.sanitize),
      transactionType = arg._4.map(_.sanitize),
      currency = arg._5.map(_.sanitize),
      partialMatchFields = arg._6)
  }

  private type TxnConfigUUID = UUID
  private type CustomerUUID = UUID
  implicit class CustomerTxnConfigQueryParamsToDomainAdapter(val arg: (TxnConfigUUID, CustomerUUID)) extends AnyVal {
    def asDomain = {
      TxnConfigCriteria(id = Some(arg._1.toUUIDLike), customerId = Some(arg._2.toUUIDLike))
    }
  }
}
