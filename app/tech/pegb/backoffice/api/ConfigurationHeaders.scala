package tech.pegb.backoffice.api

import play.api.mvc.Request
import tech.pegb.backoffice.util.AppConfig

import scala.util.Try

trait ConfigurationHeaders {
  val appConfig: AppConfig

  val strictDeserializationKey = appConfig.HeaderKeys.StrictDeserializationKey

  def isDeserializationStrict[T](implicit ctx: Request[T]): Boolean = {
    //Try(ctx.headers.get(strictDeserializationKey).forall(_.toBoolean)).fold(_ ⇒ true, identity)
    ctx.headers.get(strictDeserializationKey).fold(false)(bool ⇒ Try(bool.toBoolean).fold(_ ⇒ true, identity))
  }
}
