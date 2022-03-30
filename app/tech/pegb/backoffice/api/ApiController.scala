package tech.pegb.backoffice.api

import java.util.UUID

import cats.implicits._
import play.api.http._
import play.api.libs.json._
import play.api.mvc._
import tech.pegb.backoffice.api.ApiErrorCodes._
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.makerchecker.controller.MakerCheckerMgmtController.ApiKeyMissingOrMismatch
import tech.pegb.backoffice.api.model.{SuccessfulStatus, SuccessfulStatuses}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, Logging}

import scala.concurrent.Future

abstract class ApiController(controllerComponents: ControllerComponents /*, ws: WSClient*/ )
  extends AbstractController(controllerComponents) with Logging with Results {
  this: RequiredHeaders ⇒

  import RequiredHeaders._

  protected def requestLogger[T](request: Request[T]): Unit = {
    val user = this.getRequestFrom(request)
    val introMsg = if (user == this.defaultUnknownUser) s"Proxy received from unauthenticated user"
    else s"Received from user-> $user"
    logger.info(s"$introMsg from ip-> ${request.remoteAddress} this request-> ${request.method.toUpperCase} ${request.path} -d ${request.body.toString} with request id ${getRequestId(request)}")
  }

  protected def responseLogger[T](request: Request[T], status: Int, maybePayload: Option[String]): Unit = {
    logger.info(s"Response for request ${request.method.toUpperCase} ${request.path} with request id ${getRequestId(request)}:")
    logger.info(s"Status: ${status}, ${maybePayload.map(p ⇒ s"Payload: ${p.trimIfTooLong}").getOrElse("Payload: empty")}")
  }

  def LoggedAction[A >: AnyContent](action: Request[A] ⇒ Result) = Action { request ⇒
    requestLogger(request)
    action(request)
  }

  def LoggedAction[A >: AnyContent](parser: BodyParser[A])(action: Request[A] ⇒ Result) = Action(parser) { request ⇒
    requestLogger(request)
    action(request)
  }

  def LoggedAsyncAction[A >: AnyContent](action: Request[A] ⇒ Future[Result]) = Action.async { request ⇒
    requestLogger(request)
    action(request)
  }

  def LoggedAsyncAction[A](parser: BodyParser[A])(action: Request[A] ⇒ Future[Result]) = Action.async(parser) { request ⇒
    requestLogger(request)
    action(request)
  }

  def validateApiKey[T](implicit ctx: Request[T], appConfig: AppConfig) =
    getRequestApiKey match {
      case Some(apikey) if apikey == appConfig.ApiKeys.BackofficeAuth ⇒ Right("Source of request is trusted")
      case _ ⇒ Left(ApiKeyMissingOrMismatch)
    }

  def handleApiNoContentResponse[R](r: Either[ApiError, Unit], defaultOkStatus: SuccessfulStatus = SuccessfulStatuses.NoContent)(implicit request: Request[R]): Result = {
    val result = r.fold(
      makeApiErrorResponse,
      _ ⇒ {
        defaultOkStatus match {
          case SuccessfulStatuses.NoContent ⇒ NoContent
          case SuccessfulStatuses.Ok ⇒ Ok
          case SuccessfulStatuses.Accepted ⇒ Accepted
          case SuccessfulStatuses.Created ⇒ Created
        }
      })
    responseLogger(request, result.header.status, None)
    result.withStandardHeaders
  }

  def handleApiResponse[T, R](
    r: Either[ApiError, T],
    defaultOkStatus: SuccessfulStatus = SuccessfulStatuses.Ok,
    withBody: Boolean = true)(implicit tWrites: Writes[T], request: Request[R]): Result = {
    val result = r.fold(
      makeApiErrorResponse,
      body ⇒ {
        (defaultOkStatus, withBody, body) match {
          case (SuccessfulStatuses.NoContent, _, _) ⇒ NoContent

          case (SuccessfulStatuses.Ok, true, body: String) ⇒ Ok(body)
          case (SuccessfulStatuses.Ok, true, body) ⇒ Ok(body.json)
          case (SuccessfulStatuses.Ok, false, _) ⇒ Ok

          case (SuccessfulStatuses.Accepted, true, body: String) ⇒ Accepted(body)
          case (SuccessfulStatuses.Accepted, true, body) ⇒ Accepted(body.json)
          case (SuccessfulStatuses.Accepted, false, _) ⇒ Accepted

          case (SuccessfulStatuses.Created, true, body: String) ⇒ Created(body)
          case (SuccessfulStatuses.Created, true, body) ⇒ Created(body.json)
          case (SuccessfulStatuses.Created, false, _) ⇒ Created
        }
      })

    val maybePayload = r.fold(_ ⇒ None, {
      case b: String ⇒ b.toOption
      case b ⇒ Json.prettyPrint(b.json).toOption
    })

    responseLogger(request, result.header.status, maybePayload)
    result.withStandardHeaders
  }

  def executeIfGET[T, R](toBeExecuted: ⇒ T, orElseReturn: T)(implicit ctx: Request[R]): T = {
    if (ctx.method === HttpVerbs.GET) toBeExecuted else orElseReturn
  }

  protected val maxLength = 2048000 //2048 KB

  def text: BodyParser[String] = {
    parse match {
      case p: DefaultPlayBodyParsers ⇒
        p.when(
          _.contentType.exists(_.equalsIgnoreCase("application/json")),
          p.tolerantText(maxLength),
          p.errorHandler.onClientError(_, BAD_REQUEST, "Expecting application/json body"))
      case _ ⇒
        parse.text
    }
  }

  //TODO remove freeText once front-end is passing application/json content type header and json payload for all DELETE requests
  def freeText: BodyParser[String] = {
    parse match {
      case p: DefaultPlayBodyParsers ⇒
        p.when(
          _ ⇒ true,
          p.tolerantText(maxLength),
          p.errorHandler.onClientError(_, BAD_REQUEST, "Expecting body"))
      case _ ⇒
        parse.text
    }
  }

  protected def makeApiErrorResponse[T]: ApiError ⇒ Result = error ⇒ {
    val responseBody = error.json
    error.code match {
      case ApiErrorCodes.MalformedRequest ⇒
        logger.error(s"malformed request: reason $error, responding with 400(BadRequest)")
        BadRequest(responseBody)
      case ApiErrorCodes.InvalidRequest ⇒
        logger.error(s"invalid request: reason $error, responding with 400(BadRequest)")
        BadRequest(responseBody)
      case ApiErrorCodes.NotAuthorized ⇒
        logger.error(s"unauthorized request: reason $error, responding with 401(Unauthorized)")
        Unauthorized(responseBody)
      case ApiErrorCodes.Forbidden ⇒
        logger.error(s"request is forbidden: reason $error, responding with 403(Forbidden)")
        Forbidden(responseBody)
      case ApiErrorCodes.NotFound ⇒
        logger.error(s"requested entity not found: reason $error, responding with 404(NotFound)")
        NotFound(responseBody)
      case ApiErrorCodes.Conflict ⇒
        logger.error(s"encountered conflict: reason $error, responding with 409(Conflict)")
        Conflict(responseBody)
      case ApiErrorCodes.PreconditionFailed ⇒
        logger.error(s"pre condition failed: reason $error, responding with 412(PreconditionFailed)")
        PreconditionFailed(responseBody)
      case ApiErrorCodes.NotAvailable ⇒
        logger.error(s"service is not available: reason $error, responding with 503(ServiceUnavailable)")
        ServiceUnavailable(responseBody)
      case ApiErrorCodes.Unknown ⇒
        logger.error(s"unknown error: reason $error, responding with 500(InternalServerError)")
        InternalServerError(responseBody)
      case ApiErrorCodes.AccountTemporarilyLocked ⇒
        logger.error(s"account is temporarily locked : reason $error, responding with 429(TooManyRequests)")
        TooManyRequests(responseBody)
      case ApiErrorCodes.CaptchaRequired ⇒
        logger.error(s"captcha is required: reason $error, responding with 401(Unauthorized)")
        Unauthorized(responseBody)
      case ApiErrorCodes.Timeout ⇒
        logger.error(s"Internal timeout encountered: $error")
        RequestTimeout(responseBody)
      case other ⇒
        logger.error(s"Unexpected $other: reason $error, responding with 500(InternalServerError)")
        InternalServerError(responseBody)
    }
  }

}

