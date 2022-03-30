package tech.pegb.backoffice.api.communication.controllers

import com.google.inject.Inject
import play.api.mvc._
import tech.pegb.backoffice.api.{ApiController, RequiredHeaders}
import tech.pegb.backoffice.api.communication.dto.{CoreEvent, EventTypes, PayloadKeys}
import tech.pegb.backoffice.domain.ErrorCodes
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.Future
import scala.util.Try
import tech.pegb.backoffice.domain.application.abstraction.WalletApplicationManagement

//as described in https://pegb-tech.atlassian.net/wiki/spaces/PW/pages/538214441/Activation+flow+endpoints#Activationflow&endpoints-CoreEndpointsfortheBackoffice

class WalletCoreEventsController @Inject() (
    controllerComponents: ControllerComponents,
    executionContexts: WithExecutionContexts,
    val appConfig: AppConfig,
    walletApplicationMgmt: WalletApplicationManagement) extends ApiController(controllerComponents) with RequiredHeaders {

  implicit val ec = executionContexts.genericOperations

  def receiveCoreEvents(): Action[CoreEvent] = LoggedAsyncAction(parse.json[CoreEvent]) { ctx ⇒
    val coreEvent = ctx.body

    logger.info(s"received request from wallet core [${ctx.remoteAddress}] ${ctx.path} -d ${coreEvent}")

    coreEvent.`type` match {
      case EventTypes.ApplicationApproved ⇒
        coreEvent.payload.fields.find(_._1 == PayloadKeys.Id) match {
          case Some(approveApplicationPayload) ⇒

            Try(approveApplicationPayload._2.as[Int]).fold(
              error ⇒
                Future.successful(BadRequest("id expected should be integer")),
              applicationId ⇒ {
                walletApplicationMgmt
                  .persistApprovedFilesByInternalApplicationId(applicationId).map(_.fold(
                    error ⇒ {
                      if (error.code == ErrorCodes.Unknown) {
                        InternalServerError(error.message)
                      } else {
                        BadRequest(error.message)
                      }
                    },
                    approvedIds ⇒ NoContent))
              })
          case None ⇒
            Future.successful(BadRequest(s"id expected for event type [${EventTypes.ApplicationApproved}]"))
        }

      case _ ⇒
        Future.successful(BadRequest(s"event type [${coreEvent.`type`}] is not recognized"))
    }
  }

}
