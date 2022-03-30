package tech.pegb.backoffice.dao.auth

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.DaoError
import tech.pegb.backoffice.dao.auth.dto.{BackOfficeUserCriteria, BackOfficeUserToInsert, BackOfficeUserToUpdate}
import tech.pegb.backoffice.dao.auth.sql.{BackOfficeUserSqlDao, BusinessUnitSqlDao}
import tech.pegb.backoffice.dao.model.{CriteriaField, OrderingSet}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.core.PegBTestApp

class BackOfficeUserSqlDaoSpec extends PegBTestApp {

  override val additionalBindings = super.additionalBindings ++
    Seq()

  lazy val backOfficeUserSqlDao = fakeApplication().injector.instanceOf[BackOfficeUserSqlDao]
  override def initSql =
    s"""
       |CREATE ALIAS UNIX_TIMESTAMP AS $$$$ long getUnixTimestamp(){
       | return java.time.Instant.now().toEpochMilli();
       |} $$$$;
       |
       |INSERT INTO roles (id,`name`,is_active,level,created_by, created_at) VALUES
       |('7eba8b9b-c43f-4122-976d-c6d76d77890a','role10',0,1,'system', now() ),
       |('32ec03f3-d1dc-11e8-bcd3-000c291e73b1','admin01',1,1,'system',now() );
       |
       |INSERT INTO business_units(id, name, is_active, created_by, created_at)
       |VALUES
       |('12f98f77-b503-470a-ba53-7c23e120774c', 'Finance1',    '1', 'pegbuser', now()),
       |('0ea73ef3-b7c0-41c6-95bf-65e09f576e4b', 'RnD1',        '0', 'pegbuser', now());
       |
       |INSERT INTO back_office_users(id, userName, password, roleId, businessUnitId, email, phoneNumber, firstName, middleName, lastName, description, homePage, activeLanguage, customData, lastLoginTimestamp, is_active, created_by, updated_by, created_at, updated_at)
       |VALUES
       |('0dc7209a-5588-4d86-affd-1474981e2860', 'lloyd', 'password1', '7eba8b9b-c43f-4122-976d-c6d76d77890a', '0ea73ef3-b7c0-41c6-95bf-65e09f576e4b', 'edano@pegb.tech', '0544451678', 'Lloyd', 'Pepito', 'Edano', NULL, NULL, 'Filipino', NULL, NULL, 1, 'pegbuser', 'pegbuser', '2019-10-01 00:00:00', '2019-10-01 00:00:00'),
       |('90924dd9-210f-4a4f-9576-9eede7548d12', 'ujali', 'password2', '32ec03f3-d1dc-11e8-bcd3-000c291e73b1', '0ea73ef3-b7c0-41c6-95bf-65e09f576e4b', 'tyagi@pegb.tech', '0544451668', 'Ujali', 'Ragu', 'Tyagi', NULL, NULL, 'Hindi', NULL, NULL, 1, 'pegbuser', 'pegbuser', '2019-10-01 00:00:00', '2019-10-01 00:00:00'),
       |('0b6664ee-84a6-4a69-bf9f-61296dc5a455', 'david', 'password3', '7eba8b9b-c43f-4122-976d-c6d76d77890a', '12f98f77-b503-470a-ba53-7c23e120774c', 'salgado@pegb.tech', '0544451658', 'David', 'Ker', 'Salgado', NULL, NULL, 'Filipino', NULL, NULL, 1, 'pegbuser', 'pegbuser', '2019-10-01 00:00:00', '2019-10-01 00:00:00');
     """.stripMargin

  override def cleanupSql =
    s"""
       |DELETE FROM back_office_users;
       |DELETE FROM roles;
       |DELETE FROM business_units;
     """.stripMargin

