package tech.pegb.backoffice.domain.account

import java.sql.Connection
import java.time._
import java.util.{Currency, UUID}

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.PlaySpec
import tech.pegb.backoffice.dao.DaoError
import tech.pegb.backoffice.dao.account.abstraction.AccountDao
import tech.pegb.backoffice.dao.account.dto.{AccountCriteria, AccountToInsert}
import tech.pegb.backoffice.dao.account.entity.Account
import tech.pegb.backoffice.dao.account.sql.AccountSqlDao
import tech.pegb.backoffice.dao.customer.abstraction.{IndividualUserDao, UserDao}
import tech.pegb.backoffice.dao.customer.entity.{IndividualUser, User}
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes, OrderingSet}
import tech.pegb.backoffice.dao.settings.abstraction.SystemSettingsDao
import tech.pegb.backoffice.dao.transaction.abstraction.TransactionDao
import tech.pegb.backoffice.dao.transaction.dto.TransactionCriteria
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.account.dto.AccountToCreate
import tech.pegb.backoffice.domain.account.implementation.AccountMgmtService
import tech.pegb.backoffice.domain.account.model.AccountAttributes._
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes._
import tech.pegb.backoffice.domain.transaction.dto.{TransactionCriteria ⇒ newTxnCriteria}
import tech.pegb.backoffice.mapping.dao.domain.account.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.account.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.transaction.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, UUIDLike, WithExecutionContexts}
import tech.pegb.core.TestExecutionContext

