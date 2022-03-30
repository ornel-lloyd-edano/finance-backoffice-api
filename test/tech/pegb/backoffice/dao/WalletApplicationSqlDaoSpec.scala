package test.tech.pegb.backoffice.dao

import java.time._
import java.util.UUID

import org.scalamock.scalatest.MockFactory
import play.api.db.DBApi
import tech.pegb.backoffice.application.KafkaDBSyncService
import tech.pegb.backoffice.dao.model.{Ordering ⇒ Order}
import tech.pegb.backoffice.dao.application.dto.{WalletApplicationCriteria, WalletApplicationToCreate, WalletApplicationToUpdate}
import tech.pegb.backoffice.dao.application.entity.WalletApplication
import tech.pegb.backoffice.dao.application.sql.WalletApplicationSqlDao
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.core.PegBTestApp

import scala.collection.mutable

class WalletApplicationSqlDaoSpec extends PegBTestApp with MockFactory {

  val dbApi: DBApi = inject[DBApi]
  val kafkaDBSyncService: KafkaDBSyncService = inject[KafkaDBSyncService]
  val walletApplicationDao = new WalletApplicationSqlDao(dbApi, kafkaDBSyncService)

  private val userIdOne = UUID.randomUUID()
  private val userIdTwo = UUID.randomUUID()
  private val userIdThree = UUID.randomUUID()
  private val testUuidOne = UUID.randomUUID()
  private val testUuidTwo = UUID.randomUUID()
  private val testUuidThree = UUID.randomUUID()

  val mockClockStart = Clock.fixed(Instant.ofEpochMilli(1547125283000L), ZoneId.systemDefault())
  val mockClockEnd = Clock.fixed(Instant.ofEpochMilli(1548939683000L), ZoneId.systemDefault())

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
       |VALUES(1, '${userIdOne.toString}', 'user01', 'pword', 'alice@gmail.com',  '2018-10-01 00:00:00', null, 'sub_one',  'type_one', 'ACTIVE', null, null, 'SuperUser', '2018-10-01 00:00:00', null, null);
       |
       |INSERT INTO users(id, uuid, username, password, email, activated_at, password_updated_at, subscription, type, status, tier, segment, created_by, created_at, updated_by, updated_at)
       |VALUES(2, '${userIdTwo.toString}', 'user02', 'pword', 'alice@gmail.com',  '2018-10-01 00:00:00', null, 'sub_one',  'type_one', 'PASSIVE', null, null, 'SuperUser', '2018-10-01 00:00:00', null, null);
       |
       |INSERT INTO users(id, uuid, username, password, email, activated_at, password_updated_at, subscription, type, status, tier, segment, created_by, created_at, updated_by, updated_at)
       |VALUES(3, '${userIdThree.toString}', 'user03', 'pword', 'alice@gmail.com',  '2018-10-01 00:00:00', null, 'sub_one',  'type_one', 'ACTIVE', null, null, 'SuperUser', '2018-10-01 00:00:00', null, null);
       |
       |INSERT INTO individual_users(msisdn, user_id, type, name, fullname, person_id, gender, company, birthdate, birth_place, nationality, occupation, employer, created_at, created_by, updated_at, updated_by) VALUES
       |('96506821078829', '1', 'type_two', 'Ujali', 'Ujali Sharma','Asteroid_M', 'F', 'PegB', '1989-03-10', 'Delhi', 'Indian', 'Programmer', 'PegB Tech', '2019-01-01 00:00:00', 'SuperUser', null, null),
       |('98717192132324', '2', 'type_two', 'David', ' David Salgado','Asteroid_M', 'M', 'PegB', '1989-03-10', 'Delhi', 'Indian', 'Programmer', 'EMAAR', '2019-01-01 00:00:00', 'SuperUser', null, null),
       |('97177842889921', '3', 'type_two', 'Dima', 'Dima Linou','Asteroid_M', 'M', 'PegB', '1989-03-10', 'Delhi', 'Indian', 'Programmer', 'EMAAR', '2019-01-01 00:00:00', 'SuperUser', null, null);
       |
       |
       |INSERT INTO user_applications(id, uuid, user_id, status, stage, rejection_reason, created_by, created_at, updated_by, updated_at, person_id_original, person_id_updated)
       |VALUES (1, '${testUuidOne.toString}', 1, 'pending', 'new', null, 'pegbuser', '${LocalDateTime.now(mockClockStart)}', 'pegbuser', '${LocalDateTime.now(mockClockStart)}', 'asteroid_m', 'asteroid_m');
       |
       |INSERT INTO user_applications(id, uuid, user_id, status, stage, rejection_reason, created_by, created_at, updated_by, updated_at, person_id_original, person_id_updated)
       |VALUES (2, '${testUuidTwo.toString}', 2, 'approved', 'ocr', null, 'ujali', '${LocalDateTime.now(mockClockStart)}', 'ujali', '${LocalDateTime.now(mockClockStart)}', 'asteroid_m', 'asteroid_m');
       |
       |INSERT INTO user_applications(id, uuid, user_id, status, stage, rejection_reason, created_by, created_at, updated_by, updated_at,person_id_original, person_id_updated)
       |VALUES (3, '${testUuidThree.toString}', 3, 'rejected', 'new', 'test', 'david', '${LocalDateTime.now(mockClockEnd)}', 'david', '${LocalDateTime.now(mockClockEnd)}', 'asteroid_m', 'asteroid_m');
       |
      |""".stripMargin

  override def cleanupSql =
    """
      |DELETE FROM user_applications;
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

