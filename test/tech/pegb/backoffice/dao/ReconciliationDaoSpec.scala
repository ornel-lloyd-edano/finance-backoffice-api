package tech.pegb.backoffice.dao

import java.time._
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.db.DBApi
import play.api.test.Injecting
import tech.pegb.backoffice.dao.DaoError.PreconditionFailed
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes, OrderingSet, Ordering ⇒ DaoOrdering}
import tech.pegb.backoffice.dao.reconciliation.dto.{InternalReconDetailsCriteria, InternalReconSummaryCriteria}
import tech.pegb.backoffice.dao.reconciliation.model.{InternReconDailySummaryResult, InternReconDailySummaryResultToUpdate, InternReconResult}
import tech.pegb.backoffice.dao.reconciliation.postgresql.ReconciliationDao
import tech.pegb.core.PegBTestApp

import scala.math.BigDecimal.RoundingMode

class ReconciliationDaoSpec extends PegBTestApp with MockFactory with Injecting with ScalaFutures {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())

  lazy val reconciliationDao = fakeApplication().injector.instanceOf[ReconciliationDao]

  override def initDb(): Unit = {
    val dbApi = inject[DBApi]
    val db = dbApi.database("reports")
    val connection = db.getConnection()
    connection.prepareStatement(initSql).executeUpdate()
    connection.commit()
  }

  val summaryId = "1"
  val accountId = "1"

  val summaryResult = InternReconDailySummaryResult(
    id = "123",
    reconDate = LocalDate.of(2019, 5, 15),
    accountId = accountId,
    accountNumber = "some account num",
    accountType = "distribution",
    accountMainType = "liability",
    userId = 1,
    userUuid = "2205409c-1c83-11e9-a2a9-000c297e3e45",
    userFullName = none,
    anyCustomerName = Some("ujali"),
    currency = "KES",
    endOfDayBalance = BigDecimal(3200.5000).setScale(2, RoundingMode.HALF_DOWN),
    valueChange = BigDecimal(300.0000).setScale(2, RoundingMode.HALF_DOWN),
    transactionTotalAmount = BigDecimal(9999.0000).setScale(2, RoundingMode.HALF_DOWN),
    transactionTotalCount = 100,
    problematicTxnCount = 1,
    status = "FAIL",
    comments = None,
    updatedAt = None,
    updatedBy = None)

  val summaryWithDate =

    InternReconDailySummaryResult(
      id = "1",
      reconDate = LocalDate.of(1970, 1, 1),
      accountId = accountId,
      accountNumber = "some account number",
      accountType = "distribution",
      accountMainType = "liability",
      userId = 2,
      userUuid = "some user uuid",
      userFullName = none,
      anyCustomerName = Some("ujali"),
      currency = "KES",
      endOfDayBalance = BigDecimal(100.0000),
      valueChange = BigDecimal(50.0000),
      transactionTotalAmount = BigDecimal(200.0000),
      transactionTotalCount = 100,
      problematicTxnCount = 3,
      status = "SUCCESS",
      comments = none,
      updatedAt = none,
      updatedBy = none)

  val summary = InternReconDailySummaryResult(
    id = summaryId,
    reconDate = LocalDate.now(mockClock),
    accountId = accountId,
    accountNumber = "some account number",
    accountType = "distribution",
    accountMainType = "liability",
    userId = 2,
    userUuid = "some user uuid",
    userFullName = "david salgado".some,
    currency = "KES",
    endOfDayBalance = BigDecimal(100.00),
    valueChange = BigDecimal(50.00),
    transactionTotalAmount = BigDecimal(200.00),
    transactionTotalCount = 100,
    problematicTxnCount = 3,
    status = "SUCCESS",
    comments = None,
    updatedAt = None,
    updatedBy = None)

  val lastTxnIdOfBeforeSummary = 999
  val txnCreatedAt: LocalDate = summary.reconDate.minusDays(5L)
  val someUnrelatedAccountToSummary = 1234

  val tableCreationSql: String =
    """
      |CREATE TABLE IF NOT EXISTS transactions (
      |  unique_id int(10) unsigned NOT NULL,
      |  id varchar(36)  NOT NULL,
      |  sequence int(10) unsigned NOT NULL,
      |  primary_account_id int(10) unsigned NOT NULL,
      |  primary_account_uuid varchar(50) NOT NULL,
      |  primary_account_number varchar(50) NOT NULL,
      |  primary_account_type varchar(50) NOT NULL,
      |  primary_account_main_type varchar(50) NOT NULL,
      |  primary_account_user_id INT NOT NULL,
      |  primary_account_user_uuid varchar(36) NOT NULL,
      |  secondary_account_id int(10) unsigned NOT NULL,
      |  secondary_account_uuid varchar(50) NOT NULL,
      |  secondary_account_number varchar(50) NOT NULL,
      |  receiver_phone varchar(50) DEFAULT NULL,
      |  direction varchar(50) DEFAULT NULL,
      |  type varchar(50) DEFAULT NULL,
      |  amount decimal(10,2) DEFAULT NULL,
      |  currency varchar(10) DEFAULT NULL,
      |  exchange_rate decimal(10,2) DEFAULT NULL,
      |  channel varchar(50) DEFAULT NULL,
      |  other_party varchar(50) DEFAULT NULL,
      |  instrument varchar(50) DEFAULT NULL,
      |  instrument_id varchar(50) DEFAULT NULL,
      |  latitude decimal(12,10) DEFAULT NULL,
      |  longitude decimal(13,10) DEFAULT NULL,
      |  explanation varchar(256) DEFAULT NULL,
      |  status varchar(50) DEFAULT NULL,
      |  created_at datetime DEFAULT NULL,
      |  updated_at datetime DEFAULT NULL,
      |  effective_rate decimal(10,2) DEFAULT NULL,
      |  cost_rate decimal(10,2) DEFAULT NULL,
      |  previous_balance decimal(10,2) DEFAULT NULL,
      |  PRIMARY KEY (unique_id)
      |  );
      |
      |
      |CREATE TABLE IF NOT EXISTS accounts (
      |id int(10) unsigned NOT NULL PRIMARY KEY,
      |uuid varchar default 'missing',
      |number varchar default 'missing',
      |name varchar default 'missing',
      |account_type varchar default 'missing',
      |is_main_account boolean default null,
      |user_id int default null,
      |user_uuid varchar default 'missing',
      |currency varchar default 'missing',
      |balance decimal(30,4) default null,
      |blocked_balance decimal(30,4) default null,
      |status varchar default 'missing',
      |closed_at datetime DEFAULT NULL,
      |last_transaction_at datetime DEFAULT NULL,
      |created_at datetime DEFAULT NULL,
      |updated_at datetime DEFAULT NULL,
      |updated_by varchar default null,
      |created_by varchar default 'missing',
      |main_type varchar default 'missing'
      |);
      |
      |create table users
      |(
      |  id                  int 															 NOT NULL ,
      |  uuid                varchar(36)                        NOT NULL UNIQUE,
      |  username            varchar(100)                       null,
      |  fullname            varchar(200)                       null,
      |  password            varchar(100)                       null,
      |  type                varchar(30)                        null,
      |  tier                varchar(100)                       null,
      |  segment             varchar(100)                       null,
      |  subscription        varchar(100)                       null,
      |  email               varchar(100)                       null,
      |  user_status         varchar(30)                        null,
      |  activated_at        timestamp                          null,
      |  password_updated_at timestamp                          null,
      |  created_at          timestamp                          NOT NULL,
      |  created_by          varchar(36)                        NOT NULL,
      |  updated_at          timestamp    							default  null,
      |  updated_by          varchar(36)                        null
      |
      | );
      |
      |CREATE TABLE individual_users
      |(
      |  id integer,
      |  msisdn character varying(50),
      |  user_id integer,
      |  type character varying(30),
      |  name character varying(100),
      |  company character varying(100),
      |  birthdate timestamp without time zone,
      |  birth_place character varying(30),
      |  nationality character varying(15),
      |  occupation character varying(40),
      |  employer character varying(100),
      |  created_at timestamp without time zone NOT NULL,
      |  created_by character varying(36) NOT NULL,
      |  updated_at timestamp without time zone,
      |  updated_by character varying(36),
      |  fullname character varying(100),
      |  gender character varying(10),
      |  person_id character varying(50),
      |  document_number character varying(50),
      |  document_type character varying(50),
      |  action_type character varying(20),
      |  kafka_partition integer,
      |  kafka_offset bigint,
      |  kafka_ts bigint,
      |  device_token character varying(200)
      |);
      |
      |create table business_users
      |(
      |	id                      integer      auto_increment primary key,
      |	uuid                    varchar(36)      not null,
      |	user_id                 integer		 not null,
      |	business_name           varchar(36)      not null,
      |	brand_name              varchar(36)      not null,
      |	business_category       varchar(36)      not null,
      |	business_type           varchar(50)      not null,
      |	registration_number     varchar(50)      not null,
      |	tax_number              varchar(50)      not null,
      |	registration_date       datetime         not null,
      |	currency_id             integer          not null,
      |	collection_account_id   integer		           null,
      |	distribution_account_id integer              null,
      |	default_contact_id      integer              null,
      |	created_by              varchar(50)      not null,
      |	updated_by              varchar(50)          null,
      |	created_at              timestamp        not null,
      |	updated_at              timestamp        not null
      |);
      |
      |CREATE TABLE IF NOT EXISTS `internal_recon_tracker`(
      |  `id` varchar(36) NOT NULL PRIMARY KEY,
      |  `recon_date` datetime NOT NULL,
      |  `start_account_id` INT DEFAULT NULL,
      |  `end_account_id` INT DEFAULT NULL,
      |  `status` varchar(15) NOT NULL,
      |  `last_successful_account_id` INT DEFAULT NULL,
      |  `total_num_accounts` INT NOT NULL
      |);
      |
      |CREATE TABLE IF NOT EXISTS `internal_recon_daily_summary` (
      |  `id` varchar(36) NOT NULL PRIMARY KEY,
      |  `recon_date` datetime NOT NULL,
      |  `account_id` varchar(36) NOT NULL,
      |  `account_number` varchar(50) NOT NULL,
      |  `account_type` varchar(50) NOT NULL,
      |  `main_account_type` varchar(50) NOT NULL,
      |  `user_id` int NOT NULL,
      |  `user_uuid` varchar(36) NOT NULL,
      |  `currency` varchar(3) NOT NULL,
      |  `end_of_day_balance` decimal(20,4) NOT NULL,
      |  `value_change` decimal(20,4) NOT NULL,
      |  `transaction_total_amount` decimal(20,4) NOT NULL,
      |  `transaction_total_count` INT NOT NULL,
      |  `problematic_transaction_count` INT NOT NULL,
      |  `status` varchar(15) NOT NULL,
      |  `comments` varchar(256) DEFAULT NULL,
      |  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
      |  `updated_at` datetime DEFAULT NULL,
      |  `updated_by` varchar(36) DEFAULT NULL);
      |
      |CREATE TABLE IF NOT EXISTS `internal_recon_daily_details` (
      |  `id` varchar(36) NOT NULL PRIMARY KEY,
      |  `internal_reconciliation_summary_id` varchar(36) NOT NULL,
      |  `recon_date` datetime NOT NULL,
      |  `account_id` varchar(36) NOT NULL,
      |  `account_number` varchar(50) NOT NULL,
      |  `currency` varchar(3) NOT NULL,
      |  `current_txn_id` bigint NOT NULL,
      |  `current_txn_sequence` bigint NOT NULL,
      |  `current_txn_direction` varchar(10) NOT NULL,
      |  `current_txn_timestamp` datetime NOT NULL,
      |  `current_txn_amount` decimal(20,4) NOT NULL,
      |  `current_txn_previous_balance` decimal(20,4) NULL,
      |
      |  `previous_txn_id` bigint DEFAULT NULL,
      |  `previous_txn_sequence` bigint DEFAULT NULL,
      |  `previous_txn_direction` varchar(10) DEFAULT NULL,
      |  `previous_txn_timestamp` datetime DEFAULT NULL,
      |  `previous_txn_amount` decimal(20,4) DEFAULT NULL,
      |  `previous_txn_previous_balance` decimal(20,4) DEFAULT NULL,
      |
      |  `recon_status` varchar(15) NOT NULL,
      |  `created_at` datetime DEFAULT CURRENT_TIMESTAMP);
      |
    """.stripMargin

  override def initSql =
    s"""
       |
       |SET SCHEMA $reportsSchema;
       |
       |$tableCreationSql
       |INSERT INTO users (id,uuid,username,fullname, password,type,tier,segment,subscription,email,
       |user_status,activated_at,password_updated_at,created_at,created_by,updated_at,updated_by) VALUES
       |('1', '2205409c-1c83-11e9-a2a9-000c297e3e45', 'ujali','ujali tyagi', 'password', 'individual', 'basic', 'new',
       | 'standard', NULL, 'active', '2019-01-03 06:26:21', '2019-01-03 06:26:21', '2019-01-20 07:22:46', 'admin',
       |  '2019-01-03 06:26:21', NULL),
       |('2', 'efe3b069-476e-4e36-8d22-53176438f55f', '+97123456789', 'david salgado',
       |'885B3EDC975842A4D84B9DF5D29E009641A4562259DA2D2D49FBB19CEF56E2E5', 'individual', 'basic', 'new',
       |'standard', NULL, 'waiting_for_activation', NULL, NULL, '2019-01-20 09:15:29', 'wallet_api', '2019-01-20 09:15:29',
       | 'wallet_api'),
       |('3', '910f02a0-48ef-418d-ac0a-06eff7ac9c90', '+971544451674','lloyd edano',
       |'5A697C6768FCB4F363874B4D73C517A6E7F8932D23C31B6EA52BEBD2C3F4AA05',
       | 'individual', 'basic', 'new', 'standard', NULL, 'waiting_for_activation', NULL, NULL, '2019-01-20 09:50:55',
       |  'wallet_api', '2019-01-20 09:50:55', 'wallet_api');
       |
       |INSERT INTO accounts(id, uuid, number, name, main_type, account_type, is_main_account,
       |user_id, user_uuid, currency, balance, blocked_balance, status, closed_at,
       |last_transaction_at, created_at, updated_at, created_by, updated_by)
       |VALUES
       |($accountId, 'some uuid', 'george account num', 'george account name', 'asset', 'standard wallet', 'true',
       |'123', '2205409c-1c83-11e9-a2a9-000c297e3e45', 'KES', '5555.00', null, 'active', null,
       |now(), now(), now(), 'admin', 'admin'),
       |($someUnrelatedAccountToSummary, 'some uuid', 'loyd account num', 'loyd account name', 'asset', 'standard wallet', 'true',
       |'456', 'efe3b069-476e-4e36-8d22-53176438f55f', 'KES', '108999.00', null, 'active', null,
       |now(), now(), now(), 'admin', 'admin');
       |
       |INSERT INTO internal_recon_daily_summary(id, recon_date, account_id, account_number, account_type, main_account_type, user_id, user_uuid, currency, end_of_day_balance,
       |value_change, transaction_total_amount, transaction_total_count, problematic_transaction_count, status, created_at)
       |VALUES
       |('123', '2019-05-15', $accountId, 'some account num','distribution', 'liability', '1','2205409c-1c83-11e9-a2a9-000c297e3e45', 'KES', '3200.50', '300', '9999.00', '100', '1', 'FAIL', now()),
       |('124', '2019-05-20', 'loyd account', 'some account num', 'distribution', 'liability', '1','efe3b069-476e-4e36-8d22-53176438f55f', 'KES', '1600.50', '400', '1000.00', '30', '1', 'FAIL', now()),
       |('125', '2019-05-25', 'chiggy account', 'some account num', 'distribution', 'liability', '1','910f02a0-48ef-418d-ac0a-06eff7ac9c90', 'KES', '7100.50', '100', '3000.00', '50', '1', 'FAIL', now());
       |
       |INSERT INTO internal_recon_daily_details(id, internal_reconciliation_summary_id, recon_date, account_id, account_number, currency, current_txn_id, current_txn_sequence, current_txn_direction, current_txn_timestamp,current_txn_amount, current_txn_previous_balance, previous_txn_id, previous_txn_sequence, previous_txn_direction, previous_txn_timestamp, previous_txn_amount, previous_txn_previous_balance, recon_status, created_at)
       |VALUES
       |('127', '1', '2019-05-15', '1', 'some account num', 'KES', '1', '1', 'DEBIT', '2019-05-15', '3200.50', NULL, NULL, NULL, NULL, NULL, NULL, '1', 'FAIL', now());
       |
       |INSERT INTO transactions(id,unique_id, sequence, primary_account_id, primary_account_uuid, primary_account_number,
       |primary_account_type, primary_account_main_type, primary_account_user_id, primary_account_user_uuid,
       |secondary_account_id, secondary_account_uuid, secondary_account_number, receiver_phone, direction,
       |type, amount, currency, exchange_rate, channel, other_party, instrument, instrument_id,
       |latitude, longitude, explanation, status, created_at, updated_at,
       |effective_rate, cost_rate, previous_balance)
       |VALUES
       |('1','1', '1', '$someUnrelatedAccountToSummary', 'some uuid', 'some acc num',
       |'standard wallet', 'liability',  '1', 'some user uuid',
       |'5678', 'some uuid', 'some acc num', 'some phone num', 'credit',
       |'p2p', '9999.50', 'AED', null, 'some channel', null, null, null,
       |null, null, null, 'success', '${summary.reconDate.minusDays(2L)}', null, null, null, '850.20'),
       |
       |('$lastTxnIdOfBeforeSummary','2', '1', '${summary.accountId}', 'some uuid', 'george account num',
       |'standard wallet', 'liability', '1', 'some user uuid',
       |'5678', 'some uuid', 'some acc num', 'some phone num', 'credit',
       |'p2p', '10000.99', 'AED', null, 'some channel', null, null, null,
       |null, null, null, 'success', '$txnCreatedAt', null, null, null, '999.50');
       |
    """.stripMargin

  val r =
    s"""
      |create table business_users
      |(
      |	id                      integer      auto_increment primary key,
      |	uuid                    varchar(36)      not null,
      |	user_id                 integer		 not null,
      |	business_name           varchar(36)      not null,
      |	brand_name              varchar(36)      not null,
      |	business_category       varchar(36)      not null,
      |	business_type           varchar(50)      not null,
      |	registration_number     varchar(50)      not null,
      |	tax_number              varchar(50)      not null,
      |	registration_date       datetime         not null,
      |	currency_id             integer          not null,
      |	collection_account_id   integer		           null,
      |	distribution_account_id integer              null,
      |	default_contact_id      integer              null,
      |	created_by              varchar(50)      not null,
      |	updated_by              varchar(50)          null,
      |	created_at              timestamp        not null,
      |	updated_at              timestamp        not null
      |);
      |
      |INSERT INTO business_users(id, uuid, user_id, business_name, brand_name, business_category, business_type,
      | registration_number, tax_number, registration_date, currency_id, collection_account_id, created_by, updated_by, created_at, updated_at) VALUES
      | (1, '',1,'Universal catering co','Costa Coffee DSO','Restaurant-5812', '213/564654EE','A2135468977M','01-01-1996', 1,1,1,1,'Ujali',null,,'$txnCreatedAt', null)
    """.stripMargin

  override def cleanupSql =
    """
      |DELETE FROM users;
      |DELETE FROM transactions;
      |DELETE FROM accounts;
      |DELETE FROM internal_recon_daily_summary;
      |DELETE FROM internal_recon_daily_details;
      |DELETE FROM accounts;
      |DELETE FROM transactions;
      |DELETE FROM business_users;
    """.stripMargin

  val reconSeq = Seq(
    InternReconResult(
      id = UUID.randomUUID().toString,
      internReconSummaryResultId = summaryId,
      reconDate = LocalDate.now(mockClock),
      accountId = summary.accountId,
      accountNumber = "some account number",
      currency = "KES",
      currentTransactionId = 1L,
      currentTxnSequence = 1,
      currentTxnDirection = "credit",
      currentTxnTimestamp = LocalDateTime.now(),
      currentTxnAmount = BigDecimal(100),
      previousTxnAmount = None,
      currentTxnPreviousBalance = None,
      previousTxnPreviousBalance = None,
      reconStatus = "SUCCESS"),
    InternReconResult(
      id = UUID.randomUUID().toString,
      internReconSummaryResultId = summaryId,
      reconDate = LocalDate.now(mockClock),
      accountId = summary.accountId,
      accountNumber = "some account number",
      currency = "KES",
      currentTransactionId = 2L,
      currentTxnSequence = 1,
      currentTxnDirection = "debit",
      currentTxnTimestamp = LocalDateTime.now(),
      currentTxnAmount = BigDecimal(150),
      previousTxnAmount = None,
      currentTxnPreviousBalance = None,
      previousTxnPreviousBalance = None,
      reconStatus = "SUCCESS"),
    InternReconResult(
      id = UUID.randomUUID().toString,
      internReconSummaryResultId = summaryId,
      reconDate = LocalDate.now(mockClock),
      accountId = summary.accountId,
      accountNumber = "some account number",
      currency = "KES",
      currentTransactionId = 3L,
      currentTxnSequence = 1,
      currentTxnDirection = "debit",
      currentTxnTimestamp = LocalDateTime.now(),
      currentTxnAmount = BigDecimal(150),
      previousTxnAmount = None,
      currentTxnPreviousBalance = None,
      previousTxnPreviousBalance = None,
      reconStatus = "FAIL"))

  "ReconciliationDao" should {

    "return InternReconDailySummaryResult by summaryId" in {
      val getInternReconResultsResponse = reconciliationDao.getInternReconDailySummaryResult("123")

      whenReady(getInternReconResultsResponse) { actual ⇒
        actual mustBe Right(Some(summaryResult))
      }
    }

    "return list of list of InternReconDailySummaryResult filtered by date range without extra filter" in {
      val startReconDate = LocalDateTime.of(2019, 5, 15, 0, 0, 0) //Some start date
      val endReconDate = LocalDateTime.of(2019, 5, 15, 23, 59, 59) //Some end date

      val dateRangeCriteria = CriteriaField("recon_date", (startReconDate, endReconDate), MatchTypes.InclusiveBetween)
      val criteria = InternalReconSummaryCriteria(
        maybeDateRange = Some(dateRangeCriteria))
      val getInternReconResultsResponseF = reconciliationDao.getInternReconDailySummaryResults(Some(criteria), None, None, None)

      whenReady(getInternReconResultsResponseF) { getInternReconResultsResponse ⇒

        getInternReconResultsResponse mustBe Right(Seq(summaryResult))
      }
    }

    "return list of internalReconResult by summaryId" in {

      val getInternReconResultsResponse = reconciliationDao.getInternReconResultsBySummaryId(summaryId)

      whenReady(getInternReconResultsResponse) { actual ⇒
        actual.map(_.map(_.id)) mustBe Right(Seq("127"))
      }
    }

    "return list of internalReconResult filtered by date range" in {

      val startReconDate = LocalDateTime.of(2019, 5, 15, 0, 0, 0) //Some start date
      val endReconDate = LocalDateTime.of(2019, 5, 15, 23, 59, 59) //Some end date

      val dateRange = CriteriaField(ReconciliationDao.ReconDetailsTable.cReconDate, (startReconDate, endReconDate), MatchTypes.InclusiveBetween)

      val criteria = InternalReconDetailsCriteria(mayBeDateRange = Some(dateRange))
      val ordering = OrderingSet(DaoOrdering(ReconciliationDao.ReconDetailsTable.cCurrentTxnId, DaoOrdering.ASC))
      val getInternReconResultsResponse = reconciliationDao.getInternReconDetailsByCriteria(Some(criteria), Some(ordering), None, None)

      whenReady(getInternReconResultsResponse) { actual ⇒
        actual.map(_.map(_.id)) mustBe Right(Seq("127"))
      }
    }

    "return list of list of internalReconResult filtered by date range with direction and status filter" in {
      val startReconDate = LocalDateTime.now(mockClock) //Some start date
      val endReconDate = LocalDateTime.now(mockClock) //Some end date
      val filters = InternalReconDetailsCriteria(
        mayBeDateRange = Some(CriteriaField(
          ReconciliationDao.ReconDetailsTable.cReconDate,
          (startReconDate, endReconDate), MatchTypes.InclusiveBetween)),
        maybeAccountNumber = Some(CriteriaField(ReconciliationDao.ReconDetailsTable.cAccountNumber, "some account num")),
        maybeCurrency = Some(CriteriaField(ReconciliationDao.ReconDetailsTable.cCurrency, "KES")))

      val ordering = OrderingSet(DaoOrdering(ReconciliationDao.ReconDetailsTable.cCurrentTxnId, DaoOrdering.ASC))
      val getInternReconResultsResponse = reconciliationDao.getInternReconDetailsByCriteria(Some(filters), Some(ordering), None, None)

      whenReady(getInternReconResultsResponse) { actual ⇒
        actual.map(_.map(_.id)) mustBe Right(reconSeq.filter(r ⇒ r.accountNumber == "some account num" && r.currency == "KES").map(_.id))
      }
    }

    "return updated reconResultSummary in updateInternReconDailySummaryResult" in {
      val id = "123"
      val updateAt = LocalDateTime.now(mockClock)
      val updatedBy = "pegbuser"
      val dto = InternReconDailySummaryResultToUpdate(
        status = "SOLVED".some,
        comments = "2500 KES transaction is created to resolved".some,
        updatedAt = updateAt,
        updatedBy = updatedBy,
        lastUpdatedAt = None)

      val result = reconciliationDao.updateInternReconDailySummaryResult(id, dto)

      whenReady(result) { actual ⇒
        val expected = summaryResult.copy(
          status = "SOLVED",
          comments = "2500 KES transaction is created to resolved".some,
          updatedAt = updateAt.some,
          updatedBy = updatedBy.some)
        actual mustBe Right(expected.some)
      }
    }

    "return precondition failed in updateInternReconDailySummaryResult when lastupdatedAT is wrong" in {
      val id = "123"
      val updateAt = LocalDateTime.now(mockClock)
      val updatedBy = "pegbuser"
      val dto = InternReconDailySummaryResultToUpdate(
        status = "SOLVED".some,
        comments = "2500 KES transaction is created to resolved".some,
        updatedAt = updateAt,
        updatedBy = updatedBy,
        lastUpdatedAt = LocalDateTime.now().some)

      val result = reconciliationDao.updateInternReconDailySummaryResult(id, dto)

      whenReady(result) { actual ⇒

        actual mustBe Left(PreconditionFailed("Update failed. Internal Recon daily Summary 123 has been modified by another process."))
      }
    }

    "return None if entity to update is not found" in {
      val id = "test_one"
      val updateAt = LocalDateTime.now(mockClock)
      val updatedBy = "pegbuser"
      val dto = InternReconDailySummaryResultToUpdate(
        status = "SOLVED".some,
        comments = "2500 KES transaction is created to resolved".some,
        updatedAt = updateAt,
        updatedBy = updatedBy,
        lastUpdatedAt = LocalDateTime.now().some)

      val result = reconciliationDao.updateInternReconDailySummaryResult(id, dto)

      whenReady(result) { actual ⇒
        actual mustBe Right(none)
      }
    }

  }

}
