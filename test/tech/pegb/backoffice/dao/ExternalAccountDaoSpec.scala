package tech.pegb.backoffice.dao

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

import tech.pegb.backoffice.dao.account.abstraction.ExternalAccountDao
import tech.pegb.backoffice.dao.account.dto.{ExternalAccountCriteria, ExternalAccountToCreate, ExternalAccountToUpdate}
import tech.pegb.backoffice.dao.account.entity.ExternalAccount
import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao
import tech.pegb.backoffice.dao.customer.sql.UserSqlDao
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes, OrderingSet}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.core.PegBTestApp

class ExternalAccountDaoSpec extends PegBTestApp {

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
       |CREATE TABLE IF NOT EXISTS `business_user_external_accounts` (
       |  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
       |  `uuid` varchar(36) NOT NULL UNIQUE,
       |  `user_id` int(10) unsigned NOT NULL,
       |  `provider` varchar(100) NOT NULL,
       |  `account_number` varchar(100) NOT NULL,
       |  `account_holder` varchar(100) NOT NULL,
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
       |INSERT INTO business_user_external_accounts
       |(id, uuid, user_id, provider, account_number, account_holder, currency_id, created_by, created_at, updated_by, updated_at)
       |VALUES
       |(1, uuid(), 1, 'Metrobank', '0000-890123', 'George Ogalo', 2, 'system', now(), null, null),
       |(2, uuid(), 1, 'Mashreq', '934932849-890123', 'George Michaels', 3, 'system', now(), null, null),
       |(3, uuid(), 1, 'First Abu Dabi Bank', '0000-111111112', 'Darth Maul', 1, 'system', now(), null, null);
     """.stripMargin

  val dao = inject[ExternalAccountDao]

  "ExternalAccountDao" should {
    val uuidToBeUsedLater = UUID.randomUUID()
    "insert row in table business_user_external_accounts" in {
      val dto: ExternalAccountToCreate = ExternalAccountToCreate(
        uuid = uuidToBeUsedLater,
        userId = 1,
        provider = "RAK Bank",
        accountNumber = "09190000345",
        accountHolder = "Darth Vader",
        currencyId = 1,
        createdBy = "unit test",
        createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS),
        updatedBy = Some("other user"),
        updatedAt = Some(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)))
      val result = dao.insertExternalAccount(dto)
      val expected: ExternalAccount = ExternalAccount(
        id = 4,
        uuid = dto.uuid,
        userId = dto.userId,
        userUuid = UUID.fromString("a57291af-c840-4ab1-bb4d-4baed930ed58"),
        provider = dto.provider,
        accountNumber = dto.accountNumber,
        accountHolder = dto.accountHolder,
        currencyId = dto.currencyId,
        currencyName = "KES",
        createdBy = dto.createdBy,
        createdAt = dto.createdAt,
        updatedBy = dto.updatedBy,
        updatedAt = dto.updatedAt)
      result mustBe Right(expected)
    }

    "fail to insert row in business_user_external_accounts if uuid already exists" in {
      val dto: ExternalAccountToCreate = ExternalAccountToCreate(
        uuid = uuidToBeUsedLater, //already exist
        userId = 1,
        provider = "Noor Bank",
        accountNumber = "09190000345",
        accountHolder = "Storm Trooper",
        currencyId = 1,
        createdBy = "unit test",
        createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS),
        updatedBy = Some("other user"),
        updatedAt = Some(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)))
      val result = dao.insertExternalAccount(dto)
      val expected: DaoError = DaoError.ConstraintViolationError("Unique constraint on any of these columns [id, uuid] was violated")
      result mustBe Left(expected)
    }

    "fail to insert row in business_user_external_accounts if currency_id is not found" in {
      val dto: ExternalAccountToCreate = ExternalAccountToCreate(
        uuid = UUID.randomUUID(),
        userId = 1,
        provider = "Noor Bank",
        accountNumber = "09190000345",
        accountHolder = "Storm Trooper",
        currencyId = 99, //not found
        createdBy = "unit test",
        createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS),
        updatedBy = Some("other user"),
        updatedAt = Some(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)))
      val result = dao.insertExternalAccount(dto)
      val expected: DaoError = DaoError.ConstraintViolationError("Foreign key to any of these columns [user_id, currency_id] was violated")

      result mustBe Left(expected)
    }

    "fail to insert row in business_user_external_accounts if user_id is not found" in {
      val dto: ExternalAccountToCreate = ExternalAccountToCreate(
        uuid = UUID.randomUUID(),
        userId = 55, //not found
        provider = "Noor Bank",
        accountNumber = "09190000345",
        accountHolder = "Storm Trooper",
        currencyId = 1,
        createdBy = "unit test",
        createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS),
        updatedBy = Some("other user"),
        updatedAt = Some(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)))
      val result = dao.insertExternalAccount(dto)
      val expected: DaoError = DaoError.ConstraintViolationError("Foreign key to any of these columns [user_id, currency_id] was violated")

      result mustBe Left(expected)
    }

    "get specific row by id in table business_user_external_accounts" in {
      val id: Int = 4
      val result = dao.getExternalAccountById(id)
      val expected: ExternalAccount = ExternalAccount(
        id = 4,
        uuid = uuidToBeUsedLater,
        userId = 1,
        userUuid = UUID.fromString("a57291af-c840-4ab1-bb4d-4baed930ed58"),
        provider = "RAK Bank",
        accountNumber = "09190000345",
        accountHolder = "Darth Vader",
        currencyId = 1,
        currencyName = "KES",
        createdBy = "unit test",
        createdAt = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS),
        updatedBy = Some("other user"),
        updatedAt = Some(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)))
      result mustBe Right(Some(expected))
    }

    "get all rows in table business_user_external_accounts" in {
      val criteria: ExternalAccountCriteria = ExternalAccountCriteria()
      val result = dao.getExternalAccountByCriteria(criteria, None, None, None)
      val expected: Seq[Int] = Seq(1, 2, 3, 4)
      result.map(_.map(_.id)) mustBe Right(expected)
    }

    "get all rows in table business_user_external_accounts ordered by currency DESC" in {
      val criteria: ExternalAccountCriteria = ExternalAccountCriteria()
      val orderBy: OrderingSet = OrderingSet(ExternalAccount.cCurrencyName, "DESC")
      val result = dao.getExternalAccountByCriteria(criteria, Some(orderBy), None, None)
      val expected: Seq[(Int, String)] = Seq(1 → "USD", 2 → "PHP", 3 → "KES", 4 → "KES")
      result.map(_.map(result ⇒ result.id → result.currencyName)) mustBe Right(expected)
    }

    "get rows in table business_user_external_accounts paginated with limit = 2 and offset = 1" in {
      val criteria: ExternalAccountCriteria = ExternalAccountCriteria()
      val result = dao.getExternalAccountByCriteria(criteria, None, Some(2), Some(1))
      val expected: Seq[Int] = Seq(2, 3)
      result.map(_.map(_.id)) mustBe Right(expected)
    }

    "get rows in table business_user_external_accounts filter by user_uuid" in {
      val userUuid = "a57291af-c840-4ab1-bb4d-4baed930ed58"
      val criteria: ExternalAccountCriteria =
        ExternalAccountCriteria(userUuid = Some(CriteriaField(UserSqlDao.uuid, userUuid)))
      val result = dao.getExternalAccountByCriteria(criteria, None, None, None)
      val expected: Seq[(Int, String)] = Seq(1 → userUuid, 2 → userUuid, 3 → userUuid, 4 → userUuid)
      result.map(_.map(result ⇒ result.id → result.userUuid.toString)) mustBe Right(expected)
    }

    "get rows in table business_user_external_accounts filter with partial match on provider" in {
      val partialProvider = "bank"
      val criteria: ExternalAccountCriteria =
        ExternalAccountCriteria(provider = Some(CriteriaField(ExternalAccount.cProvider, partialProvider, MatchTypes.Partial)))
      val result = dao.getExternalAccountByCriteria(criteria, None, None, None)
      val expected: Seq[(Int, String)] = Seq(1 → "Metrobank", 3 → "First Abu Dabi Bank", 4 → "RAK Bank")
      result.map(_.map(result ⇒ result.id → result.provider)) mustBe Right(expected)
    }

    "get rows in table business_user_external_accounts filter with partial match on account holder" in {
      val partialAccountHolder = "Darth"
      val criteria: ExternalAccountCriteria =
        ExternalAccountCriteria(accountHolder = Some(CriteriaField(ExternalAccount.cAccountHolder, partialAccountHolder, MatchTypes.Partial)))
      val result = dao.getExternalAccountByCriteria(criteria, None, None, None)
      val expected: Seq[(Int, String)] = Seq(3 → "Darth Maul", 4 → "Darth Vader")
      result.map(_.map(result ⇒ result.id → result.accountHolder)) mustBe Right(expected)
    }

    "count all rows in table business_user_external_accounts" in {
      val criteria: ExternalAccountCriteria = ExternalAccountCriteria()
      val result = dao.countExternalAccount(criteria)
      val expected: Int = 4
      result mustBe Right(expected)
    }

    "count all rows with KES cuurency in table business_user_external_accounts" in {
      val criteria: ExternalAccountCriteria = ExternalAccountCriteria(currencyName = Some(CriteriaField(CurrencySqlDao.cName, "KES")))
      val result = dao.countExternalAccount(criteria)
      val expected: Int = 2
      result mustBe Right(expected)
    }

    "update specific row in table business_user_external_accounts by id" in {
      val dto: ExternalAccountToUpdate = ExternalAccountToUpdate(
        provider = Some("Dunia Finance"),
        updatedBy = Some("new user"),
        updatedAt = Some(LocalDateTime.now),
        lastUpdatedAt = Some(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)))
      val id: Int = 4
      val criteria: ExternalAccountCriteria =
        ExternalAccountCriteria(id = Some(CriteriaField(ExternalAccount.cId, id)))
      val result = dao.updateExternalAccount(criteria, dto)
      val expected: (Int, String, String) = (4, "Dunia Finance", "Darth Vader")
      result.map(_.map(result ⇒ (result.id, result.provider, result.accountHolder)).head) mustBe Right(expected)
    }

    "update rows in table business_user_external_accounts by user id and created_by" in {
      val dto: ExternalAccountToUpdate = ExternalAccountToUpdate(
        provider = Some("Deem Finance"),
        updatedBy = Some("new user 2"),
        updatedAt = Some(LocalDateTime.now.truncatedTo(ChronoUnit.MINUTES)),
        lastUpdatedAt = None)
      val userId: Int = 1
      val createdBy = "system"
      val criteria: ExternalAccountCriteria =
        ExternalAccountCriteria(
          userId = Some(CriteriaField(ExternalAccount.cUserId, userId)),
          createdBy = Some(CriteriaField(ExternalAccount.cCreatedBy, createdBy)))
      val result = dao.updateExternalAccount(criteria, dto)
      val expected: Seq[(Int, Int, String)] = Seq(
        (1, 1, "Deem Finance"), (2, 1, "Deem Finance"), (3, 1, "Deem Finance"))
      result.map(_.map(result ⇒ (result.id, result.userId, result.provider))) mustBe Right(expected)
    }

    "update rows in table business_user_external_accounts with partial match on account number" in {
      val dto: ExternalAccountToUpdate = ExternalAccountToUpdate(
        provider = Some("Deem Finance"),
        updatedBy = Some("new user 2"),
        updatedAt = Some(LocalDateTime.now.truncatedTo(ChronoUnit.MINUTES)),
        lastUpdatedAt = Some(LocalDateTime.now.truncatedTo(ChronoUnit.MINUTES)))
      val partialAccountNum: String = "0000"
      val criteria: ExternalAccountCriteria =
        ExternalAccountCriteria(accountNumber = Some(CriteriaField(ExternalAccount.cAccountNum, partialAccountNum, MatchTypes.Partial)))
      val result = dao.updateExternalAccount(criteria, dto)
      val expected: Seq[(Int, String)] = Seq(1 → "0000-890123", 3 → "0000-111111112", 4 → "09190000345")
      result.map(_.map(result ⇒ (result.id, result.accountNumber))) mustBe Right(expected)
    }

    "fail to update row by id if updated currency is not found" in {
      val missingCurrency: Int = 6
      val dto: ExternalAccountToUpdate = ExternalAccountToUpdate(
        currencyId = Some(missingCurrency),
        lastUpdatedAt = Some(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)))
      val id: Int = 1
      val criteria: ExternalAccountCriteria =
        ExternalAccountCriteria(id = Some(CriteriaField(ExternalAccount.cId, id)))
      val result = dao.updateExternalAccount(criteria, dto)
      val expected: DaoError = DaoError.ConstraintViolationError("Foreign key to any of these columns [user_id, currency_id] was violated")
      result mustBe Left(expected)
    }

    "fail to update row by id if updated user id is not found" in {
      val missingUserId: Int = 9
      val dto: ExternalAccountToUpdate = ExternalAccountToUpdate(
        userId = Some(missingUserId),
        lastUpdatedAt = Some(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)))
      val id: Int = 1
      val criteria: ExternalAccountCriteria =
        ExternalAccountCriteria(id = Some(CriteriaField(ExternalAccount.cId, id)))
      val result = dao.updateExternalAccount(criteria, dto)
      val expected: DaoError = DaoError.ConstraintViolationError("Foreign key to any of these columns [user_id, currency_id] was violated")
      result mustBe Left(expected)
    }

    "fail to update row if lastUpdatedAt is not the most recent updated_at" in {
      val staleLastUpdatedAt: LocalDateTime = LocalDateTime.now().minusMinutes(15)

      val dto: ExternalAccountToUpdate = ExternalAccountToUpdate(
        accountHolder = Some("Valentina Mileva"),
        lastUpdatedAt = Some(staleLastUpdatedAt))
      val id: Int = 1
      val criteria: ExternalAccountCriteria =
        ExternalAccountCriteria(id = Some(CriteriaField(ExternalAccount.cId, id)))
      val result = dao.updateExternalAccount(criteria, dto)
      val expected: DaoError = DaoError.PreconditionFailed(s"Failed to update stale external account. Entity has been updated recently.")
      result mustBe Left(expected)
    }

    "delete row in table business_user_external_accounts by id" in {
      val lastUpdatedAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
      val id: Int = 1
      val criteria: ExternalAccountCriteria =
        ExternalAccountCriteria(id = Some(CriteriaField(ExternalAccount.cId, id)))
      val result = dao.deleteExternalAccount(criteria, Some(lastUpdatedAt))
      val expected = Some(())
      result mustBe Right(expected)
    }

    "delete rows in table business_user_external_accounts by currency" in {
      val lastUpdatedAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
      val currency: String = "PHP"
      val criteria: ExternalAccountCriteria =
        ExternalAccountCriteria(currencyName = Some(CriteriaField(CurrencySqlDao.cName, currency)))
      val result = dao.deleteExternalAccount(criteria, Some(lastUpdatedAt))
      val expected = Some(())
      result mustBe Right(expected)
    }

    "delete rows in table business_user_external_accounts with partial match on account number" in {
      val lastUpdatedAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
      val partialAccountNumber: String = "000-111"
      val criteria: ExternalAccountCriteria =
        ExternalAccountCriteria(accountNumber = Some(CriteriaField(ExternalAccount.cAccountNum, partialAccountNumber, MatchTypes.Partial)))
      val result = dao.deleteExternalAccount(criteria, Some(lastUpdatedAt))
      val expected = Some(())
      result mustBe Right(expected)
    }

    "not fail in deleting even if rows are already deleted" in {
      val lastUpdatedAt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)
      val partialAccountNumber: String = "000-111"
      val criteria: ExternalAccountCriteria =
        ExternalAccountCriteria(accountNumber = Some(CriteriaField(ExternalAccount.cAccountNum, partialAccountNumber, MatchTypes.Partial)))
      val result = dao.deleteExternalAccount(criteria, Some(lastUpdatedAt))
      val expected = None
      result mustBe Right(expected)
    }

    "fail deleting a row if lastUpdatedAt is not the most recent updated_at" in {
      val staleLastUpdatedAt: LocalDateTime = LocalDateTime.now().minusHours(1)
      val id: Int = 4
      val criteria: ExternalAccountCriteria =
        ExternalAccountCriteria(id = Some(CriteriaField(ExternalAccount.cId, id)))
      val result = dao.deleteExternalAccount(criteria, Some(staleLastUpdatedAt))
      val expected: DaoError = DaoError.PreconditionFailed(s"Failed to delete stale external account. Entity has been updated recently.")
      result mustBe Left(expected)
    }
  }
}
