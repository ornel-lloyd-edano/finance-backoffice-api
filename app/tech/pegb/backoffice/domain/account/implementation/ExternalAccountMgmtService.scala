package tech.pegb.backoffice.domain.account.implementation

import java.time.LocalDateTime
import java.util.Currency

import cats.data._
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import tech.pegb.backoffice.core.integration.CoreApiClient
import tech.pegb.backoffice.dao.account.abstraction.ExternalAccountDao
import tech.pegb.backoffice.dao.currency.abstraction.CurrencyDao
import tech.pegb.backoffice.dao.customer.abstraction.UserDao
import tech.pegb.backoffice.domain.BaseService
import tech.pegb.backoffice.domain.account.abstraction.ExternalAccountManagement
import tech.pegb.backoffice.domain.account.dto.{ExternalAccountCriteria, ExternalAccountToCreate, ExternalAccountToUpdate}
import tech.pegb.backoffice.domain.account.model.ExternalAccount
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.account.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.account.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.backoffice.util.Implicits._

import scala.concurrent.Future

@Singleton
class ExternalAccountMgmtService @Inject() (
    conf: AppConfig,
    executionContexts: WithExecutionContexts,
    userDao: UserDao,
    currencyDao: CurrencyDao,
    externalAccountDao: ExternalAccountDao,
    coreApiClient: CoreApiClient) extends ExternalAccountManagement with BaseService {

  implicit val ec = executionContexts.blockingIoOperations

  def createExternalAccount(dto: ExternalAccountToCreate): Future[ServiceResponse[ExternalAccount]] = Future {
    for {
      validatedDto ← dto.validate
      userId ← userDao.getInternalUserId(validatedDto.customerId.toString).fold(_.asDomainError.toLeft, {
        case Some(id) ⇒ Right(id)
        case None ⇒ Left(notFoundError(s"Failed to create external account [${dto.toSmartString}]. Customer id [${dto.customerId}] is not found."))
      })
      currencyId ← currencyDao.getAll.fold(
        _.asDomainError.toLeft,
        _.find(_.name === validatedDto.currency).headOption match {
          case Some(currency) ⇒ Right(currency.id)
          case None ⇒ Left(notFoundError(s"Failed to create external account [${dto.toSmartString}]. Currency [${Currency.getInstance(validatedDto.currency).getDisplayName}] is not configured in the system."))
        })
      _ ← {
        val daoDto = ExternalAccountCriteria(
          customerId = dto.customerId.toUUIDLike.toOption,
          externalProvider = dto.externalProvider.toOption,
          externalAccountNumber = dto.externalAccountNumber.toOption).asDao()

        externalAccountDao.getExternalAccountByCriteria(daoDto, None, None, None)(None).fold(
          _.asDomainError.toLeft,
          _.headOption match {
            case Some(duplicate) ⇒ Left(duplicateError(s"Existing external account found with same customer_id, provider and account_number."))
            case None ⇒ Right(())
          })
      }
      result ← {
        val daoDto = validatedDto.asDao(userId, currencyId)
        externalAccountDao.insertExternalAccount(daoDto).fold(_.asDomainError.toLeft, _.asDomain.toRight)
      }
    } yield {
      result
    }
  }.recover {
    case err: Exception ⇒
      logger.error(s"[createExternalAccount] Unexpected error with dto [${dto.toSmartString}]", err)
      unknownError(s"Unexpected error while creating external account [${dto.toSmartString}]").toLeft
  }

  def getExternalAccountByCriteria(criteria: ExternalAccountCriteria, orderBy: Seq[Ordering],
    limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[ExternalAccount]]] = Future {

    val results = externalAccountDao.getExternalAccountByCriteria(criteria.asDao(), orderBy.asDao, limit, offset)
      .bimap(_.asDomainError, _.map(_.asDomain))

    results.map(_.map(result ⇒ result.id → result.validate).filter(_._2.isLeft).foreach({
      case (id, Left(error)) ⇒
        logger.warn(s"[getExternalAccountByCriteria] inconsistent external account with id [${id}] found in the database: ${error.message}")
      case _ ⇒
    }))

    results
  }

  def getLatestVersion(criteria: ExternalAccountCriteria): Future[ServiceResponse[Option[ExternalAccount]]] = Future {
    externalAccountDao.getExternalAccountByCriteria(criteria.asDao(), Seq(Ordering("updated_at", Ordering.DESCENDING)).asDao, Some(1), None)
      .bimap(_.asDomainError, _.headOption.map(_.asDomain))
  }.recover {
    case err: Exception ⇒
      logger.error(s"[getLatestVersion] Unexpected error with dto [${criteria.toSmartString}]", err)
      unknownError(s"Unexpected error while fetching the latest external account by criteria [${criteria.toSmartString}]").toLeft
  }

  def count(criteria: ExternalAccountCriteria): Future[ServiceResponse[Int]] = Future {
    externalAccountDao.countExternalAccount(criteria.asDao()).leftMap(_.asDomainError)
  }.recover {
    case err: Exception ⇒
      logger.error(s"[count] Unexpected error with dto [${criteria.toSmartString}]", err)
      unknownError(s"Unexpected error while counting external account by criteria [${criteria.toSmartString}]").toLeft
  }

  def updateExternalAccount(criteria: ExternalAccountCriteria, dto: ExternalAccountToUpdate): Future[ServiceResponse[ExternalAccount]] = {
    (for {
      validatedDto ← EitherT.fromEither[Future](dto.validate)

      currencyId ← EitherT.fromEither[Future](validatedDto.currency.map { currency ⇒
        currencyDao.getAll.fold(
          _.asDomainError.toLeft,
          _.find(c ⇒ currency.lenientContains(c.name)).headOption match {
            case Some(currency) ⇒ Right(Some(currency.id))
            case None ⇒ Left(notFoundError(s"Failed to update external account [${dto.toSmartString}]. Currency [${currency}] is not configured in the system."))
          })
      }.getOrElse(Right(None)))

      _ ← {
        val duplicateFinder = criteria.copy(
          id = None,
          externalProvider = validatedDto.externalProvider,
          externalAccountNumber = validatedDto.externalAccountNumber).asDao()
        val result = externalAccountDao.getExternalAccountByCriteria(duplicateFinder, None, None, None)(None).fold(
          _.asDomainError.toLeft,
          _.headOption match {
            case Some(duplicate) ⇒ Left(duplicateError(s"Duplicate external account found if update [${dto.toSmartString}] is applied using criteria [${criteria.toSmartString}]."))
            case None ⇒ Right(())
          })
        EitherT.fromEither[Future](result)
      }

      flattenedCriteria ← {
        val result = externalAccountDao.getExternalAccountByCriteria(criteria.asDao(), None, None, None)(None).fold(
          _.asDomainError.toLeft,
          results ⇒ results.nonEmpty match {
            case true ⇒ Right(ExternalAccountCriteria(anyIds = Some(results.map(_.uuid).toSet)))
            case false ⇒ Left(notFoundError(s"External account using this criteria [${criteria.toSmartString}] was not found."))
          })
        EitherT.fromEither[Future](result)
      }

      dtoWithLatestLastUpdatedAt ← {
        EitherT(getLatestVersion(criteria)).map(_.map(latestExternalAccount ⇒
          validatedDto.copy(lastUpdatedAt = latestExternalAccount.updatedAt)))
      }

      result ← {
        val result = externalAccountDao.updateExternalAccount(flattenedCriteria.asDao(), dtoWithLatestLastUpdatedAt.getOrElse(validatedDto).asDao(currencyId))
          .fold(
            _.asDomainError.toLeft,
            _.headOption match {
              case Some(updated) ⇒
                Right(updated.asDomain)
              case None ⇒
                Left(notFoundError(s"External account using this criteria [${criteria.toSmartString}] was not found."))
            })
        EitherT.fromEither[Future](result)
      }
    } yield {
      result
    }).value
  }.recover {
    case err: Exception ⇒
      logger.error(s"[updateExternalAccount] Unexpected error with dto [${dto.toSmartString}]", err)
      unknownError(s"Unexpected error while updating external account by criteria [${dto.toSmartString}]").toLeft
  }

  def deleteExternalAccount(criteria: ExternalAccountCriteria, lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]] = {
    (for {

      defaultLastUpdatedAt ← EitherT(getLatestVersion(criteria).map(_.map(_.flatMap(_.updatedAt))))

      result ← EitherT.fromEither[Future]({
        externalAccountDao.deleteExternalAccount(criteria.asDao(), lastUpdatedAt.orElse(defaultLastUpdatedAt))
          .bimap(_.asDomainError, {
            case Some(_) ⇒ ()
            case None ⇒
              logger.warn(s"Nothing to delete. External account with this criteria [${criteria.toSmartString}] was not found.")
              ()
          })

      })
    } yield {
      result
    }).value
  }.recover {
    case err: Exception ⇒
      logger.error(s"[deleteExternalAccount] Unexpected error with dto [${criteria.toSmartString}]", err)
      unknownError(s"Unexpected error while deleting external account by criteria [${criteria.toSmartString}]").toLeft
  }

}
