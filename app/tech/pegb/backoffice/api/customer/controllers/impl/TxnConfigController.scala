package tech.pegb.backoffice.api.customer.controllers.impl

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.customer.controllers
import tech.pegb.backoffice.api.customer.dto._
import tech.pegb.backoffice.api.error.jackson.Implicits._
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.{GenericRequestWithUpdatedAt, PaginatedResult, SuccessfulStatuses}
import tech.pegb.backoffice.api.{ApiController, ConfigurationHeaders, RequiredHeaders}
import tech.pegb.backoffice.domain.account.abstraction.ExternalAccountManagement
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.mapping.api.domain.account.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.customer.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class TxnConfigController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    externalAccountMgmt: ExternalAccountManagement,
    implicit val appConfig: AppConfig)
  extends ApiController(controllerComponents)
  with RequiredHeaders
  with ConfigurationHeaders with controllers.TxnConfigController {

  import ApiController._
  import RequiredHeaders._
  import tech.pegb.backoffice.api.ApiErrors._

  implicit val executionContext: ExecutionContext = executionContexts.genericOperations

  def getTxnConfig(id: UUID): Action[AnyContent] = ???

  def getTxnConfigByCriteria(
    customerId: Option[UUIDLike],
    customerName: Option[String],
    currency: Option[String],
    transactionType: Option[String],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int], offset: Option[Int]): Action[AnyContent] = ???

  def createTxnConfig: Action[String] = ???

  def updateTxnConfig(id: UUID): Action[String] = ???

  def deleteTxnConfig(id: UUID): Action[String] = ???
}
