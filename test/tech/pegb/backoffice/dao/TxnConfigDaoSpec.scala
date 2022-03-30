package tech.pegb.backoffice.dao

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao
import tech.pegb.backoffice.dao.customer.sql.UserSqlDao
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes, OrderingSet}
import tech.pegb.backoffice.dao.transaction.abstraction.TransactionConfigDao
import tech.pegb.backoffice.dao.transaction.entity.TxnConfig
import tech.pegb.backoffice.dao.transaction.dto._
import tech.pegb.core.PegBTestApp

class TxnConfigDaoSpec extends PegBTestApp {

  override def initSql =
    s"""
       |
       |CREATE TABLE IF NOT EXISTS `users` (
       |	id                  INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
       |	uuid                VARCHAR(36)  NOT NULL UNIQUE,
       |	username            VARCHAR(100) NULL UNIQUE,
       |	password            VARCHAR(100) NULL,
       |	type                VARCHAR(30)  NULL,
       |	tier                VARCHAR(100) NULL,
       |	segment             VARCHAR(100) NULL,
       |	subscription        VARCHAR(100) NULL,
       |	email               VARCHAR(100) NULL,
       |	status              VARCHAR(30)  NULL,
       |	activated_at        DATETIME     NULL,
       |	password_updated_at DATETIME     NULL,
       |	created_at          DATETIME     NOT NULL,
       |	created_by          VARCHAR(36)  NOT NULL,
       |	updated_at          DATETIME     NULL,
       |	updated_by          VARCHAR(36)  NULL
       |);
       |
       |INSERT INTO users(id, uuid, username, password, email, activated_at, password_updated_at, subscription, type, status, tier, segment, created_by, created_at, updated_by, updated_at)
       |VALUES(1, 'a57291af-c840-4ab1-bb4d-4baed930ed58', 'user01', 'pword', 'alice@gmail.com',  '2018-10-01 00:00:00', null, 'sub_one',  'type_one', 'ACTIVE', null, null, 'SuperUser', '2018-10-01 00:00:00', null, null);
       |
       |CREATE TABLE IF NOT EXISTS `currencies` (
       |  `id` TINYINT(3) UNSIGNED NOT NULL AUTO_INCREMENT,
       |  `currency_name` char(3) NOT NULL UNIQUE,
       |  `description` VARCHAR(100) DEFAULT NULL,
       |  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
       |  `created_by`  VARCHAR(100) NOT NULL,
       |  `updated_at` DATETIME DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
       |  `updated_by`  VARCHAR(100) DEFAULT NULL,
       |  `is_active` TINYINT(4) NOT NULL DEFAULT '1',
       |  `icon`  VARCHAR(20) NULL,
       |  PRIMARY KEY (`id`)
       |);
       |
       |INSERT INTO currencies(id, currency_name, description, created_at, updated_at, is_active)
       |VALUES
       |('1', 'KES', 'Kenya shilling', now(), null, 1),
       |('2', 'USD', 'US Dollar', now(), null, 1),
       |('3', 'PHP', 'Philippine Peso', now(), null, 1);
       |
       |CREATE TABLE IF NOT EXISTS `business_user_txn_configs` (
       |  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
       |  `uuid` varchar(36) NOT NULL UNIQUE,
       |  `user_id` int(10) unsigned NOT NULL,
       |  `transaction_type` varchar(50) NOT NULL,
       |  `currency_id` tinyint(3) unsigned NOT NULL,
       |  `created_by` varchar(50) NOT NULL,
       |  `created_at` datetime,
       |  `updated_by` varchar(50) DEFAULT NULL,
       |  `updated_at` datetime,
       |  PRIMARY KEY (`id`),
       |  FOREIGN KEY (`user_id`) REFERENCES `users` (`id`),
       |  FOREIGN KEY (`currency_id`) REFERENCES `currencies` (`id`)
       |);
       |
       |INSERT INTO business_user_txn_configs
       |(id, uuid, user_id, transaction_type, currency_id, created_by, created_at, updated_by, updated_at)
       |VALUES
       |(1, uuid(), 1, 'cashout', 2, 'system', now(), null, null),
       |(2, uuid(), 1, 'cashin', 3, 'system', now(), null, null),
       |(3, uuid(), 1, 'p2p', 1, 'system', now(), null, null);
     """.stripMargin

  val dao = inject[TransactionConfigDao]