  "WalletApplicationSqlDao getWalletApplicationById" should {

    "return Right[Option[WalletApplication]] if wallet application was found" in {
      val result = walletApplicationDao.getWalletApplicationByUUID(testUuidOne)

      val walletApplicationOne = WalletApplication.getEmpty.copy(
        id = 1,
        uuid = testUuidOne,
        userUuid = userIdOne,

        msisdn = Some("96506821078829"),
        status = "pending",
        applicationStage = "new",
        checkedAt = None,
        checkedBy = None,
        rejectionReason = None,
        createdAt = LocalDateTime.now(mockClockStart),
        createdBy = "pegbuser",
        personIdOriginal = Some("asteroid_m"),
        personIdUpdated = Some("asteroid_m"),
        updatedAt = Some(LocalDateTime.now(mockClockStart)),
        updatedBy = Some("pegbuser"))

      val expectedResult = walletApplicationOne

      result.isRight mustBe true
      result.right.get.get mustBe expectedResult

    }

    "return Right[Option[WalletApplication]] if wallet application is found by internal id" in {
      val result = walletApplicationDao.getWalletApplicationByInternalId(1)

      val walletApplicationOne = WalletApplication.getEmpty.copy(
        id = 1,
        uuid = testUuidOne,
        userUuid = userIdOne,

        msisdn = Some("96506821078829"),
        status = "pending",
        applicationStage = "new",
        checkedAt = None,
        checkedBy = None,
        rejectionReason = None,
        createdAt = LocalDateTime.now(mockClockStart),
        createdBy = "pegbuser",
        personIdOriginal = Some("asteroid_m"),
        personIdUpdated = Some("asteroid_m"),
        updatedAt = Some(LocalDateTime.now(mockClockStart)),
        updatedBy = Some("pegbuser"))

      val expectedResult = walletApplicationOne

      result.isRight mustBe true
      result.right.get.get mustBe expectedResult

    }

    "return Right[Option[WalletApplication]] if wallet application is not found by internal id" in {
      val result = walletApplicationDao.getWalletApplicationByInternalId(10)

      result.isRight mustBe true
      result.right.get mustBe None

    }

    "return Right[None] if wallet application was not found" in {
      val result = walletApplicationDao.getWalletApplicationByUUID(UUID.randomUUID())

      result.isRight mustBe true
      result.right.get mustBe None
    }

  }

  "WalletApplicationSqlDao getWalletApplicationByUserUuid" should {

    "return Right[Set[WalletApplication]] if wallet application was found by user uuid" in {
      val result = walletApplicationDao.getWalletApplicationByUserUuid(userIdOne)

      val walletApplicationOne = WalletApplication.getEmpty.copy(
        id = 1,
        uuid = testUuidOne,
        userUuid = userIdOne,

        msisdn = Some("96506821078829"),
        status = "pending",
        applicationStage = "new",
        checkedAt = None,
        checkedBy = None,
        rejectionReason = None,
        createdAt = LocalDateTime.now(mockClockStart),
        createdBy = "pegbuser",
        personIdOriginal = Some("asteroid_m"),
        personIdUpdated = Some("asteroid_m"),
        updatedAt = Some(LocalDateTime.now(mockClockStart)),
        updatedBy = Some("pegbuser"))

      val expectedResult = walletApplicationOne

      result.isRight mustBe true
      result.right.get mustBe Set(expectedResult)

    }

    "return Right[Set.empty] if wallet application was not found by userUuid" in {
      val result = walletApplicationDao.getWalletApplicationByUserUuid(UUID.randomUUID())

      result.isRight mustBe true
      result.right.get mustBe Set.empty
    }

  }

