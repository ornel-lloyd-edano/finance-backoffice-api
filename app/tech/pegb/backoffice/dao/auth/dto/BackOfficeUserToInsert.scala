package tech.pegb.backoffice.dao.auth.dto

import java.time.LocalDateTime

case class BackOfficeUserToInsert(
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
    businessUnitId: String,
    isActive: Int,
    createdBy: String,
    createdAt: LocalDateTime,
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime]) {

}

object BackOfficeUserToInsert {
  val empty = BackOfficeUserToInsert(userName = "", password = None, email = "", phoneNumber = None,
    firstName = "", middleName = None, lastName = "", description = None, homePage = None,
    activeLanguage = None, customData = None, lastLoginTimestamp = None, roleId = "",
    businessUnitId = "", isActive = 1, createdBy = "", createdAt = LocalDateTime.now, updatedBy = None, updatedAt = None)
}
