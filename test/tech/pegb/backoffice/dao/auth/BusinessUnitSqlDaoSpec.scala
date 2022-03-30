package tech.pegb.backoffice.dao.auth

import java.time.LocalDateTime

import play.api.inject.Binding
import tech.pegb.backoffice.dao.DaoError
import tech.pegb.backoffice.dao.auth.dto.{BusinessUnitCriteria, BusinessUnitToInsert, BusinessUnitToUpdate}
import tech.pegb.backoffice.dao.auth.sql.BusinessUnitSqlDao
import tech.pegb.backoffice.dao.auth.entity.BusinessUnit
import tech.pegb.backoffice.dao.model.{CriteriaField, OrderingSet}
import tech.pegb.core.PegBTestApp
import tech.pegb.backoffice.util.Implicits._

class BusinessUnitSqlDaoSpec extends PegBTestApp {

  override val additionalBindings: Seq[Binding[_]] = super.additionalBindings ++
    Seq()

  lazy val businessUnitSqlDao: BusinessUnitSqlDao = fakeApplication().injector.instanceOf[BusinessUnitSqlDao]

  override def initSql: String =
    s"""
       |INSERT INTO business_units(id, name, is_active, created_by, created_at, updated_by, updated_at)
       |VALUES
       |('02f98f77-b503-470a-ba53-7c23e120774c', 'Finance',    '1', 'pegbuser', '2019-01-01 00:00:00', null, null),
       |('c5ecd0ff-893a-4abb-8e5b-5e8aab102bd7', 'Management', '1', 'pegbuser', '2019-01-01 00:00:00', null, null),
       |('51fe1a5a-8a76-4b04-9057-62b5b1dfae4f', 'Legal',      '0', 'pegbuser', '2019-01-01 00:00:00', null, null),
       |('4b2b7321-44ab-4597-abe4-1b648bd1699a', 'Operations', '1', 'pegbuser', '2019-01-01 00:00:00', null, null),
       |('9ea73ef3-b7c0-41c6-95bf-65e09f576e4b', 'RnD',        '0', 'pegbuser', '2019-01-01 00:00:00', null, null);
     """.stripMargin

  override def cleanupSql: String =
    s"""
       |DELETE FROM business_units;
     """.stripMargin

  "BusinessUnitSqlDao" should {
    "insert a new BusinessUnit" in {
      val businessUnitToInsert = BusinessUnitToInsert(
        name = "Maintenance",
        isActive = 1,
        createdBy = "Lloyd",
        createdAt = LocalDateTime.of(2019, 10, 17, 0, 0, 0, 0),
        updatedBy = None, updatedAt = None)

      val result = businessUnitSqlDao.create(businessUnitToInsert)

      val expected = BusinessUnit(
        id = "random internally generated id",
        name = businessUnitToInsert.name,
        isActive = businessUnitToInsert.isActive,
        createdAt = Some(businessUnitToInsert.createdAt),
        createdBy = Some(businessUnitToInsert.createdBy),
        updatedAt = businessUnitToInsert.updatedAt,
        updatedBy = businessUnitToInsert.updatedBy)

      result.isRight mustBe true
      result.right.get.copy(id = "random internally generated id") mustBe expected
    }

    "fail to insert a new BusinessUnit if integrity constraints violated" in {
      val businessUnitToInsert = BusinessUnitToInsert(
        name = "Maintenance",
        isActive = 1,
        createdBy = "Lloyd",
        createdAt = LocalDateTime.of(2019, 10, 17, 0, 0),
        updatedBy = None, updatedAt = None)

      val result = businessUnitSqlDao.create(businessUnitToInsert)

      result.isLeft mustBe true
      result.left.get mustBe DaoError.EntityAlreadyExistsError(s"Failed to insert business unit [${businessUnitToInsert.toSmartString}]. Id or name may already be existing.")
    }

    "get BusinessUnit by criteria" in {

      val criteria = BusinessUnitCriteria(createdBy = Some(CriteriaField(BusinessUnitSqlDao.cCreatedBy, "pegbuser")))
      val result = businessUnitSqlDao.getBusinessUnitsByCriteria(criteria, None, None, None)

      val expected = Set(("Finance", Some("pegbuser")), ("Management", Some("pegbuser")),
        ("Legal", Some("pegbuser")), ("Operations", Some("pegbuser")), ("RnD", Some("pegbuser")))

      result.isRight mustBe true
      result.right.get.map(bu ⇒ (bu.name, bu.createdBy)).toSet mustBe expected
    }

    "get all BusinessUnit ordered by name and paginated with limit=3 and offset=2" in {

      val noCriteria = BusinessUnitCriteria()
      val result = businessUnitSqlDao.getBusinessUnitsByCriteria(noCriteria, Some(OrderingSet("name", "ASC")),
        maybeLimit = Some(3), maybeOffset = Some(2))

      val expected = Seq("Finance", "Management",
        "Legal", "RnD", "Maintenance", "Operations").sorted.drop(2).take(3)

      result.isRight mustBe true
      result.right.get.map(_.name) mustBe expected
    }

    "count BusinessUnit by criteria" in {

      val criteria = BusinessUnitCriteria(isActive = Some(CriteriaField[Int](BusinessUnitSqlDao.cIsActive, 1)))
      val result = businessUnitSqlDao.countBusinessUnitsByCriteria(criteria)

      val expected = 4

      result.isRight mustBe true
      result.right.get mustBe expected
    }

    "update BusinessUnit by id" in {
      val businessUnitToUpdate = BusinessUnitToUpdate(
        name = Some("Accounting Department"),
        isActive = None,
        updatedBy = "David",
        updatedAt = LocalDateTime.now.withNano(0),
        lastUpdatedAt = None)

      val result = businessUnitSqlDao.update("02f98f77-b503-470a-ba53-7c23e120774c", businessUnitToUpdate)

      val expected = Some(BusinessUnit(
        id = "02f98f77-b503-470a-ba53-7c23e120774c",
        name = "Accounting Department",
        isActive = 1,
        createdBy = Some("pegbuser"),
        createdAt = Some(LocalDateTime.of(2019, 1, 1, 0, 0, 0, 0)),
        updatedBy = Some("David"),
        updatedAt = Some(businessUnitToUpdate.updatedAt)))

      result.isRight mustBe true
      result.right.get mustBe expected
    }

    "fail to update BusinessUnit by id if last_updated_at is not NULL and not the same as updated_at" in {
      val businessUnitToUpdate = BusinessUnitToUpdate(
        name = Some("New Accounting Department"),
        isActive = None,
        updatedBy = "Daniela",
        updatedAt = LocalDateTime.now,
        lastUpdatedAt = Some(LocalDateTime.of(2019, 1, 1, 0, 0, 0, 0)))

      val result = businessUnitSqlDao.update("02f98f77-b503-470a-ba53-7c23e120774c", businessUnitToUpdate)

      result.isLeft mustBe true
      result.left.get mustBe DaoError.PreconditionFailed(s"Update failed. Business unit 02f98f77-b503-470a-ba53-7c23e120774c has been modified by another process.")
    }
  }
}
