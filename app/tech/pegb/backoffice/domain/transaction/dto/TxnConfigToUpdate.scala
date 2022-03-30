package tech.pegb.backoffice.domain.transaction.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.domain.Validatable
import tech.pegb.backoffice.domain.transaction.model.TxnConfig

case class TxnConfigToUpdate(
    transactionType: Option[String],
    currency: Option[String],
    updatedBy: String,
    updatedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime] = None) extends Validatable[TxnConfigToUpdate] {
  def validate = {
    for {
      _ ← TxnConfig.validateCommonFields(transactionType, currency, None, Some(updatedBy))
    } yield {
      this
    }
  }
}