class AccountMgmtServiceSpec extends PlaySpec with MockFactory with ScalaFutures {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val config = AppConfig("application.test.conf")
  val executionContexts: WithExecutionContexts = TestExecutionContext
  val mockUserDao = stub[UserDao]
  val mockIndividualUserDao = stub[IndividualUserDao]
  val mockAccountDao = stub[AccountDao]
  val mockTxnDao = stub[TransactionDao]
  val mockSystemSettingsDao = stub[SystemSettingsDao]
  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())

  //test data
  val businessUserUUID = UUID.randomUUID()
  val businessUser = User(
    id = 1,
    uuid = businessUserUUID.toString,
    userName = "bu_username",
    password = Option("bu_password"),
    `type` = Option(config.BusinessUserType),
    tier = None,
    segment = None,
    subscription = None,
    email = Option("bu@gmail.com"),
    status = Option(config.ActivatedBusinessUserStatus),
    activatedAt = Option(LocalDateTime.now(mockClock)),
    passwordUpdatedAt = None,
    createdAt = LocalDateTime.now(mockClock),
    createdBy = "pegbuser",
    updatedAt = None,
    updatedBy = None)

  val individualUserUUID = UUID.randomUUID()
  val individualUser = User(
    id = 1,
    uuid = individualUserUUID.toString,
    userName = "iu_username",
    password = Option("iu_password"),
    `type` = Option(config.IndividualUserType),
    tier = None,
    segment = None,
    subscription = None,
    email = Option("iu@gmail.com"),
    status = Option(config.ActivatedBusinessUserStatus),
    activatedAt = Option(LocalDateTime.now(mockClock)),
    passwordUpdatedAt = None,
    createdAt = LocalDateTime.now(mockClock),
    createdBy = "pegbuser",
    updatedAt = None,
    updatedBy = None)

  //existing accounts
  val phpAccount = Account.getEmpty.copy(
    id = 101,
    uuid = UUID.randomUUID().toString,
    accountNumber = "php_001",
    userId = 1,
    accountName = "PHP ACCOUNT",
    accountType = config.WalletAccountType,
    isMainAccount = false.some,
    currency = "PHP",
    balance = BigDecimal(0).some,
    blockedBalance = BigDecimal(0).some,
    status = "New".some,
    closedAt = None,
    lastTransactionAt = None,
    mainType = "liability",
    createdAt = LocalDateTime.now(mockClock),
    createdBy = "",
    updatedAt = None,
    updatedBy = None)
  val aedAccount = Account.getEmpty.copy(
    id = 100,
    uuid = UUID.randomUUID().toString,
    accountNumber = "aed_001",
    userId = 1,
    accountName = "AED ACCOUNT",
    accountType = config.WalletAccountType,
    isMainAccount = false.some,
    currency = "AED",
    balance = BigDecimal(0).some,
    blockedBalance = BigDecimal(0).some,
    status = "New".some,
    closedAt = None,
    lastTransactionAt = None,
    mainType = "liability",
    createdAt = LocalDateTime.now(mockClock),
    createdBy = "",
    updatedAt = None,
    updatedBy = None)

  val userAccountsSet = Set(phpAccount, aedAccount)

  "AccountMgmtService" should {
    val mockDoneAt = LocalDateTime.now(mockClock)
    val mockDoneBy = "pegbuser"

    val accountMgmtService = new AccountMgmtService(
      conf = config,
      executionContexts = executionContexts,
      userDao = mockUserDao,
      individualUserDao = mockIndividualUserDao,
      accountDao = mockAccountDao,
      txnDao = mockTxnDao,
      systemSettingsDao = mockSystemSettingsDao) {
      override val clock = mockClock
    }

    "Not yet succeed for businessUser createAccount" in {
      val createTime = ZonedDateTime.now(mockClock)

      val accountToCreate = AccountToCreate(
        customerId = businessUserUUID,
        accountNumber = Option(AccountNumber("0001")),
        accountName = Option(NameAttribute("USD Wallet")),
        accountType = AccountType(config.WalletAccountType),
        isMainAccount = true,
        currency = Currency.getInstance("USD"),
        initialBalance = Option(BigDecimal(10000)),
        accountStatus = Option(AccountStatus("NEW")),
        mainType = AccountMainType("liability"),
        createdBy = mockDoneBy,
        createdAt = createTime.toLocalDateTimeUTC)

      val expectedAccount = Account.getEmpty.copy(
        id = 1,
        uuid = UUID.randomUUID().toString,
        accountNumber = accountToCreate.accountNumber.map(_.underlying).get,
        userId = 1,
        accountName = accountToCreate.accountName.map(_.underlying).get,
        accountType = accountToCreate.accountType.underlying,
        isMainAccount = accountToCreate.isMainAccount.some,
        currency = accountToCreate.currency.getCurrencyCode,
        balance = accountToCreate.initialBalance,
        blockedBalance = BigDecimal(0).some,
        status = accountToCreate.accountStatus.map(_.underlying).get.some,
        closedAt = None,
        lastTransactionAt = None,
        mainType = "liability",
        createdAt = mockDoneAt,
        createdBy = mockDoneBy,
        updatedAt = None,
        updatedBy = None)

      (mockUserDao.getUser _).when(businessUserUUID.toString).returns(Right(Some(businessUser)))
      (mockAccountDao.getAccountsByUserId _).when(businessUserUUID.toString).returns(Right(Set.empty))

      val expectedCriteria = AccountCriteria(userId = Option(CriteriaField(AccountSqlDao.cUserId, businessUserUUID.toString)))
      (mockAccountDao.countTotalAccountsByCriteria _).when(expectedCriteria).returns(Right(0))

      //currently no implementation for business user create account
      (mockIndividualUserDao.getIndividualUser _).when(businessUserUUID.toString).returns(Right(None))

      (mockAccountDao.insertAccount(_: AccountToInsert)(_: Option[Connection])).when(accountToCreate.asDao.get, *).returns(Right(expectedAccount))

      val actualResult = accountMgmtService.createAccount(accountToCreate, Option(config.BusinessUserType))

      whenReady(actualResult) { actual ⇒
        actual.isLeft mustBe true
        actual.left.get.message mustBe s"Failed to create account. This user type [${config.BusinessUserType}] is not yet supported."
        //actual.isRight mustBe true
        //actual.right.get mustBe expectedAccount.asDomain(businessUserUUID).get
      }
    }
    "Succeed for individualUser createAccount" in {
      val createTime = ZonedDateTime.now(mockClock)

      val accountToCreate = AccountToCreate(
        customerId = individualUserUUID,
        accountNumber = Option(AccountNumber("0001")),
        accountName = Option(NameAttribute("USD Wallet")),
        accountType = AccountType(config.WalletAccountType),
        isMainAccount = true,
        currency = Currency.getInstance("USD"),
        initialBalance = Option(BigDecimal(10000)),
        accountStatus = Option(AccountStatus("NEW")),
        mainType = AccountMainType("liability"),
        createdBy = mockDoneBy,
        createdAt = createTime.toLocalDateTimeUTC)

      val expectedIndividualUser = IndividualUser.getEmpty.copy(
        uuid = individualUserUUID.toString, msisdn = "+971544451679")

      val expectedAccount = Account.getEmpty.copy(
        id = 1,
        uuid = UUID.randomUUID().toString,
        accountNumber = "1.0",
        userId = 1,
        userUuid = UUID.randomUUID().toString,
        accountName = s"${expectedIndividualUser.msisdn}_${config.WalletAccountType}",
        accountType = accountToCreate.accountType.underlying,
        isMainAccount = accountToCreate.isMainAccount.some,
        currency = accountToCreate.currency.getCurrencyCode,
        balance = accountToCreate.initialBalance,
        blockedBalance = BigDecimal(0).some,
        status = accountToCreate.accountStatus.map(_.underlying).get.some,
        closedAt = None,
        lastTransactionAt = None,
        mainType = "liability",
        createdAt = mockDoneAt,
        createdBy = mockDoneBy,
        updatedAt = None,
        updatedBy = None)

      (mockUserDao.getUser _).when(individualUserUUID.toString).returns(Right(Some(individualUser)))
      (mockAccountDao.getAccountsByUserId _).when(individualUserUUID.toString).returns(Right(Set.empty))

      val expectedCriteria = AccountCriteria(userId = Option(CriteriaField(AccountSqlDao.cUserId, individualUserUUID.toString)))
      (mockAccountDao.countTotalAccountsByCriteria _).when(expectedCriteria).returns(Right(0))

      (mockIndividualUserDao.getIndividualUser _).when(individualUserUUID.toString).returns(Right(Option(expectedIndividualUser)))

      val expectedAccountToCreate = accountToCreate.copy(
        accountNumber = Some(AccountNumber("1.1")),
        accountName = Some(NameAttribute(s"${expectedIndividualUser.msisdn}_${config.WalletAccountType}")),
        accountStatus = Some(AccountStatus(config.NewlyCreatedAccountStatus))).asDao.get

      (mockAccountDao.insertAccount(_: AccountToInsert)(_: Option[Connection])).when(expectedAccountToCreate, *).returns(Right(expectedAccount))

      val actualResult = accountMgmtService.createAccount(accountToCreate, Option(config.IndividualUserType))

      whenReady(actualResult) { actual ⇒
        actual.isRight mustBe true
        actual.right.get mustBe expectedAccount.asDomain.get
      }
    }

    "execute on fly aggregation for accounts" in {
      val createTime = ZonedDateTime.now(mockClock)

      val accountToCreate = AccountToCreate(
        customerId = individualUserUUID,
        accountNumber = Option(AccountNumber("0001")),
        accountName = Option(NameAttribute("USD Wallet")),
        accountType = AccountType(config.WalletAccountType),
        isMainAccount = true,
        currency = Currency.getInstance("USD"),
        initialBalance = Option(BigDecimal(10000)),
        accountStatus = Option(AccountStatus("NEW")),
        mainType = AccountMainType("liability"),
        createdBy = mockDoneBy,
        createdAt = createTime.toLocalDateTimeUTC)

      val expectedIndividualUser = IndividualUser.getEmpty.copy(
        uuid = individualUserUUID.toString, msisdn = "+971544451679")

      val expectedAccount = Account.getEmpty.copy(
        id = 1,
        uuid = UUID.randomUUID().toString,
        accountNumber = "1.0",
        userId = 1,
        userUuid = UUID.randomUUID().toString,
        userName = Some("mpesa"),
        accountName = s"${expectedIndividualUser.msisdn}_${config.WalletAccountType}",
        accountType = accountToCreate.accountType.underlying,
        isMainAccount = accountToCreate.isMainAccount.some,
        currency = accountToCreate.currency.getCurrencyCode,
        blockedBalance = BigDecimal(0).some,
        balance = accountToCreate.initialBalance,
        status = accountToCreate.accountStatus.map(_.underlying).get.some,
        closedAt = None,
        lastTransactionAt = None,
        mainType = "liability",
        createdAt = mockDoneAt,
        createdBy = mockDoneBy,
        updatedAt = None,
        updatedBy = None)

      val fromDate = LocalDate.of(2019, 8, 29).atTime(0, 0, 0)
      val toDate = LocalDate.of(2019, 8, 29).atTime(23, 59, 59)

      val expectedAccountCriteria = AccountCriteria(accountNumbers = Option(CriteriaField("number", Set("1.0"), MatchTypes.In)))
      val txnCriteriaInput = newTxnCriteria(accountNumbers = Seq("1.0"), startDate = Some(fromDate),
        endDate = Some(toDate))
      val txnCriteria = newTxnCriteria(accountId = Some(UUIDLike(expectedAccount.uuid)), startDate = Some(fromDate),
        endDate = Some(toDate))
      (mockAccountDao.getAccountsByCriteria(
        _: Option[AccountCriteria],
        _: Option[OrderingSet], _: Option[Int], _: Option[Int])).when(Option(expectedAccountCriteria), None, None, None)
        .returns(Right(Seq(expectedAccount)))
      (mockTxnDao.getOnFlyAggregation(_: TransactionCriteria, _: Boolean))
        .when(txnCriteria.asDao(), true)
        .returns(Right(Option((BigDecimal(0.00), BigDecimal(0.00), BigDecimal(0.00)))))

      val resultF = accountMgmtService.executeOnFlyAggregation(txnCriteriaInput, Seq.empty, None, None)

      whenReady(resultF) { result ⇒
        result.isRight
      }

    }

    "execute on fly aggregation for accounts when no account numbers are provided" in {

      val fromDate = LocalDate.of(2019, 8, 29).atTime(0, 0, 0)
      val toDate = LocalDate.of(2019, 8, 29).atTime(23, 59, 59)

      val expectedAccountCriteria = AccountCriteria(accountNumbers = Option(CriteriaField("number", Set(), MatchTypes.In)))
      val txnCriteria = newTxnCriteria(accountId = None, startDate = Some(fromDate),
        endDate = Some(toDate))
      val txnCriteriaInput = newTxnCriteria(accountNumbers = Seq.empty, startDate = Some(fromDate),
        endDate = Some(toDate))

      (mockAccountDao.getAccountsByCriteria(
        _: Option[AccountCriteria],
        _: Option[OrderingSet], _: Option[Int], _: Option[Int])).when(Some(expectedAccountCriteria), None, None, None)
        .returns(Right(Seq.empty))
      (mockTxnDao.getOnFlyAggregation(_: TransactionCriteria, _: Boolean))
        .when(txnCriteria.asDao(), true)
        .returns(Right(Option((BigDecimal(0.00), BigDecimal(0.00), BigDecimal(0.00)))))

      val resultF = accountMgmtService.executeOnFlyAggregation(txnCriteriaInput, Seq.empty, None, None)

      whenReady(resultF) { result ⇒
        result.isRight
      }

    }

    "Fail when user is not found" in {
      val createTime = ZonedDateTime.now(mockClock)

      val accountToCreate = AccountToCreate(
        customerId = individualUserUUID,
        accountNumber = Option(AccountNumber("0001")),
        accountName = Option(NameAttribute("USD Wallet")),
        accountType = AccountType(config.WalletAccountType),
        isMainAccount = true,
        currency = Currency.getInstance("USD"),
        initialBalance = Option(BigDecimal(10000)),
        accountStatus = Option(AccountStatus("NEW")),
        mainType = AccountMainType("liability"),
        createdBy = mockDoneBy,
        createdAt = createTime.toLocalDateTimeUTC)

      (mockUserDao.getUser _).when(individualUserUUID.toString).returns(Right(None))

      val actualResult = accountMgmtService.createAccount(accountToCreate)
      val expected = ServiceError.notFoundError(s"Cannot create account for user ${individualUserUUID}: User not found", UUID.randomUUID().toOption)
      whenReady(actualResult) { actual ⇒
        actual.isLeft mustBe true
        actual.left.get.message mustBe expected.message
        actual.left.get.equals(expected) mustBe true
      }
    }

    "Fail when user is not activated" in {
      val createTime = ZonedDateTime.now(mockClock)

      val accountToCreate = AccountToCreate(
        customerId = individualUserUUID,
        accountNumber = Option(AccountNumber("0001")),
        accountName = Option(NameAttribute("USD Wallet")),
        accountType = AccountType(config.WalletAccountType),
        isMainAccount = true,
        currency = Currency.getInstance("USD"),
        initialBalance = Option(BigDecimal(10000)),
        accountStatus = Option(AccountStatus("NEW")),
        mainType = AccountMainType("liability"),
        createdBy = mockDoneBy,
        createdAt = createTime.toLocalDateTimeUTC)

      val newIndividualUser = individualUser.copy(status = Option(config.PassiveUserStatus))

      (mockUserDao.getUser _).when(individualUserUUID.toString).returns(Right(Some(newIndividualUser)))
      val actualResult = accountMgmtService.createAccount(accountToCreate)
      val expected = ServiceError.validationError(s"Cannot create account for user ${individualUserUUID}: User is deactivated", UUID.randomUUID().toOption)
      whenReady(actualResult) { actual ⇒
        actual.isLeft mustBe true
        actual.left.get.message mustBe expected.message
        actual.left.get.equals(expected) mustBe true
      }
    }

    "Fail when incorrect userType" in {
      val createTime = ZonedDateTime.now(mockClock)

      val accountToCreate = AccountToCreate(
        customerId = individualUserUUID,
        accountNumber = Option(AccountNumber("0001")),
        accountName = Option(NameAttribute("USD Wallet")),
        accountType = AccountType(config.WalletAccountType),
        isMainAccount = true,
        currency = Currency.getInstance("USD"),
        initialBalance = Option(BigDecimal(10000)),
        accountStatus = Option(AccountStatus("NEW")),
        mainType = AccountMainType("liability"),
        createdBy = mockDoneBy,
        createdAt = createTime.toLocalDateTimeUTC)

      (mockUserDao.getUser _).when(individualUserUUID.toString).returns(Right(Some(individualUser)))
      val actualResult = accountMgmtService.createAccount(accountToCreate, Some(config.BusinessUserType))
      val expected = ServiceError.validationError(
        s"Error on UserType validation user of $individualUserUUID. " +
          s"Found usertype = ${config.IndividualUserType}, Expected userType = ${config.BusinessUserType}.",
        UUID.randomUUID().toOption)

      whenReady(actualResult) { actual ⇒
        actual.isLeft mustBe true
        actual.left.get.message mustBe expected.message
        actual.left.get.equals(expected) mustBe true
      }
    }

    "Fail when user is individual and account to create in not wallet type" in {
      val createTime = ZonedDateTime.now(mockClock)

      val accountToCreate = AccountToCreate(
        customerId = individualUserUUID,
        accountNumber = Option(AccountNumber("0001")),
        accountName = Option(NameAttribute("USD Wallet")),
        accountType = AccountType("OTHER_WALLET"),
        isMainAccount = true,
        currency = Currency.getInstance("USD"),
        initialBalance = Option(BigDecimal(10000)),
        accountStatus = Option(AccountStatus("NEW")),
        mainType = AccountMainType("liability"),
        createdBy = mockDoneBy,
        createdAt = createTime.toLocalDateTimeUTC)

      (mockUserDao.getUser _).when(individualUserUUID.toString).returns(Right(Some(individualUser)))

      val actualResult = accountMgmtService.createAccount(accountToCreate)
      val expected = ServiceError.validationError(
        s"Cannot create account for individual user ${individualUserUUID}: " +
          s"IndividualUser can only create ${config.WalletAccountType}",
        UUID.randomUUID().toOption)
      whenReady(actualResult) { actual ⇒
        actual.isLeft mustBe true
        actual.left.get.message mustBe expected.message
        actual.left.get.equals(expected) mustBe true
      }
    }

    "Fail when user has a main account already" in {
      val createTime = ZonedDateTime.now(mockClock)

      val accountToCreate = AccountToCreate(
        customerId = individualUserUUID,
        accountNumber = Option(AccountNumber("0001")),
        accountName = Option(NameAttribute("USD Wallet")),
        accountType = AccountType(config.WalletAccountType),
        isMainAccount = true,
        currency = Currency.getInstance("USD"),
        initialBalance = Option(BigDecimal(10000)),
        accountStatus = Option(AccountStatus("NEW")),
        mainType = AccountMainType("liability"),
        createdBy = mockDoneBy,
        createdAt = createTime.toLocalDateTimeUTC)

      val main_account = Account.getEmpty.copy(
        id = 100,
        uuid = UUID.randomUUID().toString,
        accountNumber = "kes_001",
        userId = 1,
        accountName = "KES ACCOUNT",
        accountType = config.WalletAccountType,
        isMainAccount = true.some,
        currency = "KES",
        balance = BigDecimal(0).some,
        blockedBalance = BigDecimal(0).some,
        status = "New".some,
        closedAt = None,
        lastTransactionAt = None,
        mainType = "liability",
        createdAt = LocalDateTime.now(mockClock),
        createdBy = "",
        updatedAt = None,
        updatedBy = None)

      (mockUserDao.getUser _).when(individualUserUUID.toString).returns(Right(Some(individualUser)))
      (mockAccountDao.getAccountsByUserId _).when(individualUserUUID.toString).returns(Right(userAccountsSet + main_account))

      val actualResult = accountMgmtService.createAccount(accountToCreate)
      val expected = ServiceError.validationError(
        s"Cannot create main account for user ${individualUserUUID}: " +
          s"User already has a main account",
        UUID.randomUUID().toOption)
      whenReady(actualResult) { actual ⇒
        actual.isLeft mustBe true
        actual.left.get.message mustBe expected.message
        actual.left.get.equals(expected) mustBe true
      }
    }

    "Fail when user has a existing account with same currency" in {
      val createTime = ZonedDateTime.now(mockClock)

      val accountToCreate = AccountToCreate(
        customerId = individualUserUUID,
        accountNumber = Option(AccountNumber("0001")),
        accountName = Option(NameAttribute("USD Wallet")),
        accountType = AccountType(config.WalletAccountType),
        isMainAccount = true,
        currency = Currency.getInstance("USD"),
        initialBalance = Option(BigDecimal(10000)),
        accountStatus = Option(AccountStatus("NEW")),
        mainType = AccountMainType("liability"),
        createdBy = mockDoneBy,
        createdAt = createTime.toLocalDateTimeUTC)

      val usdAccount = Account.getEmpty.copy(
        id = 100,
        uuid = UUID.randomUUID().toString,
        accountNumber = "usd_001",
        userId = 1,
        accountName = "USD ACCOUNT",
        accountType = config.WalletAccountType,
        isMainAccount = false.some,
        currency = "USD",
        balance = BigDecimal(0).some,
        blockedBalance = BigDecimal(0).some,
        status = "New".some,
        closedAt = None,
        lastTransactionAt = None,
        mainType = "liability",
        createdAt = LocalDateTime.now(mockClock),
        createdBy = "",
        updatedAt = None,
        updatedBy = None)

      (mockUserDao.getUser _).when(individualUserUUID.toString).returns(Right(Some(individualUser)))
      (mockAccountDao.getAccountsByUserId _).when(individualUserUUID.toString).returns(Right(userAccountsSet + usdAccount))

      val actualResult = accountMgmtService.createAccount(accountToCreate)
      val expected = ServiceError.validationError(
        s"Cannot create account for individual user ${individualUserUUID}: " +
          s"User already has an account with currency ${accountToCreate.currency}",
        UUID.randomUUID().toOption)
      whenReady(actualResult) { actual ⇒
        actual.isLeft mustBe true
        actual.left.get.message mustBe expected.message
        actual.left.get.equals(expected) mustBe true
      }
    }

    "Fail when createAccount encountered daoError in getUser" in {
      val createTime = ZonedDateTime.now(mockClock)

      val accountToCreate = AccountToCreate(
        customerId = individualUserUUID,
        accountNumber = Option(AccountNumber("0001")),
        accountName = Option(NameAttribute("USD Wallet")),
        accountType = AccountType(config.WalletAccountType),
        isMainAccount = true,
        currency = Currency.getInstance("USD"),
        initialBalance = Option(BigDecimal(10000)),
        accountStatus = Option(AccountStatus("NEW")),
        mainType = AccountMainType("liability"),
        createdBy = mockDoneBy,
        createdAt = createTime.toLocalDateTimeUTC)

      (mockUserDao.getUser _).when(individualUserUUID.toString).returns(Left(DaoError.GenericDbError("some dao error")))

      val actualResult = accountMgmtService.createAccount(accountToCreate)
      val expected = ServiceError.unknownError(s"some dao error", UUID.randomUUID().toOption)
      whenReady(actualResult) { actual ⇒
        actual.isLeft mustBe true
        actual.left.get.message mustBe expected.message
        actual.left.get.equals(expected) mustBe true
      }
    }

    "Fail when createAccount encountered daoError in getAccounts" in {
      val createTime = ZonedDateTime.now(mockClock)

      val accountToCreate = AccountToCreate(
        customerId = individualUserUUID,
        accountNumber = Option(AccountNumber("0001")),
        accountName = Option(NameAttribute("USD Wallet")),
        accountType = AccountType(config.WalletAccountType),
        isMainAccount = true,
        currency = Currency.getInstance("USD"),
        initialBalance = Option(BigDecimal(10000)),
        accountStatus = Option(AccountStatus("NEW")),
        mainType = AccountMainType("liability"),
        createdBy = mockDoneBy,
        createdAt = createTime.toLocalDateTimeUTC)

      (mockUserDao.getUser _).when(individualUserUUID.toString).returns(Right(Some(individualUser)))
      (mockAccountDao.getAccountsByUserId _).when(individualUserUUID.toString).returns(Left(DaoError.GenericDbError("some dao error")))

      val actualResult = accountMgmtService.createAccount(accountToCreate)
      val expected = ServiceError.unknownError(s"some dao error", UUID.randomUUID().toOption)
      whenReady(actualResult) { actual ⇒
        actual.isLeft mustBe true
        actual.left.get.message mustBe expected.message
        actual.left.get.equals(expected) mustBe true
      }
    }

    "Fail when createAccount encountered daoError in insertAccount" in {
      val createTime = ZonedDateTime.now(mockClock)

      val accountToCreate = AccountToCreate(
        customerId = individualUserUUID,
        accountName = None,
        accountNumber = None,
        accountType = AccountType(config.WalletAccountType),
        isMainAccount = false,
        currency = Currency.getInstance("USD"),
        initialBalance = Option(BigDecimal(10000)),
        accountStatus = Option(AccountStatus("NEW")),
        mainType = AccountMainType("liability"),
        createdBy = mockDoneBy,
        createdAt = createTime.toLocalDateTimeUTC)

      val expectedIndividualUser = IndividualUser.getEmpty.copy(
        uuid = individualUserUUID.toString, msisdn = "+971544451679")

      val expectedAccountToCreate = accountToCreate.copy(
        accountNumber = Some(AccountNumber("1.1")),
        accountName = Some(NameAttribute(s"${expectedIndividualUser.msisdn}_${config.WalletAccountType}")),
        accountStatus = Some(AccountStatus(config.NewlyCreatedAccountStatus))).asDao.get

      (mockUserDao.getUser _).when(individualUserUUID.toString).returns(Right(Some(individualUser)))

      (mockAccountDao.getAccountsByUserId _).when(individualUserUUID.toString).returns(Right(userAccountsSet))

      val expectedCriteria = AccountCriteria(userId = Option(CriteriaField(AccountSqlDao.cUserId, individualUserUUID.toString)))
      (mockAccountDao.countTotalAccountsByCriteria _).when(expectedCriteria).returns(Right(0))

      (mockIndividualUserDao.getIndividualUser _).when(individualUserUUID.toString).returns(Right(Option(expectedIndividualUser)))

      (mockAccountDao.insertAccount(_: AccountToInsert)(_: Option[Connection])).when(expectedAccountToCreate, *).returns(Left(DaoError.GenericDbError("some dao error")))

      val actualResult = accountMgmtService.createAccount(accountToCreate, Option(config.IndividualUserType))
      val expected = ServiceError.unknownError(s"some dao error", UUID.randomUUID().toOption)
      whenReady(actualResult) { actual ⇒
        actual.isLeft mustBe true
        actual.left.get.message mustBe expected.message
        actual.left.get.equals(expected) mustBe true
      }
    }
  }
}
