package tech.pegb.backoffice.dao

import java.time._
import java.util.UUID

import cats.implicits._
import tech.pegb.backoffice.dao.customer.abstraction.BusinessUserDao
import tech.pegb.backoffice.dao.customer.dto.BusinessUserCriteria
import tech.pegb.backoffice.dao.customer.entity.BusinessUser
import tech.pegb.backoffice.dao.model.{Ordering, OrderingSet}
import tech.pegb.backoffice.util.AppConfig
import tech.pegb.core.PegBTestApp

class BusinessUserDaoSpec extends PegBTestApp {

  val config = inject[AppConfig]

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  private val dao = inject[BusinessUserDao]

  val buaId1 = 1
  val buaId2 = 2
  val buaId3 = 3
  val buaUUID1 = UUID.randomUUID()
  val buaUUID2 = UUID.randomUUID()
  val buaUUID3 = UUID.randomUUID()

  val buaIdentityInfoId1 = 1
  val buaIdentityInfoUUID1 = UUID.randomUUID()

  override def initSql =
    s"""
       |INSERT INTO users(id, uuid, username, password, email, activated_at, password_updated_at, subscription, type, status, tier, segment, created_by, created_at, updated_by, updated_at)
       |VALUES
       |('1', '5744db00-2b50-4a34-8348-35f0030c3b1d', 'user01', 'pword', 'george@gmail.com',  '2018-01-01 00:00:00', null, 'sub_one',  'type_one', 'ACTIVE', null, null, 'SuperUser', '2018-01-01 00:00:00', null, null),
       |('2', '4634db00-2b61-4a23-8348-35f0030c3b1d', 'user02', 'pword', 'ujali@gmail.com',   '2018-01-01 00:00:00', null, 'sub_one',  'type_one', 'ACTIVE', null, null, 'SuperUser', '2018-01-01 00:00:00', null, null),
       |('3', '3be6a9e7-52ca-4e2c-a89c-35b480fdfdca', 'user03', 'pword', 'david@gmail.com',   '2018-01-01 00:00:00', null, 'sub_one',  'type_one', 'ACTIVE', null, null, 'SuperUser', '2018-01-01 00:00:00', null, null),
       |('4', '594c8e61-5e78-40ab-b85d-9556533b9c6f', 'user04', 'pword', 'alex@gmail.com',    '2018-01-01 00:00:00', null, 'sub_one',  'type_one', 'ACTIVE', null, null, 'SuperUser', '2018-01-01 00:00:00', null, null);
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
       |('1', '1c15bdb9-5d42-4a84-922a-0d01f8e5de71', '4716 4157 0907 3361', '3', 'George Ogalo', '1', '1', '2', '0.0', '0.0', 'INACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null, 'liability'),
       |('2', 'c86eb85f-70f8-4ccd-ae28-68bb08cc1fa5', '8912 3287 1209 3422', '4', 'Ujali Tyagi', '0', '1', '1', '20.0', '50.0', 'ACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null, 'liability');
       |
       |INSERT INTO business_users(id, uuid, user_id, business_name, brand_name, business_category, business_type,
       | registration_number, tax_number, registration_date, currency_id, collection_account_id, distribution_account_id, default_contact_id,
       | created_by, updated_by, created_at, updated_at) VALUES
       | (1, 'bf43d63e-6348-4138-9387-298f54cf6857',3,'Universal catering co','Costa Coffee DSO','Restaurant-5812','Merchant', '213/564654EE','A2135468977M', '2019-01-01', 1,
       | 1,1,1,'pegbuser','pegbuser','2018-10-16 00:00:00', '2018-10-16 00:00:00'),
       | (2, '1465d0e9-422f-4ba2-b436-0359a635a27a',4,'Subway','Subway DSO','Restaurant-5812','Merchant', '111/564654EE','B2135468977M','1990-01-01', 2,
       | 2,2,2,'pegbuser','pegbuser','2018-10-17 00:00:00', '2018-10-17 00:00:00');
     """.stripMargin

