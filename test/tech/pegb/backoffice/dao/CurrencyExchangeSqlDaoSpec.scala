package tech.pegb.backoffice.dao

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import tech.pegb.backoffice.dao.DaoError.EntityNotFoundError
import tech.pegb.backoffice.dao.currencyexchange.dto.CurrencyExchangeCriteria
import tech.pegb.backoffice.dao.currencyexchange.entity.CurrencyExchange
import tech.pegb.backoffice.dao.currencyexchange.sql.CurrencyExchangeSqlDao
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes, Ordering}
import tech.pegb.backoffice.util.AppConfig
import tech.pegb.core.PegBTestApp

class CurrencyExchangeSqlDaoSpec extends PegBTestApp with MockFactory {

  lazy val currencyExchangeSqlDao = inject[CurrencyExchangeSqlDao]
  val appConfig = inject[AppConfig]

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  val today = LocalDateTime.now()

  val usdFx = UUID.randomUUID()
  val eurFx = UUID.randomUUID()
  val cnyFx = UUID.randomUUID()
  val chfFx = UUID.randomUUID()

  val kesId = 1
  val usdId = 2
  val euroId = 3
  val cnyId = 4
  val chfId = 5

  val usdEscrowId = 1
  val usdEscrowUUID = UUID.randomUUID()
  val usdBalance = BigDecimal(17526.50)
  val euroEscrowId = 2
  val euroEscrowUUID = UUID.randomUUID()
  val euroBalance = BigDecimal(10519.0)
  val cnyEscrowId = 3
  val cnyEscrowUUID = UUID.randomUUID()
  val cnyBalance = BigDecimal(0.00)
  val chfEscrowId = 4
  val chfEscrowUUID = UUID.randomUUID()
  val chfBalance = BigDecimal(0.00)
  val kesEscrowId = 5
  val kesEscrowUUID = UUID.randomUUID()

  val currencyCloudProviderId = 1
  val eburyProviderId = 2

  val fx1 = CurrencyExchange(
    id = 1,
    uuid = usdFx.toString,
    currencyId = usdId,
    currencyCode = "USD",
    baseCurrencyId = kesId,
    baseCurrency = "KES",
    rate = BigDecimal(99.9800),
    providerId = currencyCloudProviderId,
    provider = "Currency Cloud",
    targetCurrencyAccountId = usdEscrowId,
    targetCurrencyAccountUuid = usdEscrowUUID.toString,
    baseCurrencyAccountId = kesEscrowId,
    baseCurrencyAccountUuid = kesEscrowUUID.toString,
    balance = usdBalance,
    status = "active",
    updatedAt = Some(LocalDateTime.now(mockClock)),
    updatedBy = Some("pegbuser"))
  val fx2 = CurrencyExchange(
    id = 2,
    uuid = eurFx.toString,
    currencyId = euroId,
    currencyCode = "EUR",
    baseCurrencyId = kesId,
    baseCurrency = "KES",
    rate = BigDecimal(112.1020),
    providerId = currencyCloudProviderId,
    provider = "Currency Cloud",
    targetCurrencyAccountId = euroEscrowId,
    targetCurrencyAccountUuid = euroEscrowUUID.toString,
    baseCurrencyAccountId = kesEscrowId,
    baseCurrencyAccountUuid = kesEscrowUUID.toString,
    balance = euroBalance,
    status = "active",
    updatedAt = Some(LocalDateTime.now(mockClock)),
    updatedBy = Some("pegbuser"))
  val fx3 = CurrencyExchange(
    id = 3,
    uuid = cnyFx.toString,
    currencyId = cnyId,
    currencyCode = "CNY",
    baseCurrencyId = kesId,
    baseCurrency = "KES",
    rate = BigDecimal(75.9501),
    providerId = eburyProviderId,
    provider = "Ebury",
    targetCurrencyAccountId = cnyEscrowId,
    targetCurrencyAccountUuid = cnyEscrowUUID.toString,
    baseCurrencyAccountId = kesEscrowId,
    baseCurrencyAccountUuid = kesEscrowUUID.toString,
    balance = cnyBalance,
    status = "active",
    updatedAt = Some(LocalDateTime.now(mockClock)),
    updatedBy = Some("pegbuser"))
  val fx4 = CurrencyExchange(
    id = 4,
    uuid = chfFx.toString,
    currencyId = chfId,
    currencyCode = "CHF",
    baseCurrencyId = kesId,
    baseCurrency = "KES",
    rate = BigDecimal(152.2014),
    providerId = eburyProviderId,
    provider = "Ebury",
    targetCurrencyAccountId = chfEscrowId,
    targetCurrencyAccountUuid = chfEscrowUUID.toString,
    baseCurrencyAccountId = kesEscrowId,
    baseCurrencyAccountUuid = kesEscrowUUID.toString,
    balance = chfBalance,
    status = "inactive",
    updatedAt = Some(LocalDateTime.now(mockClock)),
    updatedBy = Some("pegbuser"))

