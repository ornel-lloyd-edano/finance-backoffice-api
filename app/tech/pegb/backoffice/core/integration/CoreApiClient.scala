package tech.pegb.backoffice.core.integration

import java.time.LocalDateTime
import java.util.UUID

import cats.implicits._
import javax.inject.Inject
import org.slf4j.Logger
import play.api.libs.json.{Json, _}
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.core.integration.abstraction._
import tech.pegb.backoffice.core.integration.dto.{BusinessUserCreateResponse, ReverseTxnRequest, ReversedTxnResponse, TxnCancellationRequest}
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.HttpClientService.HttpResponse
import tech.pegb.backoffice.domain.{HttpClient, ServiceError}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, ExecutionContexts}

import scala.concurrent.{ExecutionContext, Future}

class CoreApiClient @Inject() (
    conf: AppConfig,
    apiClient: HttpClient,
    executionContexts: ExecutionContexts)
  extends CurrencyExchangeCoreApiClient
  with LimitProfileCoreApiClient
  with FeeProfileCoreApiClient
  with ManualTransactionCoreApiClient
  with SavingOptionsCoreApiClient
  with TransactionsCoreApiClient
  with BusinessUserCoreApiClient {

  import CoreApiClient._

  //TODO refactor to CurrentlyExchangeApiClient
  private val activateFxUrl: String = conf.CoreCurrencyExchangeActivationUrl

  //TODO refactor to an generic trait for notification purpose (ex. notifySpreadUpdated, notifyLimitProfileUpdated etc)
  private val notifyUrl: String = conf.BackOfficeEventsNotificationUrl
  private implicit val ec: ExecutionContext = executionContexts.blockingIoOperations

  override val createManualTxnUrl: String = conf.CreateManualTxnUrl
  override val createManualTxnFxUrl: String = conf.CreateManualTxnFxUrl

  def getCancelTxnUrl: String = conf.CancelTransactionUrl
  def getReverseTxnUrl: String = conf.ReverseTransactionUrl

  def getDeactivateSavingGoalUrl: String = conf.CoreDeactivateSavingGoalUrl

  def getDeactivateAutoDeductUrl: String = conf.CoreDeactivateAutoDeductUrl

  def getDeactivateRoundupUrl: String = conf.CoreDeactivateRoundUpUrl

  def createBusinessUserUrl: String = conf.CreateBusinessUserUrl

  def resetVelocityPortalUserPinUrl: String = conf.ResetVelocityPortalUserPinUrl

  //TODO use proper DTO class instead of writing the payload structure by hand
  override def updateFxStatus(
    id: Long,
    status: String,
    reason: String,
    updatedBy: String,
    lastUpdatedAt: Option[LocalDateTime])(
    implicit
    requestId: UUID): Future[ServiceResponse[Unit]] = {
    val url = s"$activateFxUrl/{id}".replace("{id}", id.toString)
    val payload = JsObject(Map(
      "status" → JsString(status),
      "updated_by" → JsString(updatedBy),
      "reason" → JsString(updatedBy),
      "last_updated_at" → lastUpdatedAt.fold[JsValue](JsNull)(f ⇒ JsString(f.toString))))
    apiClient.request(conf.CoreCurrencyExchangeActivationUrlVerb, url, payload.some)
      .map(_.asServiceResponse(logger, unknownError(_)))
      .recover(recoverFromFutureError(url))
  }

  //TODO use proper DTO class instead of writing the payload structure by hand
  override def batchUpdateFxStatus(
    idSeq: Seq[Long],
    status: String,
    reason: String,
    updatedBy: String,
    lastUpdatedAt: Option[LocalDateTime],
    rates: Map[String, BigDecimal] = Map.empty)(implicit requestId: UUID): Future[ServiceResponse[Unit]] = {
    val url = activateFxUrl
    val payload = JsObject(Map(
      "ids" → JsArray(idSeq.map(JsNumber(_))),
      "status" → JsString(status),
      "updated_by" → JsString(updatedBy),
      "reason" → JsString(updatedBy))) ++ Json.obj("rates" -> rates) //refactor later
    apiClient.request(conf.CoreCurrencyExchangeActivationUrlVerb, url, payload.some)
      .map(_.asServiceResponse(logger, unknownError(_)))
      .recover(recoverFromFutureError(url))
  }

  //TODO use proper DTO class instead of writing the payload structure by hand
  override def notifySpreadUpdated(spreadId: Int)(implicit requestId: UUID): Future[ServiceResponse[Unit]] = {
    val url = notifyUrl
    val payload = JsObject(Map(
      "type" → JsString("currency_spread_update"),
      "payload" → JsObject(Map(
        "ids" → JsArray(Seq(JsNumber(BigDecimal(spreadId))))))))
    apiClient.request(conf.BackOfficeEventsNotificationUrlVerb, url, payload.some)
      .map(_.asServiceResponse(logger, unknownError(_)))
      .recover(recoverFromFutureError(url))
  }

  //TODO use proper DTO class instead of writing the payload structure by hand
  override def notifyLimitProfileUpdated(limitId: Int)(implicit requestId: UUID): Future[ServiceResponse[Unit]] = {
    val url = notifyUrl
    val payload = JsObject(Map(
      "type" → JsString("limit_profile_update"),
      "payload" → JsObject(Map(
        "ids" → JsArray(Seq(JsNumber(limitId)))))))
    apiClient.request(conf.BackOfficeEventsNotificationUrlVerb, url, payload.some)
      .map(_.asServiceResponse(logger, unknownError(_)))
      .recover(recoverFromFutureError(url))
  }

  //TODO use proper DTO class instead of writing the payload structure by hand
  override def notifyFeeProfileUpdated(
    feeProfileId: Int)(
    implicit
    requestId: UUID): Future[ServiceResponse[Unit]] = {
    val url = notifyUrl
    val payload = JsObject(Map(
      "type" → JsString("fee_profile_update"),
      "payload" → JsObject(Map(
        "ids" → JsArray(Seq(JsNumber(feeProfileId)))))))
    apiClient.request(conf.BackOfficeEventsNotificationUrlVerb, url, payload.some)
      .map(_.asServiceResponse(logger, unknownError(_)))
      .recover(recoverFromFutureError(url))
  }

  protected def recoverFromFutureError[U](
    url: ⇒ String): PartialFunction[Throwable, ServiceResponse[U]] = {
    case exc ⇒
      logger.error(s"Unexpected error when calling $url", exc)
      Left(unknownError(s"Unexpected error when calling $url: ${exc.getMessage}"))
  }

  override def createManualTransaction(settlementId: Int, dto: Seq[ManualTxnLinesToCreateCoreDto], isFx: Boolean): Future[ServiceResponse[Unit]] = {
    val coreURL = if (isFx) createManualTxnFxUrl else createManualTxnUrl
    val url = s"$coreURL".replace("{id}", settlementId.toString)
    val payload = JsArray(dto.map(item ⇒ Json.parse(item.toJsonStr)))
    apiClient.request(conf.CreateManualTxnUrlVerb, url, payload.some)
      .map(r ⇒ r.asServiceResponse(logger, unknownError(_)))
      .recover(recoverFromFutureError(url))
  }

  def deactivateSavingGoal(goalId: Long)(implicit requestId: UUID): Future[ServiceResponse[Unit]] = {
    val url = s"$getDeactivateSavingGoalUrl"
      .replace("{goal_id}", goalId.toString)

    val payload = JsObject(Map("is_active" → JsFalse))
    apiClient.request(conf.CoreDeactivateSavingGoalUrlVerb, url, payload.some)
      .map(_.asServiceResponse(logger, unknownError(_)))
      .recover(recoverFromFutureError(url))
  }

  def deactivateAutoDeductSaving(goalId: Long)(implicit requestId: UUID): Future[ServiceResponse[Unit]] = {
    val url = s"$getDeactivateAutoDeductUrl"
      .replace("{goal_id}", goalId.toString)

    val payload = JsObject(Map("is_active" → JsFalse))
    apiClient.request(conf.CoreDeactivateAutoDeductUrlVerb, url, payload.some)
      .map(_.asServiceResponse(logger, unknownError(_)))
      .recover(recoverFromFutureError(url))
  }

  def deactivateRoundUpSaving(goalId: Long)(implicit requestId: UUID): Future[ServiceResponse[Unit]] = {
    val url = s"$getDeactivateRoundupUrl"
      .replace("{goal_id}", goalId.toString)

    val payload = JsObject(Map("is_active" → JsFalse))
    apiClient.request(conf.CoreDeactivateRoundUpUrlVerb, url, payload.some)
      .map(_.asServiceResponse(logger, unknownError(_)))
      .recover(recoverFromFutureError(url))
  }

  implicit val impLogger = logger

  def cancelTransaction(dto: TxnCancellationRequest): Future[ServiceResponse[Unit]] = {
    println(s"cancelTransaction ${dto.toSmartString}")
    getCancelTxnUrl.safeReplaceIdInUrl("{id}", dto.id, { cancelTxnUrl ⇒
      apiClient.request(
        method = conf.CancelTransactionUrlVerb,
        url = cancelTxnUrl, data = dto.json.toOption)
        .map(_.asServiceResponse(logger, unknownError(_)))
        .recover(recoverFromFutureError(cancelTxnUrl))
    })
  }

  def revertTransaction(dto: ReverseTxnRequest): Future[ServiceResponse[ReversedTxnResponse]] = {
    getReverseTxnUrl.safeReplaceIdInUrl("{id}", dto.id, { revertTxnUrl ⇒
      apiClient.request(
        conf.ReverseTransactionUrlVerb,
        url = revertTxnUrl, data = dto.json.toOption)
        .map(_.asServiceResponseWithJsonBody(logger, unknownError(_)).map(responsePayload ⇒ {
          logger.debug(s"response payload from $revertTxnUrl is $responsePayload")
          responsePayload.as[ReversedTxnResponse]
        }))
        .recover(recoverFromFutureError(revertTxnUrl))
    })

  }

  def createBusinessUserApplication(
    applicationId: Int,
    createdBy: String): Future[ServiceResponse[BusinessUserCreateResponse]] = {

    val url = createBusinessUserUrl
    val payload = JsObject(Map(
      "application_id" → JsNumber(applicationId),
      "created_by" → JsString(createdBy)))

    apiClient.request(conf.CreateBusinessUserUrlVerb, url, payload.some)
      .map(_.asServiceResponseWithJsonBodyAndTimeout(logger, unknownError(_), "Please try again after 5 minutes").map(responsePayload ⇒ {
        logger.debug(s"response payload from $url is $responsePayload")
        responsePayload.as[BusinessUserCreateResponse]
      }))
      .recover(recoverFromFutureError(url))

  }

  def resetVelocityPortalUserPin(
    vpUserId: Int,
    reason: String,
    updatedBy: String,
    lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]] = {
    val url = resetVelocityPortalUserPinUrl
      .replace("{id}", vpUserId.toString)

    val payload = JsObject(Map(
      "reason" → JsString(reason),
      "updated_by" → JsString(updatedBy),
      "last_updated_at" → lastUpdatedAt.fold[JsValue](JsNull)(f ⇒ JsString(f.toString))))

    apiClient.request(conf.ResetVelocityPortalUserPinUrlVerb, url, payload.some)
      .map(_.asServiceResponse(logger, unknownError(_)))
      .recover(recoverFromFutureError(url))

  }
}