  "BusinessUserDao" should {
    val b1 = BusinessUser(
      id = 1,
      uuid = "bf43d63e-6348-4138-9387-298f54cf6857",
      userId = 3,
      userUUID = "3be6a9e7-52ca-4e2c-a89c-35b480fdfdca",

      businessName = "Universal catering co",
      brandName = "Costa Coffee DSO",
      businessCategory = "Restaurant-5812",
      businessType = "Merchant",

      registrationNumber = "213/564654EE",
      taxNumber = "A2135468977M".some,
      registrationDate = LocalDate.of(2019, 1, 1).some,
      currencyId = 1,
      collectionAccountId = 1.some,
      collectionAccountUUID = "1c15bdb9-5d42-4a84-922a-0d01f8e5de71".some,
      distributionAccountId = 1.some,
      distributionAccountUUID = "1c15bdb9-5d42-4a84-922a-0d01f8e5de71".some,

      createdAt = LocalDateTime.of(2018, 10, 16, 0, 0, 0),
      createdBy = "pegbuser",
      updatedAt = LocalDateTime.of(2018, 10, 16, 0, 0, 0).some,
      updatedBy = "pegbuser".some)

    val b2 = BusinessUser(
      id = 2,
      uuid = "1465d0e9-422f-4ba2-b436-0359a635a27a",
      userId = 4,
      userUUID = "594c8e61-5e78-40ab-b85d-9556533b9c6f",

      businessName = "Subway",
      brandName = "Subway DSO",
      businessCategory = "Restaurant-5812",
      businessType = "Merchant",

      registrationNumber = "111/564654EE",
      taxNumber = "B2135468977M".some,
      registrationDate = LocalDate.of(1990, 1, 1).some,
      currencyId = 2,
      collectionAccountId = 2.some,
      collectionAccountUUID = "c86eb85f-70f8-4ccd-ae28-68bb08cc1fa5".some,
      distributionAccountId = 2.some,
      distributionAccountUUID = "c86eb85f-70f8-4ccd-ae28-68bb08cc1fa5".some,

      createdAt = LocalDateTime.of(2018, 10, 17, 0, 0, 0),
      createdBy = "pegbuser",
      updatedAt = LocalDateTime.of(2018, 10, 17, 0, 0, 0).some,
      updatedBy = "pegbuser".some)

    "return business users matching criteria in getBusinessUsersByCriteria - no filters" in {
      val criteria = BusinessUserCriteria()

      val orderingSet = OrderingSet(Ordering("business_name", Ordering.DESC)).some
      val resp = dao.getBusinessUserByCriteria(criteria, orderingSet, None, None)

      resp mustBe Right(Seq(b1, b2))

    }
    "return business users matching criteria in getBusinessUsersByCriteria - businessName, brandName, registrationNumber" in {
      val criteria = BusinessUserCriteria(
        businessName = model.CriteriaField("", "Subway").some,
        brandName = model.CriteriaField("", "Subway DSO").some,
        registrationNumber = model.CriteriaField("", "111/564654EE").some,
      )

      val orderingSet = OrderingSet(Ordering("business_name", Ordering.DESC)).some
      val resp = dao.getBusinessUserByCriteria(criteria, orderingSet, None, None)

      resp mustBe Right(Seq(b2))

    }
    "return business users matching criteria in getBusinessUsersByCriteria - businessName only" in {
      val criteria = BusinessUserCriteria(
        businessName = model.CriteriaField("", "Subway").some,
      )

      val orderingSet = OrderingSet(Ordering("business_name", Ordering.DESC)).some
      val resp = dao.getBusinessUserByCriteria(criteria, orderingSet, None, None)

      resp mustBe Right(Seq(b2))

    }
    "return business users matching criteria in getBusinessUsersByCriteria - brandName only" in {
      val criteria = BusinessUserCriteria(
        brandName = model.CriteriaField("", "Subway DSO").some)

      val orderingSet = OrderingSet(Ordering("business_name", Ordering.DESC)).some
      val resp = dao.getBusinessUserByCriteria(criteria, orderingSet, None, None)

      resp mustBe Right(Seq(b2))

    }
    "return business users matching criteria in getBusinessUsersByCriteria - registrationNumber only" in {
      val criteria = BusinessUserCriteria(
        registrationNumber = model.CriteriaField("", "111/564654EE").some,
      )

      val orderingSet = OrderingSet(Ordering("business_name", Ordering.DESC)).some
      val resp = dao.getBusinessUserByCriteria(criteria, orderingSet, None, None)

      resp mustBe Right(Seq(b2))

    }
    "return business users matching criteria in getBusinessUsersByCriteria - businessType only" in {
      val criteria = BusinessUserCriteria(
        businessType = model.CriteriaField("", "Merchant").some,
      )

      val orderingSet = OrderingSet(Ordering("business_name", Ordering.DESC)).some
      val resp = dao.getBusinessUserByCriteria(criteria, orderingSet, None, None)

      resp mustBe Right(Seq(b1, b2))

    }
  }
}