  override def initSql =
    s"""
       |CREATE ALIAS DATE AS $$$$ java.time.LocalDate getDateFromTimestamp(String value){
       | return java.time.LocalDate.parse(value.substring(0, 10), java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
       |} $$$$;
       |
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
       |VALUES('1', '$usdEscrowUUID', '4716 4157 0907 3361', 1, 'USD Escrow', '1', '1', '$usdId', '17526.5', '0.0', 'ACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null, 'liability' );
       |
       |INSERT INTO accounts(id, uuid, number, user_id, name, is_main_account, account_type_id, currency_id, balance, blocked_balance, status, closed_at, last_transaction_at, created_at, created_by, updated_at, updated_by, main_type)
       |VALUES('2', '$euroEscrowUUID', '8912 3287 1209 3422', 1, 'EUR Escrow', '0', '1', '$euroId', '10519.0', '0.0', 'ACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null, 'liability' );
       |
       |INSERT INTO accounts(id, uuid, number, user_id, name, is_main_account, account_type_id, currency_id, balance, blocked_balance, status, closed_at, last_transaction_at, created_at, created_by, updated_at, updated_by, main_type)
       |VALUES('3', '$cnyEscrowUUID', '8912 3287 1209 3423', 1, 'CNY Escrow', '0', '1', '$cnyId', '0.0', '0.0', 'ACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null, 'liability' );
       |
       |INSERT INTO accounts(id, uuid, number, user_id, name, is_main_account, account_type_id, currency_id, balance, blocked_balance, status, closed_at, last_transaction_at, created_at, created_by, updated_at, updated_by, main_type)
       |VALUES('4', '$chfEscrowUUID', '4716 4157 0907 3364', 1, 'CHF Escrow', '1', '1', '$chfId', '0.0', '0.0', 'ACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null, 'liability' );
       |
       |INSERT INTO accounts(id, uuid, number, user_id, name, is_main_account, account_type_id, currency_id, balance, blocked_balance, status, closed_at, last_transaction_at, created_at, created_by, updated_at, updated_by, main_type)
       |VALUES('5', '$kesEscrowUUID', '4716 4157 0907 3365', 1, 'KES Escrow', '1', '1', '$kesId', '20000.0', '0.0', 'ACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null, 'liability' );
       |
       |INSERT INTO providers
       |(id, user_id, service_id, name, transaction_type, icon, label, pg_institution_id,
       |utility_payment_type, utility_min_payment_amount, utility_max_payment_amount,
       |is_active, created_by, updated_by, created_at, updated_at)
       |VALUES
       |('1', '1', null, 'Currency Cloud', 'txn type 1', 'icon 1', 'label 1', '1', null, '0.1', '1000', '1', 'unit test', 'unit test', now(), now()),
       |('2', '1', null, 'Ebury', 'txn type 2', 'icon 2', 'label 2', '1', null, '0.1', '1000', '1', 'unit test', 'unit test', now(), now());
       |
       |
       |INSERT INTO currency_rates(id, uuid, currency_id, base_currency_id, rate, provider_id, status,  updated_by, updated_at) VALUES
       |(1, '$usdFx', 2, 1, 99.9800, 1, 'active', 'pegbuser', '$now' ),
       |(2, '$eurFx', 3, 1, 112.1020, 1, 'active',  'pegbuser', '$now' ),
       |(3, '$cnyFx', 4, 1, 75.9501, 2, 'active',  'pegbuser', '$now'),
       |(4, '$chfFx', 5, 1, 152.2014, 2, 'inactive',  'pegbuser', '$now');
       |
       |
       |INSERT INTO users(id, uuid, username, password, email, subscription, type, status, tier, segment, activated_at, password_updated_at, created_at, created_by, updated_at, updated_by)
       |VALUES('10', 'aaa32571-cf16-4abc-ac38-38d58f9cbab5', null, null, null, null, null, null, null, null, now(), null, now(), 'SuperAdmin', null, null);
       |
       |INSERT INTO users(id, uuid, username, password, email, subscription, type, status, tier, segment, activated_at, password_updated_at, created_at, created_by, updated_at, updated_by)
       |VALUES('11', 'aaa4db00-2b61-4a23-8348-35f0030c3b1d', null, null, null, null, null, null, null, null, now(), null, now(), 'SuperAdmin', null, null);
       |
       |INSERT INTO accounts(id, uuid, number, user_id, name, is_main_account, account_type_id, currency_id, balance, blocked_balance, status, closed_at, last_transaction_at, created_at, created_by, updated_at, updated_by)
       |VALUES('10', 'aaa5bdb9-5d42-4a84-922a-0d01f8e5de71', '1111 4157 0907 3361', '10', 'George Ogalo', '1', '1', '$kesId', '0.0', '0.0', 'INACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null );
       |
       |INSERT INTO accounts(id, uuid, number, user_id, name, is_main_account, account_type_id, currency_id, balance, blocked_balance, status, closed_at, last_transaction_at, created_at, created_by, updated_at, updated_by)
       |VALUES('11', 'aaaeb85f-70f8-4ccd-ae28-68bb08cc1fa5', '1111 3287 1209 3422', '10', 'George Ogalo', '0', '1', '$usdId', '20.0', '50.0', 'ACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null );
       |
       |INSERT INTO accounts(id, uuid, number, user_id, name, is_main_account, account_type_id, currency_id, balance, blocked_balance, status, closed_at, last_transaction_at, created_at, created_by, updated_at, updated_by)
       |VALUES('20', 'aaa1cedf-b4ad-4daf-8019-35ff82c015cc', '1111 3287 1209 3423', '11', 'Jose Rizal', '0', '1', '$kesId', '30.0', '60.0', 'ACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null );
       |
       |INSERT INTO accounts(id, uuid, number, user_id, name, is_main_account, account_type_id, currency_id, balance, blocked_balance, status, closed_at, last_transaction_at, created_at, created_by, updated_at, updated_by)
       |VALUES('21', 'aaa0ef14-b219-42b3-8160-5f08a2bcc73a', '1111 4157 0907 3364', '11', 'Jose Rizal', '1', '1', '$usdId', '0.0', '0.0', 'INACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null );
       |
       |INSERT INTO transactions(unique_id, id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
       |VALUES(1,'1549449579', 1, 10, $kesId, 'debit', 'currency_exchange', 1250.00, 1, 'IOS_APP', 'some explanation', 'success', '$today', '2019-01-01 00:00:00');
       |
       |INSERT INTO transactions(unique_id, id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
       |VALUES(2,'1549449579', 2, $kesId, 10, 'credit', 'currency_exchange', 1250.00, 1, 'IOS_APP', 'some explanation', 'success', '$today', '2019-01-01 00:00:00');
       |
       |INSERT INTO transactions(unique_id, id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
       |VALUES(3,'1549449579', 3, $usdId, 11, 'debit', 'currency_exchange', 15.00, 1, 'IOS_APP', 'some explanation', 'success', '$today', '2019-01-01 00:00:00');
       |
       |INSERT INTO transactions(unique_id, id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
       |VALUES(4,'1549449579', 4, 11, $usdId, 'credit', 'currency_exchange', 15.00, 1, 'IOS_APP', 'some explanation', 'success', '$today', '2019-01-01 00:00:00');
       |
       |INSERT INTO transactions(unique_id, id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
       |VALUES(5,'1549449580', 1, 20, $kesId, 'debit', 'currency_exchange', 1250.00, 1, 'IOS_APP', 'some explanation', 'success', '$today', '2019-01-01 00:00:00');
       |
       |INSERT INTO transactions(unique_id, id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
       |VALUES(6,'1549449580', 2, $kesId, 20, 'credit', 'currency_exchange', 1250.00, 1, 'IOS_APP', 'some explanation', 'success', '$today', '2019-01-01 00:00:00');
       |
       |INSERT INTO transactions(unique_id, id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
       |VALUES(7,'1549449580', 3, $usdId, 21, 'debit', 'currency_exchange', 10.00, 1, 'IOS_APP', 'some explanation', 'success', '$today', '2019-01-01 00:00:00');
       |
       |INSERT INTO transactions(unique_id, id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
       |VALUES(8,'1549449580', 4, 21, $usdId, 'credit', 'currency_exchange', 10.00, 1, 'IOS_APP', 'some explanation', 'success', '$today', '2019-01-01 00:00:00');
       |
       |INSERT INTO transactions(unique_id, id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
       |VALUES(9,'1549449581', 1, 10, $euroId, 'debit', 'currency_exchange', 50.00, 1, 'IOS_APP', 'some explanation', 'success', '$today', '2019-01-01 00:00:00');
       |
       |INSERT INTO transactions(unique_id, id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
       |VALUES(10,'1549449581', 2, $euroId, 10, 'credit', 'currency_exchange', 50.00, 1, 'IOS_APP', 'some explanation', 'success', '$today', '2019-01-01 00:00:00');
       |
       |INSERT INTO transactions(unique_id, id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
       |VALUES(11,'1549449581', 3, $usdId, 10, 'debit', 'currency_exchange', 999.00, 1, 'IOS_APP', 'some explanation', 'success', '$today', '2019-01-01 00:00:00');
       |
       |INSERT INTO transactions(unique_id, id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
       |VALUES(12,'1549449581', 4, 10, $usdId, 'credit', 'currency_exchange', 999.00, 1, 'IOS_APP', 'some explanation', 'success', '$today', '2019-01-01 00:00:00');
       |
       |INSERT INTO transactions(unique_id, id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
       |VALUES(13,'1549446333', 1, 20, $kesId, 'debit', 'currency_exchange', 500.00, 1, 'IOS_APP', 'some explanation', 'success', '2018-12-26 03:07:30', '2019-01-01 00:00:00');
       |
       |INSERT INTO transactions(unique_id, id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
       |VALUES(14,'1549446333', 2, $kesId, 10, 'credit', 'currency_exchange', 500.00, 1, 'IOS_APP', 'some explanation', 'success', '2018-12-26 03:07:30', '2019-01-01 00:00:00');
       |
       |INSERT INTO transactions(unique_id, id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
       |VALUES(15,'1549446333', 3, $usdId, 21, 'debit', 'currency_exchange', 500.00, 1, 'IOS_APP', 'some explanation', 'success', '2018-12-26 03:07:30', '2019-01-01 00:00:00');
       |
       |INSERT INTO transactions(unique_id, id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
       |VALUES(16,'1549446333', 4, 21, $usdId, 'credit', 'currency_exchange', 500.00, 1, 'IOS_APP', 'some explanation', 'success', '2018-12-26 03:07:30', '2019-01-01 00:00:00');
       |
       |INSERT INTO transactions(unique_id, id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
       |VALUES(17,'1549446999', 1, 4, $usdId, 'debit', 'p2p_domestic', 200.00, 1, 'ANDROID_APP', 'some explanation', 'success', '$today', '2019-06-01 00:00:00');
       |
       |INSERT INTO transactions(unique_id, id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, created_at, updated_at)
       |VALUES(18,'1549446999', 2, $usdId, 4, 'credit', 'p2p_domestic', 200.00, 1, 'ANDROID_APP', 'some explanation', 'success', '$today', '2019-01-01 00:00:00');
       |
    """.stripMargin

