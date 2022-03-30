package tech.pegb.backoffice.dao.auth.entity

import java.time.LocalDateTime

case class BackOfficeUser(
    id: String,
    userName: String,
    password: Option[String],
    email: String,
    phoneNumber: Option[String],
    firstName: String,
    middleName: Option[String],
    lastName: String,
    description: Option[String],
    homePage: Option[String],
    activeLanguage: Option[String],
    customData: Option[String],
    lastLoginTimestamp: Option[Long],
    roleId: String,
    roleName: String,
    roleLevel: Int,
    roleCreatedBy: Option[String],
    roleUpdatedBy: Option[String],
    roleCreatedAt: Option[LocalDateTime],
    roleUpdatedAt: Option[LocalDateTime],
    businessUnitId: String,
    businessUnitName: String,
    businessUnitCreatedBy: Option[String],
    businessUnitUpdatedBy: Option[String],
    businessUnitCreatedAt: Option[LocalDateTime],
    businessUnitUpdatedAt: Option[LocalDateTime],
    isActive: Int,
    createdBy: Option[String],
    updatedBy: Option[String],
    createdAt: Option[LocalDateTime],
    updatedAt: Option[LocalDateTime])

object BackOfficeUser {
  val empty = BackOfficeUser(id = "", userName = "", password = None, email = "", phoneNumber = None,
    firstName = "", middleName = None, lastName = "", description = None, homePage = None,
    activeLanguage = None, customData = None, lastLoginTimestamp = None,
    roleId = "", roleName = "", roleLevel = 1, roleCreatedBy = Some(""), roleUpdatedBy = None, roleCreatedAt = Some(LocalDateTime.now), roleUpdatedAt = None,
    businessUnitId = "", businessUnitName = "", businessUnitCreatedBy = Some(""), businessUnitUpdatedBy = None, businessUnitCreatedAt = Some(LocalDateTime.now),
    businessUnitUpdatedAt = None, isActive = 1, createdBy = Some(""), createdAt = Some(LocalDateTime.now),
    updatedBy = None, updatedAt = None)
}
