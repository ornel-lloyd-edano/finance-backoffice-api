package tech.pegb.backoffice.domain.account.model

import java.time.LocalDateTime
import java.util.{Currency, UUID}

import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.ServiceError.validationError
import tech.pegb.backoffice.domain.{Validatable}
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

case class ExternalAccount(
    id: UUID,
    customerId: UUID,
    externalProvider: String,
    externalAccountNumber: String,
    externalAccountHolder: String,
    currency: String,
    createdBy: String,
    createdAt: LocalDateTime,
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime]) extends Validatable[ExternalAccount] {

  def validate: ServiceResponse[ExternalAccount] = {
    for {
      _ ← ExternalAccount.validateCommonFields(externalProvider.toOption, externalAccountNumber.toOption,
        externalAccountHolder.toOption, currency.toOption, createdBy.toOption, updatedBy)
    } yield {
      this
    }
  }
}

object ExternalAccount {

  def validateCommonFields(
    externalProvider: Option[String],
    externalAccountNumber: Option[String],
    externalAccountHolder: Option[String],
    currency: Option[String],
    createdBy: Option[String],
    updatedBy: Option[String]): ServiceResponse[Unit] = {
    for {
      _ ← externalProvider.map(v ⇒ if (v.trim.nonEmpty) Right(()) else Left(validationError("Provider cannot be empty"))).getOrElse(Right(()))
      _ ← externalAccountNumber.map(v ⇒ if (v.trim.nonEmpty) Right(()) else Left(validationError("Account number cannot be empty"))).getOrElse(Right(()))
      _ ← externalAccountHolder.map(v ⇒ if (v.trim.nonEmpty) Right(()) else Left(validationError("Account holder cannot be empty"))).getOrElse(Right(()))
      _ ← currency.map(v ⇒ if (v.trim.nonEmpty) Right(()) else Left(validationError("Currency cannot be empty"))).getOrElse(Right(()))
      _ ← currency.map(v ⇒ Try(Currency.getInstance(v)).toEither.fold(_ ⇒ Left(validationError(s"Invalid currency [$v]")), _ ⇒ Right(()))).getOrElse(Right(()))
      _ ← createdBy.map(v ⇒ if (v.trim.nonEmpty) Right(()) else Left(validationError("created_by cannot be empty"))).getOrElse(Right(()))
      _ ← updatedBy.map(v ⇒ if (v.trim.nonEmpty) Right(()) else Left(validationError("updated_by cannot be empty"))).getOrElse(Right(()))
    } yield {
      ()
    }
  }

}
