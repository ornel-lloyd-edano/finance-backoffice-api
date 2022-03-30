package tech.pegb.backoffice.dao.reconciliation.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.reconciliation.dto.{InternalReconDetailsCriteria, InternalReconSummaryCriteria}
import tech.pegb.backoffice.dao.reconciliation.model._
import tech.pegb.backoffice.dao.reconciliation.postgresql

import scala.concurrent.Future

@ImplementedBy(classOf[postgresql.ReconciliationDao])
trait ReconciliationDao extends Dao {

  def getInternReconDetailsByCriteria(
    criteria: Option[InternalReconDetailsCriteria],
    ordering: Option[OrderingSet],
    limit: Option[Int], offset: Option[Int]): Future[DaoResponse[Seq[InternReconResult]]]

  def countInternReconDetailsByCriteria(
    criteria: Option[InternalReconDetailsCriteria]): Future[DaoResponse[Int]]

  def getInternReconResultsBySummaryId(summaryId: String): Future[DaoResponse[Seq[InternReconResult]]]

  def getInternReconDailySummaryResult(id: String): Future[DaoResponse[Option[InternReconDailySummaryResult]]]

  def getInternReconDailySummaryResults(
    criteria: Option[InternalReconSummaryCriteria],
    ordering: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int]): Future[DaoResponse[Seq[InternReconDailySummaryResult]]]

  def countInternReconDailySummaryResults(criteria: Option[InternalReconSummaryCriteria]): Future[DaoResponse[Int]]

  def updateInternReconDailySummaryResult(
    id: String, reconResultToUpdate: InternReconDailySummaryResultToUpdate): Future[DaoResponse[Option[InternReconDailySummaryResult]]]
}

