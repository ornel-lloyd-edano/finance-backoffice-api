package tech.pegb.backoffice.dao

import java.time.LocalDateTime

import org.scalamock.scalatest.MockFactory
import play.api.inject.bind
import scala.math.BigDecimal.RoundingMode
import tech.pegb.backoffice.dao.account.dto.{AccountCriteria, AccountToInsert, AccountToUpdate}
import tech.pegb.backoffice.dao.account.entity.Account
import tech.pegb.backoffice.dao.account.sql.AccountSqlDao
import tech.pegb.backoffice.dao.customer.abstraction.{CompanyDao, CustomerAttributesDao}
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes, Ordering, OrderingSet}
import tech.pegb.core.PegBTestApp

import scala.collection.mutable

class AccountSqlDaoSpec extends PegBTestApp with MockFactory {

  import Dao._

  private val mockedCustomer: CustomerAttributesDao = stub[CustomerAttributesDao]
  private val mockedCompany: CompanyDao = stub[CompanyDao]

  override val additionalBindings = super.additionalBindings ++
    Seq(
      bind[CustomerAttributesDao].to(mockedCustomer),
      bind[CompanyDao].to(mockedCompany))

  lazy val accountSqlDao = fakeApplication().injector.instanceOf[AccountSqlDao]

  override def initSql =
    s"""
       |INSERT INTO account_types(id, account_type_name, description, created_at, updated_at, is_active)
       |VALUES('1', 'WALLET', 'standard account type for individual users', now(), null, 1);
       |
       |INSERT INTO currencies(id, currency_name, description, created_at, updated_at, is_active)
       |VALUES('1', 'AED', 'default currency for UAE', now(), null, 1);
       |
       |INSERT INTO currencies(id, currency_name, description, created_at, updated_at, is_active)
       |VALUES('2', 'KES', 'default currency for KENYA', now(), null, 1);
       |
       |INSERT INTO users(id, uuid, username, password, email, subscription, type, status, tier, segment, activated_at, password_updated_at, created_at, created_by, updated_at, updated_by)
       |VALUES
       |('1', 'bcc32571-cf16-4abc-ac38-38d58f9cbab5', null, null, null, null, 'individual', null, null, null, now(), null, now(), 'SuperAdmin', null, null),
       |('2', '4634db00-2b61-4a23-8348-35f0030c3b1d', null, null, null, null, 'individual', null, null, null, now(), null, now(), 'SuperAdmin', null, null);
       |
       |INSERT INTO individual_users
       |(msisdn, user_id, type, name, fullname, gender, company, birthdate, birth_place, nationality, occupation, employer, created_at, created_by, updated_at, updated_by)
       |VALUES
       |('971544465329', '1', 'type_one', 'David', 'David Ker Salgado', 'M', 'PegB', '1990-01-01', 'Dubai', 'Filipini', 'Developer', 'PegB', '2018-10-01 00:00:00', 'SuperUser', null, null),
       |('971544451679', '2', 'type_two', 'Ornel Lloyd', null, 'M', 'PegB', '1980-01-01', 'Dubai', 'Filipini', 'Developer', 'PegB', '2018-10-01 00:00:00', 'SuperUser', null, null);
       |
       |INSERT INTO accounts(id, uuid, number, user_id, name, is_main_account, account_type_id, currency_id, balance, blocked_balance, status, closed_at, last_transaction_at, created_at, created_by, updated_at, updated_by, main_type)
       |VALUES('1', '1c15bdb9-5d42-4a84-922a-0d01f8e5de71', '4716 4157 0907 3361', '1', 'George Ogalo', '1', '1', '2', '0.0', '0.0', 'INACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null, 'liability'),
       |('2', 'c86eb85f-70f8-4ccd-ae28-68bb08cc1fa5', '8912 3287 1209 3422', '2', 'Ujali Tyagi', '0', '1', '1', '20.0', '50.0', 'ACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null, 'liability'),
       |('3', '2c41cedf-b4ad-4daf-8019-35ff82c015cc', '8912 3287 1209 3423', '2', 'Ujali Tyagi', '0', '1', '1', '30.0', '60.0', 'ACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null, 'liability'),
       |('4', 'b370ef14-b219-42b3-8160-5f08a2bcc73a', '4716 4157 0907 3364', '1', 'George Ogalo', '1', '1', '1', '0.0', '0.0', 'INACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null, 'asset');
    """.stripMargin

