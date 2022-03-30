package tech.pegb.backoffice.domain.auth.dto

import java.time.LocalDateTime
import java.util.UUID

import play.api.libs.json.JsValue
import tech.pegb.backoffice.domain.auth.model.Email

case class BackOfficeUserToUpdate(
    password: Option[String] = None,
    roleId: Option[UUID] = None,
    businessUnitId: Option[UUID] = None,
    email: Option[Email] = None,
    phoneNumber: Option[String] = None,
    customData: Option[JsValue] = None,
    homePage: Option[String] = None,
    description: Option[String] = None,
    activeLanguage: Option[String] = None,
    updatedBy: String,
    updatedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime] = None) {

}

object BackOfficeUserToUpdate {
  val empty = BackOfficeUserToUpdate(updatedBy = "", updatedAt = LocalDateTime.now)
}
