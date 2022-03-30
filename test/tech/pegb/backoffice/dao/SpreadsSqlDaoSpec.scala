package tech.pegb.backoffice.dao

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import org.scalamock.scalatest.MockFactory
import play.api.db.DBApi
import tech.pegb.backoffice.dao.DaoError.PreconditionFailed
import tech.pegb.backoffice.dao.currencyexchange.abstraction.SpreadsDao
import tech.pegb.backoffice.dao.currencyexchange.dto.{SpreadCriteria, SpreadToInsert, SpreadUpdateDto}
import tech.pegb.backoffice.dao.currencyexchange.entity.Spread
import tech.pegb.backoffice.dao.model._
import tech.pegb.core.PegBTestApp

class SpreadsSqlDaoSpec extends PegBTestApp with MockFactory {

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  val usdFx = UUID.randomUUID()
  val eurFx = UUID.randomUUID()
  val cnyFx = UUID.randomUUID()
  val chfFx = UUID.randomUUID()

  val usdSpread = UUID.randomUUID()
  val eurSpread = UUID.randomUUID()
  val cnySpread = UUID.randomUUID()
  val chfSpread = UUID.randomUUID()

  val kesId = 1
  val usdId = 2
  val euroId = 3
  val cnyId = 4
  val chfId = 5

  val usdEscrowId = 1
  val usdEscrowUUID = UUID.randomUUID()
  val euroEscrowId = 2
  val euroEscrowUUID = UUID.randomUUID()
  val cnyEscrowId = 3
  val cnyEscrowUUID = UUID.randomUUID()
  val chfEscrowId = 4
  val chfEscrowUUID = UUID.randomUUID()
  val kesEscrowId = 5
  val kesEscrowUUID = UUID.randomUUID()

