package tech.pegb.backoffice.dao

import java.time._
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import tech.pegb.backoffice.dao.customer.abstraction.UserDao
import tech.pegb.backoffice.dao.customer.dto.GenericUserCriteria
import tech.pegb.backoffice.dao.customer.entity.{BusinessUser, GenericUser, IndividualUser, User}
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes, Ordering, OrderingSet}
import tech.pegb.core.PegBTestApp

class UserSqlDaoSpec extends PegBTestApp with MockFactory {

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())

  private val testUuidOne = UUID.randomUUID().toString
  private val testUuidTwo = UUID.randomUUID().toString
  private val testUuidThree = UUID.randomUUID().toString
  private val testUuidFour = UUID.randomUUID().toString
  private val testUuid5 = UUID.randomUUID().toString

  val userSqlDao = inject[UserDao]

  override def initSql =
    s"""
      |INSERT INTO users(id, uuid, username, password, email, activated_at, password_updated_at, subscription, type, status, tier, segment, created_by, created_at, updated_by, updated_at)
      |VALUES(1, '$testUuidOne', 'user01', 'pword', 'alice@gmail.com',  '2018-10-01 00:00:00', null, 'sub_one',  'type_one', 'ACTIVE', null, null, 'SuperUser', '2018-10-01 00:00:00', null, null);
      |
      |INSERT INTO users(id, uuid, username, password, email, activated_at, password_updated_at, subscription, type, status, tier, segment, created_by, created_at, updated_by, updated_at)
      |VALUES(2, '$testUuidTwo', 'user02', 'pword02', 'ujali@gmail.com',  '2018-10-16 00:00:00', null, 'sub_one',  'type_one', 'ACTIVE', null, null, 'SuperUser', '2018-10-16 00:00:00', null, null);
      |
      |INSERT INTO users(id, uuid, username, password, email, activated_at, password_updated_at, subscription, type, status, tier, segment, created_by, created_at, updated_by, updated_at)
      |VALUES(3, '$testUuidThree', 'david_merchant', 'pword03', 'david@gmail.com',  '2018-10-15 00:00:00', null, 'sub_one',  'type_one', 'ACTIVE', null, null, 'SuperUser', '2018-10-16 00:00:00', null, null);
      |
      |INSERT INTO users(id, uuid, username, password, email, activated_at, password_updated_at, subscription, type, status, tier, segment, created_by, created_at, updated_by, updated_at)
      |VALUES(4, '$testUuidFour', 'user04', 'pword04', 'loyd@gmail.com',  '2018-10-14 00:00:00', null, 'sub_one',  'type_one', 'ACTIVE', null, null, 'SuperUser', '2018-10-16 00:00:00', null, null);
      |
      |INSERT INTO users(id, uuid, username, password, email, activated_at, password_updated_at, subscription, type, status, tier, segment, created_by, created_at, updated_by, updated_at)
      |VALUES(5, '$testUuid5', 'cybersource', 'pword05', 'cybersource@gmail.com',  '2018-10-14 00:00:00', null, 'sub_one',  'business_user', 'ACTIVE', null, null, 'SuperUser', '2018-10-16 00:00:00', null, null);
      |
      |INSERT INTO account_types(id, account_type_name, description, created_at, updated_at, is_active)
      |VALUES('1', 'WALLET', 'standard account type for individual users', now(), null, 1);
      |
      |INSERT INTO currencies(id, currency_name, description, created_at, updated_at, is_active)
      |VALUES('1', 'KES', 'Kenya shilling', now(), null, 1);
      |
      |INSERT INTO currencies(id, currency_name, description, created_at, updated_at, is_active)
      |VALUES('2', 'USD', 'US Dollar', now(), null, 1);
      |
      |INSERT INTO currencies(id, currency_name, description, created_at, updated_at, is_active)
      |VALUES('3', 'EUR', 'EURO', now(), null, 1);
      |
      |INSERT INTO currencies(id, currency_name, description, created_at, updated_at, is_active)
      |VALUES('4', 'CNY', 'default currency for China', now(), null, 1);
      |
      |INSERT INTO currencies(id, currency_name, description, created_at, updated_at, is_active)
      |VALUES('5', 'CHF', 'default currency for Switzerland', now(), null, 1);
      |
      |INSERT INTO accounts(id, uuid, number, user_id, name, is_main_account, account_type_id, currency_id, balance, blocked_balance, status, closed_at, last_transaction_at, created_at, created_by, updated_at, updated_by, main_type)
      |VALUES
      |('1', '1c15bdb9-5d42-4a84-922a-0d01f8e5de71', '4716 4157 0907 3361', '1', 'George Ogalo', '1', '1', '2', '0.0', '0.0', 'INACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null, 'liability'),
      |('2', 'c86eb85f-70f8-4ccd-ae28-68bb08cc1fa5', '8912 3287 1209 3422', '2', 'Ujali Tyagi', '0', '1', '1', '20.0', '50.0', 'ACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null, 'liability');
      |
      |INSERT INTO business_users(id, uuid, user_id, business_name, brand_name, business_category, business_type,
      | registration_number, tax_number, registration_date, currency_id, collection_account_id, distribution_account_id, default_contact_id,
      | created_by, updated_by, created_at, updated_at) VALUES
      | (1, 'bf43d63e-6348-4138-9387-298f54cf6857',3,'Universal catering co','Costa Coffee DSO','Restaurant-5812','Merchant', '213/564654EE','A2135468977M',now(), 1,
      | 1,1,1,'Ujali','Ujali','2018-10-16 00:00:00', '2018-10-16 00:00:00');
      |
      |INSERT INTO individual_users(msisdn, user_id, type, name, fullname, gender, company, birthdate, birth_place, nationality, occupation, employer, created_at, created_by, updated_at, updated_by) VALUES
      |('971544465329', '1', 'type_one', 'Alice', 'Alice Smith', 'F', 'PegB', '1990-01-01', 'Dubai', 'Emirati', 'Manager', 'EMAAR', '2018-10-01 00:00:00', 'SuperUser', null, null),
      |('97177842881232', '2', 'type_two', 'Ujali', 'Ujali Tyagi', 'F', 'PegB', '1989-03-10', 'Delhi', 'Indian', 'Programmer', 'PegB Tech', '2018-10-01 00:00:00', 'SuperUser', null, null),
      |('96506821078829', '4', 'type_two', 'Loyd', 'Loyd cybersource', 'M', 'PegB', '1989-03-10', 'Delhi', 'Indian', 'Programmer', 'PegB Tech', '2019-01-01 00:00:00', 'SuperUser', null, null);
      |
    """.stripMargin

  "UserSqlDao" should {
    val iu1 = IndividualUser(
      id = 1,
      uuid = s"$testUuidOne",
      username = Some("user01"),
      password = Some("pword"),
      tier = None,
      segment = None,
      subscription = Some("sub_one"),
      email = Some("alice@gmail.com"),
      status = "ACTIVE",
      msisdn = "971544465329",
      `type` = Some("type_one"),
      name = Some("Alice"),
      fullName = Some("Alice Smith"),
      gender = Some("F"),
      personId = None,
      documentNumber = None,
      documentType = None,
      company = Some("PegB"),
      birthDate = Some(LocalDate.of(1990, 1, 1)),
      birthPlace = Some("Dubai"),
      nationality = Some("Emirati"),
      occupation = Some("Manager"),
      employer = Some("EMAAR"),
      createdAt = LocalDateTime.of(2018, 10, 1, 0, 0, 0, 0),
      createdBy = "SuperUser",
      updatedAt = None,
      updatedBy = None,
      activatedAt = Some(LocalDateTime.of(2018, 10, 1, 0, 0)))

    val iu2 = IndividualUser(
      id = 2,
      uuid = s"$testUuidTwo",
      username = Some("user02"),
      password = Some("pword02"),
      tier = None,
      segment = None,
      subscription = Some("sub_one"),
      email = Some("ujali@gmail.com"),
      status = "ACTIVE",
      msisdn = "97177842881232",
      `type` = Some("type_two"),
      name = Some("Ujali"),
      fullName = Some("Ujali Tyagi"),
      gender = Some("F"),
      personId = None,
      documentNumber = None,
      documentType = None,
      company = Some("PegB"),
      birthDate = Some(LocalDate.of(1989, 3, 10)),
      birthPlace = Some("Delhi"),
      nationality = Some("Indian"),
      occupation = Some("Programmer"),
      employer = Some("PegB Tech"),
      createdAt = LocalDateTime.of(2018, 10, 1, 0, 0, 0, 0),
      createdBy = "SuperUser",
      updatedAt = None,
      updatedBy = None,
      activatedAt = Some(LocalDateTime.of(2018, 10, 16, 0, 0)))

    val iu4 = IndividualUser(
      id = 4,
      uuid = s"$testUuidFour",
      username = Some("user04"),
      password = Some("pword04"),
      tier = None,
      segment = None,
      subscription = Some("sub_one"),
      email = Some("loyd@gmail.com"),
      status = "ACTIVE",
      msisdn = "96506821078829",
      `type` = Some("type_two"),
      name = Some("Loyd"),
      fullName = Some("Loyd cybersource"),
      gender = Some("M"),
      personId = None,
      documentNumber = None,
      documentType = None,
      company = Some("PegB"),
      birthDate = Some(LocalDate.of(1989, 3, 10)),
      birthPlace = Some("Delhi"),
      nationality = Some("Indian"),
      occupation = Some("Programmer"),
      employer = Some("PegB Tech"),
      createdAt = LocalDateTime.of(2019, 1, 1, 0, 0, 0, 0),
      createdBy = "SuperUser",
      updatedAt = None,
      updatedBy = None,
      activatedAt = Some(LocalDateTime.of(2018, 10, 14, 0, 0)))

    val genericUser1 = GenericUser(
      id = 1,
      uuid = testUuidOne,
      userName = "user01",
      password = Some("pword"),
      customerType = Some("type_one"),
      tier = None,
      segment = None,
      subscription = Some("sub_one"),
      email = Some("alice@gmail.com"),
      status = Some("ACTIVE"),
      activatedAt = Option(LocalDateTime.of(2018, 10, 1, 0, 0, 0)),
      passwordUpdatedAt = None,
      createdAt = LocalDateTime.of(2018, 10, 1, 0, 0, 0),
      createdBy = "SuperUser",
      updatedAt = None,
      updatedBy = None,

      customerName = "Alice Smith".some,
      businessUserFields = none,
      individualUserFields = iu1.some)

    val bu1 = BusinessUser(
      id = 1,
      uuid = "bf43d63e-6348-4138-9387-298f54cf6857",
      userId = 3,
      userUUID = testUuidThree,
      businessName = "Universal catering co",
      brandName = "Costa Coffee DSO",
      businessCategory = "Restaurant-5812",
      businessType = "Merchant",

      registrationNumber = "213/564654EE",
      taxNumber = "A2135468977M".some,
      registrationDate = LocalDate.now().some,
      currencyId = 1,
      collectionAccountId = 1.some,
      collectionAccountUUID = "1c15bdb9-5d42-4a84-922a-0d01f8e5de71".some,
      distributionAccountId = 1.some,
      distributionAccountUUID = "1c15bdb9-5d42-4a84-922a-0d01f8e5de71".some,

      createdBy = "Ujali",
      createdAt = LocalDateTime.of(2018, 10, 16, 0, 0),
      updatedAt = LocalDateTime.of(2018, 10, 16, 0, 0).some,
      updatedBy = "Ujali".some)

    val genericUser2 = GenericUser(
      id = 2,
      uuid = testUuidTwo,
      userName = "user02",
      password = Some("pword02"),
      customerType = Some("type_one"),
      tier = None,
      segment = None,
      subscription = Some("sub_one"),
      email = Some("ujali@gmail.com"),
      status = Some("ACTIVE"),
      activatedAt = Option(LocalDateTime.of(2018, 10, 16, 0, 0, 0)),
      passwordUpdatedAt = None,
      createdAt = LocalDateTime.of(2018, 10, 16, 0, 0, 0),
      createdBy = "SuperUser",
      updatedAt = None,
      updatedBy = None,

      customerName = "Ujali Tyagi".some,
      businessUserFields = none,
      individualUserFields = iu2.some)

    val genericUser3 = GenericUser(
      id = 3,
      uuid = testUuidThree,
      userName = "david_merchant",
      password = Some("pword03"),
      customerType = Some("type_one"),
      tier = None,
      segment = None,
      subscription = Some("sub_one"),
      email = Some("david@gmail.com"),
      status = Some("ACTIVE"),
      activatedAt = Option(LocalDateTime.of(2018, 10, 15, 0, 0, 0)),
      passwordUpdatedAt = None,
      createdAt = LocalDateTime.of(2018, 10, 16, 0, 0, 0),
      createdBy = "SuperUser",
      updatedAt = None,
      updatedBy = None,

      customerName = "Universal catering co".some,
      businessUserFields = bu1.some,
      individualUserFields = none)

    val genericUser4 = GenericUser(
      id = 4,
      uuid = testUuidFour,
      userName = "user04",
      password = Some("pword04"),
      customerType = Some("type_one"),
      tier = None,
      segment = None,
      subscription = Some("sub_one"),
      email = Some("loyd@gmail.com"),
      status = Some("ACTIVE"),
      activatedAt = Option(LocalDateTime.of(2018, 10, 14, 0, 0, 0)),
      passwordUpdatedAt = None,
      createdAt = LocalDateTime.of(2018, 10, 16, 0, 0, 0),
      createdBy = "SuperUser",
      updatedAt = None,
      updatedBy = None,

      customerName = "Loyd cybersource".some,
      businessUserFields = none,
      individualUserFields = iu4.some)

    val genericUser5 = GenericUser(
      id = 5,
      uuid = testUuid5,
      userName = "cybersource",
      password = Some("pword05"),
      customerType = Some("business_user"),
      tier = None,
      segment = None,
      subscription = Some("sub_one"),
      email = Some("cybersource@gmail.com"),
      status = Some("ACTIVE"),
      activatedAt = Option(LocalDateTime.of(2018, 10, 14, 0, 0, 0)),
      passwordUpdatedAt = None,
      createdAt = LocalDateTime.of(2018, 10, 16, 0, 0, 0),
      createdBy = "SuperUser",
      updatedAt = None,
      updatedBy = None,

      customerName = "cybersource".some,
      businessUserFields = none,
      individualUserFields = none)

    "get user by primary id" in {

      val result = userSqlDao.getUserByUserId(1)

      val expectedUser = User(
        id = 1,
        uuid = testUuidOne,
        userName = "user01",
        password = Some("pword"),
        `type` = Some("type_one"),
        tier = None,
        segment = None,
        subscription = Some("sub_one"),
        email = Some("alice@gmail.com"),
        status = Some("ACTIVE"),
        activatedAt = Option(LocalDateTime.of(2018, 10, 1, 0, 0, 0)),
        passwordUpdatedAt = None,
        createdAt = LocalDateTime.of(2018, 10, 1, 0, 0, 0),
        createdBy = "SuperUser",
        updatedAt = None,
        updatedBy = None)

      result.isRight mustBe true
      result.right.get mustBe Some(expectedUser)
    }

    "get generic user by criteria - no filter" in {
      val criteria = GenericUserCriteria()
      val result = userSqlDao.getUserByCriteria(criteria, None, None, None)

      result mustBe Right(Seq(genericUser1, genericUser2, genericUser3, genericUser4, genericUser5))
    }
    "get generic user by criteria - anyName filter" in {
      val criteria = GenericUserCriteria(anyName = CriteriaField("", "cyberso", MatchTypes.Partial).some)
      val orderingSet = OrderingSet(Ordering("type", Ordering.ASC)).some
      val result = userSqlDao.getUserByCriteria(criteria, orderingSet, None, None)

      result mustBe Right(Seq(genericUser5, genericUser4))
    }
    "get generic user by criteria - name filter" in {
      val criteria = GenericUserCriteria(name = CriteriaField("", "Universal catering co", MatchTypes.Partial).some)
      val orderingSet = OrderingSet(Ordering("type", Ordering.ASC)).some
      val result = userSqlDao.getUserByCriteria(criteria, orderingSet, None, None)

      result mustBe Right(Seq(genericUser3))
    }
    "get generic user by criteria - created_at sorter" in {
      val criteria = GenericUserCriteria(name = CriteriaField("", "Universal catering co", MatchTypes.Partial).some)
      val orderingSet = OrderingSet(Ordering("created_at", Ordering.ASC)).some
      val result = userSqlDao.getUserByCriteria(criteria, orderingSet, None, None)

      result mustBe Right(Seq(genericUser3))
    }
    "get count user by criteria - no filter" in {
      val criteria = GenericUserCriteria()
      val result = userSqlDao.countUserByCriteria(criteria)

      result mustBe Right(5)
    }
    "get count user by criteria - anyName filter" in {
      val criteria = GenericUserCriteria(anyName = CriteriaField("", "cyberso", MatchTypes.Partial).some)
      val result = userSqlDao.countUserByCriteria(criteria)

      result mustBe Right(2)
    }
    "get count user by criteria - name filter" in {
      val criteria = GenericUserCriteria(name = CriteriaField("", "Universal catering co", MatchTypes.Partial).some)
      val result = userSqlDao.countUserByCriteria(criteria)

      result mustBe Right(1)
    }
  }
}
