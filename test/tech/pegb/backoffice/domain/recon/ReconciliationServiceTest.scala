package tech.pegb.backoffice.domain.recon

import java.time._
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FunSuiteLike, MustMatchers}
import tech.pegb.backoffice.dao.reconciliation.abstraction.ReconciliationDao
import tech.pegb.backoffice.dao.reconciliation.model.InternReconDailySummaryResult
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.reconciliation.abstraction.ExternalTransactionsProvider
import tech.pegb.backoffice.domain.reconciliation.dto.InternReconDailySummaryResultResolve
import tech.pegb.backoffice.domain.reconciliation.implementation.ReconciliationService
import tech.pegb.backoffice.domain.reconciliation.model.ReconciliationStatuses.SOLVED
import tech.pegb.backoffice.mapping.dao.domain.reconciliation.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.reconciliation.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.core.TestExecutionContext

import scala.concurrent.Future

class ReconciliationServiceTest extends FunSuiteLike with MockFactory with MustMatchers with ScalaFutures {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())

  val executionContexts: WithExecutionContexts = TestExecutionContext
  val mockedReconDao = stub[ReconciliationDao]
  val mockedExternalTxnProvider = stub[ExternalTransactionsProvider]

  val reconciliationService = new ReconciliationService(
    executionContexts,
    mockedReconDao,
    mockedExternalTxnProvider)

  test("ReconciliationService should return updated dto in updateInternalReconSummary") {

    val id = "123"
    val updateAt = LocalDateTime.now(mockClock)
    val updatedBy = "pegbuser"
    val dto = InternReconDailySummaryResultResolve(
      comments = "2500 KES transaction is created to resolved",
      updatedAt = updateAt,
      updatedBy = updatedBy,
      lastUpdatedAt = LocalDateTime.now().some)

    val summaryResultDao = InternReconDailySummaryResult(
      id = "123",
      reconDate = LocalDate.of(2019, 5, 15),
      accountId = "1",
      accountNumber = "some account num",
      accountType = "distribution",
      accountMainType = "liability",
      userId = 1,
      userUuid = "2205409c-1c83-11e9-a2a9-000c297e3e45",
      userFullName = "ujali tyagi".some,
      currency = "KES",
      endOfDayBalance = BigDecimal(3200.5000),
      valueChange = BigDecimal(300.0000),
      transactionTotalAmount = BigDecimal(9999.0000),
      transactionTotalCount = 100,
      problematicTxnCount = 1,
      status = "FAIL",
      comments = none,
      updatedAt = none,
      updatedBy = none)

    val updatedDto = summaryResultDao.copy(
      comments = "2500 KES transaction is created to resolved".some,
      updatedAt = updateAt.some,
      updatedBy = updatedBy.some)

    (mockedReconDao.getInternReconDailySummaryResult _).when(id)
      .returns(Future.successful(Right(summaryResultDao.some)))

    (mockedReconDao.updateInternReconDailySummaryResult _).when(id, dto.asDao(SOLVED.underlying))
      .returns(Future.successful(Right(updatedDto.some)))

    val result = reconciliationService.resolveInternalReconSummary(id, dto)

    whenReady(result) { actual ⇒
      actual mustBe Right(updatedDto.asDomainDto(updatedDto.endOfDayBalance - updatedDto.transactionTotalAmount))
    }

  }

  test("ReconciliationService should return validation error in updateInternalReconSummary when trying to resolve non-problem") {

    val id = "123"
    val updateAt = LocalDateTime.now(mockClock)
    val updatedBy = "pegbuser"
    val dto = InternReconDailySummaryResultResolve(
      comments = "2500 KES transaction is created to resolved",
      updatedAt = updateAt,
      updatedBy = updatedBy,
      lastUpdatedAt = LocalDateTime.now().some)

    val summaryResultDao = InternReconDailySummaryResult(
      id = "123",
      reconDate = LocalDate.of(2019, 5, 15),
      accountId = "1",
      accountNumber = "some account num",
      accountType = "distribution",
      accountMainType = "liability",
      userId = 1,
      userUuid = "2205409c-1c83-11e9-a2a9-000c297e3e45",
      userFullName = "ujali tyagi".some,
      currency = "KES",
      endOfDayBalance = BigDecimal(3200.5000),
      valueChange = BigDecimal(300.0000),
      transactionTotalAmount = BigDecimal(9999.0000),
      transactionTotalCount = 100,
      problematicTxnCount = 1,
      status = "SUCCESS",
      comments = none,
      updatedAt = none,
      updatedBy = none)

    (mockedReconDao.getInternReconDailySummaryResult _).when(id)
      .returns(Future.successful(Right(summaryResultDao.some)))

    val result = reconciliationService.resolveInternalReconSummary(id, dto)

    whenReady(result) { actual ⇒
      actual mustBe Left(ServiceError.validationError("Status of recon daily to resolve must be 'FAIL'", UUID.randomUUID().toOption))
    }

  }

  test("ReconciliationService should return notfoundError in updateInternalReconSummary when dao returns None") {

    val id = "123"
    val updateAt = LocalDateTime.now(mockClock)
    val updatedBy = "pegbuser"
    val dto = InternReconDailySummaryResultResolve(
      comments = "2500 KES transaction is created to resolved",
      updatedAt = updateAt,
      updatedBy = updatedBy,
      lastUpdatedAt = LocalDateTime.now().some)

    (mockedReconDao.getInternReconDailySummaryResult _).when(id)
      .returns(Future.successful(Right(none)))

    val result = reconciliationService.resolveInternalReconSummary(id, dto)

    whenReady(result) { actual ⇒
      actual mustBe Left(ServiceError.notFoundError("Recon Daily Summary with id: 123 not Found", UUID.randomUUID().toOption))
    }

  }
}
