package tech.pegb.backoffice.domain.transaction

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.inject.bind
import play.api.test.Injecting
import tech.pegb.backoffice.dao.account.abstraction.AccountDao
import tech.pegb.backoffice.dao.account.entity.Account
import tech.pegb.backoffice.dao.customer.abstraction.UserDao
import tech.pegb.backoffice.dao.customer.entity.User
import tech.pegb.backoffice.dao.transaction.abstraction.TransactionDao
import tech.pegb.backoffice.dao.transaction.entity.Transaction
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.domain.transaction.dto.TransactionCriteria
import tech.pegb.backoffice.domain.transaction.implementation.TransactionMgmtService
import tech.pegb.backoffice.mapping.dao.domain.transaction.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.transaction.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.math.BigDecimal.RoundingMode

class TransactionMgmtServiceSpec extends PegBNoDbTestApp with GuiceOneAppPerSuite with Injecting with MockFactory with ScalaFutures {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val accountDao = stub[AccountDao]
  val transactionDao = stub[TransactionDao]
  val userDao = stub[UserDao]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[AccountDao].to(accountDao),
      bind[TransactionDao].to(transactionDao),
      bind[UserDao].to(userDao),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val config = inject[AppConfig]
  val transactionMgmtService = inject[TransactionMgmtService]
  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())

  val userId = UUID.randomUUID()
  val user = User(
    id = 1,
    uuid = userId.toString,
    userName = "user01",
    password = Some("pword"),
    `type` = Some("type_one"),
    tier = None,
    segment = None,
    subscription = Some("sub_one"),
    email = Some("alice@gmail.com"),
    status = Some("ACTIVE"),
    activatedAt = Option(LocalDateTime.now(mockClock)),
    passwordUpdatedAt = None,
    createdAt = LocalDateTime.now(mockClock),
    createdBy = "SuperUser",
    updatedAt = None,
    updatedBy = None)

  val accountId1 = UUID.randomUUID()
  private val account1 = Account.getEmpty.copy(
    id = 1,
    uuid = accountId1.toString,
    accountNumber = "4716 4157 0907 3361",
    userId = 1,
    accountName = "George Ogalo",
    accountType = "WALLET",
    isMainAccount = true.some,
    currency = "AED",
    balance = BigDecimal(0.00000).setScale(4, RoundingMode.HALF_UP).some,
    blockedBalance = BigDecimal(0.00000).setScale(4, RoundingMode.HALF_UP).some,
    status = "active".some,
    closedAt = None,
    lastTransactionAt = None,
    mainType = "liability",
    createdAt = LocalDateTime.of(2018, 12, 25, 0, 0, 0),
    createdBy = "SuperAdmin",
    updatedAt = None,
    updatedBy = None)
  val accountId2 = UUID.randomUUID()

  val accountId3 = UUID.randomUUID()

  val accountId4 = UUID.randomUUID()

  val transactionList = Seq(
    Transaction.getEmpty.copy(
      id = "1549446333",
      primaryAccountInternalId = 3,
      secondaryAccountInternalId = 1,
      direction = Some("credit"),
      `type` = Some("merchant_payment"),
      amount = Some(BigDecimal(500.00)),
      currency = Some("AED"),
      channel = Some("IOS_APP"),
      explanation = Some("some explanation"),
      status = Some("success"),
      instrument = None,
      createdAt = Some(LocalDateTime.of(2018, 12, 26, 3, 7, 30, 0))),
    Transaction.getEmpty.copy(
      id = "1549446333",
      primaryAccountInternalId = 1,
      secondaryAccountInternalId = 3,
      direction = Some("debit"),
      `type` = Some("merchant_payment"),
      amount = Some(BigDecimal(500.00)),
      currency = Some("AED"),
      channel = Some("IOS_APP"),
      explanation = Some("some explanation"),
      status = Some("success"),
      instrument = None,
      createdAt = Some(LocalDateTime.of(2018, 12, 26, 3, 7, 30, 0))),
    Transaction.getEmpty.copy(
      id = "1549446999",
      primaryAccountInternalId = 1,
      secondaryAccountInternalId = 4,
      direction = Some("credit"),
      `type` = Some("p2p_domestic"),
      amount = Some(BigDecimal(200.00)),
      currency = Some("AED"),
      channel = Some("ANDROID_APP"),
      explanation = Some("some explanation"),
      status = Some("success"),
      instrument = None,
      createdAt = Some(LocalDateTime.of(2018, 12, 25, 14, 27, 30, 0))),
    Transaction.getEmpty.copy(
      id = "1549446999",
      primaryAccountInternalId = 4,
      secondaryAccountInternalId = 1,
      direction = Some("debit"),
      `type` = Some("p2p_domestic"),
      amount = Some(BigDecimal(200.00)),
      currency = Some("AED"),
      channel = Some("ANDROID_APP"),
      explanation = Some("some explanation"),
      status = Some("success"),
      instrument = None,
      createdAt = Some(LocalDateTime.of(2018, 12, 25, 14, 27, 30, 0))),
    Transaction.getEmpty.copy(
      id = "1549449579",
      primaryAccountInternalId = 2,
      secondaryAccountInternalId = 1,
      direction = Some("credit"),
      `type` = Some("p2p_domestic"),
      amount = Some(BigDecimal(1250.00)),
      currency = Some("AED"),
      channel = Some("IOS_APP"),
      explanation = Some("some explanation"),
      status = Some("success"),
      instrument = None,
      createdAt = Some(LocalDateTime.of(2018, 12, 25, 0, 0, 0, 0))),
    Transaction.getEmpty.copy(
      id = "1549449579",
      primaryAccountInternalId = 1,
      secondaryAccountInternalId = 2,
      direction = Some("debit"),
      `type` = Some("p2p_domestic"),
      amount = Some(BigDecimal(1250.00)),
      currency = Some("AED"),
      channel = Some("IOS_APP"),
      explanation = Some("some explanation"),
      status = Some("success"),
      instrument = None,
      createdAt = Some(LocalDateTime.of(2018, 12, 25, 0, 0, 0, 0))))

  "TransactionMgmtService" should {
    "execute countTransactionsByCriteria get number of transactions " in {
      val criteria = TransactionCriteria(
        customerId = Some(userId.toUUIDLike),
        accountId = Some(accountId1.toUUIDLike),
        startDate = Some(LocalDateTime.of(2018, 12, 25, 0, 0, 0, 0)),
        endDate = Some(LocalDateTime.of(2018, 12, 31, 0, 0, 0, 0)),
        transactionType = None, channel = None, status = None)

      (userDao.getUser _).when(userId.toString).returns(Right(Some(user)))
      (accountDao.getAccount _).when(accountId1.toString).returns(Right(Some(account1)))
      (transactionDao.countTotalTransactionsByCriteria _).when(criteria.asDao()).returns(Right(transactionList.size))

      val result = transactionMgmtService.countTransactionsByCriteria(criteria)
      whenReady(result) { actual ⇒
        actual.isRight mustBe true
        actual.right.get mustBe 6
      }
    }

    "return list of transaction of account of user" in {
      val criteria = TransactionCriteria(customerId = Some(userId.toUUIDLike), accountId = Some(accountId1.toUUIDLike), startDate = Some(LocalDateTime.of(2018, 12, 25, 0, 0, 0, 0)), endDate = Some(LocalDateTime.of(2018, 12, 31, 0, 0, 0, 0)), transactionType = None, channel = None, status = None)

      val ordering = Seq(Ordering("created_at", Ordering.DESCENDING), Ordering("channel", Ordering.ASCENDING), Ordering("direction", Ordering.ASCENDING))

      (transactionDao.getTransactionsByCriteria _).when(criteria.asDao(), ordering.map(_.asDao), None, None)
        .returns(Right(transactionList))
      val result = transactionMgmtService.getTransactionsByCriteria(criteria, ordering, None, None)

      whenReady(result) { actual ⇒
        actual.isRight mustBe true
        actual.right.get mustBe transactionList.map(_.asDomain.toEither.right.get)
      }
    }

    "return empty seq when account does not have any transactions" in {
      val criteria = TransactionCriteria(customerId = Some(userId.toUUIDLike), accountId = Some(accountId1.toUUIDLike), startDate = Some(LocalDateTime.of(2018, 12, 25, 0, 0, 0, 0)), endDate = Some(LocalDateTime.of(2018, 12, 31, 0, 0, 0, 0)), transactionType = None, channel = None, status = None)

      val ordering = Seq(Ordering("created_at", Ordering.DESCENDING), Ordering("channel", Ordering.ASCENDING), Ordering("direction", Ordering.ASCENDING))

      (userDao.getUser _).when(userId.toString).returns(Right(Some(user)))
      (accountDao.getAccount _).when(accountId1.toString).returns(Right(Some(account1)))
      (transactionDao.getTransactionsByCriteria _).when(criteria.asDao(), ordering.map(_.asDao), None, None)
        .returns(Right(Nil))

      val result = transactionMgmtService.getTransactionsByCriteria(criteria, ordering, None, None)

      whenReady(result) { actual ⇒
        actual.isRight mustBe true
        actual.right.get mustBe Nil
      }
    }
  }
}
