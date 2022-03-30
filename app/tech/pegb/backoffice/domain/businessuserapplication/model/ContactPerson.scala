package tech.pegb.backoffice.domain.businessuserapplication.model

import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.{ServiceError, Validatable, ValidatableAsGroup}
import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.domain.businessuserapplication.abstraction.{ContactTypes, VelocityUserLevels}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.Msisdn
import tech.pegb.backoffice.util.Implicits._

case class ContactPerson(
    contactType: String,
    name: String,
    middleName: Option[String],
    surname: String,
    phoneNumber: Option[Msisdn],
    email: Option[Email],
    idType: String, //TODO objectify
    velocityUser: Option[String],
    isDefault: Option[Boolean]) extends Validatable[Unit] { //TODO objectify

  def validate: ServiceResponse[Unit] = {
    (ContactTypes.toSeq.contains(contactType), velocityUser.map(VelocityUserLevels.toSeq.contains(_))) match {
      case (false, _) ⇒
        Left(ServiceError.validationError(s"invalid contact_type [$contactType]. Valid choices: ${ContactTypes.toSeq.defaultMkString}"))
      case (_, Some(false)) ⇒
        Left(ServiceError.validationError(s"invalid velocity user level [${velocityUser.get}]. Valid choices: ${VelocityUserLevels.toSeq.defaultMkString}"))
      case _ ⇒
        Right(())
    }
  }

}

object ContactPerson {

  implicit object ContactPersonsValidation extends ValidatableAsGroup[ContactPerson] {
    def validate(arg: Iterable[ContactPerson]): ServiceResponse[Unit] = {

      val contactPersonIdentity = arg.map(cp ⇒
        (cp.name.toLowerCase.trim, cp.middleName.map(_.toLowerCase.trim), cp.surname.toLowerCase.trim, cp.email.map(_.value.toLowerCase.trim))).toSeq
      val moreThanOneDefaultContact = arg.filter(_.isDefault.contains(true)).size > 1

      (contactPersonIdentity.size == contactPersonIdentity.distinct.size, moreThanOneDefaultContact) match {
        case (false, _) ⇒
          Left(ServiceError.validationError("Duplicate contact person details found"))
        case (_, true) ⇒
          Left(ServiceError.validationError("Only 1 default contact is allowed"))
        case _ ⇒
          Right(())
      }
    }
  }

  implicit class ContactPersonsGroupOperations(val arg: Iterable[ContactPerson]) extends AnyVal {
    //if no contact person in the list is default contact, this will assign the first one as default contact
    def makeSureAtLeastOneDefaultContact: Iterable[ContactPerson] = {
      if (arg.find(_.isDefault.contains(true)).isDefined) {
        arg
      } else {
        arg.headOption.map(c ⇒ c.copy(isDefault = Some(true))) ++ (if (arg.nonEmpty) arg.tail else Nil)
      }
    }
  }

}
