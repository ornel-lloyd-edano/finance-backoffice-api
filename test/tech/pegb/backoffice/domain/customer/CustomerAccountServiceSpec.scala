package tech.pegb.backoffice.domain.customer

import java.sql.Connection
import java.time._
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.PlaySpec
import tech.pegb.backoffice.dao.account.abstraction.AccountDao
import tech.pegb.backoffice.dao.account.dto.{AccountCriteria, AccountToUpdate}
import tech.pegb.backoffice.dao.account.entity.Account
import tech.pegb.backoffice.dao.account.sql.{AccountSqlDao, AccountTypesSqlDao}
import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao
import tech.pegb.backoffice.dao.customer.abstraction.{BusinessUserDao, CustomerAttributesDao, IndividualUserDao, UserDao}
import tech.pegb.backoffice.dao.customer.entity._
import tech.pegb.backoffice.dao.model.CriteriaField
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.account.abstraction.AccountManagement
import tech.pegb.backoffice.domain.account.dto.{AccountCriteria ⇒ DomainAccountCriteria}
import tech.pegb.backoffice.domain.account.model.AccountAttributes._
import tech.pegb.backoffice.domain.account.model.{Account ⇒ DomainAccount}
import tech.pegb.backoffice.domain.customer.implementation.CustomerAccountService
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes._
import tech.pegb.backoffice.mapping.dao.domain.account.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.core.TestExecutionContext

