package tech.pegb.backoffice.dao

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import org.scalamock.scalatest.MockFactory
import play.api.db.DBApi
import tech.pegb.backoffice.application.KafkaDBSyncService
import tech.pegb.backoffice.dao.application.sql.WalletApplicationSqlDao
import tech.pegb.backoffice.dao.customer.abstraction.UserDao
import tech.pegb.backoffice.dao.customer.dto.{CustomerAggregation, IndividualUserCriteria, IndividualUserToUpdate}
import tech.pegb.backoffice.dao.customer.entity.IndividualUser
import tech.pegb.backoffice.dao.customer.sql.IndividualUserSqlDao
import tech.pegb.backoffice.dao.model.GroupOperationTypes.IsNotNull
import tech.pegb.backoffice.dao.model.{CriteriaField, GroupingField, MatchTypes, Ordering}
import tech.pegb.backoffice.dao.transaction.dto.TransactionCriteria
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.core.PegBTestApp

class IndividualUserSqlDaoSpec extends PegBTestApp with MockFactory {

  val dbApi: DBApi = inject[DBApi]
  val kafkaDBSyncService = inject[KafkaDBSyncService]

  private val testUuidOne = UUID.randomUUID().toString
  private val testUuidTwo = UUID.randomUUID().toString
  private val testUuidThree = UUID.randomUUID().toString
  private val testUuidFour = UUID.randomUUID().toString

  private val mockedUserDao = stub[UserDao]

  override def initSql =
    s"""
       |INSERT INTO user_types(type_name, description, created_at, created_by, updated_at, updated_by, is_active)
       |VALUES('type_one', null, now(), 'SuperUser', null, null, 1);
       |
       |INSERT INTO subscriptions(subscription_name, description, created_at, created_by, updated_at, updated_by, is_active)
       |VALUES('sub_one', null, now(), 'SuperUser', null, null, 1);
       |
       |INSERT INTO subscriptions(subscription_name, description, created_at, created_by, updated_at, updated_by, is_active)
       |VALUES('sub_two', null, now(), 'SuperUser', null, null, 1);
       |
       |INSERT INTO nationalities(nationality_name, description, created_at, created_by, updated_at, updated_by, is_active)
       |VALUES('Emirati', null, now(), 'SuperUser', null, null, 1);
       |
       |INSERT INTO nationalities(nationality_name, description, created_at, created_by, updated_at, updated_by, is_active)
       |VALUES('Indian', null, now(), 'SuperUser', null, null, 1);
       |
       |INSERT INTO occupations(occupation_name, description, created_at, created_by, updated_at, updated_by, is_active)
       |VALUES('Manager', null, now(), 'SuperUser', null, null, 1);
       |
       |INSERT INTO occupations(occupation_name, description, created_at, created_by, updated_at, updated_by, is_active)
       |VALUES('Programmer', null, now(), 'SuperUser', null, null, 1);
       |
       |INSERT INTO employers(employer_name, description, created_at, created_by, updated_at, updated_by, is_active)
       |VALUES('EMAAR', null, now(), 'SuperUser', null, null, 1);
       |
       |INSERT INTO employers(employer_name, description, created_at, created_by, updated_at, updated_by, is_active)
       |VALUES('PegB Tech', null, now(), 'SuperUser', null, null, 1);
       |
       |INSERT INTO companies(company_name, company_full_name, created_at, created_by, updated_at, updated_by, is_active)
       |VALUES('PegB', 'PegB Technology FZE', now(), 'SuperUser', null, null, 1);
       |
       |INSERT INTO user_status(status_name, description, created_at, created_by, updated_at, updated_by, is_active)
       |VALUES('ACTIVE','when customer passed all requirements for wallet application', now(), 'SuperUser', null, null, 1);
       |
       |INSERT INTO users(id, uuid, username, password, email, activated_at, password_updated_at, subscription, type, status, tier, segment, created_by, created_at, updated_by, updated_at)
       |VALUES(1, '$testUuidOne', 'user01', 'pword', 'alice@gmail.com',  '2018-10-01 00:00:00', null, 'sub_one',  'type_one', 'ACTIVE', null, null, 'SuperUser', '2018-10-01 00:00:00', null, null);
       |
       |INSERT INTO users(id, uuid, username, password, email, activated_at, password_updated_at, subscription, type, status, tier, segment, created_by, created_at, updated_by, updated_at)
       |VALUES(2, '$testUuidTwo', 'user02', 'pword02', 'ujali@gmail.com',  '2018-10-16 00:00:00', null, 'sub_one',  'type_one', 'ACTIVE', null, null, 'SuperUser', '2018-10-16 00:00:00', null, null);
       |
       |INSERT INTO users(id, uuid, username, password, email, activated_at, password_updated_at, subscription, type, status, tier, segment, created_by, created_at, updated_by, updated_at)
       |VALUES(3, '$testUuidThree', 'user03', 'pword03', 'david@gmail.com',  '2018-10-15 00:00:00', null, 'sub_one',  'type_one', 'ACTIVE', null, null, 'SuperUser', '2018-10-16 00:00:00', null, null);
       |
       |INSERT INTO users(id, uuid, username, password, email, activated_at, password_updated_at, subscription, type, status, tier, segment, created_by, created_at, updated_by, updated_at)
       |VALUES(4, '$testUuidFour', 'user04', 'pword04', 'dima@gmail.com',  '2018-10-14 00:00:00', null, 'sub_one',  'type_one', 'ACTIVE', null, null, 'SuperUser', '2018-10-16 00:00:00', null, null);
       |
       |INSERT INTO individual_users(msisdn, user_id, type, name, fullname,      gender,    person_id, document_number, document_type,   document_model,         company, birthdate, birth_place, nationality, occupation, employer, created_at, created_by, updated_at, updated_by) VALUES
       |('971544465329', '1', 'type_one', 'Alice',                'Alice Smith', 'F',       'pid1234', 'docnum1234',    'identity-card', 'Kenya Identity Card',  'PegB', '1990-01-01', 'Dubai', 'Emirati', 'Manager', 'EMAAR', '2018-10-01 00:00:00', 'SuperUser', null, null),
       |('97177842881232', '2', 'type_two', 'Ujali',              'Ujali Tyagi', 'F',       'pid4567', 'docnum4567',    'identity-card', 'Kenya Identity Card',  'PegB', '1989-03-10', 'Delhi', 'Indian', 'Programmer', 'PegB Tech', '2018-10-01 00:00:00', 'SuperUser', null, null),
       |('58218147523112', '3', 'type_two', 'David',              'David Edano', 'M',       'pid4567', 'docnum4567',    'identity-card', 'Kenya Identity Card',  'PegB', '1989-03-10', 'Dubai', 'Emirati', 'Manager', 'EMAAR', '2018-10-02 00:00:00', 'SuperUser', null, null),
       |('96506821078829', '4', 'type_two', 'Ujali',             'Raghu Sharma', 'F',       'pid1234', 'docnum1234',    'identity-card', 'Kenya Identity Card',  'PegB', '1989-03-10', 'Delhi', 'Indian', 'Programmer', 'PegB Tech', '2019-01-01 00:00:00', 'SuperUser', null, null);
       |
    """.stripMargin

