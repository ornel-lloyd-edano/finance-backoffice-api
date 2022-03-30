package tech.pegb.backoffice.mapping.api.domain.customer

import java.time.ZonedDateTime
import java.util.UUID

import tech.pegb.backoffice.api.customer.dto.{ContactAddressToCreate, ContactAddressToUpdate, ContactToCreate, ContactToUpdate}
import tech.pegb.backoffice.{api, domain}
import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.domain.customer.dto
import tech.pegb.backoffice.domain.customer.dto.{GenericUserCriteria, IndividualUserCriteria, SavingOptionCriteria}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.{Msisdn, _}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.UUIDLike

import scala.util.Try
object Implicits {

  private type Msisdn = Option[String]
  private type UserId = Option[UUIDLike]
  private type Name = Option[String]
  private type FullName = Option[String]
  private type Status = Option[String]
  private type AnyName = Option[String]
  private type PartialMatch = Set[String]
  implicit class QueryParamIndividualUserCriteriaAdapter(val arg: (Msisdn, UserId, Name, FullName, Status, PartialMatch)) extends AnyVal {
    def asDomain = Try(IndividualUserCriteria(

      msisdnLike = arg._1.map(MsisdnLike),
      userId = arg._2,
      name = arg._3.map(f ⇒ NameAttribute(f.sanitize)),
      fullName = arg._4.map(l ⇒ NameAttribute(l.sanitize)),
      status = arg._5.map(s ⇒ CustomerStatus(s.sanitize)),
      partialMatchFields = arg._6))
  }

  implicit class QueryParamGenericUserCriteriaAdapter(val arg: (Msisdn, UserId, Name, FullName, Status, AnyName, PartialMatch)) extends AnyVal {
    def asDomain = Try(GenericUserCriteria(

      msisdnLike = arg._1.map(MsisdnLike),
      userId = arg._2,
      name = arg._3.map(f ⇒ NameAttribute(f.sanitize)),
      fullName = arg._4.map(l ⇒ NameAttribute(l.sanitize)),
      status = arg._5.map(s ⇒ CustomerStatus(s.sanitize)),
      anyName = arg._6.map(s ⇒ NameAttribute(s.sanitize)),
      partialMatchFields = arg._7))
  }

  implicit class IndividualUserToUpdateMapper(val arg: api.customer.dto.IndividualUserToUpdate) extends AnyVal {
    def asDomain = Try(domain.customer.dto.IndividualUserToUpdate(
      userName = None,
      password = None,
      tier = None,
      segment = None,
      subscription = None,
      email = None, //arg.email.map(e ⇒ Email(value = e)),
      status = None,
      msisdn = Option(Msisdn(arg.msisdn)),
      individualUserType = None,
      name = None,
      fullName = None,
      gender = None,
      personId = None,
      documentNumber = None,
      documentType = None,
      birthDate = None, //arg.birthDate,
      birthPlace = None, //arg.birthPlace.map(NameAttribute(_)),
      nationality = None, //arg.nationality.map(NameAttribute(_)),
      occupation = None, //arg.occupation.map(NameAttribute(_)),
      companyName = None, //arg.companyName.map(NameAttribute(_)),
      employer = None //arg.employer.map(NameAttribute(_)
    ))
  }

  implicit class SavingOptionCriteriaConverter(arg: (Option[UUID], Option[String])) {
    def asDomain =
      SavingOptionCriteria(userUuid = arg._1, isActive = arg._2.map(s ⇒ if (s === "active") true else false))
  }

  implicit class ContactToCreateAdapter(val arg: ContactToCreate) extends AnyVal {
    def asDomain(
      requestId: UUID,
      userId: UUID,
      doneAt: ZonedDateTime,
      doneBy: String): Try[dto.ContactToCreate] = Try {
      dto.ContactToCreate(
        uuid = requestId,
        userUuid = userId,
        contactType = arg.contactType.sanitize,
        name = arg.name.sanitize,
        middleName = arg.middleName.map(_.sanitize),
        surname = arg.surname.sanitize,
        phoneNumber = Msisdn(arg.phoneNumber.sanitize),
        email = Email(arg.email.sanitize),
        idType = arg.idType.sanitize,
        createdBy = doneBy.sanitize,
        createdAt = doneAt.toLocalDateTimeUTC,
        isActive = true)
    }
  }

  implicit class ContactToUpdateAdapter(val arg: ContactToUpdate) extends AnyVal {
    def asDomain(doneAt: ZonedDateTime, doneBy: String): Try[dto.ContactToUpdate] = Try {
      dto.ContactToUpdate(
        contactType = arg.contactType.map(_.sanitize),
        name = arg.name.map(_.sanitize),
        middleName = arg.middleName.map(_.sanitize),
        surname = arg.surname.map(_.sanitize),
        phoneNumber = arg.phoneNumber.map(x ⇒ Msisdn(x.sanitize)),
        email = arg.email.map(x ⇒ Email(x.sanitize)),
        idType = arg.idType.map(_.sanitize),
        isActive = None,
        updatedBy = doneBy.sanitize,
        updatedAt = doneAt.toLocalDateTimeUTC,
        lastUpdatedAt = arg.lastUpdatedAt.map(_.toLocalDateTimeUTC))
    }
  }

  implicit class ContactAddressToCreateAdapter(val arg: ContactAddressToCreate) extends AnyVal {
    def asDomain(
      requestId: UUID,
      userId: UUID,
      doneAt: ZonedDateTime,
      doneBy: String): Try[dto.ContactAddressToCreate] = Try {
      dto.ContactAddressToCreate(
        uuid = requestId,
        userUuid = userId,
        addressType = arg.addressType.sanitize,
        country = arg.country.sanitize,
        city = arg.city.sanitize,
        postalCode = arg.postalCode.map(_.sanitize),
        address = arg.address.map(_.sanitize),
        coordinateX = arg.coordinateX,
        coordinateY = arg.coordinateY,
        createdBy = doneBy.sanitize,
        createdAt = doneAt.toLocalDateTimeUTC,
        isActive = true)
    }
  }

  implicit class ContactAddressToUpdateAdapter(val arg: ContactAddressToUpdate) extends AnyVal {
    def asDomain(doneAt: ZonedDateTime, doneBy: String): Try[dto.ContactAddressToUpdate] = Try {
      dto.ContactAddressToUpdate(
        addressType = arg.addressType.map(_.sanitize),
        country = arg.country.map(_.sanitize),
        city = arg.city.map(_.sanitize),
        postalCode = arg.postalCode.map(_.sanitize),
        address = arg.address.map(_.sanitize),
        coordinateX = arg.coordinateX,
        coordinateY = arg.coordinateY,
        isActive = None,
        updatedBy = doneBy.sanitize,
        updatedAt = doneAt.toLocalDateTimeUTC,
        lastUpdatedAt = arg.lastUpdatedAt.map(_.toLocalDateTimeUTC))
    }
  }

}