  "WalletApplicationSqlDao getWalletApplicationsByCriteria" should {

    "return Right[Seq[WalletApplication]] for all wallet applications in db" in {
      val walletApplicationOne = WalletApplication.getEmpty.copy(
        id = 1,
        uuid = testUuidOne,
        userUuid = userIdOne,

        msisdn = Some("96506821078829"),
        status = "pending",
        applicationStage = "new",
        checkedAt = None,
        checkedBy = None,
        rejectionReason = None,
        createdAt = LocalDateTime.now(mockClockStart),
        createdBy = "pegbuser",
        personIdOriginal = Some("asteroid_m"),
        personIdUpdated = Some("asteroid_m"),
        updatedAt = Some(LocalDateTime.now(mockClockStart)),
        updatedBy = Some("pegbuser"))

      val walletApplicationTwo = WalletApplication.getEmpty.copy(
        id = 2,
        uuid = testUuidTwo,
        userUuid = userIdTwo,

        msisdn = Some("98717192132324"),
        status = "approved",
        applicationStage = "ocr",
        checkedAt = None,
        checkedBy = None,
        rejectionReason = None,
        createdAt = LocalDateTime.now(mockClockStart),
        createdBy = "ujali",
        personIdOriginal = Some("asteroid_m"),
        personIdUpdated = Some("asteroid_m"),
        updatedAt = Some(LocalDateTime.now(mockClockStart)),
        updatedBy = Some("ujali"))

      val walletApplicationThree = WalletApplication.getEmpty.copy(
        id = 3,
        uuid = testUuidThree,
        userUuid = userIdThree,

        msisdn = Some("97177842889921"),
        status = "rejected",
        applicationStage = "new",
        checkedAt = None,
        checkedBy = None,
        rejectionReason = Some("test"),
        createdAt = LocalDateTime.now(mockClockEnd),
        createdBy = "david",
        personIdOriginal = Some("asteroid_m"),
        personIdUpdated = Some("asteroid_m"),
        updatedAt = Some(LocalDateTime.now(mockClockEnd)),
        updatedBy = Some("david"))

      val expectedResult = Seq(walletApplicationOne, walletApplicationTwo, walletApplicationThree)

      val result = walletApplicationDao.getWalletApplicationsByCriteria(WalletApplicationCriteria.getEmpty, None, None, None)

      result.isRight mustBe true
      result.right.get mustBe expectedResult
    }

    "return Right[Seq[WalletApplication]] for wallet applications in db for search criteria of date range " in {
      val criteria = WalletApplicationCriteria.getEmpty
        .copy(
          createdAtStartingFrom = Some(LocalDate.of(2019, 1, 1)),
          createdAtUpTo = Some(LocalDate.of(2019, 1, 31)))
      val walletApplicationOne = WalletApplication.getEmpty.copy(
        id = 1,
        uuid = testUuidOne,
        userUuid = userIdOne,

        msisdn = Some("96506821078829"),
        status = "pending",
        applicationStage = "new",
        checkedAt = None,
        checkedBy = None,
        rejectionReason = None,
        createdAt = LocalDateTime.now(mockClockStart),
        createdBy = "pegbuser",
        personIdOriginal = Some("asteroid_m"),
        personIdUpdated = Some("asteroid_m"),
        updatedAt = Some(LocalDateTime.now(mockClockStart)),
        updatedBy = Some("pegbuser"))

      val walletApplicationTwo = WalletApplication.getEmpty.copy(
        id = 2,
        uuid = testUuidTwo,
        userUuid = userIdTwo,

        msisdn = Some("98717192132324"),
        status = "approved",
        applicationStage = "ocr",
        checkedAt = None,
        checkedBy = None,
        rejectionReason = None,
        createdAt = LocalDateTime.now(mockClockStart),
        createdBy = "ujali",
        personIdOriginal = Some("asteroid_m"),
        personIdUpdated = Some("asteroid_m"),
        updatedAt = Some(LocalDateTime.now(mockClockStart)),
        updatedBy = Some("ujali"))

      val walletApplicationThree = WalletApplication.getEmpty.copy(
        id = 3,
        uuid = testUuidThree,
        userUuid = userIdThree,

        msisdn = Some("97177842889921"),
        status = "rejected",
        applicationStage = "new",
        checkedAt = None,
        checkedBy = None,
        rejectionReason = Some("test"),
        createdAt = LocalDateTime.now(mockClockEnd),
        createdBy = "david",
        personIdOriginal = Some("asteroid_m"),
        personIdUpdated = Some("asteroid_m"),
        updatedAt = Some(LocalDateTime.now(mockClockEnd)),
        updatedBy = Some("david"))

      val expectedResult = Seq(walletApplicationOne, walletApplicationTwo, walletApplicationThree)

      val result = walletApplicationDao.getWalletApplicationsByCriteria(criteria, None, None, None)

      result.isRight mustBe true
      result.right.get mustBe expectedResult
    }

    "return Right[Seq[WalletApplication]] for all wallet applications that meet the given criteria" in {

      val criteria = WalletApplicationCriteria.getEmpty.copy(applicationStage = Some("new"), nationalId = Some("asteroid_m"), inactiveStatuses = Set("PASSIVE"))
      val walletApplicationOne = WalletApplication.getEmpty.copy(
        id = 1,
        uuid = testUuidOne,
        userUuid = userIdOne,

        msisdn = Some("96506821078829"),
        status = "pending",
        applicationStage = "new",
        checkedAt = None,
        checkedBy = None,
        rejectionReason = None,
        createdAt = LocalDateTime.now(mockClockStart),
        createdBy = "pegbuser",
        personIdOriginal = Some("asteroid_m"),
        personIdUpdated = Some("asteroid_m"),
        updatedAt = Some(LocalDateTime.now(mockClockStart)),
        updatedBy = Some("pegbuser"))

      val walletApplicationThree = WalletApplication.getEmpty.copy(
        id = 3,
        uuid = testUuidThree,
        userUuid = userIdThree,

        msisdn = Some("97177842889921"),
        status = "rejected",
        applicationStage = "new",
        checkedAt = None,
        checkedBy = None,
        rejectionReason = Some("test"),
        createdAt = LocalDateTime.now(mockClockEnd),
        createdBy = "david",
        personIdOriginal = Some("asteroid_m"),
        personIdUpdated = Some("asteroid_m"),
        updatedAt = Some(LocalDateTime.now(mockClockEnd)),
        updatedBy = Some("david"))

      val expectedResult = Seq(walletApplicationOne, walletApplicationThree)

      val result = walletApplicationDao.getWalletApplicationsByCriteria(criteria, None, None, None)

      result.isRight mustBe true
      result.right.get mustBe expectedResult
    }

    "return Right[Seq[WalletApplication]] which is a subset of wallet applications that meet the criteria because of limit and offset" in {
      val criteria = WalletApplicationCriteria.getEmpty.copy(applicationStage = Some("new"), inactiveStatuses = Set("PASSIVE"))
      val walletApplicationOne = WalletApplication.getEmpty.copy(
        id = 1,
        uuid = testUuidOne,
        userUuid = userIdOne,

        msisdn = Some("96506821078829"),
        status = "pending",
        applicationStage = "new",
        checkedAt = None,
        checkedBy = None,
        rejectionReason = None,
        createdAt = LocalDateTime.now(mockClockStart),
        createdBy = "pegbuser",
        personIdOriginal = Some("asteroid_m"),
        personIdUpdated = Some("asteroid_m"),
        updatedAt = Some(LocalDateTime.now(mockClockStart)),
        updatedBy = Some("pegbuser"))

      val expectedResult = Seq(walletApplicationOne)

      val result = walletApplicationDao.getWalletApplicationsByCriteria(criteria, None, limit = Some(1), offset = Some(0))

      result.isRight mustBe true
      result.right.get mustBe expectedResult
    }

    "return Right[Seq[WalletApplication]] which is ordered wallet applications as defined by given ordering" in {
      val criteria = WalletApplicationCriteria.getEmpty.copy(applicationStage = Some("new"), inactiveStatuses = Set("PASSIVE"))
      val ordering = OrderingSet(mutable.LinkedHashSet(Order("msisdn", Order.DESC)))
      val walletApplicationOne = WalletApplication.getEmpty.copy(
        id = 1,
        uuid = testUuidOne,
        userUuid = userIdOne,

        msisdn = Some("96506821078829"),
        status = "pending",
        applicationStage = "new",
        checkedAt = None,
        checkedBy = None,
        rejectionReason = None,
        createdAt = LocalDateTime.now(mockClockStart),
        createdBy = "pegbuser",
        personIdOriginal = Some("asteroid_m"),
        personIdUpdated = Some("asteroid_m"),
        updatedAt = Some(LocalDateTime.now(mockClockStart)),
        updatedBy = Some("pegbuser"))

      val walletApplicationThree = WalletApplication.getEmpty.copy(
        id = 3,
        uuid = testUuidThree,
        userUuid = userIdThree,

        msisdn = Some("97177842889921"),
        status = "rejected",
        applicationStage = "new",
        checkedAt = None,
        checkedBy = None,
        rejectionReason = Some("test"),
        createdAt = LocalDateTime.now(mockClockEnd),
        createdBy = "david",
        personIdOriginal = Some("asteroid_m"),
        personIdUpdated = Some("asteroid_m"),
        updatedAt = Some(LocalDateTime.now(mockClockEnd)),
        updatedBy = Some("david"))

      val expectedResult = Seq(walletApplicationThree, walletApplicationOne)

      val result = walletApplicationDao.getWalletApplicationsByCriteria(criteria, ordering = Some(ordering), None, None)

      result.isRight mustBe true
      result.right.get mustBe expectedResult
    }
  }

