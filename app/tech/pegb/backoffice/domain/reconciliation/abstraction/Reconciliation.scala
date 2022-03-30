package tech.pegb.backoffice.domain.reconciliation.abstraction

import java.time.LocalDateTime

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.domain.reconciliation.dto._
import tech.pegb.backoffice.domain.reconciliation.implementation
import tech.pegb.backoffice.domain.reconciliation.model.{ExternReconResult, InternReconResult}
import tech.pegb.backoffice.domain.transaction.model.Transaction

import scala.concurrent.Future

@ImplementedBy(classOf[implementation.ReconciliationService])
trait Reconciliation extends BaseService {

  def getInternalReconSummaries(
    criteria: Option[InternalReconSummaryCriteria],
    maybeOrderedBy: Seq[Ordering],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): Future[ServiceResponse[Seq[InternReconSummaryDto]]]

  def countInternalReconSummaries(criteria: Option[InternalReconSummaryCriteria]): Future[ServiceResponse[Int]]

  def getInternalReconResults(
    criteria: Option[InternalReconDetailsCriteria],
    maybeOrderedBy: Seq[Ordering],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): Future[ServiceResponse[Seq[InternReconResultDto]]]

  def countInternalReconResults(criteria: Option[InternalReconDetailsCriteria]): Future[ServiceResponse[Int]]

  def executeInternalReconByAccount(
    accountNumber: String,
    startDateTime: LocalDateTime,
    endDateTime: LocalDateTime): Future[ServiceResponse[Seq[InternReconResult]]]

  def executeExternalRecon(
    thirdParty: String,
    source: Option[String],
    startDateTime: Option[LocalDateTime],
    endDateTime: Option[LocalDateTime]): Future[ServiceResponse[Seq[ExternReconResult]]]

  def getTransactionsByThirdParty(
    thirdParty: String,
    startDateTime: Option[LocalDateTime],
    endDateTime: Option[LocalDateTime]): Future[ServiceResponse[Seq[Transaction]]]

  def resolveInternalReconSummary(
    id: String,
    dtoToUpdate: InternReconDailySummaryResultResolve): Future[ServiceResponse[InternReconSummaryDto]]
}