  "BackOfficeUserSqlDao" should {
    "insert a new BackOfficeUser" in {
      val dto = BackOfficeUserToInsert.empty.copy(userName = "new user 01", firstName = "George", lastName = "Ogalo",
        roleId = "32ec03f3-d1dc-11e8-bcd3-000c291e73b1", businessUnitId = "12f98f77-b503-470a-ba53-7c23e120774c",
        isActive = 0, createdAt = LocalDateTime.now().withNano(0))

      val result = backOfficeUserSqlDao.createBackOfficeUser(dto)

      result.isRight mustBe true
      result.right.get.userName mustBe dto.userName
      result.right.get.firstName mustBe dto.firstName
      result.right.get.lastName mustBe dto.lastName
      result.right.get.roleId mustBe dto.roleId
      result.right.get.roleName mustBe "admin01"
      result.right.get.businessUnitId mustBe dto.businessUnitId
      result.right.get.businessUnitName mustBe "Finance1"
      result.right.get.createdAt mustBe Some(dto.createdAt)
      result.right.get.isActive mustBe dto.isActive
    }

    "fail to insert a new BackOfficeUser if name is already existing" in {
      val dto = BackOfficeUserToInsert.empty.copy(userName = "lloyd")

      val result = backOfficeUserSqlDao.createBackOfficeUser(dto)

      result.isLeft mustBe true
      result.left.get.isInstanceOf[DaoError.EntityAlreadyExistsError] mustBe true
    }
    "fail to insert a new BackOfficeUser if email is already existing" in {
      val dto = BackOfficeUserToInsert.empty.copy(email = "tyagi@pegb.tech")

      val result = backOfficeUserSqlDao.createBackOfficeUser(dto)

      result.isLeft mustBe true
      result.left.get.isInstanceOf[DaoError.EntityAlreadyExistsError] mustBe true
    }
    "fail to insert a new BackOfficeUser if phone number is already existing" in {
      val dto = BackOfficeUserToInsert.empty.copy(phoneNumber = Some("0544451658"))

      val result = backOfficeUserSqlDao.createBackOfficeUser(dto)

      result.isLeft mustBe true
      result.left.get.isInstanceOf[DaoError.EntityAlreadyExistsError] mustBe true
    }

    "update an existing BackOfficeUser" in {
      val idToUpdate = "0dc7209a-5588-4d86-affd-1474981e2860"
      val dto = BackOfficeUserToUpdate(
        email = "newlloyd@pegb.tech".toOption, updatedBy = "admin",
        //adding plus 1 to second so it is guaranteed more updated than the record that was inserted above
        updatedAt = LocalDateTime.now.plusSeconds(1).withNano(0),
        lastUpdatedAt = LocalDateTime.of(2019, 10, 1, 0, 0, 0).toOption)

      val result = backOfficeUserSqlDao.updateBackOfficeUser(idToUpdate, dto)

      val expected = backOfficeUserSqlDao.getBackOfficeUsersByCriteria(
        BackOfficeUserCriteria(id = CriteriaField("id", idToUpdate).toOption).toOption, None, None, None)

      result.isRight mustBe true
      result.right.get mustBe expected.right.get.headOption
      result.right.get.map(_.email) mustBe dto.email
      result.right.get.get.updatedBy mustBe dto.updatedBy.toOption
      result.right.get.get.updatedAt mustBe dto.updatedAt.toOption
    }

    "fail to update an existing backoffice user if updated name is already taken" in {
      val idToUpdate = "90924dd9-210f-4a4f-9576-9eede7548d12"
      val dto = BackOfficeUserToUpdate(userName = "lloyd".toOption, updatedBy = "admin",
        updatedAt = LocalDateTime.now, lastUpdatedAt = LocalDateTime.of(2019, 10, 1, 0, 0, 0).toOption)

      val result = backOfficeUserSqlDao.updateBackOfficeUser(idToUpdate, dto)

      result.isLeft mustBe true
      result.left.get.isInstanceOf[DaoError.EntityAlreadyExistsError] mustBe true
    }
    "respond with Right(None) if backoffice user to update was not found" in {
      val idToUpdate = "xxxxx"
      val dto = BackOfficeUserToUpdate(userName = "lloyd".toOption, updatedBy = "admin",
        updatedAt = LocalDateTime.now, lastUpdatedAt = LocalDateTime.of(2019, 10, 1, 0, 0, 0).toOption)

      val result = backOfficeUserSqlDao.updateBackOfficeUser(idToUpdate, dto)

      result.isRight mustBe true
      result mustBe Right(None)
    }

    "count BackOfficeUsers without criteria" in {

      val result = backOfficeUserSqlDao.countBackOfficeUsersByCriteria(None)

      val expected = 4

      result.isRight mustBe true
      result.right.get mustBe expected
    }

    "count BackOfficeUsers from given criteria" in {

      val dto = BackOfficeUserCriteria(roleId = CriteriaField(BackOfficeUserSqlDao.cRoleId, "7eba8b9b-c43f-4122-976d-c6d76d77890a").toOption)

      val result = backOfficeUserSqlDao.countBackOfficeUsersByCriteria(Some(dto))

      val expected = 2

      result.isRight mustBe true
      result.right.get mustBe expected
    }

    "get BackOfficeUsers from given criteria" in {
      val dto = BackOfficeUserCriteria(businessUnitName = CriteriaField(BusinessUnitSqlDao.cName, "RnD1").toOption)

      val result = backOfficeUserSqlDao.getBackOfficeUsersByCriteria(Some(dto), None, None, None)

      val expected = Seq("lloyd", "ujali")

      result.isRight mustBe true
      result.right.get.map(_.userName).toSet mustBe expected.toSet
    }

    "get BackOfficeUsers from given criteria and ordering" in {
      val dto = BackOfficeUserCriteria(isActive = CriteriaField(BackOfficeUserSqlDao.cIsActive, 1).toOption)
      val ordering = OrderingSet("userName", "ASC")

      val result = backOfficeUserSqlDao.getBackOfficeUsersByCriteria(Some(dto), ordering.toOption, None, None)

      val expected = Seq("david", "lloyd", "ujali")

      result.isRight mustBe true
      result.right.get.map(_.userName) mustBe expected
    }

    "get BackOfficeUsers from given criteria, ordering and pagination" in {
      val dto = BackOfficeUserCriteria()
      val ordering = OrderingSet("firstName", "DESC")
      val limit = Some(2)
      val offset = Some(2)

      val result = backOfficeUserSqlDao.getBackOfficeUsersByCriteria(Some(dto), ordering.toOption, limit, offset)

      val expected = Seq("George", "David")

      result.isRight mustBe true
      result.right.get.map(_.firstName) mustBe expected
    }

    "get most recent updated BackOfficeUser" in {
      val result = backOfficeUserSqlDao.getMostRecentUpdatedAt(None)
      val expected = "Lloyd"

      result.isRight mustBe true
      result.right.get.map(_.firstName).get mustBe expected
    }

    "last login time should be set" in {
      val result = backOfficeUserSqlDao.updateLastLoginTimestamp("0dc7209a-5588-4d86-affd-1474981e2860")

      result.right.get.map(_.lastLoginTimestamp).get must not be None
    }
  }
}
