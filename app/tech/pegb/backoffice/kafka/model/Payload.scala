package tech.pegb.backoffice.kafka.model

import play.api.libs.json.{JsValue, Json}
import tech.pegb.backoffice.kafka.model.ActionType.ActionType

case class Payload(action: ActionType, entity: JsValue)

object Payload {
  implicit val f = Json.format[Payload]
}
