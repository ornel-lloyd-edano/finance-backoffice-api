package tech.pegb.backoffice.domain.transaction

import java.sql.Connection
import java.time.LocalDateTime
import java.util.UUID

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.inject.bind
import tech.pegb.backoffice.dao.DaoError
import tech.pegb.backoffice.dao.account.abstraction.ExternalAccountDao
import tech.pegb.backoffice.dao.account.dto.{ExternalAccountCriteria ⇒ DaoExternalAccountCriteria, ExternalAccountToCreate ⇒ DaoExternalAccountToCreate, ExternalAccountToUpdate ⇒ DaoExternalAccountToUpdate}
import tech.pegb.backoffice.dao.currency.abstraction.CurrencyDao
import tech.pegb.backoffice.dao.currency.entity.Currency
import tech.pegb.backoffice.dao.customer.abstraction.UserDao
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.transaction.abstraction.TransactionConfigDao
import tech.pegb.backoffice.dao.transaction.dto.{TxnConfigCriteria ⇒ DaoTxnConfigCriteria, TxnConfigToCreate ⇒ DaoTxnConfigToCreate, TxnConfigToUpdate⇒ DaoTxnConfigToUpdate}
import tech.pegb.backoffice.dao.transaction.entity
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.account.abstraction.ExternalAccountManagement
import tech.pegb.backoffice.domain.account.dto.{ExternalAccountCriteria, ExternalAccountToCreate, ExternalAccountToUpdate}
import tech.pegb.backoffice.domain.account.model.ExternalAccount
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.domain.transaction.abstraction.TransactionConfigManagement
import tech.pegb.backoffice.domain.transaction.dto.{TxnConfigCriteria, TxnConfigToCreate, TxnConfigToUpdate}
import tech.pegb.backoffice.domain.transaction.model.TxnConfig
import tech.pegb.backoffice.mapping.dao.domain.account.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.account.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.transaction.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.types.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.transaction.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{Logging, WithExecutionContexts}
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

class TxnConfigMgmtServiceSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures with Logging {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val txnConfigDao = stub[TransactionConfigDao]
  val customerDao = stub[UserDao]
  val currencyDao = stub[CurrencyDao]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[TransactionConfigDao].to(txnConfigDao),
      bind[UserDao].to(customerDao),
      bind[CurrencyDao].to(currencyDao),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val txnConfigMgmtService = inject[TransactionConfigManagement]

