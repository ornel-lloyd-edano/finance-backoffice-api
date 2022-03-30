package tech.pegb.backoffice.mapping.api.domain.auth.backofficeuser

import java.time.ZonedDateTime
import java.util.UUID

import tech.pegb.backoffice.api.auth.dto.{BackOfficeUserToCreate, BackOfficeUserToUpdate}
import tech.pegb.backoffice.domain.auth.dto.{BackOfficeUserCriteria, BackOfficeUserToRemove, BackOfficeUserToCreate ⇒ DomainBackOfficeUserToCreate, BackOfficeUserToUpdate ⇒ DomainBackOfficeUserToUpdate}
import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.UUIDLike

import scala.util.Try

object Implicits {
  implicit class BackOfficeUserToCreateDomainAdapter(val arg: BackOfficeUserToCreate) extends AnyVal {
    def asDomain(doneBy: String, doneAt: ZonedDateTime) = DomainBackOfficeUserToCreate(
      userName = arg.userName,
      roleId = UUID.fromString(arg.roleId),
      businessUnitId = UUID.fromString(arg.businessUnitId),
      email = Email(arg.email),
      phoneNumber = arg.phoneNumber,
      firstName = arg.firstName,
      middleName = arg.middleName,
      lastName = arg.lastName,
      description = arg.description,
      homePage = arg.homePage,
      activeLanguage = arg.activeLanguage,
      customData = arg.customData,
      createdBy = doneBy,
      createdAt = doneAt.toLocalDateTimeUTC)
  }

  private type BouId = Option[String]
  private type RoleId = Option[String]
  private type BusinessUnitId = Option[String]
  private type Username = Option[String]
  private type FirstName = Option[String]
  private type LastName = Option[String]
  private type Email = Option[String]
  private type PhoneNum = Option[String]
  private type PartialMatchFields = Set[String]
  implicit class BackOfficeUserQueryParamToCriteriaAdapter(val arg: (BouId, RoleId, BusinessUnitId, Username, FirstName, LastName, Email, PhoneNum, PartialMatchFields)) extends AnyVal {
    def asDomain = Try(BackOfficeUserCriteria(
      id = arg._1.map(UUIDLike(_)),
      roleId = arg._2.map(UUIDLike(_)),
      businessUnitId = arg._3.map(UUIDLike(_)),
      userName = arg._4.map(_.sanitize),
      firstName = arg._5.map(_.sanitize),
      lastName = arg._6.map(_.sanitize),
      email = arg._7.map(Email(_)),
      phoneNumber = arg._8.map(_.sanitize),
      partialMatchFields = arg._9))
  }

  implicit class BackOfficeToUpdateDomainAdapter(val arg: BackOfficeUserToUpdate) extends AnyVal {
    def asDomain(doneBy: String, doneAt: ZonedDateTime) = Try(DomainBackOfficeUserToUpdate(
      roleId = arg.roleId.map(UUID.fromString(_)),
      businessUnitId = arg.businessUnit.map(UUID.fromString(_)),
      email = arg.email.map(e ⇒ Email(e.sanitize)),
      phoneNumber = arg.phoneNumber.map(_.sanitize),
      updatedBy = doneBy.sanitize,
      updatedAt = doneAt.toLocalDateTimeUTC,
      lastUpdatedAt = arg.lastUpdatedAt.map(_.toLocalDateTimeUTC)))
  }

  private type RemovedBy = String
  private type RemovedAt = ZonedDateTime
  private type LastUpdatedAt = Option[ZonedDateTime]
  implicit class BackOfficeUserToRemoveDomainAdapter(val arg: (RemovedBy, RemovedAt, LastUpdatedAt)) extends AnyVal {
    def asDomain = BackOfficeUserToRemove(
      removedBy = arg._1.sanitize,
      removedAt = arg._2.toLocalDateTimeUTC,
      lastUpdatedAt = arg._3.map(_.toLocalDateTimeUTC))
  }
}
