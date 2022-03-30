package tech.pegb.backoffice.domain.makerchecker.model

import play.api.libs.json.JsObject
import tech.pegb.backoffice.domain.makerchecker.model.HttpVerbs._
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

case class MakerRequest(
    verb: HttpVerb,
    rawUrl: String,
    body: Option[JsObject],
    headers: JsObject) {

  assert(rawUrl.hasSomething, "url cannot be empty")

  assert(if (verb.isBodyRequired) body.nonEmpty else true, s"Empty body for $verb is not allowed")

  def validateURL(actualHost: String, placeHolder: String): Either[Throwable, String] = {

    (actualHost.trim.isEmpty, placeHolder.trim.isEmpty, placeHolder.trim.startsWith("$"), rawUrl.contains(placeHolder)) match {
      case (true, _, _, _) ⇒ Left(new Exception("Actual host is empty"))
      case (_, true, _, _) ⇒ Left(new Exception("Host placeholder is empty"))
      case (_, _, false, _) ⇒ Left(new Exception("Host placeholder is incorrect format"))
      case (_, _, _, false) ⇒ Left(new Exception("No placeholder was found in the task url"))
      case _ ⇒ Right(rawUrl.replace(placeHolder, actualHost))
    }
  }

  def getQueryParams: Try[Map[String, String]] = Try {
    rawUrl.split("""\?""").tail.headOption.map(_.split("""&""").map(param ⇒ {

      val array = param.split("""=""")
      val key = array.head
      val value = array.tail.head
      key → value

    }).toMap).getOrElse(Map.empty)
  }

}

object MakerRequest {
  def apply(
    verb: String,
    url: String,
    queryParams: Option[Map[String, String]],
    body: Option[JsObject],
    headers: JsObject) = {
    new MakerRequest(verb.asHttpVerb, url, body, headers)
  }
}
