package tech.pegb.backoffice.domain.account

import java.sql.Connection
import java.time.LocalDateTime
import java.util.{UUID}

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.inject.bind
import tech.pegb.backoffice.dao.DaoError
import tech.pegb.backoffice.dao.account.abstraction.ExternalAccountDao
import tech.pegb.backoffice.dao.account.dto.{ExternalAccountCriteria ⇒ DaoExternalAccountCriteria, ExternalAccountToCreate ⇒ DaoExternalAccountToCreate, ExternalAccountToUpdate ⇒ DaoExternalAccountToUpdate}
import tech.pegb.backoffice.dao.account.entity
import tech.pegb.backoffice.dao.currency.abstraction.CurrencyDao
import tech.pegb.backoffice.dao.currency.entity.Currency
import tech.pegb.backoffice.dao.customer.abstraction.UserDao
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.account.abstraction.ExternalAccountManagement
import tech.pegb.backoffice.domain.account.dto.{ExternalAccountCriteria, ExternalAccountToCreate, ExternalAccountToUpdate}
import tech.pegb.backoffice.domain.account.model.ExternalAccount
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.mapping.dao.domain.account.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.account.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.util.{Logging, WithExecutionContexts}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

class ExternalAccountMgmtServiceSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures with Logging {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val externalAccountDao = stub[ExternalAccountDao]
  val customerDao = stub[UserDao]
  val currencyDao = stub[CurrencyDao]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[ExternalAccountDao].to(externalAccountDao),
      bind[UserDao].to(customerDao),
      bind[CurrencyDao].to(currencyDao),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val externalAccountMgmtService = inject[ExternalAccountManagement]

