package tech.pegb.backoffice.domain.reconciliation.implementation

import java.time.LocalDateTime
import java.util.UUID

import cats.data.EitherT
import cats.implicits._
import com.google.inject.Inject
import tech.pegb.backoffice.dao.Dao.DaoResponse
import tech.pegb.backoffice.dao.reconciliation.abstraction.ReconciliationDao
import tech.pegb.backoffice.dao.reconciliation.model
import tech.pegb.backoffice.domain.model.{Ordering ⇒ DomainOrdering}
import tech.pegb.backoffice.domain.reconciliation.abstraction
import tech.pegb.backoffice.domain.reconciliation.dto._
import tech.pegb.backoffice.domain.reconciliation.model.ReconciliationStatuses.{NOK, SOLVED}
import tech.pegb.backoffice.domain.reconciliation.model._
import tech.pegb.backoffice.domain.{BaseService, ServiceError}
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.reconciliation.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.reconciliation.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts

import scala.concurrent.{ExecutionContext, Future}

class ReconciliationService @Inject() (
    ec: WithExecutionContexts,
    reconciliationDao: ReconciliationDao,
    val externalTxnsProvider: abstraction.ExternalTransactionsProvider) extends abstraction.Reconciliation with BaseService {

  implicit val executionContext: ExecutionContext = ec.genericOperations
  implicit val dateTimeOrdering: Ordering[LocalDateTime] = Ordering.fromLessThan(_ isBefore _)

  def getInternalReconSummaries(
    criteria: Option[InternalReconSummaryCriteria],
    maybeOrderedBy: Seq[DomainOrdering],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): Future[ServiceResponse[Seq[InternReconSummaryDto]]] = {

    val result: Future[DaoResponse[Seq[model.InternReconDailySummaryResult]]] =
      reconciliationDao.getInternReconDailySummaryResults(
        criteria.map(_.asDao),
        ordering = maybeOrderedBy.asDao,
        limit = maybeLimit,
        offset = maybeOffset)

    result.map { daoResponse ⇒
      daoResponse.map(results ⇒
        results.map(internReconDailySummaryResult ⇒ internReconDailySummaryResult
          .asDomainDto(internReconDailySummaryResult.endOfDayBalance - internReconDailySummaryResult.transactionTotalAmount)))
    }
      .map(_.asServiceResponse)
  }

  def countInternalReconSummaries(criteria: Option[InternalReconSummaryCriteria]): Future[ServiceResponse[Int]] = {
    import tech.pegb.backoffice.mapping.domain.dao.reconciliation.Implicits._

    reconciliationDao.countInternReconDailySummaryResults(criteria.map(_.asDao)).map(_.asServiceResponse)
  }

  def getInternalReconResults(
    criteria: Option[InternalReconDetailsCriteria],
    maybeOrderedBy: Seq[DomainOrdering],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): Future[ServiceResponse[Seq[InternReconResultDto]]] = {

    reconciliationDao.getInternReconDetailsByCriteria(
      criteria = criteria.map(_.asDao),
      ordering = maybeOrderedBy.asDao,
      limit = maybeLimit,
      offset = maybeOffset).map(_.asServiceResponse.map(_.map(_.asDomain)))
  }

  def countInternalReconResults(criteria: Option[InternalReconDetailsCriteria]): Future[ServiceResponse[Int]] = {

    reconciliationDao.countInternReconDetailsByCriteria(
      criteria = criteria.map(_.asDao)).map(_.asServiceResponse)

  }

  def executeInternalReconByAccount(accountNumber: String, startDateTime: LocalDateTime, endDateTime: LocalDateTime): Future[ServiceResponse[Seq[InternReconResult]]] = Future {
    ???
  }

  //get transactions join txns and accounts on primary_account_id
  //filter by accounts.accountNumber, txns.created_at between startDateTime and endDateTime
  //order by txn id ASC, created_at ASC

  //foldLeft the ordered List, on each fold convert Map[String, String] to ReconciliationResult model

  //if persistResults = true then upsert to db

  def executeExternalRecon(thirdParty: String, source: Option[String],
    startDateTime: Option[LocalDateTime],
    endDateTime: Option[LocalDateTime]) = {

    Future.successful(Right(Seq.empty))
  }

  def getTransactionsByThirdParty(
    thirdParty: String,
    startDateTime: Option[LocalDateTime],
    endDateTime: Option[LocalDateTime]) = ???

  def resolveInternalReconSummary(id: String, dtoToUpdate: InternReconDailySummaryResultResolve): Future[ServiceResponse[InternReconSummaryDto]] = {
    implicit val fakeRequestId: UUID = UUID.randomUUID()

    (for {
      getResultOption ← EitherT({
        reconciliationDao.getInternReconDailySummaryResult(id).map(_.asServiceResponse)
      })
      getResult ← EitherT.fromOption[Future](getResultOption, ServiceError.notFoundError(
        s"Recon Daily Summary with id: $id not Found", fakeRequestId.toOption))
      _ ← EitherT.cond[Future](getResult.status == NOK.underlying, getResult,
        validationError("Status of recon daily to resolve must be 'FAIL'"))
      updatedReconOption ← EitherT(reconciliationDao.updateInternReconDailySummaryResult(id, dtoToUpdate.asDao(SOLVED.underlying)).map(_.asServiceResponse))
      updateReconResult ← EitherT.fromOption[Future](updatedReconOption, ServiceError.notFoundError(
        s"Recon Daily Summary with id: $id not Found", fakeRequestId.toOption))
    } yield updateReconResult
      .asDomainDto(updateReconResult.endOfDayBalance - updateReconResult.transactionTotalAmount)).value
  }

}
