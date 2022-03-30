package tech.pegb.backoffice.api.swagger.controllers

import controllers.Assets
import javax.inject.{Inject, Singleton}
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import play.filters.csrf.CSRFAddToken
import play.filters.headers.SecurityHeadersFilter

import scala.concurrent.ExecutionContext

@Singleton
class SwaggerController @Inject() (assets: Assets, addToken: CSRFAddToken, executionContext: ExecutionContext, cc: ControllerComponents)
  extends AbstractController(cc) {

  def redirectDocs = addToken {
    Action {
      Redirect(url = "/swagger-ui/index.html", queryString = Map("url" → Seq("/swagger.json")))
    }
  }

  def serveDocs(file: String): Action[AnyContent] = addToken {
    Action.async { implicit request ⇒
      assets.at(path = "/public/lib/swagger-ui", file)(request)
        .map(_.withHeaders(SwaggerController.SwaggerCSPHeader))(executionContext)
    }
  }
}

object SwaggerController {
  private val SwaggerCSP: String = Seq(
    "default-src 'self'",
    "img-src 'self' data:",
    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
    "font-src https://fonts.gstatic.com",
    "script-src 'self' 'unsafe-inline'").mkString("; ")
  val SwaggerCSPHeader: (String, String) = SecurityHeadersFilter.CONTENT_SECURITY_POLICY_HEADER → SwaggerCSP
}
