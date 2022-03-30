package tech.pegb.backoffice.api.document.controllers

import java.nio.file.Files
import java.time.LocalDate
import java.util.UUID

import akka.util.ByteString
import cats.data.EitherT
import cats.implicits._
import com.google.inject.Inject
import play.api.http.HttpEntity
import play.api.libs.Files.TemporaryFile
import play.api.mvc._
import tech.pegb.backoffice.api
import tech.pegb.backoffice.api.customer.Constants
import tech.pegb.backoffice.api.document.dto.{DocumentToCreate, RejectionReason}
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.{GenericRequestWithUpdatedAt, PaginatedResult, SuccessfulStatuses}
import tech.pegb.backoffice.api._
import tech.pegb.backoffice.domain.document.abstraction.DocumentManagement
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.document.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.document.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class DocumentMgmtController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    documentMgmtService: DocumentManagement,
    latestVersionService: LatestVersionService,
    implicit val appConfig: AppConfig) extends ApiController(controllerComponents) with RequiredHeaders with ConfigurationHeaders with api.document.DocumentMgmtController {

  import ApiController._
  import ApiErrors._
  import RequiredHeaders._

  implicit val executionContext: ExecutionContext = executionContexts.blockingIoOperations

  implicit val futureTimeout: FiniteDuration = appConfig.FutureTimeout

  val formKeyForFileUpload: String = appConfig.Document.FormKeyForFileUpload
  val formKeyForJson: String = appConfig.Document.FormKeyForJson
  val DefaultOrdering = "-created_at"

  def getDocument(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId = getRequestId
    documentMgmtService.getDocument(id)
      .map(result ⇒ handleApiResponse(result.map(_.asApi.toJsonStr).leftMap(_.asApiError())))
  }

  def getDocumentFile(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId = getRequestId

    documentMgmtService.getDocumentFile(id).map(_.leftMap(_.asApiError()).fold(
      makeApiErrorResponse,
      result ⇒ Result(
        header = ResponseHeader(200, Map("id" → id.toString, "filename" → result.filename.getOrElse(""))),
        body = HttpEntity.Strict(ByteString(result.content), result.fileType.map(MimeType.fromFileExtension)))))
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

    implicit val requestId = getRequestId

    (for {

      partialMatchFields ← EitherT.fromEither[Future](partialMatch.validatePartialMatch(Constants.validDocumentMgmntPartialMatchFields)
        .leftMap(_.log().asInvalidRequestApiError()))

      criteria ← EitherT.fromEither[Future]((status, documentType, customerId, customerFullName,
        customerMsisdn, None, startDate, endDate, isCheckedAt, partialMatchFields).asDomain()
        .toEither.leftMap(_.log()
          .asInvalidRequestApiError("Invalid request to fetch documents. Value of a query parameter is not in the correct format or not among the expected values.".toOption)))

      latestVersion ← EitherT(latestVersionService.getLatestVersion(criteria)
        .map(_.leftMap(_ ⇒ "Failed getting the latest version of documents.".asUnknownApiError)))

      total ← EitherT(executeIfGET(documentMgmtService.countDocumentsByCriteria(criteria)
        .map(_.leftMap(_.asApiError("Failed to count documents".toOption)))
        .futureWithTimeout.recover {
          case e: Throwable ⇒
            logger.warn("Couldn't count documents within the specified time limit", e)
            Right(-1)
        }, NoCount.toFuture))

      results ← EitherT(executeIfGET(documentMgmtService.getDocumentsByCriteria(
        criteria,
        ordering.getOrElse(DefaultOrdering).asDomain,
        limit, offset).map(_.leftMap(_.asApiError())), NoResult.toFuture))

    } yield {
      (PaginatedResult(total, results.map(_.asApi), limit, offset).toJsonStr, latestVersion)

    }).value.map(_.toTuple2FirstOneEither).map {
      case (result, latestVersion) ⇒ handleApiResponse(result).withLatestVersionHeader(latestVersion)
    }
  }

  def createDocument(): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId = getRequestId

    (for {
      parsedRequest ← EitherT.fromEither[Future](ctx.body.as(classOf[DocumentToCreate], isDeserializationStrict)
        .toEither.leftMap(_.log()
          .asMalformedRequestApiError("Malformed request to create document. Mandatory field is missing or value of a field is of wrong type.".toOption)))

      result ← EitherT(documentMgmtService.createDocument(parsedRequest.asDomain(getRequestFrom, getRequestDate)).map(_.leftMap(_.asApiError())))

    } yield {
      result.asApi.toJsonStr
    }).value.map(handleApiResponse(_, SuccessfulStatuses.Created))

  }

  def uploadDocumentFile(documentId: UUID): Action[MultipartFormData[TemporaryFile]] = LoggedAsyncAction(parse.multipartFormData) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    (for {
      dataPart ← EitherT.fromOption[Future](ctx.body.dataParts.get(formKeyForJson), s"""Form key "${formKeyForJson}" must exist""".asInvalidRequestApiError)

      json ← EitherT.fromOption[Future](dataPart.headOption, """data part option must not be empty""".asInvalidRequestApiError)

      genericRequestWithUpdatedAt ← EitherT.fromEither[Future](json.as(classOf[GenericRequestWithUpdatedAt], isDeserializationStrict)
        .toEither.leftMap(_.asInvalidRequestApiError()))

      file ← EitherT.fromOption[Future](ctx.body.file(formKeyForFileUpload), s"Form key must be called `$formKeyForFileUpload`".asInvalidRequestApiError)

      bytes ← EitherT.fromEither[Future](Try(Files.readAllBytes(file.ref.path)).toEither
        .leftMap(_.log().asInvalidRequestApiError("Failed to upload document. Unable to read file.".toOption)))

      result ← EitherT(documentMgmtService.uploadDocumentFile(
        documentId = documentId,
        content = bytes,
        uploadedBy = getRequestFrom,
        uploadedAt = getRequestDate.toLocalDateTimeUTC,
        lastUpdatedAt = genericRequestWithUpdatedAt.lastUpdatedAt.map(_.toLocalDateTimeUTC)).map(_.leftMap(_.asApiError())))
    } yield {
      result.asApi.toJsonStr
    }).value.map(handleApiResponse(_, SuccessfulStatuses.Created))

  }

  def approveDocument(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    (for {
      parsedRequest ← EitherT.fromEither[Future](ctx.body.as(classOf[GenericRequestWithUpdatedAt], isDeserializationStrict)
        .toEither.leftMap(_.log().asMalformedRequestApiError()))

      result ← {
        val dto = (id, getRequestFrom, getRequestDate, parsedRequest.lastUpdatedAt).asDomain
        EitherT(documentMgmtService.approveDocument(dto).map(_.leftMap(_.asApiError())))
      }
    } yield {
      result.asApi.toJsonStr
    }).value.map(handleApiResponse(_))
  }

  def rejectDocument(id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId = getRequestId

    (for {
      parsedRequest ← EitherT.fromEither[Future](ctx.body.as(classOf[RejectionReason], isDeserializationStrict)
        .toEither.leftMap(_.log().asMalformedRequestApiError()))

      result ← {
        val dto = (id, getRequestFrom, getRequestDate, parsedRequest.reason, parsedRequest.lastUpdatedAt).asDomain
        EitherT(documentMgmtService.rejectDocument(dto).map(_.leftMap(_.asApiError())))
      }
    } yield {
      result.asApi.toJsonStr
    }).value.map(handleApiResponse(_))
  }

}
