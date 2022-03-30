package tech.pegb.backoffice.domain.application.implementation

import java.time.LocalDateTime
import java.util.UUID

import cats.data._
import cats.implicits._
import com.google.inject.Inject
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json._
import tech.pegb.backoffice.application.datatransfer.TransientToPersistentDocMoverT
import tech.pegb.backoffice.dao.application.abstraction.WalletApplicationDao
import tech.pegb.backoffice.dao.customer.abstraction.UserDao
import tech.pegb.backoffice.dao.document.abstraction.{DocumentImmutableFileDao, DocumentTransientFileDao}
import tech.pegb.backoffice.domain.HttpClient
import tech.pegb.backoffice.domain.BaseService
import tech.pegb.backoffice.domain.application.abstraction.WalletApplicationManagement
import tech.pegb.backoffice.domain.application.dto.WalletApplicationCriteria
import tech.pegb.backoffice.domain.application.model.WalletApplication
import tech.pegb.backoffice.domain.document.abstraction.DocumentManagement
import tech.pegb.backoffice.domain.document.dto.DocumentCriteria
import tech.pegb.backoffice.domain.document.model.DocumentStatuses
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.mapping.dao.domain.application.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.application.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.backoffice.util.Implicits._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.Try

//TODO add filter to handle deactivated users with passive status
class WalletApplicationMgmtService @Inject() (
    conf: AppConfig,
    executionContexts: WithExecutionContexts,
    documentManagement: DocumentManagement,
    httpClientService: HttpClient,
    documentTransientFileDao: DocumentTransientFileDao,
    documentImmutableFileDao: DocumentImmutableFileDao,
    walletApplicationDao: WalletApplicationDao,
    cbToHdfsDocMover: TransientToPersistentDocMoverT,
    userDao: UserDao) extends WalletApplicationManagement with BaseService {

  implicit val executionContext: ExecutionContext = executionContexts.blockingIoOperations
  private val inactiveStatuses: Set[String] = conf.InactiveStatuses

  def getWalletApplicationById(id: UUID): Future[ServiceResponse[WalletApplication]] =
    Future {
      walletApplicationDao.getWalletApplicationByUUID(id)
        .fold(
          _.asDomainError.toLeft,
          optionalDaoResult ⇒ {
            optionalDaoResult
              .fold[ServiceResponse[WalletApplication]](Left(notFoundError(s"wallet application with id $id not found"))) { walletApplication ⇒

                walletApplication.asDomain
                  .toEither.leftMap { throwable ⇒
                    logger.error(s"Error in getWalletApplicationById", throwable)
                    dtoMappingError(
                      s"""Failed to convert wallet application entity to domain.
                     | Cause by: ${throwable.getMessage.replaceAll("assertion failed:", "")}""".stripMargin)
                  }
              }
          })
    }

  def getWalletApplicationByUserUuid(userUuid: UUID): Future[ServiceResponse[Set[WalletApplication]]] = {
    Future {
      walletApplicationDao.getWalletApplicationByUserUuid(userUuid)
        .fold(
          _.asDomainError.toLeft,
          walletApplications ⇒ {

            walletApplications.map(walletApplication ⇒ walletApplication.asDomain)
              .toList.sequence[Try, WalletApplication].toEither
              .map(_.toSet)
              .leftMap { throwable ⇒
                logger.error(s"Error in getWalletApplicationsByCriteria", throwable)
                dtoMappingError(
                  s"""Failed to convert wallet application entity to domain.
                     | Cause by: ${throwable.getMessage.replaceAll("assertion failed: ", "")}""".stripMargin)
              }
          })

    }
  }

  def getWalletApplicationsByCriteria(
    criteria: WalletApplicationCriteria,
    ordering: Seq[Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[WalletApplication]]] =
    Future {
      walletApplicationDao.getWalletApplicationsByCriteria(criteria.asDao(inactiveStatuses), ordering.asDao, limit, offset)
        .fold(
          _.asDomainError.toLeft,
          walletApplications ⇒
            walletApplications.map(walletApplication ⇒ walletApplication.asDomain)
              .toList.sequence[Try, WalletApplication].toEither
              .leftMap { throwable ⇒
                logger.error(s"Error in getWalletApplicationsByCriteria", throwable)
                dtoMappingError(
                  s"""Failed to convert wallet application entity to domain.
                     | Cause by: ${throwable.getMessage.replaceAll("assertion failed: ", "")}""".stripMargin)
              })
    }

  def countWalletApplicationsByCriteria(criteria: WalletApplicationCriteria): Future[ServiceResponse[Int]] =
    Future {
      walletApplicationDao.countWalletApplicationsByCriteria(criteria.asDao(inactiveStatuses)).leftMap(_.asDomainError)
    }

  private def createCoreNotification(status: String, updatedBy: String, rejectedReason: Option[String], lastUpdatedAt: Option[LocalDateTime]): JsObject = {
    val lastUpdatedAtJs = ("last_updated_at", lastUpdatedAt.fold[JsValueWrapper](JsNull)(t ⇒ JsString(t.toString)))
    rejectedReason match {
      case Some(rr) ⇒ Json.obj("status" → status, "updated_by" → updatedBy, "rejection_reason" → rr, lastUpdatedAtJs)
      case None ⇒ Json.obj("status" → status, "updated_by" → updatedBy, lastUpdatedAtJs)
    }
  }

  def approvePendingWalletApplication(id: UUID, approvedBy: String, approvedAt: LocalDateTime, lastUpdated: Option[LocalDateTime]): Future[ServiceResponse[WalletApplication]] = {

    (for {
      walletOption ← EitherT(Future(walletApplicationDao.getWalletApplicationByUUID(id).leftMap(_.asDomainError)))
      approvedWalletApplication ← EitherT({
        val updateResult: Future[ServiceResponse[WalletApplication]] = walletOption match {
          case Some(wallet) ⇒
            httpClientService.request(
              s"${conf.CoreWalletApplicationActivationUrlVerb}",
              s"${conf.CoreWalletApplicationActivationUrl}/${wallet.id}",
              createCoreNotification(conf.ApprovedWalletApplicationStatus, approvedBy, None, lastUpdated).some).map({ res ⇒
                if (res.success) {
                  wallet.asDomain
                    .toEither.leftMap { throwable ⇒
                      logger.error(s"Error in approvePendingWalletApplication", throwable)
                      dtoMappingError(
                        s"""Failed to convert wallet application entity to domain.
                       | Cause by: ${throwable.getMessage.replaceAll("assertion failed:", "")}""".stripMargin)
                    }
                } else
                  Left(unknownError(s"Error encountered when calling core approve endpoint.${res.body.map(r ⇒ s"Reason: $r")}"))
              })

          case None ⇒ Future.successful(Left(notFoundError(s"Approve Wallet Application failed. Wallet application ${id} not found.")))
        }
        updateResult
      })
      //get updated wallet application
      updatedWalletOption ← EitherT.fromEither[Future](walletApplicationDao.getWalletApplicationByUUID(id).leftMap(_.asDomainError))
      updatedWallet ← EitherT.fromOption[Future](updatedWalletOption, notFoundError(s"Approved Wallet application ${id} not found."))
      result ← EitherT.fromEither[Future]({
        updatedWallet.asDomain.toEither.leftMap { throwable ⇒
          logger.error(s"Error in approvePendingWalletApplication", throwable)
          dtoMappingError(
            s"""Failed to convert wallet application entity to domain.
             | Cause by: ${throwable.getMessage.replaceAll("assertion failed:", "")}""".stripMargin)
        }
      })
    } yield {
      result
    }).value
  }

  def rejectPendingWalletApplication(id: UUID, rejectedBy: String, rejectedAt: LocalDateTime, reason: String, lastUpdated: Option[LocalDateTime]): Future[ServiceResponse[WalletApplication]] = {

    (for {
      walletOption ← EitherT(Future(walletApplicationDao.getWalletApplicationByUUID(id).leftMap(_.asDomainError)))
      rejectedWalletApplication ← EitherT({
        val updateResult: Future[ServiceResponse[WalletApplication]] = walletOption match {
          case Some(wallet) ⇒ httpClientService.request(
            conf.CoreWalletApplicationActivationUrlVerb,
            s"${conf.CoreWalletApplicationActivationUrl}/${wallet.id}",
            createCoreNotification(conf.RejectedWalletApplicationStatus, rejectedBy, Some(reason), lastUpdated).some).map({ res ⇒
              if (res.success) {
                val updateResult = wallet

                updateResult.asDomain
                  .toEither.leftMap { throwable ⇒
                    logger.error(s"Error in rejectPendingWalletApplication", throwable)
                    dtoMappingError(
                      s"""Failed to convert wallet application entity to domain.
                     | Cause by: ${throwable.getMessage.replaceAll("assertion failed:", "")}""".stripMargin)

                  }
              } else {
                Left(unknownError(s"Unable to notify core on approve wallet application.${res.body.map(r ⇒ s"Reason: $r")}"))
              }
            })
          case None ⇒ Future.successful(Left(notFoundError(s"Reject Wallet Application failed. Wallet application ${id} not found.")))
        }
        updateResult
      })
      //get updated wallet application
      updatedWalletOption ← EitherT.fromEither[Future](walletApplicationDao.getWalletApplicationByUUID(id).leftMap(_.asDomainError))
      updatedWallet ← EitherT.fromOption[Future](updatedWalletOption, notFoundError(s"Rejected wallet application ${id} not found."))
      result ← EitherT.fromEither[Future]({
        updatedWallet.asDomain.toEither.leftMap { throwable ⇒
          logger.error(s"Error in rejectPendingWalletApplication", throwable)
          dtoMappingError(
            s"""Failed to convert wallet application entity to domain.
               | Cause by: ${throwable.getMessage.replaceAll("assertion failed:", "")}""".stripMargin)
        }
      })
    } yield {
      result
    }).value
  }

  def persistApprovedFilesByInternalApplicationId(id: Int): Future[ServiceResponse[Seq[UUID]]] = {

    walletApplicationDao.getWalletApplicationByInternalId(id) match {
      case Left(daoError) ⇒ daoError.asDomainError.toLeft.toFuture

      case Right(None) ⇒ Future.successful(Left(notFoundError(s"Persist approved files failed. Wallet Application with id ${id} not found")))

      case Right(Some(walletApplication)) if (walletApplication.status.toLowerCase() != conf.ApprovedWalletApplicationStatus) ⇒
        Future.successful(Left(validationError(s"Persist approved files failed. Wallet with id ${id} is not in APPROVED state")))

      case Right(Some(walletApplication)) ⇒
        (for {
          documents ← EitherT(documentManagement.getDocumentsByCriteria(DocumentCriteria(walletApplicationId = Option(walletApplication.uuid), status = Some(DocumentStatuses.fromString(conf.DocumentApprovedStatus))), Seq.empty, None, None))
          hdfsInsertResultIds ← EitherT(cbToHdfsDocMover.persistTransientDocument(documents.map(_.id): _*))
        } yield {
          hdfsInsertResultIds
        }).value
    }
  }

}