  "WalletApplicationSqlDao countWalletApplicationsByCriteria" should {

    "return Right[Int] which is the total number of wallet applications that meet the given criteria" in {
      val criteria = WalletApplicationCriteria.getEmpty.copy(applicationStage = Some("new"))

      val result = walletApplicationDao.countWalletApplicationsByCriteria(criteria)

      result.isRight mustBe true
      result.right.get mustBe 2
    }
  }

  "WalletApplicationSqlDao insertWalletApplication" should {
    "insert a row in users_application table" ignore {
      val id = 99
      val walletUUID = UUID.randomUUID()
      val createdBy = "pegbuser"
      val createdAt = LocalDateTime.now(mockClockStart)
      val walletApplicationToCreate = WalletApplicationToCreate.createEmpty(walletUUID, createdBy, createdAt)
        .copy(id = Some(id), userId = 1, updatedAt = Some(createdAt))

      val result = walletApplicationDao.insertWalletApplication(walletApplicationToCreate)

      val expectedResult =
        WalletApplication.getEmpty.copy(
          id = id,
          uuid = walletUUID,
          userUuid = userIdOne,

          msisdn = Some("96506821078829"),
          status = "pending",
          applicationStage = "new",
          checkedAt = None,
          checkedBy = None,
          rejectionReason = None,
          createdAt = createdAt,
          createdBy = createdBy,
          updatedAt = Some(createdAt),
          updatedBy = None)

      result.isRight mustBe true
      result.right.get mustBe expectedResult
    }
  }

