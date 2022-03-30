package tech.pegb.backoffice.domain.auth.model

import play.api.data.validation.{Constraints, Valid}
import play.api.libs.json._

import scala.util.Try

case class Email(value: String, isPrimary: Boolean = true) {
  assert(Email.validate(value), "invalid email")

  def domain: String = Email.regex.findFirstMatchIn(value).map(_.group(1)).getOrElse("")
}

object Email {
  private final val regex = "^.*@(.*)$".r

  def validate(email: String): Boolean = {
    Constraints.emailAddress.apply(email) == Valid
  }

  implicit val emailFormat: Format[Email] = new Format[Email] {
    override def reads(json: JsValue): JsResult[Email] = json match {
      case JsString(value) ⇒ Try(Email(value)).toEither.fold(error ⇒ JsError("Cannot be parsed to email"), result ⇒ JsSuccess(result))
      case _ ⇒ JsError("Cannot be parsed to email")
    }

    override def writes(o: Email): JsValue = JsString(o.value)
  }
}