  override def cleanupSql: String =
    """
      |DELETE FROM accounts;
      |DELETE FROM account_types;
      |DELETE FROM business_users_has_extra_attributes;
      |DELETE FROM user_status_has_requirements;
      |DELETE FROM extra_attribute_types;
      |DELETE FROM business_users;
      |DELETE FROM individual_users;
      |DELETE FROM users;
    """.stripMargin

  private val account = Account.getEmpty.copy(
    id = 1,
    uuid = "1c15bdb9-5d42-4a84-922a-0d01f8e5de71",
    accountNumber = "4716 4157 0907 3361",
    userId = 1,
    userUuid = "bcc32571-cf16-4abc-ac38-38d58f9cbab5",
    userType = "individual",
    anyCustomerName = Some("David Ker Salgado"),
    individualUserName = Some("David"),
    individualUserFullName = Some("David Ker Salgado"),
    msisdn = Some("971544465329"),
    accountName = "George Ogalo",
    accountType = "WALLET",
    isMainAccount = Some(true),
    currency = "KES",
    balance = Some(BigDecimal(0.00000).setScale(4, RoundingMode.HALF_UP)),
    blockedBalance = Some(BigDecimal(0.00000).setScale(4, RoundingMode.HALF_UP)),
    status = Some("INACTIVE"),
    closedAt = None,
    lastTransactionAt = None,
    mainType = "liability",
    createdAt = LocalDateTime.of(2018, 12, 25, 0, 0, 0),
    createdBy = "SuperAdmin",
    updatedAt = None,
    updatedBy = None)

