package tech.pegb.backoffice.dao

import java.time.LocalDateTime
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import play.api.test.Injecting
import tech.pegb.backoffice.dao.account.sql.AccountSqlDao
import tech.pegb.backoffice.dao.model.GroupOperationTypes.Day
import tech.pegb.backoffice.dao.model.MatchTypes.{GreaterOrEqual, InclusiveBetween, LesserOrEqual}
import tech.pegb.backoffice.dao.model.{CriteriaField, GroupingField, Ordering}
import tech.pegb.backoffice.dao.transaction.dto.{TransactionAggregation, TransactionCriteria}
import tech.pegb.backoffice.dao.transaction.entity.Transaction
import tech.pegb.backoffice.dao.transaction.sql.TransactionSqlDao
import tech.pegb.backoffice.util.AppConfig
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.core.PegBTestApp

import scala.math.BigDecimal.RoundingMode

class TransactionSqlDaoSpec extends PegBTestApp with MockFactory with Injecting {

  lazy val transactionSqlDao = inject[TransactionSqlDao]
  val appConfig = inject[AppConfig]

  override def initSql =
    s"""
       |INSERT INTO account_types(id, account_type_name, description, created_at, updated_at, is_active)
       |VALUES('1', 'WALLET', 'standard account type for individual users', now(), null, 1);
       |
       |INSERT INTO currencies(id, currency_name, description, created_at, updated_at, is_active)
       |VALUES('1', 'AED', 'default currency for UAE', now(), null, 1);
       |
       |INSERT INTO currencies(id, currency_name, description, created_at, updated_at, is_active)
       |VALUES('2', 'KES', 'default currency for KENYA', now(), null, 1);
       |
       |INSERT INTO users(id, uuid, username, password, email, subscription, type, status, tier, segment, activated_at, password_updated_at, created_at, created_by, updated_at, updated_by)
       |VALUES('1', 'bcc32571-cf16-4abc-ac38-38d58f9cbab5', 'PegasiB21', null, null, null, 'individual', null, null, null, now(), null, now(), 'SuperAdmin', null, null);
       |
       |INSERT INTO users(id, uuid, username, password, email, subscription, type, status, tier, segment, activated_at, password_updated_at, created_at, created_by, updated_at, updated_by)
       |VALUES('2', '4634db00-2b61-4a23-8348-35f0030c3b1d', 'ILoveU3000', null, null, null, 'business', null, null, null, now(), null, now(), 'SuperAdmin', null, null);
       |
       |INSERT INTO business_users(id, uuid, user_id, business_name, brand_name, business_category, business_type,
       | registration_number, tax_number, registration_date, currency_id, collection_account_id, distribution_account_id, default_contact_id,
       | created_by, updated_by, created_at, updated_at) VALUES
       | (1, 'bf43d63e-6348-4138-9387-298f54cf6857',1,'Universal catering co','Costa Coffee DSO','Restaurant-5812','Merchant', '213/564654EE','A2135468977M',now(), 1,
       | 1,1,1,'Ujali',null,now(), null);
       |
       |INSERT INTO individual_users(msisdn, user_id, type, name, fullname, gender, company, birthdate, birth_place, nationality, occupation, employer, created_at, created_by, updated_at, updated_by) VALUES
       |('971544465329', '1', 'type_one', 'Alice', 'Alice Smith', 'F', 'PegB', '1990-01-01', 'Dubai', 'Emirati', 'Manager', 'EMAAR', '2018-10-01 00:00:00', 'SuperUser', null, null);
       |
       |INSERT INTO accounts(id, uuid, number, user_id, name, is_main_account, account_type_id, currency_id, balance, blocked_balance, status, closed_at, last_transaction_at, created_at, created_by, updated_at, updated_by)
       |VALUES('1', '1c15bdb9-5d42-4a84-922a-0d01f8e5de71', '4716 4157 0907 3361', '1', 'George Ogalo', '1', '1', '2', '0.0', '0.0', 'INACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null );
       |
       |INSERT INTO accounts(id, uuid, number, user_id, name, is_main_account, account_type_id, currency_id, balance, blocked_balance, status, closed_at, last_transaction_at, created_at, created_by, updated_at, updated_by)
       |VALUES('2', 'c86eb85f-70f8-4ccd-ae28-68bb08cc1fa5', '8912 3287 1209 3422', '2', 'Ujali Tyagi', '0', '1', '1', '20.0', '50.0', 'ACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null );
       |
       |INSERT INTO accounts(id, uuid, number, user_id, name, is_main_account, account_type_id, currency_id, balance, blocked_balance, status, closed_at, last_transaction_at, created_at, created_by, updated_at, updated_by)
       |VALUES('3', '2c41cedf-b4ad-4daf-8019-35ff82c015cc', '8912 3287 1209 3423', '2', 'Ujali Tyagi', '0', '1', '1', '30.0', '60.0', 'ACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null );
       |
       |INSERT INTO accounts(id, uuid, number, user_id, name, is_main_account, account_type_id, currency_id, balance, blocked_balance, status, closed_at, last_transaction_at, created_at, created_by, updated_at, updated_by)
       |VALUES('4', 'b370ef14-b219-42b3-8160-5f08a2bcc73a', '4716 4157 0907 3364', '1', 'George Ogalo', '1', '1', '1', '0.0', '0.0', 'INACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null );
       |
       |
       |INSERT INTO users(id, uuid, username, password, email, activated_at, password_updated_at, subscription, type, status, tier, segment, created_by, created_at, updated_by, updated_at)
       |VALUES(3, 'a57291af-c840-4ab1-bb4d-4baed930ed58', 'user01', 'pword', 'provider@gmail.com',  '2018-10-01 00:00:00', null, 'sub_one',  'type_one', 'ACTIVE', null, null, 'SuperUser', '2018-10-01 00:00:00', null, null);
       |
       |INSERT INTO providers
       |(id, user_id, service_id, name, transaction_type, icon, label, pg_institution_id,
       |utility_payment_type, utility_min_payment_amount, utility_max_payment_amount,
       |is_active, created_by, updated_by, created_at, updated_at)
       |VALUES
       |('3', '3', null, 'Mashreq', 'txn type 1', 'icon 1', 'label 1', '1', null, '0.1', '1000', '1', 'unit test', 'unit test', now(), now()),
       |('4', '3', null, 'other-party', 'txn type 2', 'icon 2', 'label 2', '1', null, '0.1', '1000', '1', 'unit test', 'unit test', now(), now());
       |
       |
       |INSERT INTO transactions(unique_id, id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, explanation, status, provider_id, created_at, updated_at)
       |VALUES
       |(1,'1549449579', 1, 1, 2, 'debit', 'p2p_domestic', 1250.000, 1, 'IOS_APP', 'some explanation', 'success', '3', '2018-12-25 00:00:00', '2019-01-01 00:00:00'),
       |
       |(2,'1549449579', 1, 2, 1, 'credit', 'p2p_domestic', 1250.000, 1, 'IOS_APP', 'some explanation', 'success', '3', '2018-12-25 00:00:00', '2019-01-01 00:00:00'),
       |
       |(3,'1549446333', 1, 1, 3, 'debit', 'merchant_payment', 500.00, 1, 'IOS_APP', 'some explanation', 'success', '3', '2018-12-26 03:07:30', '2019-01-01 00:00:00'),
       |
       |(4,'1549446333', 1, 3, 1, 'credit', 'merchant_payment', 500.00, 1, 'IOS_APP', 'some explanation', 'success', '3', '2018-12-26 03:07:30', '2019-01-01 00:00:00'),
       |
       |(5,'1549446999', 1, 4, 1, 'debit', 'p2p_domestic', 200.00, 1, 'ANDROID_APP', 'some explanation', 'success', '3', '2018-12-25 14:27:30', '2019-06-01 00:00:00'),
       |
       |(6,'1549446999', 1, 1, 4, 'credit', 'p2p_domestic', 200.00, 1, 'ANDROID_APP', 'some explanation', 'success', '3', '2018-12-25 14:27:30', '2019-01-01 00:00:00');
       |
    """.stripMargin

