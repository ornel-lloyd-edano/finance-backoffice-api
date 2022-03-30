package tech.pegb.backoffice.api.proxy

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents, MultipartFormData}
import tech.pegb.backoffice.api.auth.{AuthenticationMiddleware, AuthorizationMiddleware, MakerCheckerMiddleware}
import tech.pegb.backoffice.api.businessuserapplication.controllers.BusinessUserApplicationController
import tech.pegb.backoffice.api.model.{LocalDateTimeFrom, LocalDateTimeTo}
import tech.pegb.backoffice.api.proxy.abstraction.{ProxyController, ProxyRequestHandler}
import tech.pegb.backoffice.api.{ApiController, RequiredHeaders}
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

@Singleton
class BusinessUserApplicationProxyController @Inject() (
    controllerComponents: ControllerComponents,
    val executionContexts: WithExecutionContexts,
    val proxyResponseHandler: ProxyResponseHandler,
    val authenticationMiddleware: AuthenticationMiddleware,
    val authorizationMiddleware: AuthorizationMiddleware,
    val makerCheckerMiddleware: MakerCheckerMiddleware,
    val proxyRequestHandler: ProxyRequestHandler,
    implicit val appConfig: AppConfig)
  extends ApiController(controllerComponents) with RequiredHeaders
  with BusinessUserApplicationController with ProxyController {

  def createBusinessUserApplication(id: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id")
  }

  def getBusinessUserApplication(
    businessName: Option[String],
    brandName: Option[String],
    businessCategory: Option[String],
    stage: Option[String],
    status: Option[String],
    phoneNumber: Option[String],
    email: Option[String],
    dateFrom: Option[LocalDateTimeFrom],
    dateTo: Option[LocalDateTimeTo],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    composeProxyRequest(getRoute)
  }

  def getBusinessUserApplicationStageData(id: UUID, stage: String, status: Option[String]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id")
  }

  def getBusinessUserApplicationById(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id")
  }

  def submitBusinessUserApplication(id: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id/submit")
  }

  def approveBusinessUserApplication(id: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id/approve")
  }

  def cancelBusinessUserApplication(id: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id/cancel")
  }

  def rejectBusinessUserApplication(id: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id/reject")
  }

  def sendForCorrectionBusinessUserApplication(id: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id/send_for_correction")
  }

  def createBusinessUserApplicationConfig(id: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id/stage/config")
  }

  def getBusinessUserApplicationConfig(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id/stage/config")
  }

  def createBusinessUserApplicationContactInfo(id: UUID) = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id/stage/contact_info")
  }

  def getBusinessUserApplicationContactInfo(id: UUID) = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id/stage/contact_info")
  }

  def getBusinessUserApplicationDocuments(id: UUID) = LoggedAsyncAction { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id/stage/documents")
  }

  def createBusinessUserApplicationDocument(id: UUID): Action[MultipartFormData[TemporaryFile]] = LoggedAsyncAction(parse.multipartFormData) { implicit ctx ⇒
    composeProxyRequest(s"$getRoute/$id/stage/documents")
  }
}