  override def initSql =
    s"""
       |INSERT INTO account_types(id, account_type_name, description, created_at, updated_at, is_active)
       |VALUES('1', 'WALLET', 'standard account type for individual users', now(), null, 1);
       |
       |INSERT INTO currencies(id, currency_name, description, created_at, updated_at, is_active)
       |VALUES('1', 'KES', 'Kenya shilling', now(), null, 1);
       |
       |INSERT INTO currencies(id, currency_name, description, created_at, updated_at, is_active)
       |VALUES('2', 'USD', 'US Dollar', now(), null, 1);
       |
       |INSERT INTO currencies(id, currency_name, description, created_at, updated_at, is_active)
       |VALUES('3', 'EUR', 'EURO', now(), null, 1);
       |
       |INSERT INTO currencies(id, currency_name, description, created_at, updated_at, is_active)
       |VALUES('4', 'CNY', 'default currency for China', now(), null, 1);
       |
       |INSERT INTO currencies(id, currency_name, description, created_at, updated_at, is_active)
       |VALUES('5', 'CHF', 'default currency for Switzerland', now(), null, 1);
       |
       |INSERT INTO users(id, uuid, username, password, email, subscription, type, status, tier, segment, activated_at, password_updated_at, created_at, created_by, updated_at, updated_by)
       |VALUES('1', 'bcc32571-cf16-4abc-ac38-38d58f9cbab5', null, null, null, null, null, null, null, null, now(), null, now(), 'SuperAdmin', null, null);
       |
       |INSERT INTO accounts(id, uuid, number, user_id, name, is_main_account, account_type_id, currency_id, balance, blocked_balance, status, closed_at, last_transaction_at, created_at, created_by, updated_at, updated_by, main_type)
       |VALUES('1', '$usdEscrowUUID', '4716 4157 0907 3361', '1', 'USD Escrow', '1', '1', '$usdId', '17526.5', '0.0', 'ACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null, 'liability' );
       |
       |INSERT INTO accounts(id, uuid, number, user_id, name, is_main_account, account_type_id, currency_id, balance, blocked_balance, status, closed_at, last_transaction_at, created_at, created_by, updated_at, updated_by, main_type)
       |VALUES('2', '$euroEscrowUUID', '8912 3287 1209 3422', '1', 'EUR Escrow', '0', '1', '$euroId', '10519.0', '0.0', 'ACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null, 'liability' );
       |
       |INSERT INTO accounts(id, uuid, number, user_id, name, is_main_account, account_type_id, currency_id, balance, blocked_balance, status, closed_at, last_transaction_at, created_at, created_by, updated_at, updated_by, main_type)
       |VALUES('3', '$cnyEscrowUUID', '8912 3287 1209 3423', '1', 'CNY Escrow', '0', '1', '$cnyId', '0.0', '0.0', 'ACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null, 'liability' );
       |
       |INSERT INTO accounts(id, uuid, number, user_id, name, is_main_account, account_type_id, currency_id, balance, blocked_balance, status, closed_at, last_transaction_at, created_at, created_by, updated_at, updated_by, main_type)
       |VALUES('4', '$chfEscrowUUID', '4716 4157 0907 3364', '1', 'CHF Escrow', '1', '1', '$chfId', '0.0', '0.0', 'ACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null, 'liability' );
       |
       |INSERT INTO accounts(id, uuid, number, user_id, name, is_main_account, account_type_id, currency_id, balance, blocked_balance, status, closed_at, last_transaction_at, created_at, created_by, updated_at, updated_by, main_type)
       |VALUES('5', '$kesEscrowUUID', '4716 4157 0907 3365', '1', 'KES Escrow', '1', '1', '$kesId', '0.0', '0.0', 'ACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null, 'liability' );
       |
       |INSERT INTO providers
       |(id, user_id, service_id, name, transaction_type, icon, label, pg_institution_id,
       |utility_payment_type, utility_min_payment_amount, utility_max_payment_amount,
       |is_active, created_by, updated_by, created_at, updated_at)
       |VALUES
       |('1', '1', null, 'Currency Cloud', 'txn type 1', 'icon 1', 'label 1', '1', null, '0.1', '1000', '1', 'unit test', 'unit test', now(), now()),
       |('2', '1', null, 'Ebury', 'txn type 2', 'icon 2', 'label 2', '1', null, '0.1', '1000', '1', 'unit test', 'unit test', now(), now());
       |
       |INSERT INTO currency_rates(id, uuid, currency_id, base_currency_id, rate, provider_id, status, updated_by, updated_at) VALUES
       |(1, '$usdFx', 2, 1, 99.9800, 1, 'active', 'pegbuser', '$now' ),
       |(2, '$eurFx', 3, 1, 112.1020, 1, 'active',  'pegbuser', '$now' ),
       |(3, '$cnyFx', 4, 1, 75.9501, 2, 'active',  'pegbuser', '$now'),
       |(4, '$chfFx', 5, 1, 152.2014, 2, 'inactive',  'pegbuser', '$now');
       |
       |INSERT INTO currency_spreads(id, uuid, currency_rate_id, transaction_type, channel, institution, spread, deleted_at, created_by, created_at, updated_by, updated_at) VALUES
       |(1, '$usdSpread', 1, 'currency_exchange', null, null, 0.2, null, 'pegbuser', '$now', null, null),
       |(2, '$eurSpread', 2, 'currency_exchange', null, null, 0.25, '$now', 'pegbuser', '$now', null, null),
       |(3, '$cnySpread', 3, 'international_remittance', 'mobile_money', null, 0.15, null, 'pegbuser', '$now', null, null),
       |(4, '$chfSpread', 1, 'international_remittance', 'mobile_money', 'mashreq', 0.05, null, 'pegbuser', '$now', null, null);
     """.stripMargin

  val dbApi: DBApi = inject[DBApi]
  val spreadDao = inject[SpreadsDao]

