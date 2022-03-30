package tech.pegb.backoffice.domain.transaction.model

import java.time.LocalDateTime
import java.util.{Currency, UUID}

import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.ServiceError.validationError
import tech.pegb.backoffice.domain.Validatable
import tech.pegb.backoffice.util.Implicits._
import scala.util.Try

case class TxnConfig(
    id: UUID,
    customerId: UUID,
    transactionType: String, //supposedly to be objectified, but Erlang is maintaining the transaction model
    currency: String,
    createdBy: String,
    createdAt: LocalDateTime,
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime]) extends Validatable[TxnConfig] {

  def validate: ServiceResponse[TxnConfig] = {
    for {
      _ ← TxnConfig.validateCommonFields(transactionType.toOption, currency.toOption, createdBy.toOption, updatedBy)
    } yield {
      this
    }
  }

}

object TxnConfig {
  def validateCommonFields(
    transactionType: Option[String],
    currency: Option[String],
    createdBy: Option[String],
    updatedBy: Option[String]): ServiceResponse[Unit] = {
    for {
      _ ← transactionType.map(v ⇒ if (v.trim.nonEmpty) Right(()) else Left(validationError("Transaction type cannot be empty"))).getOrElse(Right(()))
      _ ← currency.map(v ⇒ if (v.trim.nonEmpty) Right(()) else Left(validationError("Currency cannot be empty"))).getOrElse(Right(()))
      _ ← currency.map(v ⇒ Try(Currency.getInstance(v)).toEither.fold(_ ⇒ Left(validationError(s"Invalid currency [$v]")), _ ⇒ Right(()))).getOrElse(Right(()))
      _ ← createdBy.map(v ⇒ if (v.trim.nonEmpty) Right(()) else Left(validationError("created_by cannot be empty"))).getOrElse(Right(()))
      _ ← updatedBy.map(v ⇒ if (v.trim.nonEmpty) Right(()) else Left(validationError("updated_by cannot be empty"))).getOrElse(Right(()))
    } yield {
      ()
    }
  }
}
