package tech.pegb.backoffice.domain.transaction.implementation

import java.time.LocalDateTime
import java.util.Currency

import cats.data._
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import tech.pegb.backoffice.core.integration.CoreApiClient
import tech.pegb.backoffice.dao.transaction.abstraction.TransactionConfigDao
import tech.pegb.backoffice.dao.currency.abstraction.CurrencyDao
import tech.pegb.backoffice.dao.customer.abstraction.UserDao
import tech.pegb.backoffice.dao.types.abstraction.TypesDao
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.domain.{BaseService, model}
import tech.pegb.backoffice.domain.transaction.abstraction.TransactionConfigManagement
import tech.pegb.backoffice.domain.transaction.dto.{TxnConfigCriteria, TxnConfigToCreate, TxnConfigToUpdate}
import tech.pegb.backoffice.domain.transaction.model.TxnConfig
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.transaction.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.types.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.transaction.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.backoffice.util.Implicits._

import scala.concurrent.Future

@Singleton
class TransactionConfigMgmtService @Inject() (
    conf: AppConfig,
    executionContexts: WithExecutionContexts,
    userDao: UserDao,
    currencyDao: CurrencyDao,
    txnConfigDao: TransactionConfigDao,
    typesDao: TypesDao,
    coreApiClient: CoreApiClient) extends TransactionConfigManagement with BaseService {

  implicit val ec = executionContexts.blockingIoOperations

  def createTxnConfig(dto: TxnConfigToCreate): Future[ServiceResponse[TxnConfig]] = Future {

    for {
      validatedDto ← dto.validate
      userId ← userDao.getInternalUserId(validatedDto.customerId.toString).fold(_.asDomainError.toLeft, {
        case Some(id) ⇒ Right(id)
        case None ⇒ Left(notFoundError(s"Failed to create txn config [${dto.toSmartString}]. Customer id [${dto.customerId}] is not found."))
      })
      currencyId ← currencyDao.getAll.fold(
        _.asDomainError.toLeft,
        _.find(_.name === validatedDto.currency).headOption match {
          case Some(currency) ⇒ Right(currency.id)
          case None ⇒ Left(notFoundError(s"Failed to create txn config [${dto.toSmartString}]. Currency [${Currency.getInstance(validatedDto.currency).getDisplayName}] is not configured in the system."))
        })

      txnType ← typesDao.getTransactionTypes.fold(
        _.asDomainError.toLeft,
        _.map(_.asDomain).find(_.name === dto.transactionType) match {
          case Some(txnType) ⇒ Right(txnType)
          case None ⇒ Left(notFoundError(s"Failed to create txn config [${dto.toSmartString}]. Transaction type [${dto.transactionType}] is not configured in the system."))
        })
      _ ← {
        val daoDto = TxnConfigCriteria(
          customerId = dto.customerId.toUUIDLike.toOption,
          transactionType = txnType.name.toOption,
          currency = dto.currency.toOption).asDao()

        txnConfigDao.getTxnConfigByCriteria(daoDto, None, None, None)(None).fold(
          _.asDomainError.toLeft,
          _.headOption match {
            case Some(duplicate) ⇒ Left(duplicateError(s"Existing txn config found with same customer_id, transaction_type and currency."))
            case None ⇒ Right(())
          })
      }
      result ← {
        val daoDto = validatedDto.asDao(userId, currencyId)
        txnConfigDao.insertTxnConfig(daoDto).fold(_.asDomainError.toLeft, _.asDomain.toRight)
      }
    } yield {
      result
    }
  }.recover {
    case err: Exception ⇒
      logger.error(s"[createTxnConfig] Unexpected error with dto [${dto.toSmartString}]", err)
      unknownError(s"Unexpected error while creating txn config [${dto.toSmartString}]").toLeft
  }

  def getTxnConfigByCriteria(
    criteria: TxnConfigCriteria,
    orderBy: Seq[model.Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[TxnConfig]]] = Future {

    val results = txnConfigDao.getTxnConfigByCriteria(criteria.asDao(), orderBy.asDao, limit, offset)
      .bimap(_.asDomainError, _.map(_.asDomain))

    results.map(_.map(result ⇒ result.id → result.validate).filter(_._2.isLeft).foreach({
      case (id, Left(error)) ⇒
        logger.warn(s"[getTxnConfigByCriteria] inconsistent txn config with id [${id}] found in the database: ${error.message}")
      case _ ⇒
    }))

    results
  }

  def getLatestVersion(criteria: TxnConfigCriteria): Future[ServiceResponse[Option[TxnConfig]]] = Future {
    txnConfigDao.getTxnConfigByCriteria(criteria.asDao(), Seq(Ordering("updated_at", Ordering.DESCENDING)).asDao, Some(1), None)
      .bimap(_.asDomainError, _.headOption.map(_.asDomain))
  }.recover {
    case err: Exception ⇒
      logger.error(s"[getLatestVersion] Unexpected error with dto [${criteria.toSmartString}]", err)
      unknownError(s"Unexpected error while fetching the latest txn config by criteria [${criteria.toSmartString}]").toLeft
  }

  def count(criteria: TxnConfigCriteria): Future[ServiceResponse[Int]] = Future {
    txnConfigDao.countTxnConfig(criteria.asDao()).leftMap(_.asDomainError)
  }.recover {
    case err: Exception ⇒
      logger.error(s"[count] Unexpected error with dto [${criteria.toSmartString}]", err)
      unknownError(s"Unexpected error while counting txn config by criteria [${criteria.toSmartString}]").toLeft
  }

  def updateTxnConfig(criteria: TxnConfigCriteria, dto: TxnConfigToUpdate): Future[ServiceResponse[TxnConfig]] = {
    (for {
      validatedDto ← EitherT.fromEither[Future](dto.validate)

      currencyId ← EitherT.fromEither[Future](validatedDto.currency.map { currency ⇒
        currencyDao.getAll.fold(
          _.asDomainError.toLeft,
          _.find(c ⇒ currency.lenientContains(c.name)).headOption match {
            case Some(currency) ⇒ Right(Some(currency.id))
            case None ⇒ Left(notFoundError(s"Failed to update txn config [${dto.toSmartString}]. Currency [${currency}] is not configured in the system."))
          })
      }.getOrElse(Right(None)))

      _ ← {
        val duplicateFinder = criteria.copy(
          transactionType = validatedDto.transactionType,
          currency = validatedDto.currency).asDao()
        val result = txnConfigDao.getTxnConfigByCriteria(duplicateFinder, None, None, None)(None).fold(
          _.asDomainError.toLeft,
          _.headOption match {
            case Some(duplicate) ⇒ Left(duplicateError(s"Duplicate txn config found if update [${dto.toSmartString}] is applied using criteria [${criteria.toSmartString}]."))
            case None ⇒ Right(())
          })
        EitherT.fromEither[Future](result)
      }

      flattenedCriteria ← {
        val result = txnConfigDao.getTxnConfigByCriteria(criteria.asDao(), None, None, None)(None).fold(
          _.asDomainError.toLeft,
          results ⇒ results.nonEmpty match {
            case true ⇒ Right(TxnConfigCriteria(anyIds = Some(results.map(_.uuid).toSet)))
            case false ⇒ Left(notFoundError(s"Txn config using this criteria [${criteria.toSmartString}] was not found."))
          })
        EitherT.fromEither[Future](result)
      }

      dtoWithLatestLastUpdatedAt ← {
        EitherT(getLatestVersion(criteria)).map(_.map(latestExternalAccount ⇒
          validatedDto.copy(lastUpdatedAt = latestExternalAccount.updatedAt)))
      }

      result ← {
        val result = txnConfigDao.updateTxnConfig(flattenedCriteria.asDao(), dtoWithLatestLastUpdatedAt.getOrElse(validatedDto).asDao(currencyId))
          .fold(
            _.asDomainError.toLeft,
            _.headOption match {
              case Some(updated) ⇒
                Right(updated.asDomain)
              case None ⇒
                Left(notFoundError(s"Txn config using this criteria [${criteria.toSmartString}] was not found."))
            })
        EitherT.fromEither[Future](result)
      }
    } yield {
      result
    }).value
  }.recover {
    case err: Exception ⇒
      logger.error(s"[updateTxnConfig] Unexpected error with dto [${dto.toSmartString}]", err)
      unknownError(s"Unexpected error while updating txn config by criteria [${dto.toSmartString}]").toLeft
  }

  def deleteTxnConfig(criteria: TxnConfigCriteria, lastUpdatedAt: Option[LocalDateTime]): Future[ServiceResponse[Unit]] = {
    (for {

      defaultLastUpdatedAt ← EitherT(getLatestVersion(criteria).map(_.map(_.flatMap(_.updatedAt))))

      result ← EitherT.fromEither[Future]({
        txnConfigDao.deleteTxnConfig(criteria.asDao(), lastUpdatedAt.orElse(defaultLastUpdatedAt))
          .bimap(_.asDomainError, {
            case Some(_) ⇒ ()
            case None ⇒
              logger.warn(s"Nothing to delete. Txn config with this criteria [${criteria.toSmartString}] was not found.")
              ()
          })

      })
    } yield {
      result
    }).value
  }.recover {
    case err: Exception ⇒
      logger.error(s"[deleteTxnConfig] Unexpected error with dto [${criteria.toSmartString}]", err)
      unknownError(s"Unexpected error while deleting txn config by criteria [${criteria.toSmartString}]").toLeft
  }
}
