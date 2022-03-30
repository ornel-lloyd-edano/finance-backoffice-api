package tech.pegb.backoffice.domain.businessuserapplication.model

import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.{ServiceError, Validatable, ValidatableAsGroup}
import tech.pegb.backoffice.domain.businessuserapplication.abstraction.AddressTypes
import tech.pegb.backoffice.util.Implicits._

case class ContactAddress(
    addressType: String,
    country: String,
    city: String,
    postalCode: Option[String],
    address: String,
    coordinates: Option[AddressCoordinates]) extends Validatable[Unit] {

  def validate: ServiceResponse[Unit] = {
    (AddressTypes.toSeq.contains(addressType), coordinates.map(_.validate)) match {
      case (false, _) ⇒
        Left(ServiceError.validationError(s"invalid address_type [$addressType]. Valid choices: ${AddressTypes.toSeq.defaultMkString}"))
      case (_, Some(Left(validationError))) ⇒
        Left(validationError)
      case _ ⇒
        Right(())
    }
  }

}

object ContactAddress {

  implicit object ContactAddressesValidation extends ValidatableAsGroup[ContactAddress] {
    override def validate(arg: Iterable[ContactAddress]): ServiceResponse[Unit] = {
      val addressIdentity = arg.map(addr ⇒ (addr.country.toLowerCase.trim, addr.city.toLowerCase.trim, addr.address.toLowerCase.trim)).toSeq
      val countryPostalCodes = arg.filter(_.postalCode.isDefined).map(addr ⇒ (addr.country → addr.postalCode)).toSeq
      val coordinates = arg.filter(_.coordinates.isDefined).map(_.coordinates).toSeq

      (addressIdentity.size == addressIdentity.distinct.size, countryPostalCodes.size == countryPostalCodes.distinct.size, coordinates.size == coordinates.distinct.size) match {
        case (false, _, _) ⇒
          Left(ServiceError.validationError("Duplicate address identity found"))
        case (_, false, _) ⇒
          Left(ServiceError.validationError("Duplicate postal code found"))
        case (_, _, false) ⇒
          Left(ServiceError.validationError("Duplicate coordinates found"))
        case _ ⇒ Right(())
      }
    }
  }

}
