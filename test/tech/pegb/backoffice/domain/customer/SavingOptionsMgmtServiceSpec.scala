package tech.pegb.backoffice.domain.customer

import java.time.{LocalDate, LocalDateTime}
import java.util.{Currency, UUID}

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.bind
import tech.pegb.backoffice.core.integration.abstraction.SavingOptionsCoreApiClient
import tech.pegb.backoffice.dao.account.abstraction.AccountDao
import tech.pegb.backoffice.dao.account.entity.Account
import tech.pegb.backoffice.dao.customer.abstraction.IndividualUserDao
import tech.pegb.backoffice.dao.model.CriteriaField
import tech.pegb.backoffice.dao.savings.abstraction.{AutoDeductSavingsDao, RoundUpSavingsDao, SavingGoalsDao}
import tech.pegb.backoffice.dao.savings.dto.{AutoDeductSavingsCriteria, RoundUpSavingsCriteria, SavingGoalsCriteria}
import tech.pegb.backoffice.dao.savings.entity.{AutoDeductSaving, RoundUpSaving, SavingGoal}
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.domain.customer.abstraction.{CustomerRead, SavingOptionsMgmtService}
import tech.pegb.backoffice.domain.customer.dto.SavingOptionCriteria
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes._
import tech.pegb.backoffice.domain.customer.model.IndividualUsers.{IndividualUser, IndividualUserType}
import tech.pegb.backoffice.domain.customer.model.{GenericSavingOption, SavingOptionTypes}
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.math.BigDecimal.RoundingMode

class SavingOptionsMgmtServiceSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  override implicit val patienceConfig = PatienceConfig(timeout = 5.seconds)
  implicit val timeout = Timeout(patienceConfig.timeout)

  val mockCustomerReadService = stub[CustomerRead]
  val mockSavingGoalDao = stub[SavingGoalsDao]
  val mockAutodeductSavingsDao = stub[AutoDeductSavingsDao]
  val mockRoundupSavingsDao = stub[RoundUpSavingsDao]
  val mockIndivudalUserDao = stub[IndividualUserDao]
  val mockAccountDao = stub[AccountDao]
  val mockCoreApi = stub[SavingOptionsCoreApiClient]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[CustomerRead].to(mockCustomerReadService),
      bind[SavingGoalsDao].to(mockSavingGoalDao),
      bind[AutoDeductSavingsDao].to(mockAutodeductSavingsDao),
      bind[RoundUpSavingsDao].to(mockRoundupSavingsDao),
      bind[IndividualUserDao].to(mockIndivudalUserDao),
      bind[AccountDao].to(mockAccountDao),
      bind[SavingOptionsCoreApiClient].to(mockCoreApi),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val savingOptionsService = inject[SavingOptionsMgmtService]

  private val mockCustomerId1 = UUID.randomUUID()
  private val individualUser = IndividualUser(
    uniqueId = "1",
    id = mockCustomerId1,
    userName = Some(LoginUsername("user")),
    password = Some("password"),
    tier = Some(CustomerTier("tier")),
    segment = Some(CustomerSegment("segment1")),
    subscription = Some(CustomerSubscription("customerSubscription1")),
    email = Some(Email("user@pegb.tech")),
    status = CustomerStatus("new"),

    msisdn = Msisdn("+971544451345"),
    individualUserType = Some(IndividualUserType("individualUserType")),
    name = Some("Alice"),
    fullName = Some("Alice Wonderland"),
    gender = Some("F"),
    personId = None,
    documentNumber = None,
    documentModel = None,
    birthDate = Some(LocalDate.of(1992, 1, 1)),
    birthPlace = None,
    nationality = None,
    occupation = None,
    companyName = None,
    employer = None,
    createdAt = LocalDateTime.of(2018, 1, 1, 0, 0, 0),
    createdBy = Option("pegbuser"),
    updatedAt = Option(LocalDateTime.of(2018, 1, 1, 0, 0, 0)),
    updatedBy = Option("pegbuser"),
    activatedAt = None)

  "SavingOptionsMgmtService" should {
    val account1 = Account.getEmpty.copy(
      id = 1,
      uuid = UUID.randomUUID().toString,
      accountNumber = "4716 4157 0907 3361",
      userId = 1,
      accountName = "George Ogalo",
      accountType = "WALLET",
      isMainAccount = true.some,
      currency = "AED",
      balance = BigDecimal(0.00000).setScale(4, RoundingMode.HALF_UP).some,
      blockedBalance = BigDecimal(0.00000).setScale(4, RoundingMode.HALF_UP).some,
      status = "active".some,
      closedAt = None,
      lastTransactionAt = None,
      mainType = "standard_saving",
      createdAt = LocalDateTime.of(2018, 12, 25, 0, 0, 0),
      createdBy = "SuperAdmin",
      updatedAt = None,
      updatedBy = None)
    val account2 = Account.getEmpty.copy(
      id = 2,
      uuid = UUID.randomUUID().toString,
      accountNumber = "4716 4157 0907 0000",
      userId = 1,
      accountName = "Loyd Edano",
      accountType = "WALLET",
      isMainAccount = true.some,
      currency = "AED",
      balance = BigDecimal(0.00000).setScale(4, RoundingMode.HALF_UP).some,
      blockedBalance = BigDecimal(0.00000).setScale(4, RoundingMode.HALF_UP).some,
      status = "active".some,
      closedAt = None,
      lastTransactionAt = None,
      mainType = "standard_saving",
      createdAt = LocalDateTime.of(2018, 12, 25, 0, 0, 0),
      createdBy = "SuperAdmin",
      updatedAt = None,
      updatedBy = None)

    val sg1 = SavingGoal(
      id = 1,
      uuid = UUID.randomUUID().toString,
      userId = 1,
      userUuid = mockCustomerId1.toString,
      accountId = account1.id,
      accountUuid = account1.uuid,
      currency = account1.currency,
      dueDate = LocalDate.of(2019, 12, 25),
      statusUpdatedAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0).some,
      createdAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0),
      name = "Car",
      reason = "For buying car".some,
      status = "active",
      paymentType = "manual",
      goalAmount = BigDecimal(20000),
      currentAmount = BigDecimal(7000),
      initialAmount = BigDecimal(5000),
      emiAmount = BigDecimal(1250),
      updatedAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0))

    val sg2 = SavingGoal(
      id = 2,
      uuid = UUID.randomUUID().toString,
      userId = 1,
      userUuid = mockCustomerId1.toString,
      accountId = account1.id,
      accountUuid = account1.uuid,
      currency = account1.currency,
      dueDate = LocalDate.of(2019, 12, 25),
      statusUpdatedAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0).some,
      createdAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0),
      name = "Travel",
      reason = "Trip to Jerusalem".some,
      status = "active",
      paymentType = "manual",
      goalAmount = BigDecimal(15000),
      currentAmount = BigDecimal(5000),
      initialAmount = BigDecimal(3000),
      emiAmount = BigDecimal(1000),
      updatedAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0))

    val ru1 = RoundUpSaving(
      id = 1,
      uuid = UUID.randomUUID().toString,
      userId = 1,
      userUuid = mockCustomerId1.toString,
      accountId = account1.id,
      accountUuid = account1.uuid,
      currency = "AED",
      currentAmount = BigDecimal(2500),
      roundingNearest = 10,
      statusUpdatedAt = LocalDateTime.of(2019, 2, 1, 0, 0, 0).some,
      createdAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0),
      isActive = true,
      updatedAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0))

    val ad1 = AutoDeductSaving(
      id = 1,
      uuid = UUID.randomUUID().toString,
      userId = 1,
      userUuid = mockCustomerId1.toString,
      accountId = account2.id,
      accountUuid = account2.uuid,
      currency = "AED",
      currentAmount = BigDecimal(2500),
      savingPercentage = BigDecimal(15),
      minIncome = BigDecimal(50),
      statusUpdatedAt = LocalDateTime.of(2019, 3, 1, 0, 0, 0).some,
      createdAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0),
      isActive = true,
      updatedAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0))

    "get all saving options (only active) by the customer" in {

      (mockCustomerReadService.getIndividualUser _)
        .when(mockCustomerId1)
        .returns(Future.successful(Right(individualUser)))
      (mockSavingGoalDao.getSavingOptionsByCriteria _)
        .when(SavingGoalsCriteria(userUuid = CriteriaField("", mockCustomerId1.toString).some, isActive = CriteriaField("", true).some).some, None, None, None)
        .returns(Right(Seq(sg1, sg2)))
      (mockAutodeductSavingsDao.getSavingOptionsByCriteria _)
        .when(AutoDeductSavingsCriteria(userUuid = CriteriaField("", mockCustomerId1.toString).some, isActive = CriteriaField("", true).some).some, None, None, None)
        .returns(Right(Seq(ad1)))
      (mockRoundupSavingsDao.getSavingOptionsByCriteria _)
        .when(RoundUpSavingsCriteria(userUuid = CriteriaField("", mockCustomerId1.toString).some, isActive = CriteriaField("", true).some).some, None, None, None)
        .returns(Right(Seq(ru1)))
      (mockAccountDao.getAccountsByInternalIds _)
        .when(Set(sg1.accountId, sg2.accountId, ru1.accountId, ad1.accountId))
        .returns(Right(Set(account1, account2)))

      val expected = Seq(
        GenericSavingOption(
          id = UUID.fromString(sg1.uuid),
          customerId = mockCustomerId1,
          savingType = SavingOptionTypes.SavingGoals,
          savingGoalName = sg1.name.some,
          amount = sg1.goalAmount.some,
          currentAmount = sg1.currentAmount,
          currency = Currency.getInstance(account1.currency),
          reason = sg1.reason,
          createdAt = sg1.createdAt,
          dueDate = sg1.dueDate.some,
          updatedAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0)),
        GenericSavingOption(
          id = UUID.fromString(sg2.uuid),
          customerId = mockCustomerId1,
          savingType = SavingOptionTypes.SavingGoals,
          savingGoalName = sg2.name.some,
          amount = sg2.goalAmount.some,
          currentAmount = sg2.currentAmount,
          currency = Currency.getInstance(account1.currency),
          reason = sg2.reason,
          createdAt = sg2.createdAt,
          dueDate = sg2.dueDate.some,
          updatedAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0)),
        GenericSavingOption(
          id = UUID.fromString(ad1.uuid),
          customerId = mockCustomerId1,
          savingType = SavingOptionTypes.AutoDeduct,
          amount = none,
          currentAmount = ad1.currentAmount,
          currency = Currency.getInstance(account1.currency),
          reason = none,
          createdAt = ad1.createdAt,
          dueDate = none,
          updatedAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0)),
        GenericSavingOption(
          id = UUID.fromString(ru1.uuid),
          customerId = mockCustomerId1,
          savingType = SavingOptionTypes.RoundUp,
          amount = none,
          currentAmount = ru1.currentAmount,
          currency = Currency.getInstance(account2.currency),
          reason = none,
          createdAt = ru1.createdAt,
          dueDate = none,
          updatedAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0)))

      val result = savingOptionsService.getCustomerSavingOptions(mockCustomerId1, SavingOptionCriteria(userUuid = mockCustomerId1.some, isActive = true.some).some)

      whenReady(result) { actual ⇒
        actual.isRight mustBe true
        actual.right.get mustBe expected
      }
    }

    "deactivate a specific saving option (saving goal)" in {
      val goalId = UUID.fromString(sg1.uuid)
      val updateBy = "pegbuser"
      val updatedAt = LocalDateTime.now()
      val lastUpdatedAt = LocalDateTime.now()
      implicit val requestId = UUID.randomUUID()

      (mockCustomerReadService.getIndividualUser _)
        .when(mockCustomerId1)
        .returns(Future.successful(Right(individualUser)))
      (mockSavingGoalDao.getSavingOptionsByCriteria _)
        .when(SavingGoalsCriteria(uuid = CriteriaField("", goalId.toString).some, userUuid = CriteriaField("", mockCustomerId1.toString).some, isActive = CriteriaField("", true).some).some, None, None, None)
        .returns(Right(Seq(sg1)))
      (mockAutodeductSavingsDao.getSavingOptionsByCriteria _)
        .when(AutoDeductSavingsCriteria(uuid = CriteriaField("", goalId.toString).some, userUuid = CriteriaField("", mockCustomerId1.toString).some, isActive = CriteriaField("", true).some).some, None, None, None)
        .returns(Right(Nil))
      (mockRoundupSavingsDao.getSavingOptionsByCriteria _)
        .when(RoundUpSavingsCriteria(uuid = CriteriaField("", goalId.toString).some, userUuid = CriteriaField("", mockCustomerId1.toString).some, isActive = CriteriaField("", true).some).some, None, None, None)
        .returns(Right(Nil))
      (mockAccountDao.getAccountsByInternalIds _)
        .when(Set(sg1.accountId))
        .returns(Right(Set(account1)))
      (mockCoreApi.deactivateSavingGoal(_: Long)(_: UUID))
        .when(sg1.id, requestId)
        .returns(Future.successful(Right(Unit)))
      (mockSavingGoalDao.getSavingOptionsByCriteria _)
        .when(SavingGoalsCriteria(uuid = CriteriaField("", goalId.toString).some, userUuid = CriteriaField("", mockCustomerId1.toString).some, isActive = CriteriaField("", false).some).some, None, None, None)
        .returns(Right(Seq(sg1.copy(status = "deactivated"))))

      val expected = GenericSavingOption(
        id = UUID.fromString(sg1.uuid),
        customerId = mockCustomerId1,
        savingType = SavingOptionTypes.SavingGoals,
        savingGoalName = sg1.name.some,
        amount = sg1.goalAmount.some,
        currentAmount = sg1.currentAmount,
        currency = Currency.getInstance(account1.currency),
        reason = sg1.reason,
        createdAt = sg1.createdAt,
        dueDate = sg1.dueDate.some,
        updatedAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0))

      val result = savingOptionsService.deactivateSavingOption(goalId, mockCustomerId1, updateBy, updatedAt, lastUpdatedAt.some)

      whenReady(result) { actual ⇒
        actual.isRight mustBe true
        actual.right.get mustBe expected
      }
    }

    "deactivate a specific saving option (round up)" in {
      val goalId = UUID.fromString(ru1.uuid)
      val customerId = individualUser.id
      val updateBy = "pegbuser"
      val updatedAt = LocalDateTime.now()
      val lastUpdatedAt = LocalDateTime.now()
      implicit val requestId = UUID.randomUUID()

      (mockCustomerReadService.getIndividualUser _)
        .when(mockCustomerId1)
        .returns(Future.successful(Right(individualUser)))
      (mockSavingGoalDao.getSavingOptionsByCriteria _)
        .when(SavingGoalsCriteria(uuid = CriteriaField("", goalId.toString).some, userUuid = CriteriaField("", mockCustomerId1.toString).some, isActive = CriteriaField("", true).some).some, None, None, None)
        .returns(Right(Nil))
      (mockAutodeductSavingsDao.getSavingOptionsByCriteria _)
        .when(AutoDeductSavingsCriteria(uuid = CriteriaField("", goalId.toString).some, userUuid = CriteriaField("", mockCustomerId1.toString).some, isActive = CriteriaField("", true).some).some, None, None, None)
        .returns(Right(Nil))
      (mockRoundupSavingsDao.getSavingOptionsByCriteria _)
        .when(RoundUpSavingsCriteria(uuid = CriteriaField("", goalId.toString).some, userUuid = CriteriaField("", mockCustomerId1.toString).some, isActive = CriteriaField("", true).some).some, None, None, None)
        .returns(Right(Seq(ru1)))
      (mockAccountDao.getAccountsByInternalIds _)
        .when(Set(ru1.accountId))
        .returns(Right(Set(account1)))
      (mockCoreApi.deactivateRoundUpSaving(_: Long)(_: UUID))
        .when(ru1.id, requestId)
        .returns(Future.successful(Right(Unit)))
      (mockRoundupSavingsDao.getSavingOptionsByCriteria _)
        .when(RoundUpSavingsCriteria(uuid = CriteriaField("", goalId.toString).some, userUuid = CriteriaField("", mockCustomerId1.toString).some, isActive = CriteriaField("", false).some).some, None, None, None)
        .returns(Right(Seq(ru1.copy(isActive = false))))

      val expected = GenericSavingOption(
        id = UUID.fromString(ru1.uuid),
        customerId = mockCustomerId1,
        savingType = SavingOptionTypes.RoundUp,
        amount = none,
        currentAmount = ru1.currentAmount,
        currency = Currency.getInstance(account1.currency),
        reason = none,
        createdAt = ru1.createdAt,
        dueDate = none,
        updatedAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0))

      val result = savingOptionsService.deactivateSavingOption(goalId, mockCustomerId1, updateBy, updatedAt, lastUpdatedAt.some)

      whenReady(result) { actual ⇒
        actual.isRight mustBe true
        actual.right.get mustBe expected
      }
    }

    "deactivate a specific saving option (auto-deduct)" in {
      val goalId = UUID.fromString(ad1.uuid)
      val updateBy = "pegbuser"
      val updatedAt = LocalDateTime.now()
      val lastUpdatedAt = LocalDateTime.now()
      implicit val requestId = UUID.randomUUID()

      (mockCustomerReadService.getIndividualUser _)
        .when(mockCustomerId1)
        .returns(Future.successful(Right(individualUser)))
      (mockSavingGoalDao.getSavingOptionsByCriteria _)
        .when(SavingGoalsCriteria(uuid = CriteriaField("", goalId.toString).some, userUuid = CriteriaField("", mockCustomerId1.toString).some, isActive = CriteriaField("", true).some).some, None, None, None)
        .returns(Right(Nil))
      (mockAutodeductSavingsDao.getSavingOptionsByCriteria _)
        .when(AutoDeductSavingsCriteria(uuid = CriteriaField("", goalId.toString).some, userUuid = CriteriaField("", mockCustomerId1.toString).some, isActive = CriteriaField("", true).some).some, None, None, None)
        .returns(Right(Seq(ad1)))
      (mockRoundupSavingsDao.getSavingOptionsByCriteria _)
        .when(RoundUpSavingsCriteria(uuid = CriteriaField("", goalId.toString).some, userUuid = CriteriaField("", mockCustomerId1.toString).some, isActive = CriteriaField("", true).some).some, None, None, None)
        .returns(Right(Nil))
      (mockAccountDao.getAccountsByInternalIds _)
        .when(Set(ad1.accountId))
        .returns(Right(Set(account2)))
      (mockCoreApi.deactivateAutoDeductSaving(_: Long)(_: UUID))
        .when(ad1.id, requestId)
        .returns(Future.successful(Right(Unit)))
      (mockAutodeductSavingsDao.getSavingOptionsByCriteria _)
        .when(AutoDeductSavingsCriteria(uuid = CriteriaField("", goalId.toString).some, userUuid = CriteriaField("", mockCustomerId1.toString).some, isActive = CriteriaField("", false).some).some, None, None, None)
        .returns(Right(Seq(ad1.copy(isActive = false))))

      val expected = GenericSavingOption(
        id = UUID.fromString(ad1.uuid),
        customerId = mockCustomerId1,
        savingType = SavingOptionTypes.AutoDeduct,
        amount = none,
        currentAmount = ad1.currentAmount,
        currency = Currency.getInstance(account2.currency),
        reason = none,
        createdAt = ad1.createdAt,
        dueDate = none,
        updatedAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0))

      val result = savingOptionsService.deactivateSavingOption(goalId, mockCustomerId1, updateBy, updatedAt, lastUpdatedAt.some)

      whenReady(result) { actual ⇒
        actual.isRight mustBe true
        actual.right.get mustBe expected
      }
    }

    "fail to deactivate a specific saving option if not found" in {
      val goalId = UUID.randomUUID()
      val updateBy = "pegbuser"
      val updatedAt = LocalDateTime.now()
      val lastUpdatedAt = LocalDateTime.now()
      implicit val requestId = UUID.randomUUID()

      (mockCustomerReadService.getIndividualUser _)
        .when(mockCustomerId1)
        .returns(Future.successful(Right(individualUser)))
      (mockSavingGoalDao.getSavingOptionsByCriteria _)
        .when(SavingGoalsCriteria(uuid = CriteriaField("", goalId.toString).some, userUuid = CriteriaField("", mockCustomerId1.toString).some, isActive = CriteriaField("", true).some).some, None, None, None)
        .returns(Right(Nil))
      (mockAutodeductSavingsDao.getSavingOptionsByCriteria _)
        .when(AutoDeductSavingsCriteria(uuid = CriteriaField("", goalId.toString).some, userUuid = CriteriaField("", mockCustomerId1.toString).some, isActive = CriteriaField("", true).some).some, None, None, None)
        .returns(Right(Nil))
      (mockRoundupSavingsDao.getSavingOptionsByCriteria _)
        .when(RoundUpSavingsCriteria(uuid = CriteriaField("", goalId.toString).some, userUuid = CriteriaField("", mockCustomerId1.toString).some, isActive = CriteriaField("", true).some).some, None, None, None)
        .returns(Right(Nil))
      (mockAccountDao.getAccountsByInternalIds _)
        .when(Set.empty[Int])
        .returns(Right(Set()))

      val result = savingOptionsService.deactivateSavingOption(goalId, mockCustomerId1, updateBy, updatedAt, lastUpdatedAt.some)

      whenReady(result) { actual ⇒
        actual.isLeft mustBe true
        actual.left.get mustBe ServiceError.notFoundError(s"No active saving option matches id: $goalId for customer $mockCustomerId1", requestId.toOption)
      }
    }

    "fail to deactivate a specific saving option if customer was not found or inactive" in {
      val goalId = UUID.randomUUID()
      val updateBy = "pegbuser"
      val updatedAt = LocalDateTime.now()
      val lastUpdatedAt = LocalDateTime.now()
      implicit val requestId = UUID.randomUUID()

      (mockCustomerReadService.getIndividualUser _)
        .when(mockCustomerId1)
        .returns(Future.successful(Left(ServiceError.notFoundError("Individual User not found", requestId.toOption))))

      val result = savingOptionsService.deactivateSavingOption(goalId, mockCustomerId1, updateBy, updatedAt, lastUpdatedAt.some)

      whenReady(result) { actual ⇒
        actual.isLeft mustBe true
        actual.left.get mustBe ServiceError.notFoundError("Individual User not found", requestId.toOption)
      }
    }

    "fail to deactivate when core returns an error" in {
      val goalId = UUID.fromString(sg1.uuid)
      val updateBy = "pegbuser"
      val updatedAt = LocalDateTime.now()
      val lastUpdatedAt = LocalDateTime.now()
      implicit val requestId = UUID.randomUUID()

      (mockCustomerReadService.getIndividualUser _)
        .when(mockCustomerId1)
        .returns(Future.successful(Right(individualUser)))
      (mockSavingGoalDao.getSavingOptionsByCriteria _)
        .when(SavingGoalsCriteria(uuid = CriteriaField("", goalId.toString).some, userUuid = CriteriaField("", mockCustomerId1.toString).some, isActive = CriteriaField("", true).some).some, None, None, None)
        .returns(Right(Seq(sg1)))
      (mockAutodeductSavingsDao.getSavingOptionsByCriteria _)
        .when(AutoDeductSavingsCriteria(uuid = CriteriaField("", goalId.toString).some, userUuid = CriteriaField("", mockCustomerId1.toString).some, isActive = CriteriaField("", true).some).some, None, None, None)
        .returns(Right(Nil))
      (mockRoundupSavingsDao.getSavingOptionsByCriteria _)
        .when(RoundUpSavingsCriteria(uuid = CriteriaField("", goalId.toString).some, userUuid = CriteriaField("", mockCustomerId1.toString).some, isActive = CriteriaField("", true).some).some, None, None, None)
        .returns(Right(Nil))
      (mockAccountDao.getAccountsByInternalIds _)
        .when(Set(sg1.accountId))
        .returns(Right(Set(account1)))
      (mockCoreApi.deactivateSavingGoal(_: Long)(_: UUID))
        .when(sg1.id, requestId)
        .returns(Future.successful(Left(ServiceError.unknownError("Failed API call - non-2xx response from CORE API.", requestId.toOption))))

      val result = savingOptionsService.deactivateSavingOption(goalId, mockCustomerId1, updateBy, updatedAt, lastUpdatedAt.some)

      whenReady(result) { actual ⇒
        actual.isLeft mustBe true
        actual.left.get mustBe ServiceError.unknownError("Failed API call - non-2xx response from CORE API.", requestId.toOption)
      }
    }

  }

}
