package tech.pegb.backoffice.api.customer.controllers.impl

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.Inject
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api._
import tech.pegb.backoffice.api.customer.controllers
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.GenericRequestWithUpdatedAt
import tech.pegb.backoffice.domain.customer.abstraction.SavingOptionsMgmtService
import tech.pegb.backoffice.mapping.api.domain.customer.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.savingoptions.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.Future

class SavingOptionsController @Inject() (
    executionContexts: WithExecutionContexts,
    implicit val appConfig: AppConfig,
    controllerComponents: ControllerComponents,
    savingGoalsMgmtService: SavingOptionsMgmtService)

  extends ApiController(controllerComponents) with RequiredHeaders with ConfigurationHeaders with controllers.SavingOptionsController {

  implicit val executionContext = executionContexts.blockingIoOperations

  import ApiController._
  import ApiErrors._
  import RequiredHeaders._

  def getSavingOptionsByCustomerId(id: UUID, status: Option[String]): Action[AnyContent] =

    LoggedAsyncAction { implicit ctx ⇒
      implicit val requestId: UUID = getRequestId

      (for {
        savingOptions ← EitherT(executeIfGET(savingGoalsMgmtService.getCustomerSavingOptions(id, (id.some, status).asDomain.some)
          .map(_.leftMap(_.asApiError())), NoResult.toFuture))

        latestVersion ← EitherT(savingGoalsMgmtService.getLatestVersion(savingOptions).map(_.leftMap(_.asApiError())))

      } yield (savingOptions.map(_.asApi).toJsonStr, latestVersion)).value.map(_.toTuple2FirstOneEither).map {
        case (result, latestVersion) ⇒ handleApiResponse(result).withLatestVersionHeader(latestVersion)
      }
    }

  def deactivateSavingOption(customerId: UUID, id: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId
    val updatedAt = getRequestDate.toLocalDateTimeUTC
    val updatedBy = getRequestFrom

    (for {
      parsedRequest ← EitherT.fromEither[Future](ctx.body.as(
        classOf[GenericRequestWithUpdatedAt],
        isDeserializationStrict).toEither
        .leftMap(_.asMalformedRequestApiError("Malformed request to deactivate saving option. Mandatory field is missing or value of a field is of wrong type.".toOption)))

      result ← EitherT(savingGoalsMgmtService.deactivateSavingOption(
        id,
        customerId,
        updatedBy,
        updatedAt,
        parsedRequest.lastUpdatedAt.map(_.toLocalDateTimeUTC))
        .map(_.leftMap(_.asApiError())))

    } yield {
      result.asApi.toJsonStr
    }).value.map(handleApiResponse(_))
  }

}