object CoreApiClient {

  val IncorrectUrlInConfigError = ServiceError.unknownError(s"Possibly incorrect url in config", UUID.randomUUID().toOption)

  implicit class HttpResponseEvaluator(val resp: HttpResponse) extends AnyVal {
    def asServiceResponse(
      logger: Logger,
      makeError: String ⇒ ServiceError): ServiceResponse[Unit] = {
      if (resp.success) {
        Right(())
      } else {
        logger.warn(s"Failed API call: ${resp.statusCode} - ${resp.body}")
        Left(makeError(s"Failed API call - non-2xx response from CORE API.${resp.body.map(error ⇒ s" Response body: $error")}"))
      }
    }

    def asServiceResponseWithTimeout(
      logger: Logger,
      makeError: String ⇒ ServiceError,
      timeoutAdditionalMessage: String): ServiceResponse[Unit] = {
      if (resp.success) {
        Right(())
      } else if (resp.statusCode == 408 || resp.statusCode == 504) {
        logger.warn(s"Timeout API call: ${resp.statusCode} - ${resp.body}")
        Left(ServiceError.timeOutError(s"Timeout encountered on calling CORE API. $timeoutAdditionalMessage: ${resp.statusCode} - ${resp.body.map(error ⇒ s" Response body: $error")}"))
      } else {
        logger.warn(s"Failed API call: ${resp.statusCode} - ${resp.body}")
        Left(makeError(s"Failed API call - non-2xx response from CORE API.${resp.body.map(error ⇒ s" Response body: $error")}"))
      }
    }

    def asServiceResponseWithJsonBody(
      logger: Logger,
      makeError: String ⇒ ServiceError): ServiceResponse[JsObject] = {
      if (resp.success) {
        Right(Json.parse(resp.body.getOrElse("{}")).as[JsObject])
      } else {
        logger.warn(s"Failed API call: ${resp.statusCode} - ${resp.body}")
        Left(makeError(s"Failed API call - non-2xx response from CORE API.${resp.body.map(error ⇒ s" Response body: $error")}"))
      }
    }

    def asServiceResponseWithJsonBodyAndTimeout(
      logger: Logger,
      makeError: String ⇒ ServiceError,
      timeoutAdditionalMessage: String): ServiceResponse[JsObject] = {
      if (resp.success) {
        Right(Json.parse(resp.body.getOrElse("{}")).as[JsObject])
      } else if (resp.statusCode == 408 || resp.statusCode == 504) {
        logger.warn(s"Timeout API call: ${resp.statusCode} - ${resp.body}")
        Left(ServiceError.timeOutError(s"Timeout encountered on calling CORE API. $timeoutAdditionalMessage: ${resp.statusCode} - ${resp.body.map(error ⇒ s" Response body: $error")}"))
      } else {
        logger.warn(s"Failed API call: ${resp.statusCode} - ${resp.body}")
        Left(makeError(s"Failed API call - non-2xx response from CORE API.${resp.body.map(error ⇒ s" Response body: $error")}"))
      }
    }
  }

  implicit class IdInUrlReplaceHelper(val url: String) extends AnyVal {
    def safeReplaceIdInUrl[T](toReplace: String, replacement: String, methodToDoIfReplacementSuccess: (String) ⇒ Future[ServiceResponse[T]])(implicit logger: Logger) = {
      url.smartReplace(toReplace, replacement) match {
        case (replacedUrl, timesReplaced) if timesReplaced == 0 ⇒
          logger.warn(s"No $toReplace replacements occured. ${IncorrectUrlInConfigError.message} [$url]")
          Future.successful(IncorrectUrlInConfigError.toLeft)
        case (replacedUrl, _) ⇒
          methodToDoIfReplacementSuccess(replacedUrl)
      }
    }
  }

}
