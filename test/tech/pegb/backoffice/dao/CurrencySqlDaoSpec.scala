package tech.pegb.backoffice.dao

import java.time._
import java.time.format.DateTimeFormatter

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import tech.pegb.backoffice.dao.currency.dto.{CurrencyToUpdate, CurrencyToUpsert}
import tech.pegb.backoffice.dao.currency.entity.Currency
import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao
import tech.pegb.backoffice.util.Utils
import tech.pegb.core.PegBTestApp

class CurrencySqlDaoSpec extends PegBTestApp with MockFactory {

  val createdAt = Utils.now().withNano(0)
  lazy val currencySqlDao = fakeApplication().injector.instanceOf[CurrencySqlDao]

  val now = LocalDateTime.now(Clock.fixed(Instant.ofEpochMilli(1563288490000L), ZoneId.systemDefault()))
  val localDateTimeTest = LocalDateTime.parse(
    createdAt.format(DateTimeFormatter.ISO_ZONED_DATE_TIME),
    DateTimeFormatter.ISO_ZONED_DATE_TIME)

  override def initSql: String =
    s"""
       |INSERT INTO currencies(id, currency_name, description, created_at, created_by, is_active, icon)
       |VALUES('1', 'KES', 'kenya shillings', '$now', 'dima', 1, 'icon_one'),
       |('2', 'EUR', 'euro', '$now', 'ujali', 0, 'icon_one');""".stripMargin

  override def cleanupSql =
    "DELETE FROM currencies;"

  "CurrencySqlDao" should {

    "update currency" in {

      val currencyToUpdate = CurrencyToUpdate(
        currencyName = "USD".some,
        description = "randomly updated".some,
        isActive = none,
        icon = "test".some,
        updatedAt = localDateTimeTest,
        updatedBy = "SuperUser")
      val expectedResult = Currency(
        id = 2,
        name = currencyToUpdate.currencyName.getOrElse(""),
        description = currencyToUpdate.description,
        isActive = currencyToUpdate.isActive.getOrElse(true),
        icon = currencyToUpdate.icon,
        createdAt = now,
        createdBy = "ujali",
        updatedAt = currencyToUpdate.updatedAt.some,
        updatedBy = currencyToUpdate.updatedBy.some)

      val result = currencySqlDao.update(2, currencyToUpdate)

      assert(result.isRight)
      result mustBe Right(expectedResult.some)
    }

    "get all currencies" in {
      val expectedResult = Set(
        Currency(
          id = 1,
          name = "KES",
          description = "kenya shillings".some,
          isActive = true,
          icon = "icon_one".some,
          createdAt = now,
          createdBy = "dima",
          updatedAt = none,
          updatedBy = none),
        Currency(
          id = 2,
          name = "USD",
          description = "randomly updated".some,
          isActive = true,
          icon = "test".some,
          createdAt = now,
          createdBy = "ujali",
          updatedAt = localDateTimeTest.some,
          updatedBy = "SuperUser".some))

      val result = currencySqlDao.getAll

      assert(result.isRight)
      result mustBe Right(expectedResult.toSet)
    }

    "get all currencies name" in {

      val expectedResult = Set("KES", "USD")

      val result = currencySqlDao.getAllNames

      assert(result.isRight)
      result mustBe Right(expectedResult)
    }

    "get currency name and id" in {
      val expectedResult = Seq((1, "KES"), (2, "USD"))

      val result = currencySqlDao.getCurrenciesWithId(None)

      assert(result.isRight)
      result mustBe Right(expectedResult)
    }

    "get currency name and id (isActive = true)" in {
      val expectedResult = Seq((1, "KES"))

      val result = currencySqlDao.getCurrenciesWithId(true.some)

      assert(result.isRight)
      result mustBe Right(expectedResult)
    }

    "get currency name,description and id" in {
      val expectedResult = Seq((1, "KES", "kenya shillings"), (2, "USD", "randomly updated"))

      val result = currencySqlDao.getCurrenciesWithIdExtended

      assert(result.isRight)
      result mustBe Right(expectedResult)
    }

    "check if currency is active by name" in {

      val result = currencySqlDao.isCurrencyActive("KES")

      result mustBe Right(true)
    }

    "bulk upsert currencies" in {
      val createdAt = localDateTimeTest
      val createdBy = "SuperUser"

      val dto = Seq(
        CurrencyToUpsert(
          id = 1.some,
          name = "PHP",
          description = "Philippine Peso".some,
          isActive = true,
          icon = none),

        CurrencyToUpsert(
          id = 2.some,
          name = "AED",
          description = "UAE Dirhams".some,
          isActive = true,
          icon = none),

        CurrencyToUpsert(
          id = 3.some,
          name = "KES",
          description = "Another duplicate KES".some,
          isActive = true,
          icon = none))

      val result1 = currencySqlDao.bulkUpsert(dto, createdAt, createdBy)
      result1.isRight mustBe true

      val result2 = currencySqlDao.getAll
      result2.right.get.map(_.name) mustBe Set("PHP", "AED", "KES")
    }

  }
}
