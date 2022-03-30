package tech.pegb.backoffice.domain.account.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.Validatable
import tech.pegb.backoffice.domain.account.model.ExternalAccount
import tech.pegb.backoffice.util.Implicits._

case class ExternalAccountToUpdate(
    externalProvider: Option[String] = None,
    externalAccountHolder: Option[String] = None,
    externalAccountNumber: Option[String] = None,
    currency: Option[String] = None,
    updatedBy: String,
    updatedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime] = None) extends Validatable[ExternalAccountToUpdate] {
  def validate: ServiceResponse[ExternalAccountToUpdate] = {
    for {
      _ ← ExternalAccount.validateCommonFields(externalProvider, externalAccountNumber,
        externalAccountHolder, currency, None, updatedBy.toOption)
    } yield {
      this
    }
  }
}
