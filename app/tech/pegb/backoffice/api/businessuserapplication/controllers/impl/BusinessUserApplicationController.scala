package tech.pegb.backoffice.api.businessuserapplication.controllers.impl

import java.nio.file.Files
import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject._
import play.api.libs.Files.TemporaryFile
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent, ControllerComponents, MultipartFormData}
import tech.pegb.backoffice.api.ApiErrors._
import tech.pegb.backoffice.api.businessuserapplication.controllers
import tech.pegb.backoffice.api.businessuserapplication.dto._
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model._
import tech.pegb.backoffice.api.{ApiController, ConfigurationHeaders, RequiredHeaders}
import tech.pegb.backoffice.domain.businessuserapplication.abstraction.{BusinessUserApplicationManagement, Stages, Status ⇒ BusUsrAppStatus}
import tech.pegb.backoffice.domain.document.abstraction.DocumentManagement
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.businessuserapplication.Implicits._
import tech.pegb.backoffice.mapping.api.domain.document.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.businessuserapplication.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class BusinessUserApplicationController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    latestVersionService: LatestVersionService,
    businessUserApplicationManagement: BusinessUserApplicationManagement,
    documentService: DocumentManagement,
    implicit val appConfig: AppConfig) extends ApiController(controllerComponents)
  with RequiredHeaders
  with ConfigurationHeaders
  with controllers.BusinessUserApplicationController {

  import ApiController._
  import BusinessUserApplicationController._
  import RequiredHeaders._

  implicit val executionContext: ExecutionContext = executionContexts.genericOperations
  implicit val futureTimeout: FiniteDuration = appConfig.FutureTimeout

  def createBusinessUserApplication(id: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      businessUserApplicationToCreateApi ← EitherT.fromEither[Future](
        ctx.body.toString.as(classOf[BusinessUserApplicationToCreate], isDeserializationStrict)
          .toEither.leftMap(_.asMalformedRequestApiError(MalformedCreateBusinessUserApplicationErrorMsg.some)))

      businessUserApplicationToCreate ← EitherT.fromEither[Future](
        businessUserApplicationToCreateApi.asDomain(id, doneAt, doneBy).toEither
          .leftMap(t ⇒ {
            logger.error("[createBusinessUserApplication] Error encountered while creating domain entity", t)
            t.asInvalidRequestApiError(t.getMessage.some)
          }))

      createdApplication ← EitherT(businessUserApplicationManagement.createBusinessUserApplication(businessUserApplicationToCreate))
        .map(_.asApi[BusinessUserApplicationToRead].toJsonStr)
        .leftMap(_.asApiError())

    } yield createdApplication).value.map(handleApiResponse(_, SuccessfulStatuses.Created))
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

    implicit val requestId: UUID = getRequestId

    (for {
      _ ← EitherT.fromEither[Future](
        (dateFrom, dateTo) match {
          case (Some(from), Some(to)) if (from.localDateTime.isAfter(to.localDateTime)) ⇒
            "date_from must be before or equal to date_to".asInvalidRequestApiError.toLeft
          case _ ⇒ (dateFrom, dateTo).toRight
        })

      partialMatchSet ← EitherT.fromEither[Future](partialMatch.validatePartialMatch(businessUSerApplicationPartialMatchFields)
        .leftMap(_.log().asInvalidRequestApiError()))

      ordering ← EitherT.fromEither[Future](orderBy.validateOrdering(businessUserApplicationValidSorter)
        .leftMap(_.log().asInvalidRequestApiError()))

      criteria ← EitherT.fromEither[Future](
        (businessName, brandName, businessCategory, stage, status, dateFrom, dateTo, phoneNumber, email, partialMatchSet).asDomain.toEither
          .leftMap(_.log().asInvalidRequestApiError()))

      count ← EitherT(executeIfGET(businessUserApplicationManagement.countBusinessUserApplicationByCriteria(criteria)
        .futureWithTimeout
        .map(_.leftMap(_.asApiError("failed to get the count of business user applications".some))), NoCount.toFuture))

      application ← EitherT(executeIfGET(
        businessUserApplicationManagement.getBusinessUserApplicationByCriteria(criteria, ordering, limit, offset)
          .futureWithTimeout
          .map(_.leftMap(_.asApiError("failed to get list of business user applications".some))), NoResult.toFuture))

      latestVersion ← EitherT(latestVersionService.getLatestVersion(criteria)
        .map(_.leftMap(_.asApiError("failed to get the latest version".some))))

    } yield (PaginatedResult(count, application.map(_.asApi[BusinessUserApplicationToRead]), limit, offset).toJsonStrWithoutEscape, latestVersion))
      .value.map(_.toTuple2FirstOneEither).map {
        case (result, version) ⇒ handleApiResponse(result).withLatestVersionHeader(version)
      }
  }

  def getBusinessUserApplicationStageData(id: UUID, stage: String, status: Option[String]): Action[AnyContent] = {
    (status, stage) match {
      case (Some(BusUsrAppStatus.Ongoing), Stages.Config) ⇒ ??? //Redirect to business_user_applications/:id/txn_config
      case (Some(BusUsrAppStatus.Ongoing), Stages.Contact) ⇒ ??? //Redirect to business_user_applications/:id/contact_info
      case (Some(BusUsrAppStatus.Ongoing), Stages.Docs) ⇒ ??? //Redirect to business_user_applications/:id/documents
      case (_, Stages.Identity) ⇒ getBusinessUserApplicationById(id)
      case _ ⇒ LoggedAsyncAction { implicit ctx ⇒
        NotFound.toFuture
      }
    }
  }

  def getBusinessUserApplicationById(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    businessUserApplicationManagement.getBusinessUserApplicationById(id, Nil).map(serviceResponse ⇒
      handleApiResponse(serviceResponse.map(_.asApi[BusinessUserApplicationToRead].toJsonStrWithoutEscape).leftMap(_.asApiError())))
  }

  def submitBusinessUserApplication(id: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      apiDto ← EitherT.fromEither[Future](
        ctx.body.toString.as(classOf[GenericRequestWithUpdatedAt], isDeserializationStrict)
          .toEither.leftMap(_.asMalformedRequestApiError(MalformedUpdateBusinessUserApplicationExplanationErrorMsg.some)))
      res ← EitherT(
        businessUserApplicationManagement.submitBusinessUserApplication(
          applicationId = id,
          updatedAt = doneAt.toLocalDateTimeUTC,
          updatedBy = doneBy,
          lastUpdatedAt = apiDto.lastUpdatedAt.map(_.toLocalDateTimeUTC)))
        .leftMap(_.asApiError())
    } yield {
      res
    }).value.map(handleApiNoContentResponse(_, SuccessfulStatuses.Ok))
  }

  def approveBusinessUserApplication(id: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      apiDto ← EitherT.fromEither[Future](
        ctx.body.toString.as(classOf[GenericRequestWithUpdatedAt], isDeserializationStrict)
          .toEither.leftMap(_.asMalformedRequestApiError(MalformedUpdateBusinessUserApplicationExplanationErrorMsg.some)))
      res ← EitherT(
        businessUserApplicationManagement.approveBusinessUserApplication(
          applicationId = id,
          updatedAt = doneAt.toLocalDateTimeUTC,
          updatedBy = doneBy,
          lastUpdatedAt = apiDto.lastUpdatedAt.map(_.toLocalDateTimeUTC)))
        .leftMap(_.asApiError())
    } yield {
      res
    }).value.map(handleApiNoContentResponse(_, SuccessfulStatuses.Ok))
  }

  def cancelBusinessUserApplication(id: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      apiDto ← EitherT.fromEither[Future](
        ctx.body.toString.as(classOf[BusinessUserApplicationExplanationToUpdate], isDeserializationStrict)
          .toEither.leftMap(_.asMalformedRequestApiError(MalformedUpdateBusinessUserApplicationExplanationErrorMsg.some)))
      res ← EitherT(
        businessUserApplicationManagement.cancelBusinessUserApplication(
          applicationId = id,
          explanation = apiDto.explanation,
          updatedAt = doneAt.toLocalDateTimeUTC,
          updatedBy = doneBy,
          lastUpdatedAt = apiDto.lastUpdatedAt.map(_.toLocalDateTimeUTC)))
        .leftMap(_.asApiError())
    } yield {
      res
    }).value.map(handleApiNoContentResponse(_, SuccessfulStatuses.Ok))
  }

  def rejectBusinessUserApplication(id: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      apiDto ← EitherT.fromEither[Future](
        ctx.body.toString.as(classOf[BusinessUserApplicationExplanationToUpdate], isDeserializationStrict)
          .toEither.leftMap(_.asMalformedRequestApiError(MalformedUpdateBusinessUserApplicationExplanationErrorMsg.some)))
      res ← EitherT(
        businessUserApplicationManagement.rejectBusinessUserApplication(
          applicationId = id,
          explanation = apiDto.explanation,
          updatedAt = doneAt.toLocalDateTimeUTC,
          updatedBy = doneBy,
          lastUpdatedAt = apiDto.lastUpdatedAt.map(_.toLocalDateTimeUTC)))
        .leftMap(_.asApiError())
    } yield {
      res
    }).value.map(handleApiNoContentResponse(_, SuccessfulStatuses.Ok))
  }

  def sendForCorrectionBusinessUserApplication(id: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      apiDto ← EitherT.fromEither[Future](
        ctx.body.toString.as(classOf[BusinessUserApplicationExplanationToUpdate], isDeserializationStrict)
          .toEither.leftMap(_.asMalformedRequestApiError(MalformedUpdateBusinessUserApplicationExplanationErrorMsg.some)))
      res ← EitherT(
        businessUserApplicationManagement.sendForCorrectionBusinessUserApplication(
          applicationId = id,
          explanation = apiDto.explanation,
          updatedAt = doneAt.toLocalDateTimeUTC,
          updatedBy = doneBy,
          lastUpdatedAt = apiDto.lastUpdatedAt.map(_.toLocalDateTimeUTC)))
        .leftMap(_.asApiError())
    } yield {
      res
    }).value.map(handleApiNoContentResponse(_, SuccessfulStatuses.Ok))
  }

  def createBusinessUserApplicationConfig(id: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      businessUserApplicationConfigToCreateApi ← EitherT.fromEither[Future](
        ctx.body.toString.as(classOf[BusinessUserApplicationConfigToCreate], isDeserializationStrict)
          .toEither.leftMap(_.asMalformedRequestApiError(MalformedCreateBusinessUserApplicationConfigErrorMsg.some)))
      domainDto ← EitherT.fromEither[Future](
        businessUserApplicationConfigToCreateApi.asDomain(id, doneAt, doneBy).toEither
          .leftMap(t ⇒ {
            logger.error("[createBusinessUserApplicationConfig] Error encountered while creating domain entity", t)
            t.asInvalidRequestApiError(t.toString.some)
          }))

      createdApplication ← EitherT(businessUserApplicationManagement.createBusinessUserApplicationConfig(domainDto)
        .map(_.map(_.asApi[BusinessUserApplicationConfigToRead].toJsonStr)
          .leftMap(_.asApiError())))

    } yield createdApplication).value.map(handleApiResponse(_))
  }

  def createBusinessUserApplicationContactInfo(id: UUID): Action[JsValue] = LoggedAsyncAction(parse.json) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    (for {
      businessUserApplicationContactsToCreateApi ← EitherT.fromEither[Future](
        ctx.body.toString.as(classOf[BusinessUserApplicationContactInfoToCreate], isDeserializationStrict)
          .toEither.leftMap(_.asMalformedRequestApiError(MalformedCreateBusinessUserApplicationContactErrorMsg.some)))

      contactAndAddress ← EitherT.fromEither[Future](Try {
        (
          businessUserApplicationContactsToCreateApi.contacts.map(_.asDomain.get),
          businessUserApplicationContactsToCreateApi.addresses.map(_.asDomain.get))
      }.toEither.leftMap(_.asMalformedRequestApiError(MalformedCreateBusinessUserApplicationContactErrorMsg.some)))

      createdApplication ← EitherT(businessUserApplicationManagement.createBusinessUserContactInfo(
        id,
        contactAndAddress._1, contactAndAddress._2, getRequestFrom, getRequestDate.toLocalDateTimeUTC,
        businessUserApplicationContactsToCreateApi.lastUpdatedAt.map(_.toLocalDateTimeUTC))
        .map(_.map(_.asApi[BusinessUserApplicationContactInfoToRead].toJsonStr)
          .leftMap(_.asApiError())))

    } yield createdApplication).value.map(handleApiResponse(_))
  }

  def getBusinessUserApplicationContactInfo(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    businessUserApplicationManagement.getBusinessUserApplicationById(id, Seq(Stages.Contact)).map(serviceResponse ⇒
      handleApiResponse(serviceResponse.map(_.asApi[BusinessUserApplicationContactInfoToRead].toJsonStrWithoutEscape).leftMap(_.asApiError())))
  }

  def getBusinessUserApplicationConfig(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    businessUserApplicationManagement.getBusinessUserApplicationById(id, Seq(Stages.Config)).map(serviceResponse ⇒
      handleApiResponse(serviceResponse.map(_.asApi[BusinessUserApplicationConfigToRead].toJsonStrWithoutEscape).leftMap(_.asApiError())))
  }

  def getBusinessUserApplicationDocuments(id: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    businessUserApplicationManagement.getBusinessUserApplicationById(id, Seq(Stages.Docs))
      .map(_.map(_.asApi[BusinessUserApplicationDocumentToRead].toJsonStr).leftMap(_.asApiError()))
      .map(handleApiResponse(_))
  }

  def createBusinessUserApplicationDocument(id: UUID): Action[MultipartFormData[TemporaryFile]] = LoggedAsyncAction(parse.multipartFormData) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    val formKeyForJson: String = appConfig.Document.FormKeyForJson
    val formKeyForFileUpload: String = appConfig.Document.FormKeyForFileUpload
    val doneBy = getRequestFrom
    val doneAt = getRequestDate

    (for {
      dataPart ← EitherT.fromOption[Future](ctx.body.dataParts.get(formKeyForJson), s"""Form key "${formKeyForJson}" must exist""".asInvalidRequestApiError)

      json ← EitherT.fromOption[Future](dataPart.headOption, """data part option must not be empty""".asInvalidRequestApiError)

      buDocToCreate ← EitherT.fromEither[Future](json.as(classOf[DocumentMetadataToCreate], isDeserializationStrict)
        .toEither.leftMap(_.asInvalidRequestApiError()).map(_.copy(applicationId = Some(id)).asDomain(doneBy, doneAt)))

      file ← EitherT.fromOption[Future](ctx.body.file(formKeyForFileUpload), s"Form key must be called `$formKeyForFileUpload`".asInvalidRequestApiError)

      bytes ← EitherT.fromEither[Future](Try(Files.readAllBytes(file.ref.path)).toEither
        .leftMap(_.log().asInvalidRequestApiError("Failed to upload document. Unable to read file.".toOption)))

      result ← EitherT(documentService.upsertBusinessUserDocument(buDocToCreate, bytes, doneBy, doneAt.toLocalDateTimeUTC)
        map (_.leftMap(_.asApiError())))

    } yield {
      result.asApi.toJsonStr
    }).value.map(handleApiResponse(_))
  }
}

