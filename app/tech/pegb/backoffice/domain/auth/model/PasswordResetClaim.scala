package tech.pegb.backoffice.domain.auth.model

import play.api.libs.json.Json

final case class PasswordResetClaim(userName: String, hashedPassword: Option[String])

object PasswordResetClaim {
  implicit val passwordResetClaimFormat = Json.format[PasswordResetClaim]
}
