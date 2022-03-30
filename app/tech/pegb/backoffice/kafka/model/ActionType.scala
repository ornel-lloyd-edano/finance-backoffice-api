package tech.pegb.backoffice.kafka.model

import play.api.libs.json.{Format, JsString, JsSuccess, JsValue}

object ActionType extends Enumeration {
  type ActionType = Value
  val INSERT = Value("insert")
  val UPDATE = Value("update")
  val UPSERT = Value("upsert")
  val DELETE = Value("delete")

  implicit val myEnumFormat = new Format[ActionType] {
    def reads(json: JsValue) = JsSuccess(ActionType.withName(json.as[String]))
    def writes(myEnum: ActionType) = JsString(myEnum.toString)
  }
}