  "TransactionSqlDao countTotalTransactionsByCriteria" should {

    "return Right[Int] containing count of transactions filtered by date and id" in {
      val criteria = TransactionCriteria(
        accountId = CriteriaField(TransactionSqlDao.cPrimaryAccountUuid, "1c15bdb9-5d42-4a84-922a-0d01f8e5de71").some,
        createdAt = CriteriaField(TransactionSqlDao.cCreatedAt, (
          LocalDateTime.of(2018, 12, 25, 0, 0, 0, 0),
          LocalDateTime.of(2018, 12, 31, 0, 0, 0, 0)), InclusiveBetween).some)

      val actualResponse = transactionSqlDao.countTotalTransactionsByCriteria(criteria)

      actualResponse.isRight mustBe true
      actualResponse.right.get mustBe 3
    }

    "return Right[Int] containing count of transactions will all filter present" in {
      val criteria = TransactionCriteria(
        accountId = CriteriaField(TransactionSqlDao.cPrimaryAccountUuid, "1c15bdb9-5d42-4a84-922a-0d01f8e5de71").some,
        createdAt = CriteriaField(TransactionSqlDao.cCreatedAt, (
          LocalDateTime.of(2018, 12, 25, 0, 0, 0, 0),
          LocalDateTime.of(2018, 12, 31, 0, 0, 0, 0)), InclusiveBetween).some,
        transactionType = CriteriaField(TransactionSqlDao.cType, "p2p_domestic").some,
        channel = CriteriaField(TransactionSqlDao.cChannel, "IOS_APP").some,
        status = CriteriaField(TransactionSqlDao.cStatus, "success").some)

      val actualResponse = transactionSqlDao.countTotalTransactionsByCriteria(criteria)

      actualResponse.isRight mustBe true
      actualResponse.right.get mustBe 1
    }

    "return min(realcount, cap-count)" in {
      val criteria = TransactionCriteria(
        createdAt = CriteriaField(TransactionSqlDao.cCreatedAt, (
          LocalDateTime.of(2018, 12, 25, 0, 0, 0, 0),
          LocalDateTime.of(2018, 12, 31, 0, 0, 0, 0)), InclusiveBetween).some)

      val actualResponse = transactionSqlDao.countTotalTransactionsByCriteria(criteria)

      actualResponse.isRight mustBe true
      actualResponse.right.get mustBe appConfig.PaginationMaxLimit
    }

  }

