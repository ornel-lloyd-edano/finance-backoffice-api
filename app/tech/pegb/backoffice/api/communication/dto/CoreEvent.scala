package tech.pegb.backoffice.api.communication.dto

import play.api.libs.json.{JsObject, Json}

case class CoreEvent(`type`: String, payload: JsObject)

object CoreEvent {
  val originator = "pegb_wallet_core_api"

  implicit val coreEventFormat = Json.format[CoreEvent]
}