  "ExternalAccountMgmtService" should {

    "create valid external account" in {
      val expectedCreateDto: ExternalAccountToCreate = ExternalAccountToCreate(
        id = UUID.randomUUID(),
        customerId = UUID.randomUUID(),
        externalProvider = "Central Bank of Kenya",
        externalAccountNumber = "0955100",
        externalAccountHolder = "George Ogalo",
        currency = "KES",
        createdBy = "unit test",
        createdAt = LocalDateTime.now())
      val mockUserIdResult = 10
      (customerDao.getInternalUserId _).when(expectedCreateDto.customerId.toString).returns(Right(Some(mockUserIdResult)))

      val mockCurrencyResult = Currency(
        id = 3,
        name = "KES",
        description = Some("Kenyan Shilling"),
        isActive = true,
        icon = None,
        createdBy = "unit test",
        createdAt = LocalDateTime.now,
        updatedBy = None,
        updatedAt = None)
      (currencyDao.getAll _).when().returns(Right(Set(mockCurrencyResult)))

      val expectedCriteria = ExternalAccountCriteria(
        customerId = expectedCreateDto.customerId.toUUIDLike.toOption,
        externalProvider = expectedCreateDto.externalProvider.toOption,
        externalAccountNumber = expectedCreateDto.externalAccountNumber.toOption).asDao()
      (externalAccountDao.getExternalAccountByCriteria(_: DaoExternalAccountCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(expectedCriteria, None, None, None, None).returns(Right(Nil))

      val expectedDaoDto = expectedCreateDto.asDao(mockUserIdResult, mockCurrencyResult.id)
      val mockCreateResult = entity.ExternalAccount(
        id = 1,
        uuid = expectedDaoDto.uuid,
        userId = expectedDaoDto.userId,
        userUuid = expectedCreateDto.customerId,
        provider = expectedCreateDto.externalProvider,
        accountNumber = expectedCreateDto.externalAccountNumber,
        accountHolder = expectedCreateDto.externalAccountHolder,
        currencyId = expectedDaoDto.currencyId,
        currencyName = mockCurrencyResult.name,
        createdBy = expectedDaoDto.createdBy,
        createdAt = expectedDaoDto.createdAt,
        updatedBy = None,
        updatedAt = None)
      (externalAccountDao.insertExternalAccount(_: DaoExternalAccountToCreate)(_: Option[Connection]))
        .when(expectedDaoDto, None).returns(Right(mockCreateResult))

      val result = externalAccountMgmtService.createExternalAccount(expectedCreateDto)

      val expected = ExternalAccount(
        id = expectedCreateDto.id,
        customerId = expectedCreateDto.customerId,
        externalProvider = expectedCreateDto.externalProvider,
        externalAccountNumber = expectedCreateDto.externalAccountNumber,
        externalAccountHolder = expectedCreateDto.externalAccountHolder,
        currency = expectedCreateDto.currency,
        createdBy = expectedCreateDto.createdBy,
        createdAt = expectedCreateDto.createdAt,
        updatedBy = None,
        updatedAt = None)

      whenReady(result) { result ⇒
        result mustBe Right(expected)
      }
    }

    "fail to create duplicate external account" in {
      val expectedCreateDto: ExternalAccountToCreate = ExternalAccountToCreate(
        id = UUID.randomUUID(),
        customerId = UUID.randomUUID(),
        externalProvider = "Central Bank of Kenya",
        externalAccountNumber = "0955100",
        externalAccountHolder = "George Ogalo",
        currency = "KES",
        createdBy = "unit test",
        createdAt = LocalDateTime.now())
      val mockUserIdResult = 10
      (customerDao.getInternalUserId _).when(expectedCreateDto.customerId.toString).returns(Right(Some(mockUserIdResult)))

      val mockCurrencyResult = Currency(
        id = 3,
        name = "KES",
        description = Some("Kenyan Shilling"),
        isActive = true,
        icon = None,
        createdBy = "unit test",
        createdAt = LocalDateTime.now,
        updatedBy = None,
        updatedAt = None)
      (currencyDao.getAll _).when().returns(Right(Set(mockCurrencyResult)))

      val expectedCriteria = ExternalAccountCriteria(
        customerId = expectedCreateDto.customerId.toUUIDLike.toOption,
        externalProvider = expectedCreateDto.externalProvider.toOption,
        externalAccountNumber = expectedCreateDto.externalAccountNumber.toOption).asDao()

      val mockDuplicateResult = Seq(entity.ExternalAccount(
        id = 9,
        uuid = UUID.randomUUID(),
        userId = 6,
        userUuid = UUID.randomUUID(),
        provider = expectedCreateDto.externalProvider,
        accountNumber = expectedCreateDto.externalAccountNumber,
        accountHolder = expectedCreateDto.externalAccountHolder,
        currencyId = 1,
        currencyName = expectedCreateDto.currency,
        createdBy = "unit test",
        createdAt = LocalDateTime.now,
        updatedBy = None,
        updatedAt = None))
      (externalAccountDao.getExternalAccountByCriteria(_: DaoExternalAccountCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(expectedCriteria, None, None, None, None).returns(Right(mockDuplicateResult))

      val result = externalAccountMgmtService.createExternalAccount(expectedCreateDto)

      val expected = ServiceError.duplicateError(s"Existing external account found with same customer_id, provider and account_number.")

      whenReady(result) { result ⇒
        result mustBe Left(expected)
      }
    }

    "fail to create invalid external account (empty required fields)" in {
      val expectedDto: ExternalAccountToCreate = ExternalAccountToCreate(
        id = UUID.randomUUID(),
        customerId = UUID.randomUUID(),
        externalProvider = "",
        externalAccountNumber = "0955100",
        externalAccountHolder = "George Ogalo",
        currency = "KES",
        createdBy = "unit test",
        createdAt = LocalDateTime.now())
      val result = externalAccountMgmtService.createExternalAccount(expectedDto)

      val expected = ServiceError.validationError("Provider cannot be empty")

      whenReady(result) { result ⇒
        result mustBe Left(expected)
      }
    }

    "fail to create invalid external account (invalid currency)" in {
      val expectedDto: ExternalAccountToCreate = ExternalAccountToCreate(
        id = UUID.randomUUID(),
        customerId = UUID.randomUUID(),
        externalProvider = "Central Bank of Kenya",
        externalAccountNumber = "0955100",
        externalAccountHolder = "George Ogalo",
        currency = "XYZ",
        createdBy = "unit test",
        createdAt = LocalDateTime.now())
      val result = externalAccountMgmtService.createExternalAccount(expectedDto)

      val expected = ServiceError.validationError("Invalid currency [XYZ]")

      whenReady(result) { result ⇒
        result mustBe Left(expected)
      }
    }

    "fail to create invalid external account (valid currency but not configured in the platform)" in {
      val expectedDto: ExternalAccountToCreate = ExternalAccountToCreate(
        id = UUID.randomUUID(),
        customerId = UUID.randomUUID(),
        externalProvider = "Central Bank of Kenya",
        externalAccountNumber = "0955100",
        externalAccountHolder = "George Ogalo",
        currency = "KES",
        createdBy = "unit test",
        createdAt = LocalDateTime.now())
      val mockUserIdResult = 10
      (customerDao.getInternalUserId _).when(expectedDto.customerId.toString).returns(Right(Some(mockUserIdResult)))

      (currencyDao.getAll _).when().returns(Right(Set()))

      val result = externalAccountMgmtService.createExternalAccount(expectedDto)

      val expected = ServiceError.validationError(s"Failed to create external account [${expectedDto.toSmartString}]. Currency [${java.util.Currency.getInstance(expectedDto.currency).getDisplayName}] is not configured in the system.")

      whenReady(result) { result ⇒
        result mustBe Left(expected)
      }
    }

    "fail to create invalid external account (customer id not found)" in {
      val expectedDto: ExternalAccountToCreate = ExternalAccountToCreate(
        id = UUID.randomUUID(),
        customerId = UUID.randomUUID(),
        externalProvider = "Central Bank of Kenya",
        externalAccountNumber = "0955100",
        externalAccountHolder = "George Ogalo",
        currency = "KES",
        createdBy = "unit test",
        createdAt = LocalDateTime.now())

      (customerDao.getInternalUserId _).when(expectedDto.customerId.toString).returns(Right(None))

      val result = externalAccountMgmtService.createExternalAccount(expectedDto)

      val expected = ServiceError.validationError(s"Failed to create external account [${expectedDto.toSmartString}]. Customer id [${expectedDto.customerId}] is not found.")

      whenReady(result) { result ⇒
        result mustBe Left(expected)
      }
    }

    "get external account by criteria" in {
      val criteria: ExternalAccountCriteria = ExternalAccountCriteria(
        customerId = Some(UUID.randomUUID().toUUIDLike),
        externalProvider = Some("Central Bank of Kenya"))

      val mockResults = Seq(entity.ExternalAccount(
        id = 10,
        uuid = UUID.randomUUID(),
        userId = 8,
        userUuid = criteria.customerId.get.toUUID.get,
        provider = "Central Bank of Kenya",
        accountNumber = "0009810189001",
        accountHolder = "Lloyd Edano",
        currencyId = 3,
        currencyName = "KES",
        createdBy = "unit test",
        createdAt = LocalDateTime.now,
        updatedBy = None,
        updatedAt = None),
        entity.ExternalAccount(
          id = 10,
          uuid = UUID.randomUUID(),
          userId = 8,
          userUuid = criteria.customerId.get.toUUID.get,
          provider = "Central Bank of Kenya",
          accountNumber = "0009810189002",
          accountHolder = "Alex Kim",
          currencyId = 6,
          currencyName = "AED",
          createdBy = "unit test",
          createdAt = LocalDateTime.now,
          updatedBy = None,
          updatedAt = None),
        entity.ExternalAccount(
          id = 10,
          uuid = UUID.randomUUID(),
          userId = 8,
          userUuid = criteria.customerId.get.toUUID.get,
          provider = "Central Bank of Kenya",
          accountNumber = "0009810189003",
          accountHolder = "George Ogalo",
          currencyId = 7,
          currencyName = "USD",
          createdBy = "unit test",
          createdAt = LocalDateTime.now,
          updatedBy = None,
          updatedAt = None)
      )
      (externalAccountDao.getExternalAccountByCriteria(_: DaoExternalAccountCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(criteria.asDao(), None, None, None, None).returns(Right(mockResults))

      val result = externalAccountMgmtService.getExternalAccountByCriteria(criteria, Nil, None, None)

      val expected = mockResults.map(_.asDomain)

      whenReady(result) { result ⇒
        result mustBe Right(expected)
      }
    }

    "get external account with inconsistent data without failing" in {
      val criteria: ExternalAccountCriteria = ExternalAccountCriteria(
        customerId = Some(UUID.randomUUID().toUUIDLike),
        externalProvider = Some("Central Bank of Kenya"))

      val mockResults = Seq(entity.ExternalAccount(
        id = 10,
        uuid = UUID.randomUUID(),
        userId = 8,
        userUuid = criteria.customerId.get.toUUID.get,
        provider = "Central Bank of Kenya",
        accountNumber = "", //invalid
        accountHolder = "Lloyd Edano",
        currencyId = 3,
        currencyName = "KES",
        createdBy = "unit test",
        createdAt = LocalDateTime.now,
        updatedBy = None,
        updatedAt = None),
        entity.ExternalAccount(
          id = 10,
          uuid = UUID.randomUUID(),
          userId = 8,
          userUuid = criteria.customerId.get.toUUID.get,
          provider = "Central Bank of Kenya",
          accountNumber = "0009810189002",
          accountHolder = "Alex Kim",
          currencyId = 6,
          currencyName = "123", //invalid
          createdBy = "unit test",
          createdAt = LocalDateTime.now,
          updatedBy = None,
          updatedAt = None),
        entity.ExternalAccount(
          id = 10,
          uuid = UUID.randomUUID(),
          userId = 8,
          userUuid = criteria.customerId.get.toUUID.get,
          provider = "Central Bank of Kenya",
          accountNumber = "0009810189003",
          accountHolder = "    ", //invalid
          currencyId = 7,
          currencyName = "USD",
          createdBy = "unit test",
          createdAt = LocalDateTime.now,
          updatedBy = None,
          updatedAt = None)
      )
      (externalAccountDao.getExternalAccountByCriteria(_: DaoExternalAccountCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(criteria.asDao(), None, None, None, None).returns(Right(mockResults))

      val result = externalAccountMgmtService.getExternalAccountByCriteria(criteria, Nil, None, None)

      val expected = mockResults.map(_.asDomain)

      whenReady(result) { result ⇒
        result mustBe Right(expected)
      }
    }

    "get count of external accounts based on criteria" in {
      val criteria: ExternalAccountCriteria = ExternalAccountCriteria(
        customerId = Some(UUID.randomUUID().toUUIDLike),
        externalProvider = Some("Central Bank of Kenya"))

      val mockCount = 10
      (externalAccountDao.countExternalAccount (_: DaoExternalAccountCriteria)(_: Option[Connection]))
        .when(criteria.asDao(), None).returns(Right(mockCount))

      val result = externalAccountMgmtService.count(criteria)

      val expected = mockCount

      whenReady(result) { result ⇒
        result mustBe Right(expected)
      }
    }

    "get latest version of external accounts based on criteria" in {
      val criteria: ExternalAccountCriteria = ExternalAccountCriteria(
        customerId = Some(UUID.randomUUID().toUUIDLike),
        externalProvider = Some("Central Bank of Kenya"))

      val mockResult = entity.ExternalAccount(
        id = 10,
        uuid = UUID.randomUUID(),
        userId = 8,
        userUuid = criteria.customerId.get.toUUID.get,
        provider = "Central Bank of Kenya",
        accountNumber = "0009810189003",
        accountHolder = "Ujali Tyagi",
        currencyId = 7,
        currencyName = "USD",
        createdBy = "unit test",
        createdAt = LocalDateTime.now,
        updatedBy = Some("unit tester"),
        updatedAt = Some(LocalDateTime.now))
      val expectedOrderBy = Seq(Ordering("updated_at", Ordering.DESCENDING))
      (externalAccountDao.getExternalAccountByCriteria(_: DaoExternalAccountCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(criteria.asDao(), expectedOrderBy.asDao, Some(1), None, None).returns(Right(Seq(mockResult)))

      val result = externalAccountMgmtService.getLatestVersion(criteria)

      val expected = mockResult.asDomain

      whenReady(result) { result ⇒
        result mustBe Right(Some(expected))
      }
    }

    "update external account" in {
      val criteria: ExternalAccountCriteria = ExternalAccountCriteria(
        id = Some(UUID.randomUUID().toUUIDLike),
        customerId = Some(UUID.randomUUID().toUUIDLike),
      )
      val updateDto: ExternalAccountToUpdate = ExternalAccountToUpdate(
        externalProvider = Some("Noor Bank"),
        externalAccountHolder = Some("Alex Kim"),
        updatedBy = "unit test",
        updatedAt = LocalDateTime.now())

      val expectedDuplicateFinderCriteria = ExternalAccountCriteria(
        customerId = criteria.customerId,
        externalProvider = updateDto.externalProvider,
        externalAccountNumber = updateDto.externalAccountNumber).asDao()
      (externalAccountDao.getExternalAccountByCriteria(_: DaoExternalAccountCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(expectedDuplicateFinderCriteria, None, None, None, None).returns(Right(Nil))

      val mockResultForNonFlattenedCriteria = entity.ExternalAccount(
        id = 1,
        uuid = criteria.id.get.toUUID.get,
        userId = 1,
        userUuid = criteria.customerId.get.toUUID.get,
        provider = "old provider",
        accountNumber = "00011119998001",
        accountHolder = "old account holder",
        currencyId = 8,
        currencyName = "SAR",
        createdBy = "unit test",
        createdAt = LocalDateTime.now,
        updatedBy = None,
        updatedAt = None)
      (externalAccountDao.getExternalAccountByCriteria(_: DaoExternalAccountCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(criteria.asDao(), None, None, None, None).returns(Right(Seq(mockResultForNonFlattenedCriteria)))

      val mockLatestVersionResult =
        entity.ExternalAccount(
          id = 1,
          uuid = criteria.id.get.toUUID.get,
          userId = 1,
          userUuid = criteria.customerId.get.toUUID.get,
          provider = "old provider",
          accountNumber = "00011119998001",
          accountHolder = "old account holder",
          currencyId = 8,
          currencyName = "SAR",
          createdBy = "unit test",
          createdAt = LocalDateTime.now,
          updatedBy = None,
          updatedAt = None)
      (externalAccountDao.getExternalAccountByCriteria(_: DaoExternalAccountCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(criteria.asDao, Seq(Ordering("updated_at", Ordering.DESCENDING)).asDao, Some(1), None, None).returns(Right(Seq(mockLatestVersionResult)))


      val expectedDaoDto = updateDto.asDao(None)
      val mockUpdateResult = entity.ExternalAccount(
        id = 1,
        uuid = UUID.randomUUID(),
        userId = 1,
        userUuid = UUID.randomUUID(),
        provider = expectedDaoDto.provider.getOrElse("Not Available"),
        accountNumber = "00011119998001",
        accountHolder = expectedDaoDto.accountHolder.getOrElse("Not Available"),
        currencyId = 8,
        currencyName = "SAR",
        createdBy = "unit test",
        createdAt = LocalDateTime.now,
        updatedBy = expectedDaoDto.updatedBy,
        updatedAt = expectedDaoDto.updatedAt)
      val expectedFlattenedCriteria = ExternalAccountCriteria(anyIds = Some(Set(mockResultForNonFlattenedCriteria.uuid)))
      (externalAccountDao.updateExternalAccount(_: DaoExternalAccountCriteria, _: DaoExternalAccountToUpdate)(_: Option[Connection]))
        .when(expectedFlattenedCriteria.asDao, expectedDaoDto, None).returns(Right(Seq(mockUpdateResult)))

      val result = externalAccountMgmtService.updateExternalAccount(criteria, updateDto)

      val expected = ExternalAccount(
        id = mockUpdateResult.uuid,
        customerId = mockUpdateResult.userUuid,
        externalProvider = mockUpdateResult.provider,
        externalAccountNumber = mockUpdateResult.accountNumber,
        externalAccountHolder = mockUpdateResult.accountHolder,
        currency = mockUpdateResult.currencyName,
        createdBy = mockUpdateResult.createdBy,
        createdAt = mockUpdateResult.createdAt,
        updatedBy = mockUpdateResult.updatedBy,
        updatedAt = mockUpdateResult.updatedAt)

      whenReady(result) { result ⇒
        result mustBe Right(expected)
      }
    }

    "fail to update external account (currency is valid but not configured)" in {
      val criteria: ExternalAccountCriteria = ExternalAccountCriteria(
        id = Some(UUID.randomUUID().toUUIDLike),
        customerId = Some(UUID.randomUUID().toUUIDLike),
      )
      val updateDto: ExternalAccountToUpdate = ExternalAccountToUpdate(
        externalProvider = Some("Noor Bank"),
        currency = Some("AED"),
        updatedBy = "unit test",
        updatedAt = LocalDateTime.now())

      (currencyDao.getAll _).when().returns(Right(Set()))

      val result = externalAccountMgmtService.updateExternalAccount(criteria, updateDto)

      val expected = ServiceError.notFoundError(s"Failed to update external account [${updateDto.toSmartString}]. Currency [${updateDto.currency.get}] is not configured in the system.")

      whenReady(result) { result ⇒
        result mustBe Left(expected)
      }
    }

    "fail to update external account (because a successful update will lead to duplicate)" in {
      val criteria: ExternalAccountCriteria = ExternalAccountCriteria(
        id = Some(UUID.randomUUID().toUUIDLike),
        customerId = Some(UUID.randomUUID().toUUIDLike),
      )
      val updateDto: ExternalAccountToUpdate = ExternalAccountToUpdate(
        externalProvider = Some("Noor Bank"),
        externalAccountHolder = Some("Alex Kim"),
        updatedBy = "unit test",
        updatedAt = LocalDateTime.now())

      val expectedDuplicateFinderCriteria = ExternalAccountCriteria(
        customerId = criteria.customerId,
        externalProvider = updateDto.externalProvider,
        externalAccountNumber = updateDto.externalAccountNumber).asDao()
      val mockDuplicateResult = entity.ExternalAccount(
        id = 10,
        uuid = UUID.randomUUID(),
        userId = 8,
        userUuid = criteria.customerId.get.toUUID.get,
        provider = updateDto.externalProvider.getOrElse("Not Available"),
        accountNumber = updateDto.externalAccountNumber.getOrElse("Not Available"),
        accountHolder = "some external account holder",
        currencyId = 6,
        currencyName = "KES",
        createdBy = "unit test",
        createdAt = LocalDateTime.now,
        updatedBy = Some(updateDto.updatedBy),
        updatedAt = Some(updateDto.updatedAt)
      )
      (externalAccountDao.getExternalAccountByCriteria(_: DaoExternalAccountCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(expectedDuplicateFinderCriteria, None, None, None, None).returns(Right(Seq(mockDuplicateResult)))


      val result = externalAccountMgmtService.updateExternalAccount(criteria, updateDto)

      val expected = ServiceError.notFoundError(s"Duplicate external account found if update [${updateDto.toSmartString}] is applied using criteria [${criteria.toSmartString}].")

      whenReady(result) { result ⇒
        result mustBe Left(expected)
      }
    }

    "fail to update invalid external account (empty required fields)" in {
      val criteria: ExternalAccountCriteria = ExternalAccountCriteria(
        id = Some(UUID.randomUUID().toUUIDLike),
        customerId = Some(UUID.randomUUID().toUUIDLike),
      )
      val updateDto: ExternalAccountToUpdate = ExternalAccountToUpdate(
        externalProvider = Some("Noor Bank"),
        externalAccountHolder = Some(""),
        updatedBy = "unit test",
        updatedAt = LocalDateTime.now())

      val result = externalAccountMgmtService.updateExternalAccount(criteria, updateDto)

      val expected = ServiceError.validationError("Account holder cannot be empty")

      whenReady(result) { result ⇒
        result mustBe Left(expected)
      }
    }

    "fail to update external account not owned by customer (not found)" in {
      val criteria: ExternalAccountCriteria = ExternalAccountCriteria(
        id = Some(UUID.randomUUID().toUUIDLike),
        customerId = Some(UUID.randomUUID().toUUIDLike),
      )
      val updateDto: ExternalAccountToUpdate = ExternalAccountToUpdate(
        externalProvider = Some("Noor Bank"),
        externalAccountHolder = Some("Alex Kim"),
        updatedBy = "unit test",
        updatedAt = LocalDateTime.now())

      val expectedDuplicateFinderCriteria = ExternalAccountCriteria(
        customerId = criteria.customerId,
        externalProvider = updateDto.externalProvider,
        externalAccountNumber = updateDto.externalAccountNumber).asDao()
      (externalAccountDao.getExternalAccountByCriteria(_: DaoExternalAccountCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(expectedDuplicateFinderCriteria, None, None, None, None).returns(Right(Nil))

      val mockResultForNonFlattenedCriteria = Nil
      (externalAccountDao.getExternalAccountByCriteria(_: DaoExternalAccountCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(criteria.asDao(), None, None, None, None).returns(Right(mockResultForNonFlattenedCriteria))

      val result = externalAccountMgmtService.updateExternalAccount(criteria, updateDto)

      val expected = ServiceError.notFoundError(s"External account using this criteria [${criteria.toSmartString}] was not found.")

      whenReady(result) { result ⇒
        result mustBe Left(expected)
      }
    }

    "fail to update invalid external account if last updated_at is not most recent" in {
      val criteria: ExternalAccountCriteria = ExternalAccountCriteria(
        id = Some(UUID.randomUUID().toUUIDLike),
        customerId = Some(UUID.randomUUID().toUUIDLike),
      )
      val updateDto: ExternalAccountToUpdate = ExternalAccountToUpdate(
        externalProvider = Some("Noor Bank"),
        externalAccountHolder = Some("Alex Kim"),
        updatedBy = "unit test",
        updatedAt = LocalDateTime.now())

      val expectedDuplicateFinderCriteria = ExternalAccountCriteria(
        customerId = criteria.customerId,
        externalProvider = updateDto.externalProvider,
        externalAccountNumber = updateDto.externalAccountNumber).asDao()
      (externalAccountDao.getExternalAccountByCriteria(_: DaoExternalAccountCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(expectedDuplicateFinderCriteria, None, None, None, None).returns(Right(Nil))

      val mockResultForNonFlattenedCriteria = entity.ExternalAccount(
        id = 1,
        uuid = criteria.id.get.toUUID.get,
        userId = 1,
        userUuid = criteria.customerId.get.toUUID.get,
        provider = "old provider",
        accountNumber = "00011119998001",
        accountHolder = "old account holder",
        currencyId = 8,
        currencyName = "SAR",
        createdBy = "unit test",
        createdAt = LocalDateTime.now,
        updatedBy = None,
        updatedAt = None)
      (externalAccountDao.getExternalAccountByCriteria(_: DaoExternalAccountCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(criteria.asDao(), None, None, None, None).returns(Right(Seq(mockResultForNonFlattenedCriteria)))

      (externalAccountDao.getExternalAccountByCriteria(_: DaoExternalAccountCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(criteria.asDao(), Seq(Ordering("updated_at", Ordering.DESCENDING)).asDao, Some(1), None, None).returns(Right(Nil))

      val expectedFlattenedCriteria = ExternalAccountCriteria(anyIds = Some(Set(mockResultForNonFlattenedCriteria.uuid)))
      val expectedDaoDto = updateDto.asDao(None)
      val mockDaoResult = DaoError.PreconditionFailed("Some error message from dao layer")
      (externalAccountDao.updateExternalAccount(_: DaoExternalAccountCriteria, _: DaoExternalAccountToUpdate)(_: Option[Connection]))
        .when(expectedFlattenedCriteria.asDao(), expectedDaoDto, None).returns(Left(mockDaoResult))

      val result = externalAccountMgmtService.updateExternalAccount(criteria, updateDto)

      val expected = ServiceError.staleResourceAccessError(s"Some error message from dao layer")

      whenReady(result) { result ⇒
        result mustBe Left(expected)
      }
    }

    "delete external account" in {
      val criteria: ExternalAccountCriteria = ExternalAccountCriteria(
        id = Some(UUID.randomUUID().toUUIDLike),
        customerId = Some(UUID.randomUUID().toUUIDLike),
      )
      val lastUpdatedAt = None

      val expectedDeleteCriteria = criteria.asDao()
      val mockLatestVersionResult = entity.ExternalAccount(
        id = 10,
        uuid = UUID.randomUUID(),
        userId = 8,
        userUuid = criteria.customerId.get.toUUID.get,
        provider = "RAK Bank",
        accountNumber = "0192898920202021",
        accountHolder = "Ujali Tyagi",
        currencyId = 6,
        currencyName = "AED",
        createdBy = "unit test",
        createdAt = LocalDateTime.now,
        updatedBy = Some("unit test"),
        updatedAt = Some(LocalDateTime.of(2020, 4, 10, 0, 0))
      )
      (externalAccountDao.getExternalAccountByCriteria(_: DaoExternalAccountCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(expectedDeleteCriteria, Seq(Ordering("updated_at", Ordering.DESCENDING)).asDao, Some(1), None, None).returns(Right(Seq(mockLatestVersionResult)))

      (externalAccountDao.deleteExternalAccount(_: DaoExternalAccountCriteria, _: Option[LocalDateTime])(_: Option[Connection]))
        .when(expectedDeleteCriteria, lastUpdatedAt.orElse(mockLatestVersionResult.updatedAt), None).returns(Right(Option(())))

      val result = externalAccountMgmtService.deleteExternalAccount(criteria, lastUpdatedAt)

      val expected = ()

      whenReady(result) { result ⇒
        result mustBe Right(expected)
      }
    }

    "not fail if external account to be deleted was not found (ex. not owned by customer)" in {
      val criteria: ExternalAccountCriteria = ExternalAccountCriteria(
        id = Some(UUID.randomUUID().toUUIDLike),
        customerId = Some(UUID.randomUUID().toUUIDLike),
      )
      val lastUpdatedAt = None

      val expectedDeleteCriteria = criteria.asDao()
      val mockLatestVersionResult = entity.ExternalAccount(
        id = 10,
        uuid = UUID.randomUUID(),
        userId = 8,
        userUuid = criteria.customerId.get.toUUID.get,
        provider = "RAK Bank",
        accountNumber = "0192898920202021",
        accountHolder = "Ujali Tyagi",
        currencyId = 6,
        currencyName = "AED",
        createdBy = "unit test",
        createdAt = LocalDateTime.now,
        updatedBy = Some("unit test"),
        updatedAt = Some(LocalDateTime.of(2020, 4, 10, 0, 0))
      )
      (externalAccountDao.getExternalAccountByCriteria(_: DaoExternalAccountCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(expectedDeleteCriteria, Seq(Ordering("updated_at", Ordering.DESCENDING)).asDao, Some(1), None, None).returns(Right(Seq(mockLatestVersionResult)))

      (externalAccountDao.deleteExternalAccount(_: DaoExternalAccountCriteria, _: Option[LocalDateTime])(_: Option[Connection]))
        .when(expectedDeleteCriteria, lastUpdatedAt.orElse(mockLatestVersionResult.updatedAt), None).returns(Right(None))

      val result = externalAccountMgmtService.deleteExternalAccount(criteria, lastUpdatedAt)

      val expected = ()

      whenReady(result) { result ⇒
        result mustBe Right(expected)
      }
    }

    "fail to delete external account if last updated_at is not most recent" in {
      val criteria: ExternalAccountCriteria = ExternalAccountCriteria(
        id = Some(UUID.randomUUID().toUUIDLike),
        customerId = Some(UUID.randomUUID().toUUIDLike),
      )
      val lastUpdatedAt = Some(LocalDateTime.of(2019, 1, 1, 0, 0))

      val expectedDeleteCriteria = criteria.asDao()
      val mockLatestVersionResult = entity.ExternalAccount(
        id = 10,
        uuid = UUID.randomUUID(),
        userId = 8,
        userUuid = criteria.customerId.get.toUUID.get,
        provider = "RAK Bank",
        accountNumber = "0192898920202021",
        accountHolder = "Ujali Tyagi",
        currencyId = 6,
        currencyName = "AED",
        createdBy = "unit test",
        createdAt = LocalDateTime.now,
        updatedBy = Some("unit test"),
        updatedAt = Some(LocalDateTime.of(2020, 4, 10, 0, 0))
      )
      (externalAccountDao.getExternalAccountByCriteria(_: DaoExternalAccountCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(expectedDeleteCriteria, Seq(Ordering("updated_at", Ordering.DESCENDING)).asDao, Some(1), None, None).returns(Right(Seq(mockLatestVersionResult)))

      (externalAccountDao.deleteExternalAccount(_: DaoExternalAccountCriteria, _: Option[LocalDateTime])(_: Option[Connection]))
        .when(expectedDeleteCriteria, lastUpdatedAt.orElse(mockLatestVersionResult.updatedAt), None).returns(Left(DaoError.PreconditionFailed("some dao layer error")))

      val result = externalAccountMgmtService.deleteExternalAccount(criteria, lastUpdatedAt)

      val expected = ServiceError.staleResourceAccessError("some dao layer error")

      whenReady(result) { result ⇒
        result mustBe Left(expected)
      }
    }

  }
}