object BusinessUserApplicationController {
  val MalformedCreateBusinessUserApplicationErrorMsg = "Malformed request to create a business user application. Mandatory field is missing or value of a field is of wrong type."
  val MalformedCreateBusinessUserApplicationConfigErrorMsg = "Malformed request to create a business user application config. Mandatory field is missing or value of a field is of wrong type."
  val MalformedCreateBusinessUserApplicationContactErrorMsg = "Malformed request to create a business user application contact. Mandatory field is missing or value of a field is of wrong type."
  val MalformedUpdateBusinessUserApplicationExplanationErrorMsg = "Malformed request to update a business user application explanation. Mandatory field is missing or value of a field is of wrong type."

  val MalformedUpdateBusinessUserApplicationErrorMsg = "Malformed request to update a business user application. Mandatory field is missing or value of a field is of wrong type."

  val businessUserApplicationValidSorter = Set(
    "uuid", "business_name", "brand_name", "business_category", "stage", "status", "user_tier",
    "business_type", "registration_number", "tax_number", "registration_date", "explanation",
    "submitted_by", "submitted_at", "checked_by", "checked_at",
    "created_at", "created_by", "updated_at", "updated_by")

  val businessUSerApplicationPartialMatchFields = Set(
    "uuid", "business_name", "brand_name", "business_category", "registration_number", "tax_number", "explanation", "phone_number", "email")

}
