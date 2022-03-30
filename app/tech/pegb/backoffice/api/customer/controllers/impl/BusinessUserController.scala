package tech.pegb.backoffice.api.customer.controllers.impl

import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import tech.pegb.backoffice.api.ApiErrors._
import tech.pegb.backoffice.api.customer.controllers
import tech.pegb.backoffice.api.customer.dto._
import tech.pegb.backoffice.api.error.jackson.Implicits._
import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.api.model.{GenericRequestWithUpdatedAt, PaginatedResult, SuccessfulStatuses}
import tech.pegb.backoffice.api.{ApiController, ConfigurationHeaders, RequiredHeaders}
import tech.pegb.backoffice.domain.account.abstraction.ExternalAccountManagement
import tech.pegb.backoffice.domain.customer.abstraction.{AddressManagement, BusinessUserManagement, ContactManagement}
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.transaction.abstraction.TransactionConfigManagement
import tech.pegb.backoffice.domain.util.abstraction.LatestVersionService
import tech.pegb.backoffice.mapping.api.domain.account.Implicits._
import tech.pegb.backoffice.mapping.api.domain.customer.Implicits._
import tech.pegb.backoffice.mapping.api.domain.transaction.Implicits._
import tech.pegb.backoffice.mapping.domain.api.Implicits._
import tech.pegb.backoffice.mapping.domain.api.customer.Implicits._
import tech.pegb.backoffice.mapping.domain.api.transaction.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class BusinessUserController @Inject() (
    executionContexts: WithExecutionContexts,
    controllerComponents: ControllerComponents,
    latestVersionService: LatestVersionService,
    businessUserManagement: BusinessUserManagement,
    contactManagement: ContactManagement,
    addressManagement: AddressManagement,
    externalAccountMgmt: ExternalAccountManagement,
    txnConfigMgmt: TransactionConfigManagement,
    implicit val appConfig: AppConfig) extends ApiController(controllerComponents)
  with RequiredHeaders
  with ConfigurationHeaders
  with controllers.CustomerExternalAccountsController
  with controllers.CustomerTxnConfigController
  with controllers.BusinessUserController {

  import ApiController._
  import BusinessUserController._
  import RequiredHeaders._

  implicit val executionContext: ExecutionContext = executionContexts.genericOperations
  implicit val futureTimeout: FiniteDuration = appConfig.FutureTimeout

  def getVelocityPortalUsers(
    userId: UUID,
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    (for {
      ordering ← EitherT.fromEither[Future](orderBy.map(_.replace("full_name", "name")).validateOrdering(vpUserValidSorter)
        .leftMap(_.log().asInvalidRequestApiError()))

      count ← EitherT(executeIfGET(businessUserManagement.countVelocityUsers(userId)
        .futureWithTimeout
        .map(_.leftMap(_.asApiError())), NoCount.toFuture))

      vpUsers ← EitherT(executeIfGET(
        businessUserManagement.getVelocityUsers(userId, ordering, limit, offset)
          .futureWithTimeout
          .map(_.leftMap(_.asApiError())), NoResult.toFuture))

    } yield {
      PaginatedResult(count, vpUsers.map(_.asApi), None, None).toJsonStrWithoutEscape
    }).value.map(handleApiResponse(_))
  }

  def getVelocityPortalUserById(userId: UUID, vpUserId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    businessUserManagement.getVelocityUsersById(userId, vpUserId).map(serviceResponse ⇒
      handleApiResponse(serviceResponse.map(_.asApi.toJsonStr).leftMap(_.asApiError())))
  }

  def getContactsById(userId: UUID, contactId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    contactManagement.getContactInfoById(userId, contactId).map(serviceResponse ⇒
      handleApiResponse(serviceResponse.map(_.asApi.toJsonStr).leftMap(_.asApiError())))
  }

  def getAddressById(userId: UUID, addressId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId

    addressManagement.getAddressById(userId, addressId).map(serviceResponse ⇒
      handleApiResponse(serviceResponse.map(_.asApi.toJsonStr).leftMap(_.asApiError())))
  }

  def getContacts(
    userId: UUID,
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    (for {
      ordering ← EitherT.fromEither[Future](orderBy.validateOrdering(contactValidSorter)
        .leftMap(_.log().asInvalidRequestApiError()))

      contacts ← EitherT(executeIfGET(
        contactManagement.getContactInfo(userId, ordering, limit, offset)
          .futureWithTimeout
          .map(_.leftMap(_.asApiError())), NoResult.toFuture))

    } yield {
      PaginatedResult(contacts.size, contacts.map(_.asApi), None, None).toJsonStrWithoutEscape
    }).value.map(handleApiResponse(_))
  }

  def getAddress(
    userId: UUID,
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    (for {
      ordering ← EitherT.fromEither[Future](orderBy.validateOrdering(addressValidSorter)
        .leftMap(_.log().asInvalidRequestApiError()))

      address ← EitherT(executeIfGET(
        addressManagement.getAddresses(userId, ordering, limit, offset)
          .futureWithTimeout
          .map(_.leftMap(_.asApiError())), NoResult.toFuture))

    } yield {
      PaginatedResult(address.size, address.map(_.asApi), None, None).toJsonStrWithoutEscape
    }).value.map(handleApiResponse(_))
  }

  def resetVelocityPortalPin(
    userId: UUID,
    vpUserId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      resetPinRequest ← EitherT.fromEither[Future](ctx.body.as(classOf[VelocityPortalResetPinRequest], isDeserializationStrict)
        .toEither.leftMap(_.log().asMalformedRequestApiError()))

      _ ← EitherT(businessUserManagement
        .resetVelocityUserPin(
          userId = userId,
          vpUserId = vpUserId,
          reason = resetPinRequest.reason.sanitize,
          updatedBy = doneBy,
          updatedAt = doneAt.toLocalDateTimeUTC,
          lastUpdatedAt = resetPinRequest.lastUpdatedAt.map(_.toLocalDateTimeUTC)))
        .leftMap(_.asApiError())

    } yield ()).value.map(handleApiNoContentResponse(_))
  }

  def createContact(userId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      contactToCreateApi ← EitherT.fromEither[Future](ctx.body.as(classOf[ContactToCreate], isDeserializationStrict)
        .toEither.leftMap(_.log().asMalformedRequestApiError()))

      contactToCreate ← EitherT.fromEither[Future](contactToCreateApi.asDomain(requestId, userId, doneAt, doneBy)
        .toEither.leftMap(_.log().asInvalidRequestApiError("could not parse request to create Contact".some)))

      domainResult ← EitherT(contactManagement.insertContactInfo(contactToCreate))
        .map(_.asApi.toJsonStr)
        .leftMap(_.asApiError())
    } yield domainResult).value.map(handleApiResponse(_, SuccessfulStatuses.Created))
  }

  def updateContact(userId: UUID, contactId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      contactToUpdateApiDto ← EitherT.fromEither[Future](ctx.body.toString.as(
        classOf[ContactToUpdate],
        isDeserializationStrict).toEither.leftMap(_.asMalformedRequestApiError()))

      contactToUpdate ← EitherT.fromEither[Future](contactToUpdateApiDto.asDomain(doneAt, doneBy)
        .toEither.leftMap(_.log().asInvalidRequestApiError("could not parse request to update Contact".some)))

      domainResult ← EitherT(contactManagement.updateContactInfo(
        userId = userId,
        contactId = contactId,
        contactToUpdate = contactToUpdate))
        .map(_.asApi.toJsonStr)
        .leftMap(_.asApiError())
    } yield domainResult).value.map(handleApiResponse(_))

  }

  def createAddress(userId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      contactAddressToCreateApi ← EitherT.fromEither[Future](ctx.body.as(classOf[ContactAddressToCreate], isDeserializationStrict)
        .toEither.leftMap(_.log().asMalformedRequestApiError()))

      contactAddressToCreate ← EitherT.fromEither[Future](contactAddressToCreateApi.asDomain(requestId, userId, doneAt, doneBy)
        .toEither.leftMap(_.log().asInvalidRequestApiError("could not parse create request to domain".some)))

      domainResult ← EitherT(addressManagement.insertAddress(contactAddressToCreate))
        .map(_.asApi.toJsonStr)
        .leftMap(_.asApiError())
    } yield domainResult).value.map(handleApiResponse(_, SuccessfulStatuses.Created))
  }

  def updateAddress(userId: UUID, addressId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      addressToUpdateApiDto ← EitherT.fromEither[Future](ctx.body.toString.as(
        classOf[ContactAddressToUpdate],
        isDeserializationStrict).toEither.leftMap(_.asMalformedRequestApiError()))

      addressToUpdate ← EitherT.fromEither[Future](addressToUpdateApiDto.asDomain(doneAt, doneBy)
        .toEither.leftMap(_.log().asInvalidRequestApiError("could not parse create request to domain".some)))

      domainResult ← EitherT(addressManagement.updateAddress(
        userId = userId,
        addressId = addressId,
        addressToUpdate = addressToUpdate))
        .map(_.asApi.toJsonStr)
        .leftMap(_.asApiError())
    } yield domainResult).value.map(handleApiResponse(_))

  }

  def getCustomerExternalAccount(customerId: UUID, externalAccountId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val criteria = (Option(externalAccountId.toUUIDLike), Option(customerId.toUUIDLike)).asDomain
    externalAccountMgmt.getExternalAccountByCriteria(criteria, Nil, None, None)
      .map(_.fold(_.asApiError().toLeft, _.headOption match {
        case Some(externalAccount) ⇒
          externalAccount.asApi.toJsonStr.toRight
        case None ⇒ s"External account with id [$externalAccountId] was not found under customer with id [$customerId]".asNotFoundApiError.toLeft
      }))
      .map(handleApiResponse(_))
  }

  def getCustomerExternalAccountsByCriteria(
    customerId: UUID,
    externalAccountId: Option[UUIDLike],
    currency: Option[String],
    providerName: Option[String],
    accountNumber: Option[String],
    accountHolder: Option[String],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    (for {
      limit ← EitherT.fromEither[Future](Try(limit.flatMap(lim ⇒ Math.min(lim, appConfig.PaginationMaxLimit).some)
        .orElse(appConfig.PaginationLimit.some)).toEither
        .leftMap(_ ⇒ "Unexpected error during resolution of limit".asUnknownApiError))

      ordering ← EitherT.fromEither[Future](orderBy.validateOrdering(externalAccountValidSorter)
        .leftMap(_.log().asInvalidRequestApiError()))

      partialMatchFields ← EitherT.fromEither[Future](partialMatch.validatePartialMatch(externalAccountValidPartialMatch)
        .leftMap(_.log().asInvalidRequestApiError()))

      criteria ← EitherT.fromEither[Future]((externalAccountId, Some(customerId.toUUIDLike), providerName,
        accountHolder, accountNumber, currency, partialMatchFields).asDomain.toRight)

      total ← EitherT(executeIfGET(externalAccountMgmt.count(criteria), NoCount.toFuture)).leftMap(_.asApiError())

      latestVersion ← EitherT(externalAccountMgmt.getLatestVersion(criteria)).leftMap(_.asApiError())

      results ← EitherT(executeIfGET(
        externalAccountMgmt.getExternalAccountByCriteria(criteria, ordering, limit, offset),
        NoResult.toFuture)).leftMap(_.asApiError())

    } yield {
      (PaginatedResult(total, results.map(_.asApi), limit, offset).toJsonStr, latestVersion.flatMap(_.updatedAt.map(_.toString)))
    }).value.map(_.toTuple2FirstOneEither).map {
      case (result, latestVersion) ⇒ handleApiResponse(result).withLatestVersionHeader(latestVersion)
    }
  }

  def createCustomerExternalAccount(customerId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      apiDto ← EitherT.fromEither[Future](ctx.body.as(classOf[CustomerExternalAccountToCreate], isDeserializationStrict)
        .toEither.leftMap(err ⇒ err.log().asMalformedRequestApiError(err.asFriendlyErrorMsg)))

      domainDto ← EitherT.fromEither[Future](apiDto.asDomain(requestId, customerId, doneBy, doneAt).toRight)

      result ← EitherT(externalAccountMgmt.createExternalAccount(domainDto))
        .map(_.asApi.toJsonStr)
        .leftMap(_.asApiError())
    } yield result).value.map(handleApiResponse(_, SuccessfulStatuses.Created))
  }

  def updateCustomerExternalAccount(customerId: UUID, externalAccountId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    val doneAt = getRequestDate
    val doneBy = getRequestFrom
    val criteria = (Option(externalAccountId.toUUIDLike), Option(customerId.toUUIDLike)).asDomain
    (for {
      apiDto ← EitherT.fromEither[Future](ctx.body.as(classOf[ExternalAccountToUpdate], isStrict = false)
        .toEither.leftMap(err ⇒ err.log().asMalformedRequestApiError(err.asFriendlyErrorMsg)))

      domainDto ← EitherT.fromEither[Future](apiDto.asDomain(doneBy, doneAt).toRight)

      result ← EitherT(externalAccountMgmt.updateExternalAccount(criteria, domainDto))
        .map(_.asApi.toJsonStr)
        .leftMap(_.asApiError())
    } yield result).value.map(handleApiResponse(_))
  }

  def deleteCustomerExternalAccount(customerId: UUID, externalAccountId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    val doneAt = getRequestDate
    val doneBy = getRequestFrom
    val criteria = (Option(externalAccountId.toUUIDLike), Option(customerId.toUUIDLike)).asDomain
    (for {
      apiDto ← EitherT.fromEither[Future](ctx.body.as(classOf[GenericRequestWithUpdatedAt], isStrict = false)
        .toEither.leftMap(err ⇒ err.log().asMalformedRequestApiError(err.asFriendlyErrorMsg)))

      result ← EitherT(externalAccountMgmt.deleteExternalAccount(criteria, apiDto.lastUpdatedAt.map(_.toLocalDateTimeUTC)))
        .map(_ ⇒ s"""{"id":"$externalAccountId","status":"deleted"}""")
        .leftMap(_.asApiError())
    } yield result).value.map(handleApiResponse(_))
  }

  def getCustomerTxnConfig(customerId: UUID, txnConfId: UUID): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒
    implicit val requestId: UUID = getRequestId
    val criteria = (txnConfId, customerId).asDomain
    txnConfigMgmt.getTxnConfigByCriteria(criteria, Nil, None, None)
      .map(_.fold(_.asApiError().toLeft, _.headOption match {
        case Some(txnConfig) ⇒
          txnConfig.asApi.toJsonStr.toRight
        case None ⇒ s"TxnConfig with id [$txnConfId] was not found under customer with id [$customerId]".asNotFoundApiError.toLeft
      }))
      .map(handleApiResponse(_))
  }

  def getCustomerTxnConfigByCriteria(
    customerId: UUID,
    txnConfId: Option[UUIDLike],
    currency: Option[String],
    transactionType: Option[String],
    partialMatch: Option[String],
    orderBy: Option[String],
    limit: Option[Int],
    offset: Option[Int]): Action[AnyContent] = LoggedAsyncAction { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    (for {
      limit ← EitherT.fromEither[Future](Try(limit.flatMap(lim ⇒ Math.min(lim, appConfig.PaginationMaxLimit).some)
        .orElse(appConfig.PaginationLimit.some)).toEither
        .leftMap(_ ⇒ "Unexpected error during resolution of limit".asUnknownApiError))

      ordering ← EitherT.fromEither[Future](orderBy.validateOrdering(txnConfigValidSorter)
        .leftMap(_.log().asInvalidRequestApiError()))

      partialMatchFields ← EitherT.fromEither[Future](partialMatch.validatePartialMatch(txnConfigValidPartialMatch)
        .leftMap(_.log().asInvalidRequestApiError()))

      criteria ← EitherT.fromEither[Future]((txnConfId, Some(customerId.toUUIDLike), currency, None,
        transactionType, partialMatchFields).asDomain.toRight)

      total ← EitherT(executeIfGET(txnConfigMgmt.count(criteria), NoCount.toFuture)).leftMap(_.asApiError())

      latestVersion ← EitherT(txnConfigMgmt.getLatestVersion(criteria)).leftMap(_.asApiError())

      results ← EitherT(executeIfGET(
        txnConfigMgmt.getTxnConfigByCriteria(criteria, ordering, limit, offset),
        NoResult.toFuture)).leftMap(_.asApiError())

    } yield {
      (PaginatedResult(total, results.map(_.asApi), limit, offset).toJsonStr, latestVersion.flatMap(_.updatedAt.map(_.toString)))
    }).value.map(_.toTuple2FirstOneEither).map {
      case (result, latestVersion) ⇒ handleApiResponse(result).withLatestVersionHeader(latestVersion)
    }
  }

  def createCustomerTxnConfig(customerId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    val doneAt = getRequestDate
    val doneBy = getRequestFrom

    (for {
      apiDto ← EitherT.fromEither[Future](ctx.body.as(classOf[CustomerTxnConfigToCreate], isDeserializationStrict)
        .toEither.leftMap(err ⇒ err.log().asMalformedRequestApiError(err.asFriendlyErrorMsg)))

      domainDto ← EitherT.fromEither[Future](apiDto.asDomain(requestId, customerId, doneBy, doneAt).toRight)

      result ← EitherT(txnConfigMgmt.createTxnConfig(domainDto))
        .map(_.asApi.toJsonStr)
        .leftMap(_.asApiError())
    } yield result).value.map(handleApiResponse(_, SuccessfulStatuses.Created))
  }

  def updateCustomerTxnConfig(customerId: UUID, txnConfId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    val doneAt = getRequestDate
    val doneBy = getRequestFrom
    val criteria = (txnConfId, customerId).asDomain
    (for {
      apiDto ← EitherT.fromEither[Future](ctx.body.as(classOf[TxnConfigToUpdate], isStrict = false)
        .toEither.leftMap(err ⇒ err.log().asMalformedRequestApiError(err.asFriendlyErrorMsg)))

      domainDto ← EitherT.fromEither[Future](apiDto.asDomain(doneBy, doneAt).toRight)

      result ← EitherT(txnConfigMgmt.updateTxnConfig(criteria, domainDto))
        .map(_.asApi.toJsonStr)
        .leftMap(_.asApiError())
    } yield result).value.map(handleApiResponse(_))
  }

  def deleteCustomerTxnConfig(customerId: UUID, txnConfId: UUID): Action[String] = LoggedAsyncAction(text) { implicit ctx ⇒

    implicit val requestId: UUID = getRequestId

    val doneAt = getRequestDate
    val doneBy = getRequestFrom
    val criteria = (txnConfId, customerId).asDomain
    (for {
      apiDto ← EitherT.fromEither[Future](ctx.body.as(classOf[GenericRequestWithUpdatedAt], isStrict = false)
        .toEither.leftMap(err ⇒ err.log().asMalformedRequestApiError(err.asFriendlyErrorMsg)))

      result ← EitherT(txnConfigMgmt.deleteTxnConfig(criteria, apiDto.lastUpdatedAt.map(_.toLocalDateTimeUTC)))
        .map(_ ⇒ s"""{"id":"$txnConfId","status":"deleted"}""")
        .leftMap(_.asApiError())
    } yield result).value.map(handleApiResponse(_))
  }
}

object BusinessUserController {
  val vpUserValidSorter = Set(
    "name", "middle", "surname", "msisdn", "email",
    "username", "role", "status", "last_login_at",
    "created_at", "created_by", "updated_at", "updated_by")

  val contactValidSorter = Set(
    "contact_type", "name", "middle", "surname", "msisdn", "email",
    "id_type", "created_at", "created_by", "updated_at", "updated_by")

  val addressValidSorter = Set(
    "address_type", "country_name", "postal_code", "city", "address",
    "coordinate_x", "coordinate_y", "created_at", "created_by", "updated_at", "updated_by")

  val externalAccountValidSorter = Set("external_account_id", "provider", "account_holder", "account_number", "currency", "updated_at")
  val externalAccountValidPartialMatch = Set("disabled", "external_account_id", "account_holder", "account_number")

  val txnConfigValidSorter = Set("txn_config_id", "customer_id", "transaction_type", "currency", "updated_at")
  val txnConfigValidPartialMatch = Set("disabled", "txn_config_id", "customer_id")
}