  "AccountSqlDao" should {

    "insert account" in {
      val accountToInsert = AccountToInsert(
        accountNumber = "test_one",
        userId = "bcc32571-cf16-4abc-ac38-38d58f9cbab5",
        accountName = "4716 4157 0907 1111",
        accountType = "WALLET",
        isMainAccount = true,
        currency = "AED",
        balance = BigDecimal(5000),
        blockedBalance = BigDecimal(1000),
        status = "ACTIVE",
        mainType = "liability",
        createdAt = LocalDateTime.of(2019, 1, 21, 0, 0, 0),
        createdBy = "SuperUser")

      val result = accountSqlDao.insertAccount(accountToInsert)

      result.isRight
    }

    "get accounts by internal id set" in {
      val result = accountSqlDao.getAccountsByInternalIds(Set(1, 2, 3))

      val expected = Set(
        Account.getEmpty.copy(
          id = 2,
          uuid = "c86eb85f-70f8-4ccd-ae28-68bb08cc1fa5",
          accountNumber = "8912 3287 1209 3422",
          userId = 2,
          userUuid = "4634db00-2b61-4a23-8348-35f0030c3b1d",
          userType = "individual",
          anyCustomerName = Some("Ornel Lloyd"),
          individualUserName = Some("Ornel Lloyd"),
          individualUserFullName = None,
          msisdn = Some("971544451679"),
          accountName = "Ujali Tyagi",
          accountType = "WALLET",
          isMainAccount = Some(false),
          currency = "AED",
          balance = Some(BigDecimal(20.00).setScale(4, RoundingMode.HALF_UP)),
          blockedBalance = Some(BigDecimal(50.00).setScale(4, RoundingMode.HALF_UP)),
          status = Some("ACTIVE"),
          closedAt = None,
          lastTransactionAt = None,
          mainType = "liability",
          createdAt = LocalDateTime.of(2018, 12, 25, 0, 0, 0),
          createdBy = "SuperAdmin",
          updatedAt = None,
          updatedBy = None),
        Account.getEmpty.copy(
          id = 3,
          uuid = "2c41cedf-b4ad-4daf-8019-35ff82c015cc",
          accountNumber = "8912 3287 1209 3423",
          userId = 2,
          userUuid = "4634db00-2b61-4a23-8348-35f0030c3b1d",
          userType = "individual",
          anyCustomerName = Some("Ornel Lloyd"),
          individualUserName = Some("Ornel Lloyd"),
          individualUserFullName = None,
          msisdn = Some("971544451679"),
          accountName = "Ujali Tyagi",
          accountType = "WALLET",
          isMainAccount = Some(false),
          currency = "AED",
          balance = Some(BigDecimal(30.00).setScale(4, RoundingMode.HALF_UP)),
          blockedBalance = Some(BigDecimal(60.00).setScale(4, RoundingMode.HALF_UP)),
          status = Some("ACTIVE"),
          closedAt = None,
          lastTransactionAt = None,
          mainType = "liability",
          createdAt = LocalDateTime.of(2018, 12, 25, 0, 0, 0),
          createdBy = "SuperAdmin",
          updatedAt = None,
          updatedBy = None),
        Account.getEmpty.copy(
          id = 1,
          uuid = "1c15bdb9-5d42-4a84-922a-0d01f8e5de71",
          accountNumber = "4716 4157 0907 3361",
          userId = 1,
          userUuid = "bcc32571-cf16-4abc-ac38-38d58f9cbab5",
          userType = "individual",
          anyCustomerName = Some("David Ker Salgado"),
          individualUserName = Some("David"),
          individualUserFullName = Some("David Ker Salgado"),
          msisdn = Some("971544465329"),
          accountName = "George Ogalo",
          accountType = "WALLET",
          isMainAccount = Some(true),
          currency = "KES",
          balance = Some(BigDecimal(0.00000).setScale(4, RoundingMode.HALF_UP)),
          blockedBalance = Some(BigDecimal(0.00000).setScale(4, RoundingMode.HALF_UP)),
          status = Some("INACTIVE"),
          closedAt = None,
          lastTransactionAt = None,
          mainType = "liability",
          createdAt = LocalDateTime.of(2018, 12, 25, 0, 0, 0),
          createdBy = "SuperAdmin",
          updatedAt = None,
          updatedBy = None))

      result.isRight mustBe true
      result.right.get mustBe expected
    }

    "get account by id" in {
      val result = accountSqlDao.getAccount("1c15bdb9-5d42-4a84-922a-0d01f8e5de71")

      result mustBe Right(Some(account))
    }

    "get account by id and make sure it has individual user full name, if account holder is an individual user" in {
      val result = accountSqlDao.getAccount("1c15bdb9-5d42-4a84-922a-0d01f8e5de71")

      result.right.get.get.individualUserName mustBe Some("David")
      result.right.get.get.individualUserFullName mustBe Some("David Ker Salgado")
    }

    "fail to get account by id" in {
      val result = accountSqlDao.getAccount("1234567890")

      result mustBe Right(None)
    }

    "get account by user id" in {
      val result = accountSqlDao.getAccountsByUserId("bcc32571-cf16-4abc-ac38-38d58f9cbab5")

      result.map(_.map(_.id)) mustBe Right(Set(account.id, 4, 5))
    }

    "get account by criteria" in {
      val accountByCriteriaOne = Account.getEmpty.copy(
        id = 2,
        uuid = "c86eb85f-70f8-4ccd-ae28-68bb08cc1fa5",
        accountNumber = "8912 3287 1209 3422",
        userId = 2,
        userUuid = "4634db00-2b61-4a23-8348-35f0030c3b1d",
        userType = "individual",
        anyCustomerName = Some("Ornel Lloyd"),
        individualUserName = Some("Ornel Lloyd"),
        individualUserFullName = None,
        msisdn = Some("971544451679"),
        accountName = "Ujali Tyagi",
        accountType = "WALLET",
        isMainAccount = Some(false),
        currency = "AED",
        balance = Some(BigDecimal(20.00).setScale(4, RoundingMode.HALF_UP)),
        blockedBalance = Some(BigDecimal(50.00).setScale(4, RoundingMode.HALF_UP)),
        status = Some("ACTIVE"),
        closedAt = None,
        lastTransactionAt = None,
        mainType = "liability",
        createdAt = LocalDateTime.of(2018, 12, 25, 0, 0, 0),
        createdBy = "SuperAdmin",
        updatedAt = None,
        updatedBy = None)

      val accountByCriteriaTwo = Account.getEmpty.copy(
        id = 3,
        uuid = "2c41cedf-b4ad-4daf-8019-35ff82c015cc",
        accountNumber = "8912 3287 1209 3423",
        userId = 2,
        userUuid = "4634db00-2b61-4a23-8348-35f0030c3b1d",
        userType = "individual",
        anyCustomerName = Some("Ornel Lloyd"),
        individualUserName = Some("Ornel Lloyd"),
        individualUserFullName = None,
        msisdn = Some("971544451679"),
        accountName = "Ujali Tyagi",
        accountType = "WALLET",
        isMainAccount = Some(false),
        currency = "AED",
        balance = Some(BigDecimal(30.00).setScale(4, RoundingMode.HALF_UP)),
        blockedBalance = Some(BigDecimal(60.00).setScale(4, RoundingMode.HALF_UP)),
        status = Some("ACTIVE"),
        closedAt = None,
        lastTransactionAt = None,
        mainType = "liability",
        createdAt = LocalDateTime.of(2018, 12, 25, 0, 0, 0),
        createdBy = "SuperAdmin",
        updatedAt = None,
        updatedBy = None)

      val accountByCriteriaThree = Account.getEmpty.copy(
        id = 1,
        uuid = "1c15bdb9-5d42-4a84-922a-0d01f8e5de71",
        accountNumber = "4716 4157 0907 3361",
        userId = 1,
        userUuid = "bcc32571-cf16-4abc-ac38-38d58f9cbab5",
        userType = "individual",
        anyCustomerName = Some("David Ker Salgado"),
        individualUserName = Some("David"),
        individualUserFullName = Some("David Ker Salgado"),
        msisdn = Some("971544465329"),
        accountName = "George Ogalo",
        accountType = "WALLET",
        isMainAccount = Some(true),
        currency = "KES",
        balance = Some(BigDecimal(0.00000).setScale(4, RoundingMode.HALF_UP)),
        blockedBalance = Some(BigDecimal(0.00000).setScale(4, RoundingMode.HALF_UP)),
        status = Some("INACTIVE"),
        closedAt = None,
        lastTransactionAt = None,
        mainType = "liability",
        createdAt = LocalDateTime.of(2018, 12, 25, 0, 0, 0),
        createdBy = "SuperAdmin",
        updatedAt = None,
        updatedBy = None)

      val criteriaOne = AccountCriteria(
        userId = Some(CriteriaField("", "4634db00-2b61-4a23-8348-35f0030c3b1d")),
        isMainAccount = Some(CriteriaField("", false)),
        currency = Some(CriteriaField("", "AED")),
        status = Some(CriteriaField("", "ACTIVE")),
        createdBy = Some(CriteriaField("", "SuperAdmin")))

      val criteriaTwo = AccountCriteria(userId = Some(CriteriaField("", "bcc32571-cf16-4abc-ac38-38d58f9cbab5")))

      val resultOne = accountSqlDao.getAccountsByCriteria(Some(criteriaOne), limit = Some(1), offset = Some(0))
      val resultTwo = accountSqlDao.getAccountsByCriteria(Some(criteriaOne), limit = Some(1), offset = Some(1))
      val resultThree = accountSqlDao.getAccountsByCriteria(Some(criteriaTwo), offset = Some(0))

      resultOne mustBe Right(Seq(accountByCriteriaOne))
      resultTwo mustBe Right(Seq(accountByCriteriaTwo))
      resultThree.map(_.map(_.id)) mustBe Right(Seq(accountByCriteriaThree.id, 4, 5))
    }

    "get account by criteria with sort" in {
      val accountByCriteriaOne = Account.getEmpty.copy(
        id = 2,
        uuid = "c86eb85f-70f8-4ccd-ae28-68bb08cc1fa5",
        accountNumber = "8912 3287 1209 3422",
        userId = 2,
        userUuid = "4634db00-2b61-4a23-8348-35f0030c3b1d",
        userType = "individual",
        individualUserName = Some("David"),
        individualUserFullName = Some("David Ker Salgado"),
        msisdn = Some("971544451679"),
        accountName = "Ujali Tyagi",
        accountType = "WALLET",
        isMainAccount = Some(false),
        currency = "AED",
        balance = Some(BigDecimal(20.00).setScale(4, RoundingMode.HALF_UP)),
        blockedBalance = Some(BigDecimal(50.00).setScale(4, RoundingMode.HALF_UP)),
        status = Some("ACTIVE"),
        closedAt = None,
        lastTransactionAt = None,
        mainType = "liability",
        createdAt = LocalDateTime.of(2018, 12, 25, 0, 0, 0),
        createdBy = "SuperAdmin",
        updatedAt = None,
        updatedBy = None)

      val accountByCriteriaTwo = Account.getEmpty.copy(
        id = 3,
        uuid = "2c41cedf-b4ad-4daf-8019-35ff82c015cc",
        accountNumber = "8912 3287 1209 3423",
        userId = 2,
        userUuid = "4634db00-2b61-4a23-8348-35f0030c3b1d",
        userType = "individual",
        individualUserName = Some("Ornel Lloyd"),
        individualUserFullName = None,
        msisdn = Some("971544451679"),
        accountName = "Ujali Tyagi",
        accountType = "WALLET",
        isMainAccount = Some(false),
        currency = "AED",
        balance = Some(BigDecimal(30.00).setScale(4, RoundingMode.HALF_UP)),
        blockedBalance = Some(BigDecimal(60.00).setScale(4, RoundingMode.HALF_UP)),
        status = Some("ACTIVE"),
        closedAt = None,
        lastTransactionAt = None,
        mainType = "liability",
        createdAt = LocalDateTime.of(2018, 12, 25, 0, 0, 0),
        createdBy = "SuperAdmin",
        updatedAt = None,
        updatedBy = None)

      val criteriaTwo = AccountCriteria(userId = Some(CriteriaField("", "4634db00-2b61-4a23-8348-35f0030c3b1d")))
      val orderBy = Option(OrderingSet(mutable.LinkedHashSet(Ordering("balance", Ordering.DESC))))
      val resultThree = accountSqlDao.getAccountsByCriteria(Some(criteriaTwo), orderBy, offset = Some(0))

      resultThree.map(_.map(_.id)) mustBe Right(Seq(accountByCriteriaTwo.id, accountByCriteriaOne.id))
    }

    "get account count by criteria" in {
      val criteria = AccountCriteria(isMainAccount = Some(CriteriaField("", true)))
      val result = accountSqlDao.countTotalAccountsByCriteria(criteria)

      result mustBe Right(3)
    }

    "get account count by incomplete account number" in {
      val criteria = AccountCriteria(accountNumber = Some(CriteriaField("account_number", "8912", MatchTypes.Partial)))
      val result = accountSqlDao.countTotalAccountsByCriteria(criteria)

      result mustBe Right(2)
    }

    "update account by uuid" in {

      val accountToUpdate = AccountToUpdate(currency = Some("AED"), status = Some("ACTIVE"),
        balance = Some(BigDecimal("1500.50")), updatedAt = LocalDateTime.of(2019, 8, 7, 0, 0, 0), updatedBy = "Analyn")

      val result: DaoResponse[Option[Account]] = accountSqlDao.updateAccount("1c15bdb9-5d42-4a84-922a-0d01f8e5de71", accountToUpdate)

      //Should have same value as insert accounts in initSql above
      val expected = Account.getEmpty.copy(id = 1, uuid = "1c15bdb9-5d42-4a84-922a-0d01f8e5de71",
        accountNumber = "4716 4157 0907 3361", userId = 1, userUuid = "bcc32571-cf16-4abc-ac38-38d58f9cbab5",
        userType = "individual",
        anyCustomerName = Some("David Ker Salgado"),
        individualUserName = Some("David"),
        individualUserFullName = Some("David Ker Salgado"),
        msisdn = Some("971544465329"),
        accountName = "George Ogalo", accountType = "WALLET", isMainAccount = Some(true),
        currency = accountToUpdate.currency.get, balance = Some(accountToUpdate.balance.get),
        blockedBalance = Some(BigDecimal("0.0")), status = Some(accountToUpdate.status.get),
        closedAt = None, lastTransactionAt = None, mainType = "liability",
        createdAt = LocalDateTime.of(2018, 12, 25, 0, 0, 0), createdBy = "SuperAdmin",
        updatedAt = Some(accountToUpdate.updatedAt), updatedBy = Some(accountToUpdate.updatedBy))

      result mustBe Right(Some(expected))
    }

  }
}