  "TxnConfigMgmtService" should {

    "create valid txn config" in {
      val expectedCreateDto = TxnConfigToCreate(
        id = UUID.randomUUID(),
        customerId = UUID.randomUUID(),
        transactionType = "cashout",
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

      val mockTxnTypesResult = List((1, "cashout", None), (2, "cashin", None), (3, "p2p", None), (4, "international_remittance", None))
      (typesDao.getTransactionTypes _).when().returns(Right(mockTxnTypesResult))

      val expectedCriteria = TxnConfigCriteria(
        customerId = expectedCreateDto.customerId.toUUIDLike.toOption,
        transactionType = expectedCreateDto.transactionType.toOption,
        currency = expectedCreateDto.currency.toOption).asDao()
      (txnConfigDao.getTxnConfigByCriteria(_: DaoTxnConfigCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(expectedCriteria, None, None, None, None).returns(Right(Nil))

      val expectedDaoDto = expectedCreateDto.asDao(customerId = mockUserIdResult, currencyId = mockCurrencyResult.id)

      val mockCreateResult = entity.TxnConfig(
        id = 1,
        uuid = expectedDaoDto.uuid,
        userId = expectedDaoDto.userId,
        userUuid = expectedCreateDto.customerId,
        transactionType = expectedCreateDto.transactionType,
        currencyId = expectedDaoDto.currencyId,
        currencyName = mockCurrencyResult.name,
        createdBy = expectedDaoDto.createdBy,
        createdAt = expectedDaoDto.createdAt,
        updatedBy = None,
        updatedAt = None)
      (txnConfigDao.insertTxnConfig(_: DaoTxnConfigToCreate)(_: Option[Connection]))
        .when(expectedDaoDto, None).returns(Right(mockCreateResult))

      val result = txnConfigMgmtService.createTxnConfig(expectedCreateDto)

      val expected = TxnConfig(
        id = expectedCreateDto.id,
        customerId = expectedCreateDto.customerId,
        transactionType = expectedCreateDto.transactionType,
        currency = expectedCreateDto.currency,
        createdBy = expectedCreateDto.createdBy,
        createdAt = expectedCreateDto.createdAt,
        updatedBy = None,
        updatedAt = None)

      whenReady(result) { result ⇒
        result mustBe Right(expected)
      }
    }

    "fail to create duplicate txn config" in {
      val expectedCreateDto = TxnConfigToCreate(
        id = UUID.randomUUID(),
        customerId = UUID.randomUUID(),
        transactionType = "cashout",
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

      val mockTxnTypesResult = List((1, "cashout", None), (2, "cashin", None), (3, "p2p", None), (4, "international_remittance", None))
      (typesDao.getTransactionTypes _).when().returns(Right(mockTxnTypesResult))

      val expectedCriteria = TxnConfigCriteria(
        customerId = expectedCreateDto.customerId.toUUIDLike.toOption,
        transactionType = expectedCreateDto.transactionType.toOption,
        currency = expectedCreateDto.currency.toOption).asDao()
      val mockDuplicateResult = Seq(entity.TxnConfig(
        id = 1,
        uuid = UUID.randomUUID(),
        userId = mockUserIdResult,
        userUuid = expectedCreateDto.customerId,
        transactionType = expectedCreateDto.transactionType,
        currencyId = mockCurrencyResult.id,
        currencyName = mockCurrencyResult.name,
        createdBy = expectedCreateDto.createdBy,
        createdAt = expectedCreateDto.createdAt,
        updatedBy = None,
        updatedAt = None))
      (txnConfigDao.getTxnConfigByCriteria(_: DaoTxnConfigCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(expectedCriteria, None, None, None, None).returns(Right(mockDuplicateResult))

      val result = txnConfigMgmtService.createTxnConfig(expectedCreateDto)

      val expected = ServiceError.duplicateError(s"Existing txn config found with same customer_id, transaction_type and currency.")

      whenReady(result) { result ⇒
        result mustBe Left(expected)
      }
    }

    "fail to create invalid txn config (empty required fields)" in {
      val expectedCreateDto = TxnConfigToCreate(
        id = UUID.randomUUID(),
        customerId = UUID.randomUUID(),
        transactionType = "",
        currency = "KES",
        createdBy = "unit test",
        createdAt = LocalDateTime.now())
      val result = txnConfigMgmtService.createTxnConfig(expectedCreateDto)

      val expected = ServiceError.validationError("Transaction type cannot be empty")

      whenReady(result) { result ⇒
        result mustBe Left(expected)
      }
    }

    "fail to create invalid txn config (invalid currency)" in {
      val expectedCreateDto = TxnConfigToCreate(
        id = UUID.randomUUID(),
        customerId = UUID.randomUUID(),
        transactionType = "cashout",
        currency = "123",
        createdBy = "unit test",
        createdAt = LocalDateTime.now())
      val result = txnConfigMgmtService.createTxnConfig(expectedCreateDto)

      val expected = ServiceError.validationError("Invalid currency [123]")

      whenReady(result) { result ⇒
        result mustBe Left(expected)
      }
    }

    "fail to create invalid txn config (valid currency but not configured in the platform)" in {
      val expectedCreateDto = TxnConfigToCreate(
        id = UUID.randomUUID(),
        customerId = UUID.randomUUID(),
        transactionType = "cashout",
        currency = "AED",
        createdBy = "unit test",
        createdAt = LocalDateTime.now())
      val mockUserIdResult = 10
      (customerDao.getInternalUserId _).when(expectedCreateDto.customerId.toString).returns(Right(Some(mockUserIdResult)))

      (currencyDao.getAll _).when().returns(Right(Set()))

      val result = txnConfigMgmtService.createTxnConfig(expectedCreateDto)

      val expected = ServiceError.validationError(s"Failed to create txn config [${expectedCreateDto.toSmartString}]. Currency [${java.util.Currency.getInstance(expectedCreateDto.currency).getDisplayName}] is not configured in the system.")

      whenReady(result) { result ⇒
        result mustBe Left(expected)
      }
    }

    "fail to create invalid external account (customer id not found)" in {
      val expectedCreateDto = TxnConfigToCreate(
        id = UUID.randomUUID(),
        customerId = UUID.randomUUID(),
        transactionType = "cashout",
        currency = "AED",
        createdBy = "unit test",
        createdAt = LocalDateTime.now())

      (customerDao.getInternalUserId _).when(expectedCreateDto.customerId.toString).returns(Right(None))

      val result = txnConfigMgmtService.createTxnConfig(expectedCreateDto)

      val expected = ServiceError.validationError(s"Failed to create txn config [${expectedCreateDto.toSmartString}]. Customer id [${expectedCreateDto.customerId}] is not found.")

      whenReady(result) { result ⇒
        result mustBe Left(expected)
      }
    }

    "get txn config by criteria" in {
      val criteria = TxnConfigCriteria(
        customerId = Some(UUID.randomUUID().toUUIDLike),
        transactionType = Some("cashout"))

      val mockResults = Seq(
        entity.TxnConfig(
          id = 1,
          uuid = UUID.randomUUID(),
          userId = 1,
          userUuid = UUID.randomUUID(),
          transactionType = "cashout",
          currencyId = 1,
          currencyName = "KES",
          createdBy = "unit test",
          createdAt = LocalDateTime.now,
          updatedBy = None,
          updatedAt = None),
        entity.TxnConfig(
          id = 2,
          uuid = UUID.randomUUID(),
          userId = 1,
          userUuid = UUID.randomUUID(),
          transactionType = "cashout",
          currencyId = 2,
          currencyName = "AED",
          createdBy = "unit test",
          createdAt = LocalDateTime.now,
          updatedBy = None,
          updatedAt = None),
        entity.TxnConfig(
          id = 3,
          uuid = UUID.randomUUID(),
          userId = 3,
          userUuid = UUID.randomUUID(),
          transactionType = "cashout",
          currencyId = 3,
          currencyName = "USD",
          createdBy = "unit test",
          createdAt = LocalDateTime.now,
          updatedBy = None,
          updatedAt = None))
      (txnConfigDao.getTxnConfigByCriteria(_: DaoTxnConfigCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(criteria.asDao(), None, None, None, None).returns(Right(mockResults))

      val result = txnConfigMgmtService.getTxnConfigByCriteria(criteria, Nil, None, None)

      val expected = mockResults.map(_.asDomain)

      whenReady(result) { result ⇒
        result mustBe Right(expected)
      }
    }

    "get txn config with inconsistent data without failing" in {
      val criteria = TxnConfigCriteria(
        customerId = Some(UUID.randomUUID().toUUIDLike),
        transactionType = Some("cashout"))

      val mockResults = Seq(
        entity.TxnConfig(
          id = 1,
          uuid = UUID.randomUUID(),
          userId = 1,
          userUuid = UUID.randomUUID(),
          transactionType = "cashout",
          currencyId = 1,
          currencyName = "XYZ",
          createdBy = "unit test",
          createdAt = LocalDateTime.now,
          updatedBy = None,
          updatedAt = None),
        entity.TxnConfig(
          id = 2,
          uuid = UUID.randomUUID(),
          userId = 1,
          userUuid = UUID.randomUUID(),
          transactionType = "",
          currencyId = 2,
          currencyName = "AED",
          createdBy = "unit test",
          createdAt = LocalDateTime.now,
          updatedBy = None,
          updatedAt = None),
        entity.TxnConfig(
          id = 3,
          uuid = UUID.randomUUID(),
          userId = 3,
          userUuid = UUID.randomUUID(),
          transactionType = "cashout",
          currencyId = 3,
          currencyName = "USD",
          createdBy = "",
          createdAt = LocalDateTime.now,
          updatedBy = None,
          updatedAt = None))
      (txnConfigDao.getTxnConfigByCriteria(_: DaoTxnConfigCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(criteria.asDao(), None, None, None, None).returns(Right(mockResults))

      val result = txnConfigMgmtService.getTxnConfigByCriteria(criteria, Nil, None, None)

      val expected = mockResults.map(_.asDomain)

      whenReady(result) { result ⇒
        result mustBe Right(expected)
      }
    }

    "get count of txn config based on criteria" in {
      val criteria = TxnConfigCriteria(
        currency = Some("KES"))

      val mockCount = 10
      (txnConfigDao.countTxnConfig(_: DaoTxnConfigCriteria)(_: Option[Connection]))
        .when(criteria.asDao(), None).returns(Right(mockCount))

      val result = txnConfigMgmtService.count(criteria)

      val expected = mockCount

      whenReady(result) { result ⇒
        result mustBe Right(expected)
      }
    }

    "get latest version of external accounts based on criteria" in {
      val criteria = TxnConfigCriteria(
        currency = Some("KES"))

      val mockResult = entity.TxnConfig(
        id = 3,
        uuid = UUID.randomUUID(),
        userId = 3,
        userUuid = UUID.randomUUID(),
        transactionType = "cashout",
        currencyId = 3,
        currencyName = "KES",
        createdBy = "unit test",
        createdAt = LocalDateTime.now,
        updatedBy = Some("unit test"),
        updatedAt = Some(LocalDateTime.now))
      val expectedOrderBy = Seq(Ordering("updated_at", Ordering.DESCENDING))
      (txnConfigDao.getTxnConfigByCriteria(_: DaoTxnConfigCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(criteria.asDao(), expectedOrderBy.asDao, Some(1), None, None).returns(Right(Seq(mockResult)))

      val result = txnConfigMgmtService.getLatestVersion(criteria)

      val expected = mockResult.asDomain

      whenReady(result) { result ⇒
        result mustBe Right(Some(expected))
      }
    }

    "update txn config" in {
      val criteria = TxnConfigCriteria(
        id = Some(UUID.randomUUID().toUUIDLike),
        customerId = Some(UUID.randomUUID().toUUIDLike),
      )
      val updateDto = TxnConfigToUpdate(
        transactionType = Some("p2p"),
        currency = Some("USD"),
        updatedBy = "unit test",
        updatedAt = LocalDateTime.now())

      val mockCurrencyResult = Currency(
        id = 3,
        name = "USD",
        description = Some("US Dollar"),
        isActive = true,
        icon = None,
        createdBy = "unit test",
        createdAt = LocalDateTime.now,
        updatedBy = None,
        updatedAt = None)
      (currencyDao.getAll _).when().returns(Right(Set(mockCurrencyResult)))

      val expectedDuplicateFinderCriteria = criteria.copy(
        transactionType = updateDto.transactionType,
        currency = updateDto.currency).asDao()
      (txnConfigDao.getTxnConfigByCriteria(_: DaoTxnConfigCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(expectedDuplicateFinderCriteria, None, None, None, None).returns(Right(Nil))

      val mockResultForNonFlattenedCriteria = entity.TxnConfig(
        id = 3,
        uuid = UUID.randomUUID(),
        userId = 3,
        userUuid = UUID.randomUUID(),
        transactionType = "p2p",
        currencyId = 3,
        currencyName = "USD",
        createdBy = "unit test",
        createdAt = LocalDateTime.now,
        updatedBy = Some("unit test"),
        updatedAt = Some(LocalDateTime.now))
      (txnConfigDao.getTxnConfigByCriteria(_: DaoTxnConfigCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(criteria.asDao(), None, None, None, None).returns(Right(Seq(mockResultForNonFlattenedCriteria)))

      val mockLatestVersionResult =
        entity.TxnConfig(
          id = 3,
          uuid = UUID.randomUUID(),
          userId = 3,
          userUuid = UUID.randomUUID(),
          transactionType = "p2p",
          currencyId = 3,
          currencyName = "USD",
          createdBy = "unit test",
          createdAt = LocalDateTime.now,
          updatedBy = Some("unit test"),
          updatedAt = Some(LocalDateTime.now))
      (txnConfigDao.getTxnConfigByCriteria(_: DaoTxnConfigCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(criteria.asDao(), Seq(Ordering("updated_at", Ordering.DESCENDING)).asDao, Some(1), None, None).returns(Right(Seq(mockLatestVersionResult)))


      val expectedUpdateDto = updateDto.copy(lastUpdatedAt = mockLatestVersionResult.updatedAt)
        .asDao(Some(mockCurrencyResult.id))
      val mockUpdateResult = entity.TxnConfig(
        id = 3,
        uuid = UUID.randomUUID(),
        userId = 3,
        userUuid = UUID.randomUUID(),
        transactionType = "p2p",
        currencyId = 3,
        currencyName = "USD",
        createdBy = "unit test",
        createdAt = LocalDateTime.now,
        updatedBy = Some("unit test"),
        updatedAt = Some(LocalDateTime.now))
      val expectedFlattenedCriteria = TxnConfigCriteria(anyIds = Some(Set(mockResultForNonFlattenedCriteria.uuid)))
      (txnConfigDao.updateTxnConfig(_: DaoTxnConfigCriteria, _: DaoTxnConfigToUpdate)(_: Option[Connection]))
        .when(expectedFlattenedCriteria.asDao(), expectedUpdateDto, None).returns(Right(Seq(mockUpdateResult)))

      val result = txnConfigMgmtService.updateTxnConfig(criteria, updateDto)

      val expected = TxnConfig(
        id = mockUpdateResult.uuid,
        customerId = mockUpdateResult.userUuid,
        transactionType = mockUpdateResult.transactionType,
        currency = mockUpdateResult.currencyName,
        createdBy = mockUpdateResult.createdBy,
        createdAt = mockUpdateResult.createdAt,
        updatedBy = mockUpdateResult.updatedBy,
        updatedAt = mockUpdateResult.updatedAt)

      whenReady(result) { result ⇒
        result mustBe Right(expected)
      }
    }

    "fail to update txn config (currency is valid but not configured)" in {
      val criteria = TxnConfigCriteria(
        id = Some(UUID.randomUUID().toUUIDLike),
        customerId = Some(UUID.randomUUID().toUUIDLike),
      )
      val updateDto = TxnConfigToUpdate(
        transactionType = Some("p2p"),
        currency = Some("USD"),
        updatedBy = "unit test",
        updatedAt = LocalDateTime.now())

      (currencyDao.getAll _).when().returns(Right(Set()))

      val result = txnConfigMgmtService.updateTxnConfig(criteria, updateDto)

      val expected = ServiceError.notFoundError(s"Failed to update txn config [${updateDto.toSmartString}]. Currency [${updateDto.currency.get}] is not configured in the system.")

      whenReady(result) { result ⇒
        result mustBe Left(expected)
      }
    }

    "fail to update txn config (because a successful update will lead to duplicate)" in {
      val criteria = TxnConfigCriteria(
        id = Some(UUID.randomUUID().toUUIDLike),
        customerId = Some(UUID.randomUUID().toUUIDLike),
      )
      val updateDto = TxnConfigToUpdate(
        transactionType = Some("p2p"),
        currency = Some("USD"),
        updatedBy = "unit test",
        updatedAt = LocalDateTime.now())

      val mockCurrencyResult = Currency(
        id = 3,
        name = "USD",
        description = Some("US Dollar"),
        isActive = true,
        icon = None,
        createdBy = "unit test",
        createdAt = LocalDateTime.now,
        updatedBy = None,
        updatedAt = None)
      (currencyDao.getAll _).when().returns(Right(Set(mockCurrencyResult)))

      val expectedDuplicateFinderCriteria = criteria.copy(
        transactionType = updateDto.transactionType,
        currency = updateDto.currency).asDao()
      val mockDuplicateResult = entity.TxnConfig(
        id = 3,
        uuid = UUID.randomUUID(),
        userId = 3,
        userUuid = UUID.randomUUID(),
        transactionType = "p2p",
        currencyId = 3,
        currencyName = "USD",
        createdBy = "unit test",
        createdAt = LocalDateTime.now,
        updatedBy = Some("unit test"),
        updatedAt = Some(LocalDateTime.now))
      (txnConfigDao.getTxnConfigByCriteria(_: DaoTxnConfigCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(expectedDuplicateFinderCriteria, None, None, None, None).returns(Right(Seq(mockDuplicateResult)))


      val result = txnConfigMgmtService.updateTxnConfig(criteria, updateDto)

      val expected = ServiceError.notFoundError(s"Duplicate txn config found if update [${updateDto.toSmartString}] is applied using criteria [${criteria.toSmartString}].")

      whenReady(result) { result ⇒
        result mustBe Left(expected)
      }
    }

    "fail to update invalid txn config (empty required fields)" in {
      val criteria = TxnConfigCriteria(
        id = Some(UUID.randomUUID().toUUIDLike),
        customerId = Some(UUID.randomUUID().toUUIDLike),
      )
      val updateDto = TxnConfigToUpdate(
        transactionType = Some(""),
        currency = Some("USD"),
        updatedBy = "unit test",
        updatedAt = LocalDateTime.now())

      val result = txnConfigMgmtService.updateTxnConfig(criteria, updateDto)

      val expected = ServiceError.validationError("Transaction type cannot be empty")

      whenReady(result) { result ⇒
        result mustBe Left(expected)
      }
    }

    "fail to update txn config (not found)" in {
      val criteria = TxnConfigCriteria(
        id = Some(UUID.randomUUID().toUUIDLike),
        customerId = Some(UUID.randomUUID().toUUIDLike),
      )
      val updateDto = TxnConfigToUpdate(
        transactionType = Some("p2p"),
        currency = Some("USD"),
        updatedBy = "unit test",
        updatedAt = LocalDateTime.now())

      val mockCurrencyResult = Currency(
        id = 3,
        name = "USD",
        description = Some("US Dollar"),
        isActive = true,
        icon = None,
        createdBy = "unit test",
        createdAt = LocalDateTime.now,
        updatedBy = None,
        updatedAt = None)
      (currencyDao.getAll _).when().returns(Right(Set(mockCurrencyResult)))

      val expectedDuplicateFinderCriteria = criteria.copy(
        transactionType = updateDto.transactionType,
        currency = updateDto.currency).asDao()
      (txnConfigDao.getTxnConfigByCriteria(_: DaoTxnConfigCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(expectedDuplicateFinderCriteria, None, None, None, None).returns(Right(Nil))

      (txnConfigDao.getTxnConfigByCriteria(_: DaoTxnConfigCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(criteria.asDao(), None, None, None, None).returns(Right(Nil))

      val result = txnConfigMgmtService.updateTxnConfig(criteria, updateDto)

      val expected = ServiceError.notFoundError(s"Txn config using this criteria [${criteria.toSmartString}] was not found.")

      whenReady(result) { result ⇒
        result mustBe Left(expected)
      }
    }

    "fail to update invalid txn config if last updated_at is not most recent" in {
      val criteria = TxnConfigCriteria(
        id = Some(UUID.randomUUID().toUUIDLike),
        customerId = Some(UUID.randomUUID().toUUIDLike),
      )
      val updateDto = TxnConfigToUpdate(
        transactionType = Some("p2p"),
        currency = Some("USD"),
        updatedBy = "unit test",
        updatedAt = LocalDateTime.now())

      val mockCurrencyResult = Currency(
        id = 3,
        name = "USD",
        description = Some("US Dollar"),
        isActive = true,
        icon = None,
        createdBy = "unit test",
        createdAt = LocalDateTime.now,
        updatedBy = None,
        updatedAt = None)
      (currencyDao.getAll _).when().returns(Right(Set(mockCurrencyResult)))

      val expectedDuplicateFinderCriteria = criteria.copy(
        transactionType = updateDto.transactionType,
        currency = updateDto.currency).asDao()
      (txnConfigDao.getTxnConfigByCriteria(_: DaoTxnConfigCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(expectedDuplicateFinderCriteria, None, None, None, None).returns(Right(Nil))

      val mockResultForNonFlattenedCriteria = entity.TxnConfig(
        id = 3,
        uuid = UUID.randomUUID(),
        userId = 3,
        userUuid = UUID.randomUUID(),
        transactionType = "p2p",
        currencyId = 3,
        currencyName = "USD",
        createdBy = "unit test",
        createdAt = LocalDateTime.now,
        updatedBy = Some("unit test"),
        updatedAt = Some(LocalDateTime.now))
      (txnConfigDao.getTxnConfigByCriteria(_: DaoTxnConfigCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(criteria.asDao(), None, None, None, None).returns(Right(Seq(mockResultForNonFlattenedCriteria)))

      val mockLatestVersionResult =
        entity.TxnConfig(
          id = 3,
          uuid = UUID.randomUUID(),
          userId = 3,
          userUuid = UUID.randomUUID(),
          transactionType = "p2p",
          currencyId = 3,
          currencyName = "USD",
          createdBy = "unit test",
          createdAt = LocalDateTime.now,
          updatedBy = Some("unit test"),
          updatedAt = Some(LocalDateTime.now))
      (txnConfigDao.getTxnConfigByCriteria(_: DaoTxnConfigCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(criteria.asDao(), Seq(Ordering("updated_at", Ordering.DESCENDING)).asDao, Some(1), None, None).returns(Right(Seq(mockLatestVersionResult)))


      val expectedUpdateDto = updateDto.copy(lastUpdatedAt = mockLatestVersionResult.updatedAt)
        .asDao(Some(mockCurrencyResult.id))
      val mockUpdateResult = DaoError.PreconditionFailed("some dao error")
      val expectedFlattenedCriteria = TxnConfigCriteria(anyIds = Some(Set(mockResultForNonFlattenedCriteria.uuid)))
      (txnConfigDao.updateTxnConfig(_: DaoTxnConfigCriteria, _: DaoTxnConfigToUpdate)(_: Option[Connection]))
        .when(expectedFlattenedCriteria.asDao(), expectedUpdateDto, None).returns(Left(mockUpdateResult))

      val result = txnConfigMgmtService.updateTxnConfig(criteria, updateDto)

      val expected = ServiceError.staleResourceAccessError("some dao error")

      whenReady(result) { result ⇒
        result mustBe Left(expected)
      }
    }

    "delete txn config" in {
      val criteria = TxnConfigCriteria(
        id = Some(UUID.randomUUID().toUUIDLike),
        customerId = Some(UUID.randomUUID().toUUIDLike),
      )
      val lastUpdatedAt = None

      val expectedDeleteCriteria = criteria.asDao()
      val mockLatestVersionResult = entity.TxnConfig(
        id = 3,
        uuid = UUID.randomUUID(),
        userId = 3,
        userUuid = UUID.randomUUID(),
        transactionType = "p2p",
        currencyId = 3,
        currencyName = "USD",
        createdBy = "unit test",
        createdAt = LocalDateTime.now,
        updatedBy = Some("unit test"),
        updatedAt = Some(LocalDateTime.now))
      (txnConfigDao.getTxnConfigByCriteria(_: DaoTxnConfigCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(expectedDeleteCriteria, Seq(Ordering("updated_at", Ordering.DESCENDING)).asDao, Some(1), None, None).returns(Right(Seq(mockLatestVersionResult)))

      (txnConfigDao.deleteTxnConfig(_: DaoTxnConfigCriteria, _: Option[LocalDateTime])(_: Option[Connection]))
        .when(expectedDeleteCriteria, lastUpdatedAt.orElse(mockLatestVersionResult.updatedAt), None).returns(Right(Option(())))

      val result = txnConfigMgmtService.deleteTxnConfig(criteria, lastUpdatedAt)

      val expected = ()

      whenReady(result) { result ⇒
        result mustBe Right(expected)
      }
    }

    "not fail if txn config to be deleted was not found" in {
      val criteria = TxnConfigCriteria(
        id = Some(UUID.randomUUID().toUUIDLike),
        customerId = Some(UUID.randomUUID().toUUIDLike),
      )
      val lastUpdatedAt = None

      val expectedDeleteCriteria = criteria.asDao()
      val mockLatestVersionResult = entity.TxnConfig(
        id = 3,
        uuid = UUID.randomUUID(),
        userId = 3,
        userUuid = UUID.randomUUID(),
        transactionType = "p2p",
        currencyId = 3,
        currencyName = "USD",
        createdBy = "unit test",
        createdAt = LocalDateTime.now,
        updatedBy = Some("unit test"),
        updatedAt = Some(LocalDateTime.now))
      (txnConfigDao.getTxnConfigByCriteria(_: DaoTxnConfigCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(expectedDeleteCriteria, Seq(Ordering("updated_at", Ordering.DESCENDING)).asDao, Some(1), None, None).returns(Right(Seq(mockLatestVersionResult)))

      (txnConfigDao.deleteTxnConfig(_: DaoTxnConfigCriteria, _: Option[LocalDateTime])(_: Option[Connection]))
        .when(expectedDeleteCriteria, lastUpdatedAt.orElse(mockLatestVersionResult.updatedAt), None).returns(Right(None))

      val result = txnConfigMgmtService.deleteTxnConfig(criteria, lastUpdatedAt)

      val expected = ()

      whenReady(result) { result ⇒
        result mustBe Right(expected)
      }
    }

    "fail to delete txn config if last updated_at is not most recent" in {
      val criteria = TxnConfigCriteria(
        id = Some(UUID.randomUUID().toUUIDLike),
        customerId = Some(UUID.randomUUID().toUUIDLike),
      )
      val lastUpdatedAt = None

      val expectedDeleteCriteria = criteria.asDao()
      val mockLatestVersionResult = entity.TxnConfig(
        id = 3,
        uuid = UUID.randomUUID(),
        userId = 3,
        userUuid = UUID.randomUUID(),
        transactionType = "p2p",
        currencyId = 3,
        currencyName = "USD",
        createdBy = "unit test",
        createdAt = LocalDateTime.now,
        updatedBy = Some("unit test"),
        updatedAt = Some(LocalDateTime.now))
      (txnConfigDao.getTxnConfigByCriteria(_: DaoTxnConfigCriteria, _: Option[OrderingSet], _: Option[Int], _: Option[Int])(_: Option[Connection]))
        .when(expectedDeleteCriteria, Seq(Ordering("updated_at", Ordering.DESCENDING)).asDao, Some(1), None, None).returns(Right(Seq(mockLatestVersionResult)))

      val mockDeleteResult = DaoError.PreconditionFailed("some dao error")
      (txnConfigDao.deleteTxnConfig(_: DaoTxnConfigCriteria, _: Option[LocalDateTime])(_: Option[Connection]))
        .when(expectedDeleteCriteria, lastUpdatedAt.orElse(mockLatestVersionResult.updatedAt), None).returns(Left(mockDeleteResult))

      val result = txnConfigMgmtService.deleteTxnConfig(criteria, lastUpdatedAt)

      val expected = ServiceError.staleResourceAccessError("some dao error")

      whenReady(result) { result ⇒
        result mustBe Left(expected)
      }
    }

  }
}
