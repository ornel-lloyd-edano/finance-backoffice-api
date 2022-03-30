package tech.pegb.backoffice.dao

import java.time.format.DateTimeFormatter
import java.time._

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.Ignore
import tech.pegb.backoffice.dao.account.dto.{AccountTypeToUpdate, AccountTypeToUpsert}
import tech.pegb.backoffice.dao.account.entity.AccountAttributes.AccountType
import tech.pegb.backoffice.dao.account.sql.{AccountTypesSqlDao}
import tech.pegb.core.PegBTestApp

@Ignore
class AccountTypesSqlDaoSpec extends PegBTestApp with MockFactory {

  lazy val accountTypesSqlDao = fakeApplication().injector.instanceOf[AccountTypesSqlDao]

  val mockClock = Clock.fixed(Instant.ofEpochMilli(1563288490000L), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  override def initSql =
    s"""
       |INSERT INTO account_types(id, account_type_name, description, created_at,created_by, is_active)
       |VALUES('1', 'WALLET', 'standard account type for individual users', '$now', 'dima', 1),
       |('2', 'STANDARD', 'standard account type for individual users', '$now', 'david', 1),
       |('3', 'NEW', 'standard account type for individual users', '$now', 'lloyd', 1);""".stripMargin

  override def cleanupSql = "DELETE FROM account_types;"
  val createdAt = now
  val createdBy = "ujali"
  private val upsert = AccountTypeToUpsert(
    id = 1.some,
    accountTypeName = "fee_collection",
    description = "random description".some,
    isActive = true)

  private val accountTypeToUpdate = AccountTypeToUpdate(
    accountTypeName = "update_test".some,
    description = "random description update".some,
    isActive = None,
    updatedAt = now,
    updatedBy = "ujali")

  "AccountTypeSqlDao" should {

    "get all account types" in {

      val expectedResult = Set(
        AccountType(
          id = 1,
          accountTypeName = "WALLET",
          description = "standard account type for individual users".some,
          isActive = upsert.isActive,
          createdAt = now,
          createdBy = "dima",
          updatedAt = none,
          updatedBy = none),
        AccountType(
          id = 2,
          accountTypeName = "STANDARD",
          description = "standard account type for individual users".some,
          isActive = upsert.isActive,
          createdAt = now,
          createdBy = "david",
          updatedAt = none,
          updatedBy = none),
        AccountType(
          id = 3,
          accountTypeName = "NEW",
          description = "standard account type for individual users".some,
          isActive = upsert.isActive,
          createdAt = now,
          createdBy = "lloyd",
          updatedAt = none,
          updatedBy = none))

      val result = accountTypesSqlDao.getAll
      result mustBe Right(expectedResult)
    }

    "update account type" in {
      val expectedResult = AccountType(
        id = 1,
        accountTypeName = accountTypeToUpdate.accountTypeName.getOrElse(upsert.accountTypeName),
        description = accountTypeToUpdate.description,
        isActive = accountTypeToUpdate.isActive.getOrElse(upsert.isActive),
        createdAt = now,
        createdBy = "dima",
        updatedAt = accountTypeToUpdate.updatedAt.some,
        updatedBy = accountTypeToUpdate.updatedBy.some)
      val result = accountTypesSqlDao.update(1, accountTypeToUpdate)

      assert(result.isRight)
      result mustBe Right(expectedResult.some)

    }

    "bulk upsert account_types" in {

      val dto = Seq(
        AccountTypeToUpsert(
          id = 1.some,
          accountTypeName = "loyd_collection",
          description = "random description".some,
          isActive = true),

        AccountTypeToUpsert(
          id = 2.some,
          accountTypeName = "david_distribution",
          description = "random description".some,
          isActive = true),

        AccountTypeToUpsert(
          id = 3.some,
          accountTypeName = "STANDARD",
          description = "random description".some,
          isActive = true))

      val result1 = accountTypesSqlDao.bulkUpsert(dto, now, createdBy)
      result1.isRight mustBe true

      val result2 = accountTypesSqlDao.getAll
      result2.right.get.map(_.accountTypeName) mustBe Set("loyd_collection", "david_distribution", "NEW", "STANDARD", "update_test")
    }

  }
}
