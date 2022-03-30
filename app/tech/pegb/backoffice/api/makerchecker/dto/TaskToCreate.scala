package tech.pegb.backoffice.api.makerchecker.dto

import play.api.libs.json.JsObject

trait TaskToCreateI {
  val verb: String
  val url: String
  val body: Option[JsObject] //should be JsObject but problem with jackson
  val headers: JsObject //should be JsObject but problem with jackson
  val module: String
  val action: String
}

case class TaskToCreate(
    verb: String,
    url: String,
    body: Option[JsObject],
    headers: JsObject,
    module: String,
    action: String) extends TaskToCreateI {

}