  override def cleanupSql =
    """
      |DELETE FROM accounts;
      |DELETE FROM account_types;
      |DELETE FROM business_users_has_extra_attributes;
      |DELETE FROM individual_users;
      |DELETE FROM business_users;
      |DELETE FROM users;
      |DELETE FROM user_status;
      |DELETE FROM user_types;
      |DELETE FROM companies;
      |DELETE FROM subscriptions;
      |DELETE FROM nationalities;
      |DELETE FROM occupations;
      |DELETE FROM employers;
      |DELETE FROM user_status_has_requirements;
      |DELETE FROM extra_attribute_types;
    """.stripMargin

  "IndividualUserSqlDao" should {

    "get individual user and user join by uuid" in {
      val individualUserSqlDao = new IndividualUserSqlDao(dbApi, mockedUserDao, kafkaDBSyncService)
      val result = individualUserSqlDao.getIndividualUser(testUuidOne)

      val expected = IndividualUser(
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
        personId = Some("pid1234"),
        documentNumber = Some("docnum1234"),
        documentType = Some("identity-card"),
        documentModel = Some("Kenya Identity Card"),
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

      result.right.get mustBe Some(expected)
    }

    "get individual user by full_name partial match" in {
      val individualUserSqlDao = new IndividualUserSqlDao(dbApi, mockedUserDao, kafkaDBSyncService)
      val criteria = IndividualUserCriteria(
        fullName = Some(CriteriaField("fullName", "Ujali", MatchTypes.Partial)))

      val limit = Some(2)
      val offset = Some(0)
      val result = individualUserSqlDao.getIndividualUsersByCriteria(criteria, orderBy = Nil, limit = limit, offset = offset)
      val result2 = individualUserSqlDao.getIndividualUsersByCriteria(criteria, orderBy = Seq(Ordering("msisdn", Ordering.ASC)), limit = limit, offset = offset)

      val individualUserOne = IndividualUser(
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
        personId = Some("pid4567"),
        documentNumber = Some("docnum4567"),
        documentType = Some("identity-card"),
        documentModel = Some("Kenya Identity Card"),
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

      val individualUserTwo = IndividualUser(
        id = 4,
        uuid = s"$testUuidFour",
        username = Some("user04"),
        password = Some("pword04"),
        tier = None,
        segment = None,
        subscription = Some("sub_one"),
        email = Some("dima@gmail.com"),
        status = "ACTIVE",
        msisdn = "96506821078829",
        `type` = Some("type_two"),
        name = Some("Ujali"),
        fullName = Some("Raghu Sharma"),
        gender = Some("F"),
        personId = Some("pid1234"),
        documentNumber = Some("docnum1234"),
        documentType = Some("identity-card"),
        documentModel = Some("Kenya Identity Card"),
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

      val expectedResult = Seq(individualUserOne, individualUserTwo)
      val expectedResult2 = Seq(individualUserTwo, individualUserOne)

      result.right.get mustBe expectedResult
      result2.right.get mustBe expectedResult2
    }

    "update individual user by uuid" in {
      val individualUserSqlDao = new IndividualUserSqlDao(dbApi, mockedUserDao, kafkaDBSyncService)
      val individualUserToUpdate = IndividualUserToUpdate(
        `type` = Some("type"),
        msisdn = Some("971544465330"),
        name = Some("Ujali"),
        fullName = Some("Ujali Tyagi"),
        gender = Some("F"),
        personId = None,
        documentNumber = None,
        documentType = None,
        company = Some("PegB"),
        birthDate = Some(LocalDate.of(1989, 3, 10)),
        birthPlace = Some("INDIA"),
        nationality = Some("Emirati"),
        occupation = Some("Manager"),
        employer = Some("EMAAR"),
        updatedAt = Some(LocalDateTime.of(2019, 1, 1, 0, 0)),
        updatedBy = Some("Ujali"))
      (mockedUserDao.getInternalUserId _).when(testUuidOne).returns(Right(Option(1)))
      val result = individualUserSqlDao.updateIndividualUser(testUuidOne, individualUserToUpdate)

      val expected = IndividualUser(
        id = 1,
        uuid = s"$testUuidOne",
        username = Some("user01"),
        password = Some("pword"),
        tier = None,
        segment = None,
        subscription = Some("sub_one"),
        email = Some("alice@gmail.com"),
        status = "ACTIVE",
        msisdn = "971544465330",
        `type` = Some("type"),
        name = Some("Ujali"),
        fullName = Some("Ujali Tyagi"),
        gender = Some("F"),
        personId = Some("pid1234"),
        documentNumber = Some("docnum1234"),
        documentType = Some("identity-card"),
        documentModel = Some("Kenya Identity Card"),
        company = Some("PegB"),
        birthDate = Some(LocalDate.of(1989, 3, 10)),
        birthPlace = Some("INDIA"),
        nationality = Some("Emirati"),
        occupation = Some("Manager"),
        employer = Some("EMAAR"),
        createdAt = LocalDateTime.of(2018, 10, 1, 0, 0, 0, 0),
        createdBy = "SuperUser",
        updatedAt = Some(LocalDateTime.of(2019, 1, 1, 0, 0)),
        updatedBy = Some("Ujali"),
        activatedAt = Some(LocalDateTime.of(2018, 10, 1, 0, 0)))

      result.right.get mustBe Some(expected)
    }

    "get individual user based on search criteria" in {
      val individualUserSqlDao = new IndividualUserSqlDao(dbApi, mockedUserDao, kafkaDBSyncService)
      val criteria = IndividualUserCriteria(
        subscription = Some("sub_one"),
        status = Some("ACTIVE"),
        individualUserType = Some("type_two"),
        name = Some("Ujali"),
        birthDate = Some(LocalDate.of(1989, 3, 10)),
        birthPlace = Some("Delhi"),
        nationality = Some("Indian"),
        occupation = Some("Programmer"),
        company = Some("PegB"),
        employer = Some("PegB Tech"),
        createdBy = Some("SuperUser"))

      val limit = Some(2)
      val offset = Some(0)
      val result = individualUserSqlDao.getIndividualUsersByCriteria(criteria, orderBy = Nil, limit = limit, offset = offset)
      val result2 = individualUserSqlDao.getIndividualUsersByCriteria(criteria, orderBy = Seq(Ordering("msisdn", Ordering.ASC)), limit = limit, offset = offset)

      val individualUserOne = IndividualUser(
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
        personId = Some("pid4567"),
        documentNumber = Some("docnum4567"),
        documentType = Some("identity-card"),
        documentModel = Some("Kenya Identity Card"),
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

      val individualUserTwo = IndividualUser(
        id = 4,
        uuid = s"$testUuidFour",
        username = Some("user04"),
        password = Some("pword04"),
        tier = None,
        segment = None,
        subscription = Some("sub_one"),
        email = Some("dima@gmail.com"),
        status = "ACTIVE",
        msisdn = "96506821078829",
        `type` = Some("type_two"),
        name = Some("Ujali"),
        fullName = Some("Raghu Sharma"),
        gender = Some("F"),
        personId = Some("pid1234"),
        documentNumber = Some("docnum1234"),
        documentType = Some("identity-card"),
        documentModel = Some("Kenya Identity Card"),
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

      val expectedResult = Seq(individualUserOne, individualUserTwo)
      val expectedResult2 = Seq(individualUserTwo, individualUserOne)

      result.right.get mustBe expectedResult
      result2.right.get mustBe expectedResult2
    }

    "get individual user count based on search criteria" in {
      val individualUserSqlDao = new IndividualUserSqlDao(dbApi, mockedUserDao, kafkaDBSyncService)
      val criteria = IndividualUserCriteria(
        subscription = Some("sub_one"),
        status = Some("ACTIVE"),
        individualUserType = Some("type_two"),
        name = Some("Ujali"),
        birthDate = Some(LocalDate.of(1989, 3, 10)),
        birthPlace = Some("Delhi"),
        nationality = Some("Indian"),
        occupation = Some("Programmer"),
        company = Some("PegB"),
        employer = Some("PegB Tech"),
        createdBy = Some("SuperUser"))

      val result = individualUserSqlDao.countIndividualUserByCriteria(criteria)

      result.right.get mustBe 2
    }

    "update status by msisdn" in {
      val individualUserSqlDao = new IndividualUserSqlDao(dbApi, mockedUserDao, kafkaDBSyncService)

      val expectedResult = IndividualUser(
        id = 4,
        uuid = s"$testUuidFour",
        username = Some("user04"),
        password = Some("pword04"),
        tier = None,
        segment = None,
        subscription = Some("sub_one"),
        email = Some("dima@gmail.com"),
        status = "INACTIVE",
        msisdn = "96506821078829",
        `type` = Some("type_two"),
        name = Some("Ujali"),
        fullName = Some("Raghu Sharma"),
        gender = Some("F"),
        personId = Some("pid1234"),
        documentNumber = Some("docnum1234"),
        documentType = Some("identity-card"),
        documentModel = Some("Kenya Identity Card"),
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

      val result = individualUserSqlDao.updateStatusByMsisdn("96506821078829", "INACTIVE")

      result mustBe Right(Some(expectedResult))
    }

    "aggregates users by activation date different than null" in {
      val individualUserSqlDao = new IndividualUserSqlDao(dbApi, mockedUserDao, kafkaDBSyncService)

      val expectedResult = CustomerAggregation(
        count = Option(4L), sum = None, isActivated = Option(true), isActive = None)

      val trxCriteria = TransactionCriteria(direction = Some(CriteriaField("", "credit")))

      val userCriteria = IndividualUserCriteria(
        createdDateFrom = Some(LocalDate.parse("2018-01-01")),
        createdDateTo = Some(LocalDate.now().plusDays(1)),
      )

      val result = individualUserSqlDao.aggregateCustomersByCriteriaAndPivots(
        criteria = userCriteria,
        trxCriteria = trxCriteria,
        grouping = Seq[GroupingField](
          GroupingField("activated_at", Option(IsNotNull), projectionAlias = Option("is_activated")),
          GroupingField(
            "status",
            tableAlias = Option(WalletApplicationSqlDao.TableAlias),
            projectionAlias = Option("applicationStatus"))))

      result mustBe Right(List(expectedResult))
    }
  }

}