  "CurrencyExchangeSqlDao countTotalCurrencyExchangeByCriteria" should {
    "return Right[Int] containing count of currencyExchange without filter" in {
      val criteria = CurrencyExchangeCriteria()

      val actualResponse = currencyExchangeSqlDao.countTotalCurrencyExchangeByCriteria(criteria)
      actualResponse.isRight mustBe true
      actualResponse.right.get mustBe 4
    }

    "return Right[Int] containing count of currencyExchange filtered by status" in {
      val criteria = CurrencyExchangeCriteria(status = Option("active"))

      val actualResponse = currencyExchangeSqlDao.countTotalCurrencyExchangeByCriteria(criteria)
      actualResponse.isRight mustBe true
      actualResponse.right.get mustBe 3
    }

    "return Right[Int] containing count of currencyExchange filtered by base_currency" in {
      val criteria = CurrencyExchangeCriteria(baseCurrency = Option(CriteriaField("base_currency", "KES", MatchTypes.Exact)))

      val actualResponse = currencyExchangeSqlDao.countTotalCurrencyExchangeByCriteria(criteria)
      actualResponse.isRight mustBe true
      actualResponse.right.get mustBe 4
    }

    "return Right[Int] containing count of currencyExchange filtered by currency_code" in {
      val criteria = CurrencyExchangeCriteria(currencyCode = Option(CriteriaField("currency_code", "EUR", MatchTypes.Exact)))

      val actualResponse = currencyExchangeSqlDao.countTotalCurrencyExchangeByCriteria(criteria)
      actualResponse.isRight mustBe true
      actualResponse.right.get mustBe 1
    }

    "return Right[Int] containing count of currencyExchange filtered by id like" in {
      val criteria = CurrencyExchangeCriteria(id = Option(CriteriaField("id", s"$usdFx", MatchTypes.Partial)))

      val actualResponse = currencyExchangeSqlDao.countTotalCurrencyExchangeByCriteria(criteria)
      actualResponse.isRight mustBe true
      actualResponse.right.get mustBe 1
    }

    "return Right[Int] containing count of currencyExchange with multiple filters" in {
      val criteria = CurrencyExchangeCriteria(
        baseCurrency = Option(CriteriaField("base_currency", "KES", MatchTypes.Exact)),
        currencyCode = Option(CriteriaField("currency_code", "C", MatchTypes.Partial)))

      val actualResponse = currencyExchangeSqlDao.countTotalCurrencyExchangeByCriteria(criteria)
      actualResponse.isRight mustBe true
      actualResponse.right.get mustBe 2
    }
  }

