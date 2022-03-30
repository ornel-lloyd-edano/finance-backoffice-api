package tech.pegb.backoffice.domain.reconciliation.model

import java.time.{LocalDate, LocalDateTime}
import java.util.Currency

import tech.pegb.backoffice.util.Implicits._

case class InternReconResult(
    reconDate: LocalDate,
    account: AccountForRecon,
    prevTransaction: Option[ReconTransaction],
    currentTransaction: ReconTransaction) {

  //if current txn's account.mainType is liability and previous txn is credit then
  //if current txn's previous_balance = previous txn's previous_balance + previous txn's amount then
  //reconciliation status is OK else NOK
  //if current txn's account.mainType is liability and previous txn is debit then
  //if current txn's previous_balance = previous txn's previous_balance - previous txn's amount
  //reconciliation status is OK else NOK
  //if current txn's account.mainType is asset and previous txn is credit then
  //if current txn's previous_balance = previous txn's previous_balance - previous txn's amount
  //reconciliation status is OK else NOK
  //if current txn's account.mainType is asset and previous txn is debit then
  //if current txn's previous_balance = previous txn's previous_balance + previous txn's amount
  //reconciliation status is OK else NOK

  val currentPreviousBalance: Option[BigDecimal] = currentTransaction.previousBalance
  val status: ReconciliationStatus = (prevTransaction, currentPreviousBalance) match {
    case (Some(prevTxn), Some(cPrevBalance)) ⇒ getBalanceWithPreTxnAndCurrentTxnPreBalance(prevTxn, cPrevBalance)
      .map { balanceAfterTxn ⇒
        if (currentPreviousBalance.getOrElse(BigDecimal(0.0)) == balanceAfterTxn)
          ReconciliationStatuses.OK else ReconciliationStatuses.NOK
      }.getOrElse(ReconciliationStatuses.NOK)

    case (None, Some(_)) ⇒ ReconciliationStatuses.OK
    case (_, None) ⇒
      ReconciliationStatuses.NOK

  }

  def getBalanceWithPreTxnAndCurrentTxnPreBalance(
    prevTransaction: ReconTransaction,
    currentPreviousBalance: BigDecimal): Option[BigDecimal] = {
    if (prevTransaction.previousBalance.isDefined) {
      (account.mainType, prevTransaction.direction) match {
        case (AccountMainTypes.Liability, TransactionDirections.Credit) ⇒
          Some(prevTransaction.previousBalance.getOrElse(BigDecimal(0.0)) + prevTransaction.amount)
        case (AccountMainTypes.Liability, TransactionDirections.Debit) ⇒
          Some(prevTransaction.previousBalance.getOrElse(BigDecimal(0.0)) - prevTransaction.amount)
        case (AccountMainTypes.Asset, TransactionDirections.Credit) ⇒
          Some(prevTransaction.previousBalance.getOrElse(BigDecimal(0.0)) - prevTransaction.amount)
        case (AccountMainTypes.Asset, TransactionDirections.Debit) ⇒
          Some(prevTransaction.previousBalance.getOrElse(BigDecimal(0.0)) + prevTransaction.amount)
        case (_, _) ⇒ None
      }
    } else {
      None
    }
  }
}

case class ReconTransaction(
    id: String,
    uniqueId: Int,
    sequence: Int,
    accountId: String,
    amount: BigDecimal,
    currency: Currency,
    direction: TransactionDirection,
    previousBalance: Option[BigDecimal],
    dateTime: LocalDateTime)

trait TransactionDirection {
  val underlying: String

  override def toString = underlying
}

object TransactionDirections {

  case class UnknownTransactionDirection(underlying: String) extends TransactionDirection

  case object Credit extends TransactionDirection {
    override val underlying: String = "credit"
  }

  case object Debit extends TransactionDirection {
    override val underlying: String = "debit"
  }

  def fromString: PartialFunction[String, TransactionDirection] = {
    case cr if cr === Credit.underlying ⇒ Credit
    case dr if dr === Debit.underlying ⇒ Debit
    case other ⇒ UnknownTransactionDirection(other)
  }
}

trait ReconciliationStatus {
  val underlying: String

  override def toString = underlying
}

object ReconciliationStatuses {

  case class UnknownReconciliationStatus(underlying: String) extends ReconciliationStatus

  case object OK extends ReconciliationStatus {
    override val underlying: String = "SUCCESS"
  }

  case object NOK extends ReconciliationStatus {
    override val underlying: String = "FAIL"
  }

  case object SOLVED extends ReconciliationStatus {
    override val underlying: String = "SOLVED"
  }

  def fromString: PartialFunction[String, ReconciliationStatus] = {
    case ok if ok === OK.underlying ⇒ OK
    case nok if nok === NOK.underlying ⇒ NOK
    case solved if solved === SOLVED.underlying ⇒ SOLVED
    case other ⇒ UnknownReconciliationStatus(other)
  }

}

