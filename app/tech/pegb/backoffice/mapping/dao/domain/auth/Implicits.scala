package tech.pegb.backoffice.mapping.dao.domain.auth

import java.time.LocalDateTime
import java.util.UUID

import play.api.Logging
import play.api.libs.json.Json
import tech.pegb.backoffice.dao.auth.entity.{BackOfficeUser, BusinessUnit}
import tech.pegb.backoffice.domain.auth.model.{Email, BackOfficeUser ⇒ DomainBackOfficeUser, BusinessUnit ⇒ DomainBusinessUnit, Role ⇒ DomainRole}

import scala.util.Try

object Implicits extends Logging {

  implicit class BusinessUnitEntitytoDomainModelAdapter(val arg: BusinessUnit) extends AnyVal {
    def asDomain = Try(DomainBusinessUnit(
      id = UUID.fromString(arg.id),
      name = arg.name,
      createdBy = arg.createdBy.getOrElse("UNKNOWN"),
      updatedBy = arg.updatedBy,
      createdAt = arg.createdAt.getOrElse(LocalDateTime.of(1970, 1, 1, 0, 0, 0)),
      updatedAt = arg.updatedAt))
  }

  implicit class BackOfficeUserEntitytoDomainAdapter(val arg: BackOfficeUser) extends AnyVal {
    def asDomain = Try(DomainBackOfficeUser(
      id = UUID.fromString(arg.id),
      userName = arg.userName,
      hashedPassword = arg.password,
      role = DomainRole(
        id = UUID.fromString(arg.roleId),
        name = arg.roleName,
        level = arg.roleLevel,
        createdBy = arg.roleCreatedBy.getOrElse("UNKNOWN"),
        createdAt = arg.roleCreatedAt.getOrElse(LocalDateTime.of(1970, 1, 1, 0, 0, 0)),
        updatedBy = arg.roleUpdatedBy,
        updatedAt = arg.roleUpdatedAt),
      businessUnit = DomainBusinessUnit(
        id = UUID.fromString(arg.businessUnitId),
        name = arg.businessUnitName,
        createdBy = arg.businessUnitCreatedBy.getOrElse("UNKNOWN"),
        createdAt = arg.businessUnitCreatedAt.getOrElse(LocalDateTime.of(1970, 1, 1, 0, 0, 0)),
        updatedBy = arg.businessUnitUpdatedBy,
        updatedAt = arg.businessUnitUpdatedAt),
      permissions = Nil, //todo fix
      email = Email(arg.email),
      phoneNumber = arg.phoneNumber,
      firstName = arg.firstName,
      middleName = arg.middleName,
      lastName = arg.lastName,
      description = arg.description,
      homePage = arg.homePage,
      activeLanguage = arg.activeLanguage,
      customData = arg.customData.map(Json.parse(_)),
      lastLoginTimestamp = arg.lastLoginTimestamp,
      createdBy = arg.createdBy.getOrElse("UNKNOWN"),
      createdAt = arg.createdAt.getOrElse(LocalDateTime.of(1970, 1, 1, 0, 0, 0)),
      updatedBy = arg.updatedBy,
      updatedAt = arg.updatedAt))
  }

}