  "CurrencyExchangeSqlDao findById" should {
    "find existing CurrencyExchange" in {
      val actualResponse = currencyExchangeSqlDao.findById(usdFx)
      actualResponse mustBe Right(fx1)
    }

    "return error for non-existing id" in {
      val actualResponse = currencyExchangeSqlDao.findById(UUID.randomUUID())
      actualResponse.isLeft mustBe true
      actualResponse.left.get.isInstanceOf[EntityNotFoundError] mustBe true
    }

    "find existing by dbId" in {
      val actualResponse = currencyExchangeSqlDao.findById(1)
      actualResponse mustBe Right(fx1)
    }
  }

  "CurrencyExchangeSqlDao getCurrencyExchangeByCriteria" should {
    "return Seq[CurrencyExchange] containing list of currencyExchange without filter" in {
      val criteria = CurrencyExchangeCriteria()

      val actualResponse = currencyExchangeSqlDao.getCurrencyExchangeByCriteria(criteria, Nil, None, None)
      actualResponse.isRight mustBe true
      actualResponse.right.get mustBe Seq(fx1, fx2, fx3, fx4)
    }

    "return Seq[CurrencyExchange] containing list of currencyExchange with status filter" in {
      val criteria = CurrencyExchangeCriteria(status = Option("active"))

      val actualResponse = currencyExchangeSqlDao.getCurrencyExchangeByCriteria(criteria, Nil, None, None)
      actualResponse.isRight mustBe true
      actualResponse.right.get mustBe Seq(fx1, fx2, fx3)
    }

    "return Seq[CurrencyExchange] containing list of currencyExchange filtered by base_currency" in {
      val criteria = CurrencyExchangeCriteria(baseCurrency = Option(CriteriaField("base_currency", "KES", MatchTypes.Exact)))

      val actualResponse = currencyExchangeSqlDao.getCurrencyExchangeByCriteria(criteria, Nil, None, None)
      actualResponse.isRight mustBe true
      actualResponse.right.get mustBe Seq(fx1, fx2, fx3, fx4)
    }

    "return Seq[CurrencyExchange] containing list of currencyExchange filtered by currency_code" in {
      val criteria = CurrencyExchangeCriteria(currencyCode = Option(CriteriaField("currency_code", "EUR", MatchTypes.Exact)))

      val actualResponse = currencyExchangeSqlDao.getCurrencyExchangeByCriteria(criteria, Nil, None, None)
      actualResponse.isRight mustBe true
      actualResponse.right.get mustBe Seq(fx2)
    }

    "return Seq[CurrencyExchange] containing list of currencyExchange filtered by provider" in {
      val criteria = CurrencyExchangeCriteria(provider = Option(CriteriaField("provider_name", "Ebury", MatchTypes.Exact)))

      val actualResponse = currencyExchangeSqlDao.getCurrencyExchangeByCriteria(criteria, Nil, None, None)
      actualResponse.isRight mustBe true
      actualResponse.right.get mustBe Seq(fx3, fx4)
    }

    "return Seq[CurrencyExchange] containing list of currencyExchange filtered by id like" in {
      val criteria = CurrencyExchangeCriteria(id = Option(CriteriaField("id", s"$usdFx", MatchTypes.Partial)))

      val actualResponse = currencyExchangeSqlDao.getCurrencyExchangeByCriteria(criteria, Nil, None, None)
      actualResponse.isRight mustBe true
      actualResponse.right.get mustBe Seq(fx1)
    }

    "return Seq[CurrencyExchange] containing list of currencyExchange with multiple filters" in {
      val criteria = CurrencyExchangeCriteria(
        baseCurrency = Option(CriteriaField("base_currency", "KES", MatchTypes.Exact)),
        currencyCode = Option(CriteriaField("currency_code", "C", MatchTypes.Partial)))

      val actualResponse = currencyExchangeSqlDao.getCurrencyExchangeByCriteria(criteria, Nil, None, None)
      actualResponse.isRight mustBe true
      actualResponse.right.get mustBe Seq(fx3, fx4)
    }

    "return Seq[CurrencyExchange] containing list of currencyExchange without filter with ordering" in {
      val criteria = CurrencyExchangeCriteria()
      val ordering = Seq(Ordering("status", Ordering.ASC), Ordering("currency_code", Ordering.ASC))

      val actualResponse = currencyExchangeSqlDao.getCurrencyExchangeByCriteria(criteria, ordering, None, None)
      actualResponse.isRight mustBe true
      actualResponse.right.get mustBe Seq(fx3, fx2, fx1, fx4)
    }

    "return Seq[CurrencyExchange] containing list of currencyExchange without filter order by balance and rate" in {
      val criteria = CurrencyExchangeCriteria()
      val ordering = Seq(Ordering("balance", Ordering.DESC), Ordering("rate", Ordering.DESC))

      val actualResponse = currencyExchangeSqlDao.getCurrencyExchangeByCriteria(criteria, ordering, None, None)
      actualResponse.isRight mustBe true
      actualResponse.right.get mustBe Seq(fx1, fx2, fx4, fx3)
    }

    "return Seq[CurrencyExchange] containing list of currencyExchange without filter order by provider and rate" in {
      val criteria = CurrencyExchangeCriteria()
      val ordering = Seq(Ordering("provider_name", Ordering.ASC), Ordering("rate", Ordering.DESC), Ordering("updated_at", Ordering.ASC))

      val actualResponse = currencyExchangeSqlDao.getCurrencyExchangeByCriteria(criteria, ordering, None, None)
      actualResponse.isRight mustBe true
      actualResponse.right.get mustBe Seq(fx2, fx1, fx4, fx3)
    }
  }

  "CurrencyExchangeSqlDao updateCurrencyExchangeStatus" should {
    "return true if update successfully" in {
      val response = currencyExchangeSqlDao.updateCurrencyExchangeStatus(fx4.id.toInt, "active")
      response.isRight mustBe true
      response.right.get mustBe true

      val actualResponse = currencyExchangeSqlDao.findById(UUID.fromString(fx4.uuid))
      actualResponse.isRight mustBe true
      actualResponse.right.get.status mustBe "active"

    }
  }

  "CurrencyExchangeSqlDao getDailyAmount" should {
    "return correct Some(dailyAmount) if exists" in {
      val kesToUsd = currencyExchangeSqlDao.getDailyAmount(usdId, kesId)
      kesToUsd mustBe BigDecimal(25.00).some.asRight[DaoError]

      val euroToUsd = currencyExchangeSqlDao.getDailyAmount(usdId, euroId)
      euroToUsd mustBe BigDecimal(999.00).some.asRight[DaoError]
    }
    "return correct None if does not exists" in {
      val cnyToUsd = currencyExchangeSqlDao.getDailyAmount(usdId, cnyId)
      cnyToUsd mustBe none[BigDecimal].asRight[DaoError]
    }

  }
}