  "TransactionSqlDao getTransactionsByCriteria" should {

    "return Right[Seq[Transaction] containing transactions filtered by date and id" in {
      val criteria = TransactionCriteria(
        accountId = CriteriaField(TransactionSqlDao.cPrimaryAccountUuid, "1c15bdb9-5d42-4a84-922a-0d01f8e5de71").some,
        createdAt = CriteriaField(TransactionSqlDao.cCreatedAt, (
          LocalDateTime.of(2018, 12, 25, 0, 0, 0, 0),
          LocalDateTime.of(2018, 12, 31, 0, 0, 0, 0)), InclusiveBetween).some)
      val actualResponse = transactionSqlDao.getTransactionsByCriteria(criteria, Nil, None, None)

      val expected = Seq(
        Transaction(
          id = "1549449579",
          uniqueId = "1",
          sequence = 1L,
          primaryAccountInternalId = 1,
          primaryAccountUuid = UUID.fromString("1c15bdb9-5d42-4a84-922a-0d01f8e5de71"),
          primaryAccountName = "George Ogalo",
          primaryAccountNumber = "4716 4157 0907 3361",
          primaryUserUuid = UUID.fromString("bcc32571-cf16-4abc-ac38-38d58f9cbab5"),
          primaryAccountType = "WALLET",

          primaryUserType = Some("individual"),
          primaryUsername = Some("PegasiB21"),
          primaryIndividualUsersName = Some("Alice"),
          primaryIndividualUsersFullname = Some("Alice Smith"),
          primaryBusinessUsersBusinessName = Some("Universal catering co"),
          primaryBusinessUsersBrandName = Some("Costa Coffee DSO"),

          secondaryAccountInternalId = 2,
          secondaryAccountUuid = UUID.fromString("c86eb85f-70f8-4ccd-ae28-68bb08cc1fa5"),
          secondaryAccountName = "Ujali Tyagi",
          secondaryAccountNumber = "8912 3287 1209 3422",
          secondaryUserUuid = UUID.fromString("4634db00-2b61-4a23-8348-35f0030c3b1d"),
          direction = "debit".some,
          `type` = "p2p_domestic".some,
          amount = BigDecimal(1250.00).setScale(2, RoundingMode.HALF_DOWN).some,
          currency = "AED".some,
          exchangedCurrency = None,
          channel = "IOS_APP".some,
          explanation = "some explanation".some,
          effectiveRate = none,
          costRate = none,
          status = "success".some,
          instrument = none,
          createdAt = LocalDateTime.of(2018, 12, 25, 0, 0, 0, 0).some,
          updatedAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0).some,
          primaryAccountPreviousBalance = none,
          secondaryAccountPreviousBalance = none,
          provider = Some("Mashreq")),
        Transaction(
          id = "1549446333",
          uniqueId = "3",
          sequence = 1L,
          primaryAccountInternalId = 1,
          primaryAccountUuid = UUID.fromString("1c15bdb9-5d42-4a84-922a-0d01f8e5de71"),
          primaryAccountName = "George Ogalo",
          primaryAccountNumber = "4716 4157 0907 3361",
          primaryUserUuid = UUID.fromString("bcc32571-cf16-4abc-ac38-38d58f9cbab5"),
          primaryAccountType = "WALLET",

          primaryUserType = Some("individual"),
          primaryUsername = Some("PegasiB21"),
          primaryIndividualUsersName = Some("Alice"),
          primaryIndividualUsersFullname = Some("Alice Smith"),
          primaryBusinessUsersBusinessName = Some("Universal catering co"),
          primaryBusinessUsersBrandName = Some("Costa Coffee DSO"),

          secondaryAccountInternalId = 3,
          secondaryAccountUuid = UUID.fromString("2c41cedf-b4ad-4daf-8019-35ff82c015cc"),
          secondaryAccountName = "Ujali Tyagi",
          secondaryAccountNumber = "8912 3287 1209 3423",
          secondaryUserUuid = UUID.fromString("4634db00-2b61-4a23-8348-35f0030c3b1d"),
          direction = "debit".some,
          `type` = "merchant_payment".some,
          amount = BigDecimal(500.00).setScale(2, RoundingMode.HALF_DOWN).some,
          currency = "AED".some,
          exchangedCurrency = None,
          channel = "IOS_APP".some,
          explanation = "some explanation".some,
          effectiveRate = none,
          costRate = none,
          status = "success".some,
          instrument = none,
          createdAt = LocalDateTime.of(2018, 12, 26, 3, 7, 30, 0).some,
          updatedAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0).some,
          primaryAccountPreviousBalance = none,
          secondaryAccountPreviousBalance = none,
          provider = Some("Mashreq")),
        Transaction(
          id = "1549446999",
          uniqueId = "6",
          sequence = 1L,
          primaryAccountInternalId = 1,
          primaryAccountUuid = UUID.fromString("1c15bdb9-5d42-4a84-922a-0d01f8e5de71"),
          primaryAccountName = "George Ogalo",
          primaryAccountNumber = "4716 4157 0907 3361",
          primaryUserUuid = UUID.fromString("bcc32571-cf16-4abc-ac38-38d58f9cbab5"),
          primaryAccountType = "WALLET",

          primaryUserType = Some("individual"),
          primaryUsername = Some("PegasiB21"),
          primaryIndividualUsersName = Some("Alice"),
          primaryIndividualUsersFullname = Some("Alice Smith"),
          primaryBusinessUsersBusinessName = Some("Universal catering co"),
          primaryBusinessUsersBrandName = Some("Costa Coffee DSO"),

          secondaryAccountInternalId = 4,
          secondaryAccountUuid = UUID.fromString("b370ef14-b219-42b3-8160-5f08a2bcc73a"),
          secondaryAccountName = "George Ogalo",
          secondaryAccountNumber = "4716 4157 0907 3364",
          secondaryUserUuid = UUID.fromString("bcc32571-cf16-4abc-ac38-38d58f9cbab5"),
          direction = "credit".some,
          `type` = "p2p_domestic".some,
          amount = BigDecimal(200.00).setScale(2, RoundingMode.HALF_DOWN).some,
          currency = "AED".some,
          exchangedCurrency = None,
          channel = "ANDROID_APP".some,
          explanation = "some explanation".some,
          effectiveRate = none,
          costRate = none,
          status = "success".some,
          instrument = none,
          createdAt = LocalDateTime.of(2018, 12, 25, 14, 27, 30, 0).some,
          updatedAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0).some,
          primaryAccountPreviousBalance = none,
          secondaryAccountPreviousBalance = none,
          provider = Some("Mashreq")))

      actualResponse.isRight mustBe true
      actualResponse.right.get mustBe expected
    }

    "return capped Right[Seq[Transaction] when limit > pagination.max-cap" in {
      val criteria = TransactionCriteria(
        createdAt = CriteriaField(TransactionSqlDao.cCreatedAt, (
          LocalDateTime.of(2018, 12, 25, 0, 0, 0, 0),
          LocalDateTime.of(2018, 12, 31, 0, 0, 0, 0)), InclusiveBetween).some)

      val ordering = Seq(Ordering(TransactionSqlDao.cCreatedAt, Ordering.ASC), Ordering("direction", Ordering.DESC))

      val actualResponse = transactionSqlDao.getTransactionsByCriteria(criteria, ordering, Some(appConfig.PaginationMaxLimit + 1), None)

      val expected = Seq(
        Transaction(
          id = "1549449579",
          uniqueId = "1",
          sequence = 1L,
          primaryAccountInternalId = 1,
          primaryAccountUuid = UUID.fromString("1c15bdb9-5d42-4a84-922a-0d01f8e5de71"),
          primaryAccountName = "George Ogalo",
          primaryAccountNumber = "4716 4157 0907 3361",
          primaryUserUuid = UUID.fromString("bcc32571-cf16-4abc-ac38-38d58f9cbab5"),
          primaryAccountType = "WALLET",

          primaryUserType = Some("individual"),
          primaryUsername = Some("PegasiB21"),
          primaryIndividualUsersName = Some("Alice"),
          primaryIndividualUsersFullname = Some("Alice Smith"),
          primaryBusinessUsersBusinessName = Some("Universal catering co"),
          primaryBusinessUsersBrandName = Some("Costa Coffee DSO"),

          secondaryAccountInternalId = 2,
          secondaryAccountUuid = UUID.fromString("c86eb85f-70f8-4ccd-ae28-68bb08cc1fa5"),
          secondaryAccountName = "Ujali Tyagi",
          secondaryAccountNumber = "8912 3287 1209 3422",
          secondaryUserUuid = UUID.fromString("4634db00-2b61-4a23-8348-35f0030c3b1d"),
          direction = "debit".some,
          `type` = "p2p_domestic".some,
          amount = BigDecimal(1250.00).setScale(2, RoundingMode.HALF_DOWN).some,
          currency = "AED".some,
          exchangedCurrency = None,
          channel = "IOS_APP".some,
          explanation = "some explanation".some,
          effectiveRate = none,
          costRate = none,
          status = "success".some,
          instrument = none,
          createdAt = LocalDateTime.of(2018, 12, 25, 0, 0, 0, 0).some,
          updatedAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0).some,
          primaryAccountPreviousBalance = none,
          secondaryAccountPreviousBalance = none,
          provider = Some("Mashreq")),

        Transaction(
          id = "1549449579",
          uniqueId = "2",
          sequence = 1L,
          primaryAccountInternalId = 2,
          primaryAccountUuid = UUID.fromString("c86eb85f-70f8-4ccd-ae28-68bb08cc1fa5"),
          primaryAccountName = "Ujali Tyagi",
          primaryAccountNumber = "8912 3287 1209 3422",
          primaryUserUuid = UUID.fromString("4634db00-2b61-4a23-8348-35f0030c3b1d"),
          primaryAccountType = "WALLET",

          primaryUserType = Some("business"),
          primaryUsername = Some("ILoveU3000"),
          primaryBusinessUsersBusinessName = None,
          primaryBusinessUsersBrandName = None,

          secondaryAccountInternalId = 1,
          secondaryAccountUuid = UUID.fromString("1c15bdb9-5d42-4a84-922a-0d01f8e5de71"),
          secondaryAccountName = "George Ogalo",
          secondaryAccountNumber = "4716 4157 0907 3361",
          secondaryUserUuid = UUID.fromString("bcc32571-cf16-4abc-ac38-38d58f9cbab5"),
          direction = "credit".some,
          `type` = "p2p_domestic".some,
          amount = BigDecimal(1250.00).setScale(2, RoundingMode.HALF_DOWN).some,
          currency = "AED".some,
          exchangedCurrency = None,
          channel = "IOS_APP".some,
          explanation = "some explanation".some,
          effectiveRate = none,
          costRate = none,
          status = "success".some,
          instrument = none,
          createdAt = LocalDateTime.of(2018, 12, 25, 0, 0, 0, 0).some,
          updatedAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0).some,
          primaryAccountPreviousBalance = none,
          secondaryAccountPreviousBalance = none,
          provider = Some("Mashreq")),
        Transaction(
          id = "1549446999",
          uniqueId = "5",
          sequence = 1L,
          primaryAccountInternalId = 4,
          primaryAccountUuid = UUID.fromString("b370ef14-b219-42b3-8160-5f08a2bcc73a"),
          primaryAccountName = "George Ogalo",
          primaryAccountNumber = "4716 4157 0907 3364",
          primaryUserUuid = UUID.fromString("bcc32571-cf16-4abc-ac38-38d58f9cbab5"),
          primaryAccountType = "WALLET",

          primaryUserType = Some("individual"),
          primaryUsername = Some("PegasiB21"),
          primaryIndividualUsersName = Some("Alice"),
          primaryIndividualUsersFullname = Some("Alice Smith"),
          primaryBusinessUsersBusinessName = Some("Universal catering co"),
          primaryBusinessUsersBrandName = Some("Costa Coffee DSO"),

          secondaryAccountInternalId = 1,
          secondaryAccountUuid = UUID.fromString("1c15bdb9-5d42-4a84-922a-0d01f8e5de71"),
          secondaryAccountName = "George Ogalo",
          secondaryAccountNumber = "4716 4157 0907 3361",
          secondaryUserUuid = UUID.fromString("bcc32571-cf16-4abc-ac38-38d58f9cbab5"),
          direction = "debit".some,
          `type` = "p2p_domestic".some,
          amount = BigDecimal(200.00).setScale(2, RoundingMode.HALF_DOWN).some,
          currency = "AED".some,
          exchangedCurrency = None,
          channel = "ANDROID_APP".some,
          explanation = "some explanation".some,
          effectiveRate = none,
          costRate = none,
          status = "success".some,
          instrument = none,
          createdAt = LocalDateTime.of(2018, 12, 25, 14, 27, 30, 0).some,
          updatedAt = LocalDateTime.of(2019, 6, 1, 0, 0, 0).some,
          primaryAccountPreviousBalance = none,
          secondaryAccountPreviousBalance = none,
          provider = Some("Mashreq")),
        Transaction(
          id = "1549446999",
          uniqueId = "6",
          sequence = 1L,
          primaryAccountInternalId = 1,
          primaryAccountUuid = UUID.fromString("1c15bdb9-5d42-4a84-922a-0d01f8e5de71"),
          primaryAccountName = "George Ogalo",
          primaryAccountNumber = "4716 4157 0907 3361",
          primaryUserUuid = UUID.fromString("bcc32571-cf16-4abc-ac38-38d58f9cbab5"),
          primaryAccountType = "WALLET",

          primaryUserType = Some("individual"),
          primaryUsername = Some("PegasiB21"),
          primaryIndividualUsersName = Some("Alice"),
          primaryIndividualUsersFullname = Some("Alice Smith"),
          primaryBusinessUsersBusinessName = Some("Universal catering co"),
          primaryBusinessUsersBrandName = Some("Costa Coffee DSO"),

          secondaryAccountInternalId = 4,
          secondaryAccountUuid = UUID.fromString("b370ef14-b219-42b3-8160-5f08a2bcc73a"),
          secondaryAccountName = "George Ogalo",
          secondaryAccountNumber = "4716 4157 0907 3364",
          secondaryUserUuid = UUID.fromString("bcc32571-cf16-4abc-ac38-38d58f9cbab5"),
          direction = "credit".some,
          `type` = "p2p_domestic".some,
          amount = BigDecimal(200.00).setScale(2, RoundingMode.HALF_DOWN).some,
          currency = "AED".some,
          exchangedCurrency = None,
          channel = "ANDROID_APP".some,
          explanation = "some explanation".some,
          effectiveRate = none,
          costRate = none,
          status = "success".some,
          instrument = none,
          createdAt = LocalDateTime.of(2018, 12, 25, 14, 27, 30, 0).some,
          updatedAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0).some,
          primaryAccountPreviousBalance = none,
          secondaryAccountPreviousBalance = none,
          provider = Some("Mashreq")),
        Transaction(
          id = "1549446333",
          uniqueId = "3",
          sequence = 1L,
          primaryAccountInternalId = 1,
          primaryAccountUuid = UUID.fromString("1c15bdb9-5d42-4a84-922a-0d01f8e5de71"),
          primaryAccountName = "George Ogalo",
          primaryAccountNumber = "4716 4157 0907 3361",
          primaryUserUuid = UUID.fromString("bcc32571-cf16-4abc-ac38-38d58f9cbab5"),
          primaryAccountType = "WALLET",

          primaryUserType = Some("individual"),
          primaryUsername = Some("PegasiB21"),
          primaryIndividualUsersName = Some("Alice"),
          primaryIndividualUsersFullname = Some("Alice Smith"),
          primaryBusinessUsersBusinessName = Some("Universal catering co"),
          primaryBusinessUsersBrandName = Some("Costa Coffee DSO"),

          secondaryAccountInternalId = 3,
          secondaryAccountUuid = UUID.fromString("2c41cedf-b4ad-4daf-8019-35ff82c015cc"),
          secondaryAccountName = "Ujali Tyagi",
          secondaryAccountNumber = "8912 3287 1209 3423",
          secondaryUserUuid = UUID.fromString("4634db00-2b61-4a23-8348-35f0030c3b1d"),
          direction = "debit".some,
          `type` = "merchant_payment".some,
          amount = BigDecimal(500.00).setScale(2, RoundingMode.HALF_DOWN).some,
          currency = "AED".some,
          exchangedCurrency = None,
          channel = "IOS_APP".some,
          explanation = "some explanation".some,
          effectiveRate = none,
          costRate = none,
          status = "success".some,
          instrument = none,
          createdAt = LocalDateTime.of(2018, 12, 26, 3, 7, 30, 0).some,
          updatedAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0).some,
          primaryAccountPreviousBalance = none,
          secondaryAccountPreviousBalance = none,
          provider = Some("Mashreq")))

      actualResponse.isRight mustBe true
      actualResponse.right.get.size mustBe appConfig.PaginationMaxLimit
      actualResponse.right.get mustBe expected
    }

    "return Right[Seq[Transaction] containing transactions will all filter" in {
      val criteria = TransactionCriteria(
        accountId = CriteriaField(TransactionSqlDao.cPrimaryAccountUuid, "1c15bdb9-5d42-4a84-922a-0d01f8e5de71").some,
        createdAt = CriteriaField(TransactionSqlDao.cCreatedAt, (
          LocalDateTime.of(2018, 12, 25, 0, 0, 0, 0),
          LocalDateTime.of(2018, 12, 31, 0, 0, 0, 0)), InclusiveBetween).some,
        transactionType = CriteriaField(TransactionSqlDao.cType, "p2p_domestic").some,
        channel = CriteriaField(TransactionSqlDao.cChannel, "IOS_APP").some,
        status = CriteriaField(TransactionSqlDao.cStatus, "success").some)

      val actualResponse = transactionSqlDao.getTransactionsByCriteria(criteria, Nil, None, None)

      val expected = Seq(
        Transaction(
          id = "1549449579",
          uniqueId = "1",
          sequence = 1L,
          primaryAccountInternalId = 1,
          primaryAccountUuid = UUID.fromString("1c15bdb9-5d42-4a84-922a-0d01f8e5de71"),
          primaryAccountName = "George Ogalo",
          primaryAccountNumber = "4716 4157 0907 3361",
          primaryUserUuid = UUID.fromString("bcc32571-cf16-4abc-ac38-38d58f9cbab5"),
          primaryAccountType = "WALLET",

          primaryUserType = Some("individual"),
          primaryUsername = Some("PegasiB21"),
          primaryIndividualUsersName = Some("Alice"),
          primaryIndividualUsersFullname = Some("Alice Smith"),
          primaryBusinessUsersBusinessName = Some("Universal catering co"),
          primaryBusinessUsersBrandName = Some("Costa Coffee DSO"),

          secondaryAccountInternalId = 2,
          secondaryAccountUuid = UUID.fromString("c86eb85f-70f8-4ccd-ae28-68bb08cc1fa5"),
          secondaryAccountName = "Ujali Tyagi",
          secondaryAccountNumber = "8912 3287 1209 3422",
          secondaryUserUuid = UUID.fromString("4634db00-2b61-4a23-8348-35f0030c3b1d"),
          direction = "debit".some,
          `type` = "p2p_domestic".some,
          amount = BigDecimal(1250.00).setScale(2, RoundingMode.HALF_DOWN).some,
          currency = "AED".some,
          exchangedCurrency = None,
          channel = "IOS_APP".some,
          explanation = "some explanation".some,
          effectiveRate = none,
          costRate = none,
          status = "success".some,
          instrument = none,
          createdAt = LocalDateTime.of(2018, 12, 25, 0, 0, 0, 0).some,
          updatedAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0).some,
          primaryAccountPreviousBalance = none,
          secondaryAccountPreviousBalance = none,
          provider = Some("Mashreq")))

      actualResponse.isRight mustBe true
      actualResponse.right.get mustBe expected
    }

    "return Right[Seq[Transaction] containing transactions with ordering and offset" in {
      val criteria = TransactionCriteria(
        accountId = CriteriaField(TransactionSqlDao.cPrimaryAccountUuid, "1c15bdb9-5d42-4a84-922a-0d01f8e5de71").some,
        createdAt = CriteriaField(TransactionSqlDao.cCreatedAt, (
          LocalDateTime.of(2018, 12, 25, 0, 0, 0, 0),
          LocalDateTime.of(2018, 12, 31, 0, 0, 0, 0)), InclusiveBetween).some)

      val ordering = Seq(Ordering("created_at", Ordering.DESC), Ordering("channel", Ordering.ASC), Ordering("direction", Ordering.ASC))

      val actualResponse = transactionSqlDao.getTransactionsByCriteria(criteria, ordering, Some(2), Some(0))

      val expected = Seq(
        Transaction(
          id = "1549446333",
          uniqueId = "3",
          sequence = 1L,
          primaryAccountInternalId = 1,
          primaryAccountUuid = UUID.fromString("1c15bdb9-5d42-4a84-922a-0d01f8e5de71"),
          primaryAccountName = "George Ogalo",
          primaryAccountNumber = "4716 4157 0907 3361",
          primaryUserUuid = UUID.fromString("bcc32571-cf16-4abc-ac38-38d58f9cbab5"),
          primaryAccountType = "WALLET",

          primaryUserType = Some("individual"),
          primaryUsername = Some("PegasiB21"),
          primaryIndividualUsersName = Some("Alice"),
          primaryIndividualUsersFullname = Some("Alice Smith"),
          primaryBusinessUsersBusinessName = Some("Universal catering co"),
          primaryBusinessUsersBrandName = Some("Costa Coffee DSO"),

          secondaryAccountInternalId = 3,
          secondaryAccountUuid = UUID.fromString("2c41cedf-b4ad-4daf-8019-35ff82c015cc"),
          secondaryAccountName = "Ujali Tyagi",
          secondaryAccountNumber = "8912 3287 1209 3423",
          secondaryUserUuid = UUID.fromString("4634db00-2b61-4a23-8348-35f0030c3b1d"),
          direction = "debit".some,
          `type` = "merchant_payment".some,
          amount = BigDecimal(500.00).setScale(2, RoundingMode.HALF_DOWN).some,
          currency = "AED".some,
          exchangedCurrency = None,
          channel = "IOS_APP".some,
          explanation = "some explanation".some,
          effectiveRate = none,
          costRate = none,
          status = "success".some,
          instrument = none,
          createdAt = LocalDateTime.of(2018, 12, 26, 3, 7, 30, 0).some,
          updatedAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0).some,
          primaryAccountPreviousBalance = none,
          secondaryAccountPreviousBalance = none,
          provider = Some("Mashreq")),
        Transaction(
          id = "1549446999",
          uniqueId = "6",
          sequence = 1L,
          primaryAccountInternalId = 1,
          primaryAccountUuid = UUID.fromString("1c15bdb9-5d42-4a84-922a-0d01f8e5de71"),
          primaryAccountName = "George Ogalo",
          primaryAccountNumber = "4716 4157 0907 3361",
          primaryUserUuid = UUID.fromString("bcc32571-cf16-4abc-ac38-38d58f9cbab5"),
          primaryAccountType = "WALLET",

          primaryUserType = Some("individual"),
          primaryUsername = Some("PegasiB21"),
          primaryIndividualUsersName = Some("Alice"),
          primaryIndividualUsersFullname = Some("Alice Smith"),
          primaryBusinessUsersBusinessName = Some("Universal catering co"),
          primaryBusinessUsersBrandName = Some("Costa Coffee DSO"),

          secondaryAccountInternalId = 4,
          secondaryAccountUuid = UUID.fromString("b370ef14-b219-42b3-8160-5f08a2bcc73a"),
          secondaryAccountName = "George Ogalo",
          secondaryAccountNumber = "4716 4157 0907 3364",
          secondaryUserUuid = UUID.fromString("bcc32571-cf16-4abc-ac38-38d58f9cbab5"),
          direction = "credit".some,
          `type` = "p2p_domestic".some,
          amount = BigDecimal(200.00).setScale(2, RoundingMode.HALF_DOWN).some,
          currency = "AED".some,
          exchangedCurrency = None,
          channel = "ANDROID_APP".some,
          explanation = "some explanation".some,
          effectiveRate = none,
          costRate = none,
          status = "success".some,
          instrument = none,
          createdAt = LocalDateTime.of(2018, 12, 25, 14, 27, 30, 0).some,
          updatedAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0).some,
          primaryAccountPreviousBalance = none,
          secondaryAccountPreviousBalance = none,
          provider = Some("Mashreq")))

      actualResponse.isRight mustBe true
      actualResponse.right.get mustBe expected
    }

    "return Right[Option[Transaction]] containing the Transaction with most recent updated_at" in {
      val result = transactionSqlDao.getMostRecentUpdatedAt(maybeCriteria = None)

      val expected = Transaction.getEmpty.copy(
        id = "1549446999",
        uniqueId = "5",
        sequence = 1L,
        primaryAccountInternalId = 4,
        primaryAccountUuid = UUID.fromString("b370ef14-b219-42b3-8160-5f08a2bcc73a"),
        primaryAccountNumber = "4716 4157 0907 3364",
        primaryAccountName = "George Ogalo",
        primaryAccountType = "WALLET",

        primaryUserType = Some("individual"),
        primaryUsername = Some("PegasiB21"),
        primaryIndividualUsersName = Some("Alice"),
        primaryIndividualUsersFullname = Some("Alice Smith"),
        primaryBusinessUsersBusinessName = Some("Universal catering co"),
        primaryBusinessUsersBrandName = Some("Costa Coffee DSO"),

        primaryUserUuid = UUID.fromString("bcc32571-cf16-4abc-ac38-38d58f9cbab5"),
        secondaryAccountInternalId = 1,
        secondaryAccountUuid = UUID.fromString("1c15bdb9-5d42-4a84-922a-0d01f8e5de71"),
        secondaryAccountNumber = "4716 4157 0907 3361",
        secondaryAccountName = "George Ogalo",
        secondaryUserUuid = UUID.fromString("bcc32571-cf16-4abc-ac38-38d58f9cbab5"),
        direction = Some("debit"),
        `type` = Some("p2p_domestic"),
        amount = Some(BigDecimal("200.00")),
        currency = Some("AED"),
        channel = Some("ANDROID_APP"),
        explanation = Some("some explanation"),
        status = Some("success"),
        instrument = None,
        createdAt = Some(LocalDateTime.of(2018, 12, 25, 14, 27, 30, 0)),
        updatedAt = Some(LocalDateTime.of(2019, 6, 1, 0, 0, 0)),
        provider = Some("Mashreq"))

      result.right.get mustBe expected.toOption
    }

    "return inflow, outflow and net amount of txns for liability" in {
      val criteria = TransactionCriteria(
        createdAt = Some(CriteriaField("created_at", (
          LocalDateTime.of(2018, 12, 25, 0, 0, 0, 0),
          LocalDateTime.of(2018, 12, 31, 0, 0, 0, 0)), InclusiveBetween)))

      val result = transactionSqlDao.getOnFlyAggregation(criteria, isLiability = true)

      val (inflow, outflow, net) = (
        BigDecimal(1950.00).setScale(2, RoundingMode.HALF_EVEN),
        BigDecimal(1950.00).setScale(2, RoundingMode.HALF_EVEN), BigDecimal(0.0).setScale(2, RoundingMode.HALF_EVEN))

      result.right.get mustBe Some((inflow, outflow, net))
    }

    "return inflow, outflow and net amount of txns for assets" in {
      val criteria = TransactionCriteria(
        createdAt = Some(CriteriaField("created_at", (
          LocalDateTime.of(2018, 12, 25, 0, 0, 0, 0),
          LocalDateTime.of(2018, 12, 31, 0, 0, 0, 0)), InclusiveBetween)))

      val result = transactionSqlDao.getOnFlyAggregation(criteria, isLiability = false)

      val (inflow, outflow, net) = (
        BigDecimal(1950.00).setScale(2, RoundingMode.HALF_EVEN),
        BigDecimal(1950.00).setScale(2, RoundingMode.HALF_EVEN), BigDecimal(0.0).setScale(2, RoundingMode.HALF_EVEN))

      result.right.get mustBe Some((inflow, outflow, net))
    }

    "groups information in aggregates" in {
      val criteria = TransactionCriteria(
        createdAt = Some(
          CriteriaField(TransactionSqlDao.cCreatedAt, (
            LocalDateTime.of(2018, 12, 25, 0, 0, 0, 0),
            LocalDateTime.of(2018, 12, 31, 0, 0, 0, 0)),
            InclusiveBetween)),
        direction = Some(CriteriaField(TransactionSqlDao.cDirection, "credit")))

      val grouping = Seq(
        GroupingField(
          TransactionSqlDao.cCreatedAt,
          Option(Day),
          projectionAlias = Option(SqlDao.cDay)),
        GroupingField(
          TransactionSqlDao.caCurrency,
          tableAlias = Option(AccountSqlDao.CurrencyTblAlias),
          columnAlias = Option(AccountSqlDao.CurrencyTblCurrencyCode)))

      val result = transactionSqlDao.aggregateTransactionByCriteriaAndPivots(criteria, grouping)

      val expected = Seq(
        TransactionAggregation(currency = "AED".some, sum = BigDecimal(2900.0).some, count = 4L.some, day = 25.some),
        TransactionAggregation(currency = "AED".some, sum = BigDecimal(1000.0).some, count = 2L.some, day = 26.some))

      result.right.get mustBe expected
    }

    "return correct count containing transactions filtered by date_from" in {
      val criteria = TransactionCriteria(
        createdAt = Some(CriteriaField(
          "created_at",
          LocalDateTime.of(2018, 12, 26, 0, 0, 0, 0), GreaterOrEqual)))

      val actualResponse = transactionSqlDao.countTotalTransactionsByCriteria(criteria)

      val expected = 2

      actualResponse.isRight mustBe true
      actualResponse.right.get mustBe expected
    }

    "return correct count containing transactions filtered by date_to" in {
      val criteria = TransactionCriteria(
        createdAt = Some(CriteriaField(
          "created_at",
          LocalDateTime.of(2018, 12, 25, 23, 59, 59, 0), LesserOrEqual)))

      val actualResponse = transactionSqlDao.countTotalTransactionsByCriteria(criteria)

      val expected = 4

      actualResponse.isRight mustBe true
      actualResponse.right.get mustBe expected
    }

  }
}
