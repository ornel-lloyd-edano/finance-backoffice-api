package tech.pegb.backoffice.util

import java.time.{LocalDateTime, ZoneOffset, ZonedDateTime}
import java.util.Timer

import play.api.libs.json.{JsArray, JsObject, JsValue}

object Utils {

  val timer: Timer = new Timer(true)

  val tz: ZoneOffset = ZoneOffset.UTC
  def now(): ZonedDateTime = ZonedDateTime.now.withZoneSameInstant(tz)
  def nowAsLocal(): LocalDateTime = now().toLocalDateTime

  def allKeys(json: JsValue): collection.Set[String] = json match {
    case o: JsObject ⇒ o.keys ++ o.values.flatMap(allKeys)
    case JsArray(as) ⇒ as.flatMap(allKeys).toSet
    case _ ⇒ Set()
  }

}

