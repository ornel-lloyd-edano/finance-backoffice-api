package tech.pegb.backoffice.domain.transaction.model

import java.time.LocalDateTime
import java.util.{Currency, UUID}

import cats.implicits._
import tech.pegb.backoffice.util.Implicits._
import DirectionTypes._

import scala.math.BigDecimal.RoundingMode

case class ManualTransaction(
    id: UUID,
    status: String,
    reason: String,
    createdBy: String,
    createdAt: LocalDateTime,
    groupByManualTxnId: Boolean,
    lineId: Option[Int] = None,
    accountNumber: Option[String] = None,
    currency: Option[Currency] = None,
    direction: Option[DirectionType] = None,
    amount: Option[BigDecimal] = None,
    explanation: Option[String] = None,
    manualTxnLines: Seq[ManualTransactionLines] = Seq.empty) {

  ManualTransaction.validate(this)
}

object ManualTransaction {

  def validate(arg: ManualTransaction): Unit = {
    validateNonEmptyField(arg.reason, "reason")
    validateNonEmptyField(arg.status, "status")
    validateNonEmptyField(arg.createdBy, "createdBy")

    if (arg.groupByManualTxnId) {
      assert(arg.manualTxnLines.nonEmpty, "manualTxnLines must not be empty if grouping by manual txn id")

      validateManualTxnLines(arg.manualTxnLines)

      assert(arg.lineId.isEmpty, "lineId must be empty if grouping by manual txn id")
      assert(arg.accountNumber.isEmpty, "accountNumber must be empty if grouping by manual txn id")
      assert(arg.currency.isEmpty, "currency must be empty if grouping by manual txn id")
      assert(arg.direction.isEmpty, "direction must be empty if grouping by manual txn id")
      assert(arg.amount.isEmpty, "amount must be empty if grouping by manual txn id")
      assert(arg.explanation.isEmpty, "explanation must be empty if grouping by manual txn id")
    } else {
      assert(arg.manualTxnLines.isEmpty, "manualTxnLines must be empty if not grouping by manual txn id")
      assert(arg.lineId.nonEmpty, "lineId must not be empty if not grouping by manual txn id")
      assert(arg.accountNumber.nonEmpty, "accountNumber must not be empty if not grouping by manual txn id")
      assert(arg.currency.nonEmpty, "currency must not be empty if not grouping by manual txn id")
      assert(arg.direction.nonEmpty, "direction must not be empty if not grouping by manual txn id")
      assert(arg.amount.nonEmpty, "amount must not be empty if not grouping by manual txn id")
      assert(arg.explanation.nonEmpty, "explanation must not be empty if not grouping by manual txn id")
      validateNonEmptyField(arg.explanation, "explanation")
    }
  }

  def validateNonEmptyField(fieldName: String, fieldValue: String): Unit = {
    assert(fieldValue.hasSomething, s"$fieldName must not be empty")
  }

  def validateNonEmptyField(fieldName: Option[String], fieldValue: String): Unit = {
    assert(fieldValue.hasSomething, s"$fieldName must not be empty")
  }

  def validateManualTxnLines(lines: Seq[ManualTransactionLines]): Unit = {
    //NOTE it's the current limitation of the system according to Salih
    //1 debit will always have 1 credit although in real life scenario 1 debit can have 1 or more credit and vice versa
    assert(
      lines.size >= 2 &&
        lines.filter(_.direction.isCredit).size == lines.filter(_.direction.isDebit).size,
      "debit and credit manual transaction lines should come in pairs")
  }

  implicit class RichManualTxns(val manualTxns: Iterable[ManualTransaction]) extends AnyVal {
    def flattenManualTxns = {
      manualTxns.flatMap(manualTxn ⇒ {
        manualTxn.manualTxnLines.map(line ⇒ {
          ManualTransaction(
            id = manualTxn.id,
            status = manualTxn.status,
            reason = manualTxn.reason,
            createdBy = manualTxn.createdBy,
            createdAt = manualTxn.createdAt,
            groupByManualTxnId = false,
            lineId = Some(line.lineId),
            accountNumber = Some(line.accountNumber),
            currency = line.currency.some,
            direction = Some(line.direction),
            amount = Some(line.amount),
            explanation = Some(line.explanation),
            manualTxnLines = Seq.empty)
        })
      })
    }
  }

}

//TODO decouple domain read and create models later (avoid Option in currency)
case class ManualTransactionLines(
    lineId: Int,
    accountNumber: String,
    currency: Currency,
    direction: DirectionType,
    amount: BigDecimal,
    explanation: String) {

  assert(lineId > 0, "lineId must be greater than zero")
  assert(accountNumber.hasSomething, "accountNumber must not be empty")
  assert(explanation.hasSomething, "explanation must not be empty")
  assert(amount > 0, "amount must be greater than zero because direction is specified")
  assert(amount.scale <= 4, "amount must not be more than 4 decimal points")
}

object ManualTransactionLines {
  def apply(
    lineId: Int,
    accountNumber: String,
    currency: String,
    direction: String,
    amount: BigDecimal,
    explanation: String) = {
    new ManualTransactionLines(lineId = lineId, accountNumber = accountNumber,
      currency = Currency.getInstance(currency), direction = direction.toTxnDirection,
      amount = amount.setScale(2, RoundingMode.HALF_EVEN), explanation = explanation)
  }
}
