package tech.pegb.backoffice.mapping.domain.api.transaction

import java.time.LocalDateTime
import java.util.UUID

import cats.implicits._
import tech.pegb.backoffice.api.customer.dto.TxnConfigToRead
import tech.pegb.backoffice.api.transaction.dto._
import tech.pegb.backoffice.domain.transaction.model._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.domain.transaction.dto.ManualTxnToCreate
import tech.pegb.backoffice.core
import tech.pegb.backoffice.core.integration.abstraction.ManualTxnLinesToCreateCoreDto

import scala.util.Try

object Implicits {

  implicit class ManualTxnToCreateCoreAdapter(val arg: ManualTxnToCreate) extends AnyVal {
    def asApi(accountNumAndId: Map[String, Int]): Try[Seq[ManualTxnLinesToCreateCoreDto]] = Try {
      arg.manualTxnLines.sortBy(_.lineId).grouped(2).map(pair ⇒ {
        arg.manualTxnFxDetails match {
          case None ⇒
            core.integration.dto.ManualTxnLinesToCreate(
              primaryAccountId = accountNumAndId.get(pair.head.accountNumber).get,
              primaryExplanation = pair.head.explanation,
              primaryDirection = pair.head.direction.toString(),
              secondaryAccountId = accountNumAndId.get(pair.tail.head.accountNumber).get,
              secondaryExplanation = pair.tail.head.explanation,
              amount = pair.head.amount)
          case _ ⇒
            core.integration.dto.ManualTxnFxLinesToCreate(
              primaryAccountId = accountNumAndId.get(pair.head.accountNumber).get,
              primaryAmount = pair.head.amount,
              primaryExplanation = pair.head.explanation,
              secondaryAccountId = accountNumAndId.get(pair.tail.head.accountNumber).get,
              secondaryAmount = pair.tail.head.amount,
              secondaryExplanation = pair.tail.head.explanation)
        }
      }).toSeq
    }
  }

  implicit class ManualTxnLinesAdapter(val arg: ManualTransactionLines) extends AnyVal {
    def asApi(
      manualTxnId: UUID,
      status: Option[String],
      txnReason: Option[String],
      createdBy: Option[String],
      createdAt: Option[LocalDateTime]): ManualTransactionLinesToRead = {
      ManualTransactionLinesToRead(
        lineId = arg.lineId,
        manualTxnId = manualTxnId,
        account = arg.accountNumber,
        currency = arg.currency.getCurrencyCode.some,
        direction = arg.direction.toString(),
        amount = arg.amount,
        explanation = arg.explanation)
    }
  }

  implicit class ManualTxnApiAdapter(val arg: ManualTransaction) extends AnyVal {
    def asApi: ManualTransactionToRead = {
      ManualTransactionToRead(
        id = arg.id,
        status = arg.status,
        transactionReason = arg.reason,
        createdBy = arg.createdBy,
        createdAt = arg.createdAt.toZonedDateTimeUTC,
        manualTxnLines = arg.manualTxnLines.map(_.asApi(arg.id, Some(arg.status), Some(arg.reason),
          Some(arg.createdBy), Some(arg.createdAt))))
    }
  }

  implicit class SettlementFxHistoryApiAdapter(val arg: SettlementFxHistory) extends AnyVal {
    def asApi: SettlementFxHistoryToRead = {
      SettlementFxHistoryToRead(
        fxProvider = arg.fxProvider,
        fromCurrency = arg.fromCurrency,
        fromFlag = arg.fromIcon,
        toCurrency = arg.toCurrency,
        toFlag = arg.toIcon,
        fxRate = arg.fxRate,
        createdAt = arg.createdAt.toZonedDateTimeUTC)
    }
  }

  implicit class SettlementRecentAccountApiAdapter(val arg: SettlementRecentAccount) extends AnyVal {
    def asApi: SettlementRecentAccountToRead = {
      SettlementRecentAccountToRead(
        id = arg.accountId,
        customerName = arg.customerName.getOrElse("UNKNOWN"),
        accountNumber = arg.accountNumber,
        balance = arg.balance,
        currency = arg.currency)
    }
  }

  implicit class TransactionAdapter(val arg: Transaction) extends AnyVal {
    def asApi(reasonIfCancelledOrReversed: Option[String] = None): TransactionToRead = {
      TransactionToRead(
        id = arg.id,
        sequence = arg.sequence,
        primaryAccountId = arg.primaryAccountId,
        primaryAccountName = arg.primaryAccountName,
        primaryAccountNumber = arg.primaryAccountNumber,
        secondaryAccountId = arg.secondaryAccountId,
        secondaryAccountName = arg.secondaryAccountName,
        secondaryAccountNumber = arg.secondaryAccountNumber,
        primaryAccountCustomerName = arg.primaryAccountCustomerName,
        direction = arg.direction,
        `type` = arg.`type`,
        amount = arg.amount,
        currency = arg.currency.getCurrencyCode,
        exchangeCurrency = None,
        channel = arg.channel,
        explanation = arg.explanation,
        effectiveRate = arg.effectiveRate,
        costRate = arg.costRate,
        status = arg.status.underlying,
        createdAt = arg.createdAt.toZonedDateTimeUTC,
        updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC),
        previousBalance = arg.primaryAccountPreviousBalance,
        reason = reasonIfCancelledOrReversed)
    }
  }

  implicit class TxnConfigToApiAdapter(val arg: TxnConfig) extends AnyVal {
    def asApi = TxnConfigToRead(
      id = arg.id,
      customerId = arg.customerId,
      transactionType = arg.transactionType,
      currency = arg.currency,
      updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC))
  }
}
