package tech.pegb.backoffice.dao

import java.time.LocalDateTime

import org.scalamock.scalatest.MockFactory
import tech.pegb.backoffice.dao.customer.abstraction.CustomerAttributesDao
import tech.pegb.backoffice.dao.customer.entity.CustomerAttributes.CustomerStatus
import tech.pegb.core.PegBTestApp

class CustomerAttributesSqlDaoSpec extends PegBTestApp with MockFactory {

  private val customerAttributeDao = app.injector.instanceOf[CustomerAttributesDao]

  override def initSql =
    s"""
       |INSERT INTO user_status(status_name, description, created_at, created_by, updated_at, updated_by, is_active)
       |VALUES('WAITING_FOR_ACTIVATION', null, '2018-12-25 00:00:00', 'SuperUser', null, null, 1);
       |INSERT INTO user_status(status_name, description, created_at, created_by, updated_at, updated_by, is_active)
       |VALUES('ACTIVE', null, '2018-12-25 00:00:00', 'SuperUser', null, null, 1);
       |INSERT INTO user_status(status_name, description, created_at, created_by, updated_at, updated_by, is_active)
       |VALUES('INACTIVE', null, '2018-12-25 00:00:00', 'SuperUser', null, null, 1);
       |
     """.stripMargin

  override def cleanupSql =
    s"""
       |DELETE FROM user_status_has_requirements;
       |DELETE FROM user_status;
     """.stripMargin

  "CustomerAttributesDaoSql" should {
    "getCustomerStatuses" in {
      val result = customerAttributeDao.getCustomerStatuses
      val expected = Set(
        CustomerStatus(
          statusName = "WAITING_FOR_ACTIVATION",
          description = None,
          isActive = true,
          createdAt = LocalDateTime
            .of(2018, 12, 25, 0, 0, 0),
          createdBy = "SuperUser",
          updatedAt = None,
          updatedBy = None),
        CustomerStatus(
          statusName = "ACTIVE",
          description = None,
          isActive = true,
          createdAt = LocalDateTime
            .of(2018, 12, 25, 0, 0, 0),
          createdBy = "SuperUser",
          updatedAt = None,
          updatedBy = None),
        CustomerStatus(
          statusName = "INACTIVE",
          description = None,
          isActive = true,
          createdAt = LocalDateTime
            .of(2018, 12, 25, 0, 0, 0),
          createdBy = "SuperUser",
          updatedAt = None,
          updatedBy = None))

      result.isRight mustBe true
      result.right.get mustBe expected
    }
  }
}
