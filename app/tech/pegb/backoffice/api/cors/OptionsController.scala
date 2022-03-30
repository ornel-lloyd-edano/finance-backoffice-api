package tech.pegb.backoffice.api.cors

import com.google.inject.{Inject, Singleton}
import play.api.cache.SyncCacheApi
import play.api.http.HeaderNames
import play.api.mvc._
import play.api.{Configuration, Environment}

@Singleton
class OptionsController @Inject() (
    env: Environment,
    configuration: Configuration,
    cache: SyncCacheApi) extends InjectedController {

  private val methodList = Seq("GET", "POST", "PUT", "DELETE", "PATCH")

  private def getMethods(request: RequestHeader): Seq[String] = {
    val fakeRoutes = cache.getOrElseUpdate[PartialFunction[RequestHeader, Handler]]("fakeRoutes") {
      new router.Routes(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null).routes
    }
    cache.getOrElseUpdate[Seq[String]]("options.url." + request.uri) {
      methodList.filter(method ⇒ fakeRoutes.isDefinedAt(request.withMethod(method)))
    }
  }

  def options(url: String): Action[AnyContent] = Action { request ⇒
    val allowedMethods = getMethods(request)
    if (allowedMethods.nonEmpty) {
      NoContent.withHeaders(HeaderNames.ALLOW → ("OPTIONS" +: allowedMethods).mkString(", "))
    } else {
      NotFound
    }
  }

}
