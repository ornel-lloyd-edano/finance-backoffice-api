package tech.pegb.backoffice.dao

import java.time.{Clock, Instant, LocalDateTime, ZoneId}

import cats.implicits._
import tech.pegb.backoffice.dao.transaction.abstraction.TransactionReversalDao
import tech.pegb.backoffice.dao.transaction.entity.TransactionReversal
import tech.pegb.backoffice.util.AppConfig
import tech.pegb.core.PegBTestApp

class TransactionReversalSqlDaoSpec extends PegBTestApp {
  val config = inject[AppConfig]

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  private val dao = inject[TransactionReversalDao]

  override def initSql =
    s"""
       |INSERT INTO transaction_reversals
       |(id, reversed_transaction_id, reversal_transaction_id, reason, status, created_by, updated_by, created_at, updated_at)
       |VALUES
       |('6', '1562591371', '1565096864', 'any reason', 'success', '', 'UNKNOWNUSER', '2019-08-06 14:07:44', '2019-08-06 14:07:44'),
       |('7', '1557144218', '1565097071', 'any reason', 'success', '', 'UNKNOWNUSER', '2019-08-06 14:11:11', '2019-08-06 14:11:11'),
       |('4', '1557298152', '1565096657', 'any reason', 'success', '', 'UNKNOWNUSER', '2019-08-06 14:04:17', '2019-08-06 14:04:17'),
       |('5', '1561553626', '1565096770', 'any reason', 'success', '', 'UNKNOWNUSER', '2019-08-06 14:06:10', '2019-08-06 14:06:10'),
       |('2', '1563968779', '1565096169', 'any reason', 'success', '', 'UNKNOWNUSER', '2019-08-06 13:56:09', '2019-08-06 13:56:09'),
       |('1', '1556433539', '1564749073', 'test reverse', 'success', '', 'Swagger', '2019-08-02 13:31:13', '2019-08-02 13:31:13');
     """.stripMargin

  "TransactionReversal getByID" should {
    "return Right(Some(TransactionReversal)) when row is found" in {
      val resp = dao.getTransactionReversalsByCriteriaById("1562591371")

      resp.map(_.get.id) mustBe Right(6)
      resp.map(_.get.reversedTransactionId) mustBe Right("1562591371")
      resp.map(_.get.reversalTransactionId) mustBe Right("1565096864")
      resp.map(_.get.reason) mustBe Right("any reason")
      resp.map(_.get.status) mustBe Right("success")
      resp.map(_.get.createdBy) mustBe Right("")
      resp.map(_.get.updatedBy) mustBe Right("UNKNOWNUSER")
      resp.map(_.get.createdAt) mustBe Right(LocalDateTime.of(2019, 8, 6, 14, 7, 44))
      resp.map(_.get.updatedAt) mustBe Right(LocalDateTime.of(2019, 8, 6, 14, 7, 44))

    }

    "return Right(None) when row is not found" in {
      val resp = dao.getTransactionReversalsByCriteriaById("deadbeef")

      resp mustBe Right(none[TransactionReversal])
    }
  }
}