  "SpreadsSqlDao" should {
    "return Some(Spread) in getSpread(id) if uuid was found" in {
      val result = spreadDao.getSpread(usdSpread)

      val expected = Spread(id = 1, uuid = usdSpread, currencyExchangeId = 1, currencyExchangeUuid = usdFx,
        transactionType = "currency_exchange", channel = None, recipientInstitution = None,
        spread = BigDecimal("0.2000"), deletedAt = None,
        createdBy = "pegbuser", createdAt = now, updatedBy = None, updatedAt = None)

      result.right.get mustBe Option(expected)
    }

    "return None in getSpread(id) if uuid was not found" in {
      val result = spreadDao.getSpread(UUID.randomUUID())
      result.right.get mustBe None
    }

    "return Seq[Spread] in getSpreadsByCriteria which has all spreads in the table" in {
      val noCriteria = SpreadCriteria()
      val result = spreadDao.getSpreadsByCriteria(noCriteria, None, None, None)
      result.right.get.size mustBe 4
      result.right.get.map(_.uuid).toSet mustBe Set(usdSpread, eurSpread, cnySpread, chfSpread)
    }

    "return Seq[Spread] in getSpreadsByCriteria and ordered based on given simple ordering" in {
      val noCriteria = SpreadCriteria()
      val orderedByUuid = OrderingSet(Ordering("uuid", Ordering.ASC))
      val result = spreadDao.getSpreadsByCriteria(noCriteria, Option(orderedByUuid), None, None)
      result.right.get.size mustBe 4
      result.right.get.map(_.uuid) mustBe Seq(usdSpread, eurSpread, cnySpread, chfSpread).sortBy(_.toString)
    }

    "return Seq[Spread] in getSpreadsByCriteria and ordered based on given complex ordering" in {
      val noCriteria = SpreadCriteria()
      val complexMultiOrdering = OrderingSet(
        Ordering("transaction_type", Ordering.ASC), Ordering("spread", Ordering.DESC))
      val result = spreadDao.getSpreadsByCriteria(noCriteria, Option(complexMultiOrdering), None, None)
      result.right.get.size mustBe 4

      val expected = Seq(
        ("currency_exchange", BigDecimal("0.2500")),
        ("currency_exchange", BigDecimal("0.2000")),
        ("international_remittance", BigDecimal("0.1500")),
        ("international_remittance", BigDecimal("0.0500")))

      result.right.get.map(r ⇒ (r.transactionType, r.spread)) mustBe expected
    }

    "return Seq[Spread] in getSpreadsByCriteria based on given simple criteria" in {
      val noCriteria = SpreadCriteria(isDeletedAtNotNull = Option(false))
      val result = spreadDao.getSpreadsByCriteria(noCriteria, None, None, None)
      result.right.get.size mustBe 3
      result.right.get.map(_.uuid).toSet mustBe Set(usdSpread, cnySpread, chfSpread)
    }

    "return Seq[Spread] in getSpreadsByCriteria based on given complex criteria" in {
      val criteria = SpreadCriteria(
        isDeletedAtNotNull = Option(false),
        transactionType = Option("currency_exchange"))
      val result = spreadDao.getSpreadsByCriteria(criteria, None, None, None)
      result.right.get.size mustBe 1
      result.right.get.map(_.uuid).head mustBe usdSpread
    }

    "return Seq[Spread] in getSpreadsByCriteria paginated with limit and offset" in {
      val noCriteria = SpreadCriteria()
      val result = spreadDao.getSpreadsByCriteria(noCriteria, None, Option(2), Option(2))
      result.right.get.size mustBe 2
      result.right.get.map(_.uuid) mustBe Seq(cnySpread, chfSpread)
    }

    "return Int in countSpreadsByCriteria which is total number of spreads in table" in {
      val noCriteria = SpreadCriteria()
      val result = spreadDao.countSpreadsByCriteria(noCriteria)
      result.right.get mustBe 4
    }

    "return Int in countSpreadsByCriteria which is partial number of spreads in table matching the given criteria" in {
      val criteria = SpreadCriteria(
        isDeletedAtNotNull = Option(false),
        transactionType = Option("currency_exchange"))
      val result = spreadDao.countSpreadsByCriteria(criteria)
      result.right.get mustBe 1
    }

    "return newly created Spread in successful createSpread: international_remittance" in {
      //(1, '$usdFx', 2, 1, 99.9800, 100.0020, 1, 'active', 1, 1362.0000, 618.5, 'pegbuser', '$now' ),
      val fxId = 1
      val spreadToInsert = SpreadToInsert(
        currencyExchangeId = usdFx,
        transactionType = "international_remittance",
        channel = Some("bank"),
        institution = Some("Mashreq"),
        spread = BigDecimal(0.01),
        createdAt = LocalDateTime.now(mockClock),
        createdBy = "pegbuser")

      val actual = spreadDao.createSpread(spreadToInsert)

      actual.isRight mustBe true

      val actualSpread = actual.right.get

      actualSpread.currencyExchangeId mustBe fxId
      actualSpread.transactionType mustBe "international_remittance"
      actualSpread.channel.get mustBe "bank"
      actualSpread.recipientInstitution.get mustBe "Mashreq"
      actualSpread.spread mustBe BigDecimal(0.01)
      actualSpread.createdAt mustBe LocalDateTime.now(mockClock)
      actualSpread.createdBy mustBe "pegbuser"
      actualSpread.updatedAt mustBe Some(LocalDateTime.now(mockClock))
      actualSpread.updatedBy mustBe Some("pegbuser")

    }

    "return newly created Spread in successful createSpread: currency_exchange" in {
      val fxId = 1
      val spreadToInsert = SpreadToInsert(
        currencyExchangeId = usdFx,
        transactionType = "currency_exchange",
        channel = None,
        institution = None,
        spread = BigDecimal(0.01),
        createdAt = LocalDateTime.now(mockClock),
        createdBy = "pegbuser")

      val actual = spreadDao.createSpread(spreadToInsert)

      actual.isRight mustBe true

      val actualSpread = actual.right.get

      actualSpread.currencyExchangeId mustBe fxId
      actualSpread.transactionType mustBe "currency_exchange"
      actualSpread.channel mustBe None
      actualSpread.recipientInstitution mustBe None
      actualSpread.spread mustBe BigDecimal(0.01)
      actualSpread.createdAt mustBe LocalDateTime.now(mockClock)
      actualSpread.createdBy mustBe "pegbuser"

    }
    "return error createSpread when currency_exchange does not exist" in {
      val fakeCurrencyExchangeUUID = UUID.randomUUID()
      val spreadToInsert = SpreadToInsert(
        currencyExchangeId = fakeCurrencyExchangeUUID,
        transactionType = "currency_exchange",
        channel = None,
        institution = None,
        spread = BigDecimal(0.01),
        createdAt = LocalDateTime.now(mockClock),
        createdBy = "pegbuser")

      val actual = spreadDao.createSpread(spreadToInsert)

      actual.isLeft mustBe true
      actual.left.get mustBe DaoError.EntityNotFoundError(s"Currency exchange $fakeCurrencyExchangeUUID was not found")
    }

    "update spread" in {
      implicit val requestId: UUID = UUID.randomUUID()
      val expectedSpread = BigDecimal(0.45)
      val dto = SpreadUpdateDto(
        spread = expectedSpread,
        updatedBy = "pegbuser",
        updatedAt = now,
        deletedAt = None,
        lastUpdatedAt = None)
      val error = DaoError.EntityNotFoundError(s"Spread $usdSpread was not found")
      val result = spreadDao.update(usdSpread, dto)
      result.map(_.spread) mustBe Right(expectedSpread)
      val check = spreadDao.getSpread(usdSpread)
      result.map(_.spread) mustBe check.flatMap(_.map(_.spread).toRight(error))
    }

    "fail update spread (precondition fail)" in {
      implicit val requestId: UUID = UUID.randomUUID()
      val expectedSpread = BigDecimal(0.45)
      val dto = SpreadUpdateDto(
        spread = expectedSpread,
        updatedBy = "pegbuser",
        updatedAt = now,
        deletedAt = None,
        lastUpdatedAt = None)
      val result = spreadDao.update(usdSpread, dto)
      result mustBe Left(PreconditionFailed(s"Update failed. Spread ${usdSpread} has been modified by another process."))
    }

    "fail delete spread (precondition fail)" in {
      implicit val requestId: UUID = UUID.randomUUID()
      val expectedSpread = BigDecimal(0.45)
      val dto = SpreadUpdateDto(
        spread = BigDecimal(0),
        updatedBy = "pegbuser",
        updatedAt = now,
        deletedAt = Some(now),
        lastUpdatedAt = None)
      val result = spreadDao.update(usdSpread, dto)
      result mustBe Left(PreconditionFailed(s"Update failed. Spread ${usdSpread} has been modified by another process."))
    }

    "delete spread" in {
      implicit val requestId: UUID = UUID.randomUUID()
      val expectedSpread = spreadDao.getSpread(usdSpread)
        .flatMap(_.toRight(DaoError.EntityNotFoundError(s"Spread id couldn't be found ($usdSpread)")))

      val dto = SpreadUpdateDto(
        spread = BigDecimal(0),
        updatedBy = "pegbuser",
        updatedAt = now,
        deletedAt = Some(now),
        lastUpdatedAt = Some(now))
      val result = spreadDao.update(usdSpread, dto)

      result mustBe expectedSpread
      result.isRight mustBe true
      spreadDao.getSpread(usdSpread) mustBe Right(Option.empty[Spread])
    }
  }
}
