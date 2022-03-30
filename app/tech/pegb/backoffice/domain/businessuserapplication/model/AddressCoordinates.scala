package tech.pegb.backoffice.domain.businessuserapplication.model

import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.{ServiceError, Validatable}

case class AddressCoordinates(x: Double, y: Double) extends Validatable[Unit] {
  def validate: ServiceResponse[Unit] = {
    (x >= -90 && x <= 90, y >= -180 && y <= 180) match {
      case (false, _) ⇒
        Left(ServiceError.validationError(s"invalid value for x coordinate [$x]. Valid range from -90 to 90"))
      case (_, false) ⇒
        Left(ServiceError.validationError(s"invalid value for y coordinate [$y]. Valid range from -180 to 180"))
      case _ ⇒ Right(())
    }
  }
}
