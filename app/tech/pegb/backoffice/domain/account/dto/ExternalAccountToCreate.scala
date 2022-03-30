package tech.pegb.backoffice.domain.account.dto

import java.time.LocalDateTime
import java.util.{UUID}

import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.{Validatable}
import tech.pegb.backoffice.domain.account.model.ExternalAccount
import tech.pegb.backoffice.util.Implicits._

case class ExternalAccountToCreate(
    id: UUID,
    customerId: UUID,
    externalProvider: String,
    externalAccountNumber: String,
    externalAccountHolder: String,
    currency: String,
    createdBy: String,
    createdAt: LocalDateTime) extends Validatable[ExternalAccountToCreate] {

  def validate: ServiceResponse[ExternalAccountToCreate] = {
    for {
      _ ← ExternalAccount.validateCommonFields(externalProvider.toOption, externalAccountNumber.toOption,
        externalAccountHolder.toOption, currency.toOption, createdBy.toOption, None)
    } yield {
      this
    }
  }

}
