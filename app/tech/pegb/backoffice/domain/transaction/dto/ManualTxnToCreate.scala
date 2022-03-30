package tech.pegb.backoffice.domain.transaction.dto

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.{ServiceError, Validatable}
import tech.pegb.backoffice.domain.transaction.model.{ManualTransaction, ManualTransactionLines}

case class ManualTxnToCreate(
    uuid: UUID,
    reason: String,
    createdBy: String,
    createdAt: LocalDateTime,
    manualTxnFxDetails: Option[ManualTxnFxDetails],
    manualTxnLines: Seq[ManualTransactionLines]) extends Validatable[Unit] {
  import ManualTransaction._
  validateNonEmptyField(reason, "reason")
  validateNonEmptyField(createdBy, "createdBy")

  def validate: ServiceResponse[Unit] = {
    for {
      _ ← validateCurrencies
    } yield {
      ()
    }
  }

  def validateCurrencies: ServiceResponse[Unit] = {
    manualTxnFxDetails match {
      case None ⇒ Right(())
      case Some(txnFxDetails) ⇒
        if (manualTxnLines.grouped(2).toSeq.forall((txnGroup ⇒
          txnGroup.head.currency.getCurrencyCode == txnFxDetails.fromCurrency &&
            txnGroup.last.currency.getCurrencyCode == txnFxDetails.toCurrency)))
          Right(())
        else
          Left(ServiceError.validationError("primary_currency == from_currency AND secondary_currency == to_currency for all TransactionLines"))
    }
  }
}