class CustomerAccountServiceSpec extends PlaySpec with MockFactory with ScalaFutures {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val config = AppConfig("application.test.conf")
  val executionContexts: WithExecutionContexts = TestExecutionContext
  val mockUserDao = stub[UserDao]
  val mockBusinessUserDao = stub[BusinessUserDao]
  val individualUserDao = stub[IndividualUserDao]
  val mockAccountDao = stub[AccountDao]
  val mockCustomerDao = stub[CustomerAttributesDao]
  val mockAccountManagement = stub[AccountManagement]
  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())

  //test data
  val id1 = UUID.randomUUID()
  val userId1 = UUID.randomUUID()
  val internalUserId1 = 1
  val accountNumber1 = "0001"
  val account1 = Account.getEmpty.copy(
    id = 1,
    uuid = id1.toString,
    accountNumber = accountNumber1,
    userId = internalUserId1,
    userUuid = userId1.toString,
    accountName = "Alice",
    accountType = "COLLECTION",
    isMainAccount = true.some,
    currency = "KES",
    balance = BigDecimal(10000).some,
    blockedBalance = BigDecimal(0).some,
    status = "ACTIVE".some,
    closedAt = None,
    lastTransactionAt = None,
    mainType = "liability",
    createdAt = LocalDateTime.now(),
    createdBy = "Bob",
    updatedAt = None,
    updatedBy = None)

  val account1NoRemainingBalance = Account.getEmpty.copy(
    id = 1,
    uuid = id1.toString,
    accountNumber = accountNumber1,
    userId = internalUserId1,
    accountName = "Alice",
    accountType = "COLLECTION",
    isMainAccount = true.some,
    currency = "KES",
    balance = BigDecimal(0).some,
    blockedBalance = BigDecimal(0).some,
    status = "ACTIVE".some,
    closedAt = None,
    lastTransactionAt = None,
    mainType = "liability",
    createdAt = LocalDateTime.now(),
    createdBy = "Bob",
    updatedAt = None,
    updatedBy = None)

  val id2 = UUID.randomUUID()
  val userId2 = UUID.randomUUID()
  val accountNumber2 = "0002"
  val internalUserId2 = 2
  val account2 = Account.getEmpty.copy(
    id = 2,
    uuid = id2.toString,
    accountNumber = accountNumber2,
    userId = internalUserId2,
    userUuid = userId2.toString,
    accountName = "Carl",
    accountType = "DISTRIBUTION",
    isMainAccount = false.some,
    currency = "AED",
    balance = BigDecimal(5000).some,
    blockedBalance = BigDecimal(1000).some,
    status = "ACTIVE".some,
    closedAt = None,
    lastTransactionAt = None,
    mainType = "liability",
    createdAt = LocalDateTime.now(),
    createdBy = "Bob",
    updatedAt = None,
    updatedBy = None)

  val id3 = UUID.randomUUID()
  val accountNumber3 = "0003"
  val account3 = Account.getEmpty.copy(
    id = 3,
    uuid = id3.toString,
    accountNumber = accountNumber3,
    userId = internalUserId1,
    userUuid = userId1.toString,
    accountName = "Alice II",
    accountType = "DISTRIBUTION",
    isMainAccount = false.some,
    currency = "KES",
    balance = BigDecimal(9000).some,
    blockedBalance = BigDecimal(500).some,
    status = "BLOCKED".some,
    closedAt = None,
    lastTransactionAt = None,
    mainType = "liability",
    createdAt = LocalDateTime.now(),
    createdBy = "Bob",
    updatedAt = None,
    updatedBy = None)

  "CustomerAccountService getAccounts" should {
    "return accounts in relation to customerId" in {

      (mockAccountDao.getAccountsByUserId _).when(
        userId1.toString).returns(Right(Set(account1, account2)))

      val customerAccountService = new CustomerAccountService(config, executionContexts, mockUserDao, mockBusinessUserDao, individualUserDao, mockAccountDao, mockAccountManagement)

      val futureResult = customerAccountService.getAccounts(userId1)

      whenReady(futureResult) { res ⇒
        res.isRight mustBe true
        res.right.get mustEqual Set(account1, account2).flatMap(acc ⇒
          acc.asDomain.toOption)
      }
    }
  }

  "CustomerAccountService getAccountByAccountNumber" should {
    "return account in relation to accountNumber" in {

      (mockAccountDao.getAccountByAccNum _).when(
        accountNumber1).returns(Right(Option(account1)))

      (mockUserDao.getUUIDByInternalUserId _).when(internalUserId1).returns(Right(Some(userId1.toString)))

      val customerAccountService = new CustomerAccountService(config, executionContexts, mockUserDao, mockBusinessUserDao, individualUserDao, mockAccountDao, mockAccountManagement)

      val futureResult = customerAccountService.getAccountByAccountNumber(AccountNumber(accountNumber1))

      whenReady(futureResult) { res ⇒
        res.isRight mustBe true
        res.right.get mustEqual account1.asDomain.get
      }
    }
  }
  "CustomerAccountService getAccountByAccountNumber" should {
    "return notFoundException when Dao returns None" in {

      (mockAccountDao.getAccountByAccNum _).when(
        accountNumber1).returns(Right(None))

      val customerAccountService = new CustomerAccountService(config, executionContexts, mockUserDao, mockBusinessUserDao, individualUserDao, mockAccountDao, mockAccountManagement)

      val futureResult = customerAccountService.getAccountByAccountNumber(AccountNumber(accountNumber1))

      whenReady(futureResult) { res ⇒
        res.isLeft mustBe true
        res.left.get.equals(ServiceError.notFoundError(s"Account with accountNumber ${accountNumber1} not found.", UUID.randomUUID().toOption)) mustEqual true
      }
    }
  }

  //this unit test should be transfered to AccountMgmtServiceSpec
  /*"CustomerAccountService getAccountById" should {
    "return account in relation to accountId" in {

      (mockAccountDao.getAccount _).when(
        id1.toString.toString).returns(Right(Option(account1)))

      (mockUserDao.getUUIDByInternalUserId _).when(internalUserId1).returns(Right(Some(userId1.toString)))

      val customerAccountService = new CustomerAccountService(config, executionContexts, mockUserDao,
        mockBusinessUserDao, individualUserDao, mockAccountDao, mockAccountManagement)

      val futureResult = customerAccountService.getAccountById(id1)

      whenReady(futureResult) { res ⇒
        res.isRight mustBe true
        res.right.get mustEqual account1.asDomain(userId1).get
      }
    }
  }

  "CustomerAccountService getAccountById" should {
    "return notFoundException when Dao returns None" in {

      val wrongUUID = UUID.randomUUID()
      (mockAccountDao.getAccount _).when(
        wrongUUID.toString).returns(Right(None))

      val customerAccountService = new CustomerAccountService(config, executionContexts, mockUserDao,
        mockBusinessUserDao, individualUserDao, mockAccountDao, mockAccountManagement)

      val futureResult = customerAccountService.getAccountById(wrongUUID)

      whenReady(futureResult) { res ⇒
        res.isLeft mustBe true
        res.left.get.equals(ServiceError.notFoundEntityError(UUID.randomUUID(), s"Account with id ${wrongUUID.toString} not found.")) mustEqual true
      }
    }
  }*/

  "CustomerAccountService getAccountByAccountName" should {
    "return account in relation to accountName" in {

      (mockAccountDao.getAccountByAccountName _).when(
        "Carl").returns(Right(Option(account2)))

      (mockUserDao.getUUIDByInternalUserId _).when(internalUserId2).returns(Right(Some(userId2.toString)))

      val customerAccountService = new CustomerAccountService(config, executionContexts, mockUserDao, mockBusinessUserDao, individualUserDao, mockAccountDao, mockAccountManagement)

      val futureResult = customerAccountService.getAccountByAccountName(NameAttribute("Carl"))

      whenReady(futureResult) { res ⇒
        res.isRight mustBe true
        res.right.get mustEqual account2.asDomain.get
      }
    }
  }

  "CustomerAccountService getAccountByAccountName" should {
    "return notFoundException when Dao returns None" in {

      (mockAccountDao.getAccountByAccountName _).when(
        "ZEBRA").returns(Right(None))

      val customerAccountService = new CustomerAccountService(config, executionContexts, mockUserDao, mockBusinessUserDao, individualUserDao, mockAccountDao, mockAccountManagement)

      val futureResult = customerAccountService.getAccountByAccountName(NameAttribute("ZEBRA"))

      whenReady(futureResult) { res ⇒
        res.isLeft mustBe true
        res.left.get.equals(ServiceError.notFoundError(s"Account with accountName ZEBRA not found.", UUID.randomUUID().toOption)) mustEqual true
      }
    }
  }

  "CustomerAccountService getAccountsByCriteria" should {
    "return account set with criteria customerId" in {

      val customerId = userId1
      val criteria = Some(AccountCriteria(userId = Option(CriteriaField(AccountSqlDao.cUserId, customerId.toString))))
      (mockAccountDao.getAccountsByCriteria _).when(
        criteria,
        None,
        None,
        None).returns(Right(Seq(account1, account3)))

      (mockUserDao.getUUIDByInternalUserId _).when(internalUserId1).returns(Right(Some(userId1.toString)))

      val customerAccountService = new CustomerAccountService(config, executionContexts, mockUserDao, mockBusinessUserDao, individualUserDao, mockAccountDao, mockAccountManagement)

      val futureResult = customerAccountService.getAccountsByCriteria(
        DomainAccountCriteria(customerId = customerId.toUUIDLike.toOption),
        Nil,
        None, None)

      whenReady(futureResult) { res ⇒
        res.isRight mustBe true
        res.right.get mustEqual Seq(account1, account3).flatMap(acc ⇒
          acc.asDomain.toOption)
      }
    }
    "return account set with criteria isMainAccount == true" in {
      val customerId = userId1
      val isMainAccount = Option(true)
      val criteria = Some(AccountCriteria(
        userId = Option(CriteriaField(AccountSqlDao.cUserId, customerId.toString)),
        isMainAccount = isMainAccount.map(CriteriaField(AccountSqlDao.cIsMainAccount, _))))
      (mockAccountDao.getAccountsByCriteria _).when(
        criteria,
        None,
        None,
        None).returns(Right(Seq(account1)))

      (mockUserDao.getUUIDByInternalUserId _).when(internalUserId1).returns(Right(Some(userId1.toString)))

      val customerAccountService = new CustomerAccountService(config, executionContexts, mockUserDao,
        mockBusinessUserDao, individualUserDao, mockAccountDao, mockAccountManagement)

      val futureResult = customerAccountService.getAccountsByCriteria(
        DomainAccountCriteria(customerId = customerId.toUUIDLike.toOption, isMainAccount = isMainAccount),
        Nil,
        None, None)

      whenReady(futureResult) { res ⇒
        res.isRight mustBe true
        res.right.get mustEqual Seq(account1).flatMap(_.asDomain.toOption)
      }
    }
    "return account set with criteria currency" in {
      val customerId = userId1
      val currency = Option("KES")
      val criteria = Some(AccountCriteria(
        userId = Option(CriteriaField(AccountSqlDao.cUserId, customerId.toString)),
        currency = currency.map(CriteriaField(CurrencySqlDao.cName, _))))
      (mockAccountDao.getAccountsByCriteria _).when(
        criteria,
        None,
        None,
        None).returns(Right(Seq(account1)))

      (mockUserDao.getUUIDByInternalUserId _).when(internalUserId1).returns(Right(Some(userId1.toString)))

      val customerAccountService = new CustomerAccountService(config, executionContexts, mockUserDao, mockBusinessUserDao, individualUserDao, mockAccountDao, mockAccountManagement)

      val futureResult = customerAccountService.getAccountsByCriteria(
        DomainAccountCriteria(customerId = customerId.toUUIDLike.toOption, currency = currency),
        Nil,
        None, None)

      whenReady(futureResult) { res ⇒
        res.isRight mustBe true
        res.right.get mustEqual Seq(account1).flatMap(_.asDomain.toOption)
      }
    }
    "return account set with criteria status" in {
      val customerId = userId1
      val status = Option("ACTIVE")
      val criteria = Some(AccountCriteria(
        userId = Option(CriteriaField(AccountSqlDao.cUserId, customerId.toString)),
        status = status.map(CriteriaField(AccountSqlDao.cStatus, _))))
      (mockAccountDao.getAccountsByCriteria _).when(
        criteria,
        None,
        None,
        None).returns(Right(Seq(account1, account2)))

      (mockUserDao.getUUIDByInternalUserId _).when(internalUserId1).returns(Right(Some(userId1.toString)))

      (mockUserDao.getUUIDByInternalUserId _).when(internalUserId2).returns(Right(Some(userId2.toString)))

      val customerAccountService = new CustomerAccountService(config, executionContexts, mockUserDao, mockBusinessUserDao, individualUserDao, mockAccountDao, mockAccountManagement)

      val futureResult = customerAccountService.getAccountsByCriteria(
        DomainAccountCriteria(customerId = customerId.toUUIDLike.toOption, status = status),
        Nil,
        None, None)

      whenReady(futureResult) { res ⇒
        res.isRight mustBe true
        res.right.get mustEqual Seq(
          account1.asDomain.get,
          account2.asDomain.get) //.flatMap(_.asDomain(userId1).toOption)
      }
    }
    "return account set with criteria accountType" in {
      val customerId = userId1
      val accountType = Option("COLLECTION")
      val criteria = Some(AccountCriteria(
        userId = Option(CriteriaField(AccountSqlDao.cUserId, customerId.toString)),
        accountType = accountType.map(CriteriaField(AccountTypesSqlDao.cAccountType, _))))
      (mockAccountDao.getAccountsByCriteria _).when(
        criteria,
        None,
        None,
        None).returns(Right(Seq(account1)))

      (mockUserDao.getUUIDByInternalUserId _).when(internalUserId1).returns(Right(Some(userId1.toString)))

      val customerAccountService = new CustomerAccountService(config, executionContexts, mockUserDao, mockBusinessUserDao, individualUserDao, mockAccountDao, mockAccountManagement)

      val futureResult = customerAccountService.getAccountsByCriteria(
        DomainAccountCriteria(customerId = customerId.toUUIDLike.toOption, accountType = accountType),
        Nil,
        None, None)

      whenReady(futureResult) { res ⇒
        res.isRight mustBe true
        res.right.get mustEqual Seq(account1).flatMap(_.asDomain.toOption)
      }
    }
    "return account set with criteria all" in {

      val customerId = userId1
      val isMainAccount = Option(true)
      val currency = Option("KES")
      val status = Option("ACTIVE")
      val accountType = Option("COLLECTION")

      val criteria = DomainAccountCriteria(
        customerId = customerId.toUUIDLike.toOption,
        accountType = accountType,
        isMainAccount = isMainAccount,
        status = status,
        currency = currency)

      import tech.pegb.backoffice.mapping.domain.dao.account.Implicits._

      (mockAccountDao.getAccountsByCriteria _).when(
        Some(criteria.asDao),
        None,
        None,
        None).returns(Right(Seq(account1)))

      (mockUserDao.getUUIDByInternalUserId _).when(internalUserId1).returns(Right(Some(userId1.toString)))

      val customerAccountService = new CustomerAccountService(config, executionContexts, mockUserDao, mockBusinessUserDao, individualUserDao, mockAccountDao, mockAccountManagement)

      val futureResult = customerAccountService.getAccountsByCriteria(criteria, Nil, None, None)

      whenReady(futureResult) { res ⇒
        res.isRight mustBe true
        res.right.get mustEqual Seq(account1).flatMap(_.asDomain.toOption)
      }
    }
  }

  "CustomerAccountService getMainAccount" should {
    "return main account in relation to customerId" in {

      (mockAccountDao.getMainAccountByUserId _).when(
        userId1.toString).returns(Right(Option(account1)))

      val customerAccountService = new CustomerAccountService(config, executionContexts, mockUserDao, mockBusinessUserDao, individualUserDao, mockAccountDao, mockAccountManagement)

      val futureResult = customerAccountService.getMainAccount(userId1)

      whenReady(futureResult) { res ⇒
        res.isRight mustBe true
        res.right.get mustEqual account1.asDomain.get
      }
    }
  }

  "CustomerAccountService activateIndividualUserAccount" should {
    "return an individual user's previously inactive account with ACTIVE status" in {
      val doneBy = "SUPER_USER"
      val doneAt = LocalDateTime.now()
      val accountToUpdate = AccountToUpdate(
        status = Option(DomainAccount.ACTIVE),
        updatedAt = doneAt,
        updatedBy = doneBy)

      val mockAccountId = UUID.randomUUID()

      val expectedAccount = account1.copy(
        uuid = mockAccountId.toString,
        userId = internalUserId1,
        status = DomainAccount.ACTIVE.some,
        updatedAt = Option(doneAt),
        updatedBy = Option(doneBy))

      val mockActiveIndividualUser = IndividualUser.getEmpty.copy(uuid = userId1.toString, status = "ACTIVE")
      (individualUserDao.getIndividualUser _).when(userId1.toString).returns(Right(Some(mockActiveIndividualUser)))

      val mockInactiveAccount = Account.getEmpty.copy(uuid = mockAccountId.toString, status = "DEACTIVATED".some)
      (mockAccountDao.getAccount _).when(mockAccountId.toString).returns(Right(Some(mockInactiveAccount)))

      (mockAccountDao.updateAccount(_: String, _: AccountToUpdate)(_: Option[Connection])).when(
        mockAccountId.toString,
        accountToUpdate,
        *).returns(Right(Some(expectedAccount)))

      (mockUserDao.getUUIDByInternalUserId _).when(internalUserId1).returns(Right(Some(userId1.toString)))

      val customerAccountService = new CustomerAccountService(config, executionContexts, mockUserDao, mockBusinessUserDao, individualUserDao, mockAccountDao, mockAccountManagement) {
        override val clock = mockClock
      }

      val futureResult = customerAccountService.activateIndividualUserAccount(userId1, mockAccountId, doneBy, doneAt)

      whenReady(futureResult) { res ⇒
        res.isRight mustBe true
        res.right.get mustEqual expectedAccount.asDomain.get
      }

    }
  }

  "CustomerAccountService deactivateIndividualUserAccount" should {
    "return an individual user's previously active account with DEACTIVATED status" in {
      val doneBy = "SUPER_USER"
      val doneAt = LocalDateTime.now()
      val accountToUpdate = AccountToUpdate(
        status = Option(DomainAccount.BLOCKED),
        updatedAt = doneAt,
        updatedBy = doneBy)

      val mockAccountId = UUID.randomUUID()

      val expectedAccount = account1.copy(
        uuid = mockAccountId.toString,
        userId = internalUserId1,
        status = DomainAccount.BLOCKED.some,
        updatedAt = Option(doneAt),
        updatedBy = Option(doneBy))

      val mockActiveIndividualUser = IndividualUser.getEmpty.copy(uuid = userId1.toString, status = "ACTIVE")
      (individualUserDao.getIndividualUser _).when(userId1.toString).returns(Right(Some(mockActiveIndividualUser)))

      val mockActiveAccount = Account.getEmpty.copy(uuid = mockAccountId.toString, status = "ACTIVE".some)

      (mockAccountDao.getAccount _).when(mockAccountId.toString).returns(Right(Some(mockActiveAccount)))

      (mockAccountDao.updateAccount(_: String, _: AccountToUpdate)(_: Option[Connection])).when(
        mockAccountId.toString,
        accountToUpdate,
        *).returns(Right(Some(expectedAccount)))

      (mockUserDao.getUUIDByInternalUserId _).when(internalUserId1).returns(Right(Some(userId1.toString)))

      val customerAccountService = new CustomerAccountService(config, executionContexts, mockUserDao, mockBusinessUserDao, individualUserDao, mockAccountDao, mockAccountManagement) {
        override val clock = mockClock
      }

      val futureResult = customerAccountService.deactivateIndividualUserAccount(userId1, mockAccountId, doneBy, doneAt)

      whenReady(futureResult) { res ⇒
        res.isRight mustBe true
        res.right.get mustEqual expectedAccount.asDomain.get
      }

    }
  }

  "CustomerAccountService closeIndividualUserAccount" should {
    "return an individual user's previously inactive account with ACTIVE status" in {
      val doneBy = "SUPER_USER"
      val doneAt = LocalDateTime.now()
      val accountToUpdate = AccountToUpdate(
        status = Option(DomainAccount.CLOSED),
        updatedAt = doneAt,
        updatedBy = doneBy)

      val mockAccountId = UUID.randomUUID()

      val expectedAccount = account1.copy(
        uuid = mockAccountId.toString,
        userId = internalUserId1,
        status = DomainAccount.CLOSED.some,
        balance = BigDecimal("0").some,
        updatedAt = Option(doneAt),
        updatedBy = Option(doneBy))

      val mockActiveIndividualUser = IndividualUser.getEmpty.copy(uuid = userId1.toString, status = "ACTIVE")
      (individualUserDao.getIndividualUser _).when(userId1.toString).returns(Right(Some(mockActiveIndividualUser)))

      val mockActiveAccountNoBalance = Account.getEmpty.copy(uuid = mockAccountId.toString, status = "ACTIVE".some, balance = BigDecimal("0").some)
      (mockAccountDao.getAccount _).when(mockAccountId.toString).returns(Right(Some(mockActiveAccountNoBalance)))

      (mockAccountDao.updateAccount(_: String, _: AccountToUpdate)(_: Option[Connection])).when(
        mockAccountId.toString,
        accountToUpdate,
        *).returns(Right(Some(expectedAccount)))

      (mockUserDao.getUUIDByInternalUserId _).when(internalUserId1).returns(Right(Some(userId1.toString)))

      val customerAccountService = new CustomerAccountService(config, executionContexts, mockUserDao, mockBusinessUserDao, individualUserDao, mockAccountDao, mockAccountManagement) {
        override val clock = mockClock
      }

      val futureResult = customerAccountService.closeIndividualUserAccount(userId1, mockAccountId, doneBy, doneAt)

      whenReady(futureResult) { res ⇒
        res.isRight mustBe true
        res.right.get mustEqual expectedAccount.asDomain.get
      }

    }
  }

}