  "TxnConfigDao" should {
    val uuidToBeUsedLater = UUID.randomUUID()
    "insert row in table business_user_txn_configs" in {

      val dto = TxnConfigToCreate(
        uuid = uuidToBeUsedLater,
        userId = 1,
        transactionType = "international_remittance",
        currencyId = 1,
        createdBy = "unit test",
        createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS),
        updatedBy = Some("other user"),
        updatedAt = Some(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)))
      val result = dao.insertTxnConfig(dto)
      val expected = TxnConfig(
        id = 4,
        uuid = dto.uuid,
        userId = dto.userId,
        userUuid = UUID.fromString("a57291af-c840-4ab1-bb4d-4baed930ed58"),
        transactionType = "international_remittance",
        currencyId = dto.currencyId,
        currencyName = "KES",
        createdBy = dto.createdBy,
        createdAt = dto.createdAt,
        updatedBy = dto.updatedBy,
        updatedAt = dto.updatedAt)
      result mustBe Right(expected)
    }

    "fail to insert row in business_user_txn_configs if uuid already exists" in {
      val dto = TxnConfigToCreate(
        uuid = uuidToBeUsedLater,
        userId = 1,
        transactionType = "international_remittance",
        currencyId = 1,
        createdBy = "unit test",
        createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS),
        updatedBy = Some("other user"),
        updatedAt = Some(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)))
      val result = dao.insertTxnConfig(dto)
      val expected: DaoError = DaoError.ConstraintViolationError("Unique constraint on any of these columns [id, uuid] was violated")
      result mustBe Left(expected)
    }

    "fail to insert row in business_user_txn_configs if currency_id is not found" in {
      val dto = TxnConfigToCreate(
        uuid = UUID.randomUUID(),
        userId = 1,
        transactionType = "international_remittance",
        currencyId = 99,
        createdBy = "unit test",
        createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS),
        updatedBy = Some("other user"),
        updatedAt = Some(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)))
      val result = dao.insertTxnConfig(dto)
      val expected: DaoError = DaoError.ConstraintViolationError("Foreign key to any of these columns [currency_id, user_id] was violated")

      result mustBe Left(expected)
    }

    "fail to insert row in business_user_txn_configs if user_id is not found" in {
      val dto = TxnConfigToCreate(
        uuid = UUID.randomUUID(),
        userId = 99,
        transactionType = "international_remittance",
        currencyId = 1,
        createdBy = "unit test",
        createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS),
        updatedBy = Some("other user"),
        updatedAt = Some(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)))
      val result = dao.insertTxnConfig(dto)
      val expected: DaoError = DaoError.ConstraintViolationError("Foreign key to any of these columns [currency_id, user_id] was violated")

      result mustBe Left(expected)
    }

    "get specific row by id in table business_user_txn_configs" in {
      val id: Int = 4
      val result = dao.getTxnConfigById(id)
      val expected = TxnConfig(
        id = 4,
        uuid = uuidToBeUsedLater,
        userId = 1,
        userUuid = UUID.fromString("a57291af-c840-4ab1-bb4d-4baed930ed58"),
        transactionType = "international_remittance",
        currencyId = 1,
        currencyName = "KES",
        createdBy = "unit test",
        createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS),
        updatedBy = Some("other user"),
        updatedAt = Some(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)))
      result mustBe Right(Some(expected))
    }

    "get all rows in table business_user_txn_configs" in {
      val criteria = TxnConfigCriteria()
      val result = dao.getTxnConfigByCriteria(criteria, None, None, None)
      val expected: Seq[Int] = Seq(1, 2, 3, 4)
      result.map(_.map(_.id)) mustBe Right(expected)
    }

    "get all rows in table business_user_txn_configs ordered by currency DESC" in {
      val criteria = TxnConfigCriteria()
      val orderBy: OrderingSet = OrderingSet(CurrencySqlDao.cName, "DESC")
      val result = dao.getTxnConfigByCriteria(criteria, Some(orderBy), None, None)
      val expected: Seq[(Int, String)] = Seq(1 → "USD", 2 → "PHP", 3 → "KES", 4 → "KES")
      result.map(_.map(result ⇒ result.id → result.currencyName)) mustBe Right(expected)
    }

    "get rows in table business_user_txn_configs paginated with limit = 2 and offset = 1" in {
      val criteria = TxnConfigCriteria()
      val result = dao.getTxnConfigByCriteria(criteria, None, Some(2), Some(1))
      val expected: Seq[Int] = Seq(2, 3)
      result.map(_.map(_.id)) mustBe Right(expected)
    }

    "get rows in table business_user_txn_configs filter by user_uuid" in {
      val userUuid = "a57291af-c840-4ab1-bb4d-4baed930ed58"
      val criteria = TxnConfigCriteria(userUuid = Some(CriteriaField(UserSqlDao.uuid, userUuid)))
      val result = dao.getTxnConfigByCriteria(criteria, None, None, None)
      val expected: Seq[(Int, String)] = Seq(1 → userUuid, 2 → userUuid, 3 → userUuid, 4 → userUuid)
      result.map(_.map(result ⇒ result.id → result.userUuid.toString)) mustBe Right(expected)
    }

    "get rows in table business_user_txn_configs filter with partial match on transaction type" in {
      val partialTxnType = "cash"
      val criteria = TxnConfigCriteria(transactionType = Some(CriteriaField(TxnConfig.cTxnType, partialTxnType, MatchTypes.Partial)))
      val result = dao.getTxnConfigByCriteria(criteria, None, None, None)
      val expected: Seq[(Int, String)] = Seq(1 → "cashout", 2 → "cashin")
      result.map(_.map(result ⇒ result.id → result.transactionType)) mustBe Right(expected)
    }

    "count all rows in table business_user_txn_configs" in {
      val criteria = TxnConfigCriteria()
      val result = dao.countTxnConfig(criteria)
      val expected: Int = 4
      result mustBe Right(expected)
    }

    "count all rows with KES cuurency in table business_user_txn_configs" in {
      val criteria = TxnConfigCriteria(currencyName = Some(CriteriaField(CurrencySqlDao.cName, "KES")))
      val result = dao.countTxnConfig(criteria)
      val expected: Int = 2
      result mustBe Right(expected)
    }

    "update specific row in table business_user_txn_configs by id" in {
      val dto = TxnConfigToUpdate(
        transactionType = Some("utility_payment"),
        updatedBy = "new user",
        updatedAt = LocalDateTime.now.truncatedTo(ChronoUnit.MINUTES),
        lastUpdatedAt = Some(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)))
      val id: Int = 4
      val criteria = TxnConfigCriteria(id = Some(CriteriaField(TxnConfig.cId, id)))
      val result = dao.updateTxnConfig(criteria, dto)
      val expected: (Int, String, String) = (4, "KES", "utility_payment")
      result.map(_.map(result ⇒ (result.id, result.currencyName, result.transactionType)).head) mustBe Right(expected)
    }

    "update rows in table business_user_txn_configs by user id and created_by" in {
      val dto = TxnConfigToUpdate(
        transactionType = Some("utility_payment"),
        updatedBy = "new user",
        updatedAt = LocalDateTime.now.truncatedTo(ChronoUnit.MINUTES),
        lastUpdatedAt = None)
      val userId: Int = 1
      val createdBy = "system"
      val criteria = TxnConfigCriteria(
        userId = Some(CriteriaField(TxnConfig.cUserId, userId)),
        createdBy = Some(CriteriaField(TxnConfig.cCreatedBy, createdBy)))

      val result = dao.updateTxnConfig(criteria, dto)
      val expected: Seq[(Int, Int, String)] = Seq(
        (1, 1, "utility_payment"), (2, 1, "utility_payment"), (3, 1, "utility_payment"))
      result.map(_.map(result ⇒ (result.id, result.userId, result.transactionType))) mustBe Right(expected)
    }

    "update rows in table business_user_txn_configs with partial match on transaction_type" in {
      val dto = TxnConfigToUpdate(
        transactionType = Some("currency_exchange"),
        updatedBy = "new user 2",
        updatedAt = LocalDateTime.now.truncatedTo(ChronoUnit.MINUTES),
        lastUpdatedAt = Some(LocalDateTime.now.truncatedTo(ChronoUnit.MINUTES)))
      val partialTxnType: String = "util"
      val criteria = TxnConfigCriteria(
        transactionType = Some(CriteriaField(TxnConfig.cTxnType, partialTxnType, MatchTypes.Partial)))

      val result = dao.updateTxnConfig(criteria, dto)
      val expected: Seq[(Int, String)] = Seq(1 → "currency_exchange", 2 → "currency_exchange", 3 → "currency_exchange", 4 → "currency_exchange")

      result.map(_.map(result ⇒ (result.id, result.transactionType))) mustBe Right(expected)
    }

    "fail to update row by id if updated currency is not found" in {
      val missingCurrency: Int = 6
      val dto = TxnConfigToUpdate(
        currencyId = Some(missingCurrency),
        updatedBy = "unit test",
        updatedAt = LocalDateTime.now,
        lastUpdatedAt = Some(LocalDateTime.now.truncatedTo(ChronoUnit.MINUTES)))
      val id: Int = 1
      val criteria = TxnConfigCriteria(id)
      val result = dao.updateTxnConfig(criteria, dto)
      val expected: DaoError = DaoError.ConstraintViolationError("Foreign key to any of these columns [currency_id, user_id] was violated")
      result mustBe Left(expected)
    }

    "fail to update row by id if updated user id is not found" in {
      val missingUserId: Int = 9
      val dto = TxnConfigToUpdate(
        userId = Some(missingUserId),
        updatedBy = "unit test",
        updatedAt = LocalDateTime.now,
        lastUpdatedAt = Some(LocalDateTime.now.truncatedTo(ChronoUnit.MINUTES)))
      val id: Int = 1
      val criteria = TxnConfigCriteria(id)
      val result = dao.updateTxnConfig(criteria, dto)
      val expected: DaoError = DaoError.ConstraintViolationError("Foreign key to any of these columns [currency_id, user_id] was violated")
      result mustBe Left(expected)
    }

    "fail to update row if lastUpdatedAt is not the most recent updated_at" in {
      val staleLastUpdatedAt: LocalDateTime = LocalDateTime.now().minusMinutes(15)

      val dto = TxnConfigToUpdate(
        transactionType = Some("p2p"),
        updatedBy = "unit test",
        updatedAt = LocalDateTime.now,
        lastUpdatedAt = Some(staleLastUpdatedAt))
      val id: Int = 1
      val criteria = TxnConfigCriteria(id)
      val result = dao.updateTxnConfig(criteria, dto)
      val expected: DaoError = DaoError.PreconditionFailed(s"Failed to update stale txn config. Entity has been updated recently.")
      result mustBe Left(expected)
    }

    "delete row in table business_user_txn_configs by id" in {
      val lastUpdatedAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
      val id: Int = 1
      val criteria = TxnConfigCriteria(id)
      val result = dao.deleteTxnConfig(criteria, Some(lastUpdatedAt))
      val expected = Some(())
      result mustBe Right(expected)
    }

    "delete rows in table business_user_txn_configs by currency" in {
      val lastUpdatedAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
      val currency: String = "PHP"
      val criteria = TxnConfigCriteria(currencyName = Some(CriteriaField(CurrencySqlDao.cName, currency)))
      val result = dao.deleteTxnConfig(criteria, Some(lastUpdatedAt))
      val expected = Some(())
      result mustBe Right(expected)
    }

    "delete rows in table business_user_txn_configs with partial match on created_by" in {
      val lastUpdatedAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
      val partialCreatedBy: String = "unit"
      val criteria = TxnConfigCriteria(createdBy = Some(CriteriaField(TxnConfig.cCreatedBy, partialCreatedBy, MatchTypes.Partial)))
      val result = dao.deleteTxnConfig(criteria, Some(lastUpdatedAt))
      val expected = Some(())
      result mustBe Right(expected)
    }

    "not fail in deleting even if rows are already deleted" in {
      val lastUpdatedAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
      val currency: String = "PHP"
      val criteria = TxnConfigCriteria(currencyName = Some(CriteriaField(CurrencySqlDao.cName, currency)))
      val result = dao.deleteTxnConfig(criteria, Some(lastUpdatedAt))
      val expected = None
      result mustBe Right(expected)
    }

    "fail deleting a row if lastUpdatedAt is not the most recent updated_at" in {
      val staleLastUpdatedAt: LocalDateTime = LocalDateTime.now().minusHours(1)
      val id: Int = 3
      val criteria = TxnConfigCriteria(id)
      val result = dao.deleteTxnConfig(criteria, Some(staleLastUpdatedAt))
      val expected: DaoError = DaoError.PreconditionFailed(s"Failed to delete stale txn config. Entity has been updated recently.")
      result mustBe Left(expected)
    }
  }
}
