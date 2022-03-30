package tech.pegb.backoffice.domain.customer.model

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import tech.pegb.backoffice.domain.Identifiable
import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes._
import tech.pegb.backoffice.util.Implicits._

object IndividualUsers {

  case class IndividualUserType(underlying: String) {
    assert(underlying.hasSomething, "empty IndividualUserType")
    assert(underlying.matches("""[A-Za-z]+[A-Za-z0-9\-\_ ]*"""), "invalid IndividualUserType")
  }

  case class ActivatedIndividualUser(
      id: UUID,
      userName: Option[LoginUsername],
      password: Option[String],
      tier: Option[CustomerTier],
      segment: Option[CustomerSegment],
      subscription: Option[CustomerSubscription],
      email: Option[Email],
      status: CustomerStatus,

      msisdn: Msisdn,
      individualUserType: Option[IndividualUserType],
      firstName: String,
      fullName: Option[String],
      gender: Option[String],
      personId: Option[String],
      documentNumber: Option[String],
      documentType: Option[String],
      birthDate: LocalDate,
      birthPlace: NameAttribute,
      nationality: NameAttribute,
      occupation: NameAttribute,
      companyName: Option[NameAttribute],
      employer: NameAttribute,
      createdAt: LocalDateTime,
      createdBy: Option[String],
      updatedAt: Option[LocalDateTime],
      updatedBy: Option[String])

  case class IndividualUser(
      id: UUID,
      uniqueId: String,
      userName: Option[LoginUsername],
      password: Option[String],
      tier: Option[CustomerTier],
      segment: Option[CustomerSegment],
      subscription: Option[CustomerSubscription],
      email: Option[Email],
      status: CustomerStatus,
      msisdn: Msisdn,
      individualUserType: Option[IndividualUserType],
      name: Option[String],
      fullName: Option[String],
      gender: Option[String],
      personId: Option[String],
      documentNumber: Option[String],
      documentModel: Option[String],
      birthDate: Option[LocalDate],
      birthPlace: Option[NameAttribute],
      nationality: Option[NameAttribute],
      occupation: Option[NameAttribute],
      companyName: Option[NameAttribute],
      employer: Option[NameAttribute],
      createdAt: LocalDateTime,
      createdBy: Option[String],
      updatedAt: Option[LocalDateTime],
      updatedBy: Option[String],
      activatedAt: Option[LocalDateTime]) extends Identifiable

  object IndividualUser {
    def getEmpty = new IndividualUser(
      id = UUID.randomUUID(),
      userName = None,
      password = None,
      tier = None,
      segment = None,
      subscription = None,
      email = None,
      status = CustomerStatus("some status"),
      msisdn = Msisdn("111111111111"),
      individualUserType = None,
      name = None,
      fullName = None,
      gender = None,
      personId = None,
      documentNumber = None,
      documentModel = None,
      birthDate = None,
      birthPlace = None,
      nationality = None,
      occupation = None,
      companyName = None,
      employer = None,
      createdAt = LocalDateTime.now,
      createdBy = None,
      updatedAt = None,
      updatedBy = None,
      activatedAt = None,
      uniqueId = "1")
  }

}
