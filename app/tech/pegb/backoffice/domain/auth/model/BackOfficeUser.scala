package tech.pegb.backoffice.domain.auth.model

import java.time.LocalDateTime
import java.util.UUID

import play.api.libs.json.JsValue

import tech.pegb.backoffice.util.Implicits._
import BackOfficeUser._

case class BackOfficeUser(
    id: UUID,
    userName: String,
    hashedPassword: Option[String],
    role: Role,
    businessUnit: BusinessUnit,
    permissions: Seq[Permission],
    email: Email,
    phoneNumber: Option[String],
    firstName: String,
    middleName: Option[String],
    lastName: String,
    description: Option[String],
    homePage: Option[String],
    activeLanguage: Option[String],
    customData: Option[JsValue],
    lastLoginTimestamp: Option[Long],
    createdBy: String,
    createdAt: LocalDateTime,
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime]) {

  assert(isValidName(this.userName), "userName cannot be empty")
  assert(isValidName(this.firstName), "firstName cannot be empty")
  assert(isValidName(this.lastName), "lastName cannot be empty")
  if (phoneNumber.nonEmpty) {
    assert(isValidPhoneNumber(phoneNumber.get), s"invalid phoneNumber")
  }
}

object BackOfficeUser {
  def isValidName(anyName: String): Boolean = {
    anyName.hasSomething
  }

  def isValidPhoneNumber(number: String): Boolean = {
    number.hasSomething && number.matches("""[\+]?[0-9]{4,14}""")
  }
}
