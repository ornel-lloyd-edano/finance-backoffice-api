package tech.pegb.backoffice.domain.customer.model

import java.time.LocalDateTime
import java.util.UUID

import cats.implicits._
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.businessuserapplication.model.AddressCoordinates
import tech.pegb.backoffice.domain.{ServiceError, Validatable}

case class ContactAddress(
    id: Int,
    uuid: UUID,
    buApplicationId: Option[Int],
    buApplicationUuid: Option[UUID],
    userId: Option[Int],
    userUuid: Option[UUID],
    addressType: AddressType,
    countryId: Int,
    countryName: String,
    city: String,
    postalCode: Option[String],
    address: Option[String],
    coordinates: Option[AddressCoordinates],
    createdBy: String,
    createdAt: LocalDateTime,
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime],
    isActive: Boolean) extends Validatable[Unit] {

  def validate: ServiceResponse[Unit] = {
    for {
      _ ← AddressTypes.validate(addressType.toString)
      _ ← coordinates match {
        case Some(coord) ⇒ coord.validate
        case None ⇒ ().asRight[ServiceError]
      }
    } yield {
      ()
    }
  }

}
