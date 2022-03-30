package tech.pegb.backoffice.mapping.domain.api.auth

import tech.pegb.backoffice.domain.auth.model.{BackOfficeUser, BusinessUnit}
import tech.pegb.backoffice.api.auth.dto.{BackOfficeUserToRead, BusinessUnitToRead}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.mapping.domain.api.auth.role.Implicits._
import tech.pegb.backoffice.mapping.domain.api.auth.permission.Implicits._

object Implicits {

  implicit class BusinessUnitToApiAdapter(val arg: BusinessUnit) extends AnyVal {
    def asApi = BusinessUnitToRead(
      id = arg.id,
      name = arg.name,
      createdBy = arg.createdBy,
      updatedBy = arg.updatedBy,
      createdAt = arg.createdAt.toZonedDateTimeUTC,
      updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC),

      createdTime = arg.createdAt.toZonedDateTimeUTC,
      updatedTime = arg.updatedAt.map(_.toZonedDateTimeUTC))
  }

  implicit class BackOfficeUserApiAdapter(val arg: BackOfficeUser) extends AnyVal {
    def asApi = BackOfficeUserToRead(
      id = arg.id.toString,
      userName = arg.userName,
      email = arg.email.value,
      phoneNumber = arg.phoneNumber,
      firstName = arg.firstName,
      middleName = arg.middleName,
      lastName = arg.lastName,
      description = arg.description,
      homePage = arg.homePage,
      activeLanguage = arg.activeLanguage,
      lastLoginTimestamp = arg.lastLoginTimestamp,
      customData = arg.customData,
      role = arg.role.asApi,
      businessUnit = arg.businessUnit.asApi,
      permissions = arg.permissions.map(_.asApi),
      createdBy = arg.createdBy,
      updatedBy = arg.updatedBy,
      createdAt = arg.createdAt.toZonedDateTimeUTC,
      updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC),

      createdTime = arg.createdAt.toZonedDateTimeUTC,
      updatedTime = arg.updatedAt.map(_.toZonedDateTimeUTC))
  }
}