object ApiController {

  import play.api.mvc.Results._

  val NoCount = Right(0)
  val NoResult = Right(Nil)

  implicit class JsErrorInternalConverter(val jsErrors: Seq[(JsPath, Seq[JsonValidationError])]) extends AnyVal {
    private def asString(f: ((JsPath, Seq[JsonValidationError])) ⇒ String): String = {
      jsErrors.map(f).mkString(",")
    }

    def asHumanString: String = asString({
      case (jsPath, jsValidationErrors) ⇒
        val path = jsPath.toString.replace("/", "")
        val validationError = jsValidationErrors.map(_.message).mkString(",")
        s"$path:${validationError.replace("error.path.", "")}"
    })

    def asPlainPathString: String = asString({
      case (jsPath, _) ⇒
        val path = jsPath.toString.replace("/", "")
        s"$path: invalid"
    })
  }

  implicit class ApiErrorCodeToResultAdapter(val arg: ApiErrorCode) extends AnyVal {
    def asResult = arg match {
      case Unknown ⇒ Results.InternalServerError
      case MalformedRequest ⇒ Results.InternalServerError
      case BadRequest ⇒ Results.BadRequest
      case Conflict ⇒ Results.Conflict
      case NotFound ⇒ Results.NotFound
      case NotAuthorized ⇒ Results.Unauthorized
      case Forbidden ⇒ Results.Forbidden
      case NotAvailable ⇒ Results.ServiceUnavailable
      case _ ⇒ Results.InternalServerError
    }
  }

