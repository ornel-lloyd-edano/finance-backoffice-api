package tech.pegb.backoffice.domain.customer.model

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.Validatable
import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.Msisdn

case class Contact(
    id: Int,
    uuid: UUID,
    buApplicationId: Option[Int],
    buApplicationUUID: Option[UUID],
    userId: Option[Int],
    userUUID: Option[UUID],
    contactType: ContactType,
    name: String,
    middleName: Option[String],
    surname: String,
    phoneNumber: Msisdn,
    email: Email,
    idType: String,
    createdBy: String,
    createdAt: LocalDateTime,
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime],
    vpUserId: Option[Int],
    vpUserUUID: Option[UUID],
    isActive: Boolean) extends Validatable[Unit] {

  def validate: ServiceResponse[Unit] = {
    for {
      _ ← ContactTypes.validate(contactType.toString)
    } yield {
      ()
    }
  }

}
