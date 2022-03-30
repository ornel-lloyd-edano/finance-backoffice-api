package tech.pegb.backoffice.domain.transaction.dto

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.domain.Validatable
import tech.pegb.backoffice.domain.transaction.model.TxnConfig
import tech.pegb.backoffice.util.Implicits._

case class TxnConfigToCreate(
    id: UUID,
    customerId: UUID,
    transactionType: String,
    currency: String,
    createdBy: String,
    createdAt: LocalDateTime) extends Validatable[TxnConfigToCreate] {
  def validate = {
    for {
      _ ← TxnConfig.validateCommonFields(transactionType.toOption, currency.toOption, createdBy.toOption, None)
    } yield {
      this
    }
  }
}