  implicit class ErrorMsgToApiErrorAdapter(val customErrorMsg: String) extends AnyVal {
    def asNotFoundApiError(implicit requestId: UUID): ApiError = {
      ApiError(requestId, ApiErrorCodes.NotFound, customErrorMsg)
    }

    def asInvalidRequestApiError(implicit requestId: UUID): ApiError = {
      ApiError(requestId, ApiErrorCodes.InvalidRequest, customErrorMsg)
    }

    def asMalformedRequestApiError(implicit requestId: UUID): ApiError = {
      ApiError(requestId, ApiErrorCodes.MalformedRequest, customErrorMsg)
    }

    def asUnknownApiError(implicit requestId: UUID): ApiError = {
      ApiError(requestId, ApiErrorCodes.Unknown, customErrorMsg)
    }

    def asNotAuthorizedApiError(implicit requestId: UUID): ApiError = {
      ApiError(requestId, ApiErrorCodes.NotAuthorized, customErrorMsg)
    }
  }

  implicit class RichQueryParam(val queryParam: Option[String]) extends AnyVal {
    def validatePartialMatch(validPartialMatchFields: Set[String]): Either[Exception, Set[String]] = {
      queryParam.map(_.sanitize) match {
        case None ⇒
          Right(validPartialMatchFields.filterNot(_ === "disabled"))
        case Some(partialMatch) if partialMatch.contains("disabled") ⇒
          Right(Set("disabled"))
        case Some(partialMatch) ⇒
          val fields = partialMatch.toSeqByComma
          if (fields.containsOnly(validPartialMatchFields))
            Right(fields.toSet)
          else
            Left(new Exception(s"invalid field for partial matching found. " +
              s"Valid fields: ${validPartialMatchFields.toSeq.sorted.defaultMkString}"))

      }
    }

    def validateOrderBy(validOrderByFields: Set[String]): Either[Exception, Seq[String]] = {
      queryParam.map(_.sanitize) match {
        case Some(partialMatch) ⇒
          val fields = partialMatch.toSeqByComma
          if (fields.map(_.replace("-", "")).containsOnly(validOrderByFields))
            Right(fields)
          else
            Left(new Exception(s"invalid value for order_by found. " +
              s"Valid values: ${validOrderByFields.toSeq.sorted.defaultMkString}"))

        case None ⇒
          Right(Nil)
      }
    }
  }

}