  "WalletApplicationSqlDao updateWalletApplication" should {

    "return Right[Option[WalletApplication]] which is the updated wallet application depending on the update parameters" in {

      val walletApplicationToUpdate = WalletApplicationToUpdate(
        status = Option("rejected"),
        rejectionReason = Option("Invalid documents"),
        updatedBy = "George",
        updatedAt = LocalDateTime.now(mockClockStart))

      val updateResult = walletApplicationDao.updateWalletApplication(testUuidOne, walletApplicationToUpdate)

      val expectedResult =
        WalletApplication.getEmpty.copy(
          id = 1,
          uuid = testUuidOne,
          userUuid = userIdOne,

          msisdn = Some("96506821078829"),
          status = "rejected",
          applicationStage = "new",
          checkedAt = None,
          checkedBy = None,
          rejectionReason = Some("Invalid documents"),
          createdAt = LocalDateTime.now(mockClockStart),
          createdBy = "pegbuser",
          personIdOriginal = Some("asteroid_m"),
          personIdUpdated = Some("asteroid_m"),
          updatedAt = Some(LocalDateTime.now(mockClockStart)),
          updatedBy = Some("George"))

      updateResult.isRight mustBe true
      updateResult.right.get.get mustBe expectedResult

    }

    "return Right[None] if wallet application to update was not found" in {
      val walletApplicationToUpdate = WalletApplicationToUpdate(
        status = Option("rejected"),
        rejectionReason = Option("Invalid documents"),
        updatedBy = "George",
        updatedAt = LocalDateTime.now(mockClockStart))

      val unknownUUID = UUID.randomUUID()
      val updateResult = walletApplicationDao.updateWalletApplication(unknownUUID, walletApplicationToUpdate)

      updateResult mustBe Right(None)

    }

    "return Left[ConnectionError] if database cannot be reached" in {
      val dbApiTest: DBApi = inject[DBApi]

      dbApiTest.database("backoffice").shutdown()
      val walletApplicationDao = new WalletApplicationSqlDao(dbApiTest, kafkaDBSyncService)

      val result = walletApplicationDao.getWalletApplicationByUUID(testUuidOne)
      val expectedResult = ServiceError.notFoundError(s"Could not get application from db $testUuidOne", Some(UUID.randomUUID()))

      result.isLeft mustBe true
      result.left.get.message mustBe expectedResult.message
    }
  }

}
