package tech.pegb.backoffice.domain.auth.dto

import java.time.LocalDateTime
import java.util.UUID

import play.api.libs.json.JsValue
import tech.pegb.backoffice.domain.auth.model.Email

case class BackOfficeUserToCreate(
    userName: String,
    roleId: UUID,
    businessUnitId: UUID,
    email: Email,
    phoneNumber: Option[String],
    firstName: String,
    middleName: Option[String],
    lastName: String,
    description: Option[String],
    homePage: Option[String],
    activeLanguage: Option[String],
    customData: Option[JsValue],
    createdBy: String,
    createdAt: LocalDateTime) {

}

object BackOfficeUserToCreate {
  val empty = BackOfficeUserToCreate(userName = "", roleId = UUID.randomUUID(), businessUnitId = UUID.randomUUID(),
    email = Email("pegbuser@pegb.tech", true), phoneNumber = None, firstName = "", middleName = None,
    lastName = "", description = None, homePage = None, activeLanguage = None, customData = None,
    createdBy = "", createdAt = LocalDateTime.now)
}
