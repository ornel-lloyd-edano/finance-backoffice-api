package tech.pegb.backoffice.api.proxy

import java.time.LocalDate
import java.util.UUID

import com.google.inject.{Inject, Singleton}
import play.api.libs.Files.TemporaryFile
import play.api.mvc._
import tech.pegb.backoffice.api.auth.{AuthenticationMiddleware, AuthorizationMiddleware, MakerCheckerMiddleware}
import tech.pegb.backoffice.api.document.DocumentMgmtController
import tech.pegb.backoffice.api.proxy.abstraction.{ProxyController, ProxyRequestHandler}
import tech.pegb.backoffice.api.{ApiController, RequiredHeaders}
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}

@Singleton
class DocumentMgmtProxyController @Inject() (
    controllerComponents: ControllerComponents,
    val executionContexts: WithExecutionContexts,
    val proxyResponseHandler: ProxyResponseHandler,
    val authenticationMiddleware: AuthenticationMiddleware,
    val authorizationMiddleware: AuthorizationMiddleware,
    val makerCheckerMiddleware: MakerCheckerMiddleware,
    val proxyRequestHandler: ProxyRequestHandler,
    implicit val appConfig: AppConfig)

  extends ApiController(controllerComponents) with RequiredHeaders
  with DocumentMgmtController with ProxyController {

  def getDocument(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id")
  }

  def getDocumentFile(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id/file", maybeCustomContentType = Some("image/jpeg"))
  }

  def getDocumentsByFilters(
    status: Option[String],
    documentType: Option[String],
    customerId: Option[UUIDLike],
    customerFullName: Option[String],
    customerMsisdn: Option[String],
    startDate: Option[LocalDate],
    endDate: Option[LocalDate],
    isCheckedAt: Option[Boolean] = Option(false),
    partialMatch: Option[String],
    ordering: Option[String], limit: Option[Int], offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute")
  }

  def createDocument(): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute")
  }

  def uploadDocumentFile(documentId: UUID): Action[MultipartFormData[TemporaryFile]] = LoggedAsyncAction(parse.multipartFormData) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$documentId/file")
  }

  def approveDocument(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id/approve")
  }

  def rejectDocument(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id/reject")
  }

}

