package tech.pegb.backoffice.mapping.dao.domain.transaction

import java.util.{Currency, UUID}

import cats.implicits._
import tech.pegb.backoffice.dao
import tech.pegb.backoffice.dao.transaction.entity.{Settlement, SettlementFxHistory, SettlementLines, SettlementRecentAccount, TxnConfig}
import tech.pegb.backoffice.domain.transaction.model.{DirectionTypes, ManualTransaction, ManualTransactionLines, Transaction, TransactionStatus, TxnConfig ⇒ DomainTxnConfig}
import DirectionTypes._
import tech.pegb.backoffice.dao.transaction.dto.TransactionAggregation
import tech.pegb.backoffice.domain.model.TransactionAggregatation
import tech.pegb.backoffice.domain.transaction.model

import scala.util.Try

object Implicits {

  implicit class TransactionAdapter(arg: dao.transaction.entity.Transaction) {
    def asDomain = Try {
      Transaction(
        id = arg.id.toString,
        uniqueId = arg.uniqueId,
        sequence = arg.sequence,
        primaryAccountId = arg.primaryAccountUuid,
        primaryAccountName = arg.primaryAccountName,
        primaryAccountNumber = arg.primaryAccountNumber,
        primaryAccountType = arg.primaryAccountType,
        primaryAccountCustomerName =
          (arg.primaryIndividualUsersFullname, arg.primaryIndividualUsersName,
            arg.primaryBusinessUsersBusinessName, arg.primaryBusinessUsersBrandName) match {
              case (Some(fullName), _, _, _) ⇒ arg.primaryIndividualUsersFullname
              case (_, Some(name), _, _) ⇒ arg.primaryIndividualUsersName
              case (_, _, Some(name), _) ⇒ arg.primaryBusinessUsersBusinessName
              case (_, _, _, Some(name)) ⇒ arg.primaryBusinessUsersBrandName
              case _ ⇒ arg.primaryUsername
            },
        secondaryAccountId = arg.secondaryAccountUuid,
        secondaryAccountName = arg.secondaryAccountName,
        secondaryAccountNumber = arg.secondaryAccountNumber,
        direction = arg.direction.get,
        `type` = arg.`type`.get,
        amount = arg.amount.get,
        currency = arg.currency.map(Currency.getInstance(_)).get,
        exchangedCurrency = arg.exchangedCurrency.map(Currency.getInstance(_)),
        channel = arg.channel.getOrElse("UNKNOWN"),
        explanation = arg.explanation,
        effectiveRate = arg.effectiveRate,
        costRate = arg.costRate,
        status = arg.status.map(TransactionStatus(_)).get,
        instrument = arg.instrument,
        createdAt = arg.createdAt.get,
        updatedAt = arg.updatedAt,
        primaryAccountPreviousBalance = arg.primaryAccountPreviousBalance)
    }
  }

  implicit class SettlementLinesAdapter(val arg: SettlementLines) extends AnyVal {
    def asDomain(lineNum: Int) = Try(ManualTransactionLines(
      lineId = lineNum,
      accountNumber = arg.accountNumber,
      currency = Currency.getInstance(arg.currency),
      direction = arg.direction.toTxnDirection,
      amount = arg.amount,
      explanation = arg.explanation))
  }

  implicit class SettlementAdapter(val arg: Settlement) extends AnyVal {
    def asDomain = Try(ManualTransaction(
      id = UUID.fromString(arg.uuid),
      status = arg.status,
      reason = arg.transactionReason,
      createdBy = arg.createdBy,
      createdAt = arg.createdAt,
      groupByManualTxnId = true,
      manualTxnLines = arg.lines.sortBy(_.id).foldLeft(Seq.empty[ManualTransactionLines]) {
        (accumulator, currentSettlementLine) ⇒
          val lineNum = accumulator.size + 1
          accumulator :+ currentSettlementLine.asDomain(lineNum).get
      }))
  }

  implicit class SettlementFxHistoryAdapter(val arg: SettlementFxHistory) extends AnyVal {
    def asDomain = model.SettlementFxHistory(
      fxProvider = arg.fxProvider,
      fromCurrencyId = arg.fromCurrencyId,
      fromCurrency = arg.fromCurrency,
      fromIcon = arg.fromIcon,
      toCurrencyId = arg.toCurrencyId,
      toCurrency = arg.toCurrency,
      toIcon = arg.toIcon,
      fxRate = arg.fxRate,
      createdAt = arg.createdAt)
  }

  implicit class SettlementRecentAccountAdapter(val arg: SettlementRecentAccount) extends AnyVal {
    def asDomain = model.SettlementRecentAccount(
      accountId = arg.accountId,
      accountUUID = arg.accountUUID,
      customerName = arg.customerName,
      accountNumber = arg.accountNumber,
      accountName = arg.accountName,
      balance = arg.balance,
      currency = arg.currency)
  }

  implicit class TransactionAggregateAdapter(val arg: TransactionAggregation) extends AnyVal {
    def asDomain = Try(
      TransactionAggregatation(
        uniqueId = UUID.randomUUID().toString,
        direction = arg.direction,
        `type` = arg.`type`,
        amount = arg.amount,
        currency = arg.currency.map(Currency.getInstance(_)),
        exchangedCurrency = arg.exchangedCurrency.map(Currency.getInstance(_)),
        channel = arg.channel,
        effectiveRate = arg.effectiveRate,
        costRate = arg.costRate,
        status = arg.status.map(TransactionStatus(_)),
        instrument = arg.instrument,
        createdAt = arg.createdAt,
        sum = arg.sum,
        count = arg.count,
        date = arg.date,
        day = arg.day,
        month = arg.month,
        year = arg.year,
        hour = arg.hour,
        minute = arg.minute))
  }

  implicit class TxnConfigDaoToDomainAdapter(val arg: TxnConfig) extends AnyVal {
    def asDomain = DomainTxnConfig(
      id = arg.uuid,
      customerId = arg.userUuid,
      transactionType = arg.transactionType,
      currency = arg.currencyName,
      createdBy = arg.createdBy,
      createdAt = arg.createdAt,
      updatedBy = arg.updatedBy,
      updatedAt = arg.updatedAt)
  }
}
