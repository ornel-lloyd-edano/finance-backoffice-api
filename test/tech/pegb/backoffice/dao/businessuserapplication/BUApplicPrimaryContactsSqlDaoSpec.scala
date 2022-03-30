package tech.pegb.backoffice.dao.businessuserapplication

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.businessuserapplication.dto.BUApplicPrimaryContactToInsert
import tech.pegb.backoffice.dao.businessuserapplication.sql.BUApplicPrimaryContactsSqlDao
import tech.pegb.core.PegBTestApp

class BUApplicPrimaryContactsSqlDaoSpec extends PegBTestApp {

  val dao = inject[BUApplicPrimaryContactsSqlDao]

  override def initSql =
    s"""
       |INSERT INTO business_user_applications (id,uuid,business_name,brand_name,business_category,stage,status,user_tier, business_type, registration_number, tax_number, registration_date, created_by,updated_by,created_at,updated_at) VALUES
       |('1','b6239cf9-9a07-4b7f-974f-4f130d7927df','Universal Catering Co','Costa Coffee DSO','Restaurants - 5182','identity_info','ongoing', 'basic', 'merchant', '212/212EE', 'B12342M', '2019-01-01','system','system','2019-01-01 00:00:00','2019-01-01 00:00:00'),
       |('2','4483d05f-5ed0-43bb-ad6d-7bd6555d77ce','Tindahan ni Aling Nena','Nenas Store','Store - 1111','application_documents','approved', 'basic', 'merchant', '111/212EE', 'A12342M', '1990-01-01','system','system','2019-01-01 00:00:00','2019-01-01 00:00:00'),
       |('3','3f832e6c-3c50-44b4-8dca-4978308239e3','Henry Sy and kids','SM Megamall','Department Store - 9999','application_documents','pending', 'basic', 'merchant', '999/212EE', 'C12342M', '1999-01-01','system','system','2019-01-01 00:00:00','2019-01-01 00:00:00');
       |
       |CREATE TABLE `business_user_application_primary_contacts` (
       |  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
       |  `uuid` varchar(36) NOT NULL,
       |  `application_id` int(10) unsigned NOT NULL,
       |  `contact_type` varchar(30)  NOT NULL,
       |  `name` varchar(50)  NOT NULL,
       |  `middle_name` varchar(50)  DEFAULT NULL,
       |  `surname` varchar(50)  NOT NULL,
       |  `phone_number` varchar(50)  NOT NULL,
       |  `email` varchar(50)  NOT NULL,
       |  `id_type` varchar(50)  NOT NULL,
       |  `is_velocity_user` tinyint(1) NOT NULL DEFAULT '0',
       |  `velocity_level` varchar(50)  DEFAULT NULL,
       |  `is_default_contact` tinyint DEFAULT 0,
       |  `created_by` varchar(50)  NOT NULL,
       |  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
       |  `updated_by` varchar(50)  DEFAULT NULL,
       |  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
       |  PRIMARY KEY (`id`),
       |  UNIQUE KEY (`uuid`),
       |  KEY `FK_bu_app_primary_contacts_application_id_bu_applications_id` (`application_id`),
       |  CONSTRAINT `FK_bu_app_primary_contacts_application_id_bu_applications_id` FOREIGN KEY (`application_id`) REFERENCES `business_user_applications` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
       |);
       |
       |""".stripMargin

  "BUApplicPrimaryContactsSqlDao" should {
    "insert business_user_application_primary_contacts rows" in {

      val conn = dao.startTransaction.right.get

      val mockData = Seq(
        BUApplicPrimaryContactToInsert(
          applicationId = 1,
          contactType = "business owner",
          name = "Lloyd",
          middleName = Some("Pepito"),
          surname = "Edano",
          phoneNumber = "+97154445679",
          email = "o.lloyd@pegb.tech",
          idType = "Emirates ID",
          isVelocityUser = true,
          isDefaultContact = Some(true),
          velocityLevel = Some("admin"),
          createdBy = "test_user",
          createdAt = LocalDateTime.now),
        BUApplicPrimaryContactToInsert(
          applicationId = 1,
          contactType = "accountant",
          name = "George",
          middleName = None,
          surname = "Ogalo",
          phoneNumber = "+971543265189",
          email = "g.ogalo@pegb.tech",
          idType = "Emirates ID",
          isVelocityUser = true,
          velocityLevel = Some("admin"),
          isDefaultContact = Some(true),
          createdBy = "test_user",
          createdAt = LocalDateTime.now),
        BUApplicPrimaryContactToInsert(
          applicationId = 1,
          contactType = "manager",
          name = "Ujali",
          middleName = Some("Angkit"),
          surname = "Tyagi",
          phoneNumber = "+971523471430",
          email = "u.tyagi@pegb.tech",
          idType = "Emirates ID",
          isVelocityUser = true,
          velocityLevel = Some("admin"),
          isDefaultContact = Some(true),
          createdBy = "test_user",
          createdAt = LocalDateTime.now),
        BUApplicPrimaryContactToInsert(
          applicationId = 2,
          contactType = "security officer",
          name = "David",
          middleName = None,
          surname = "Salgado",
          phoneNumber = "+971540912395",
          email = "d.salgado@pegb.tech",
          idType = "Emirates ID",
          isVelocityUser = false,
          velocityLevel = None,
          isDefaultContact = Some(true),
          createdBy = "test_user",
          createdAt = LocalDateTime.now))

      val result = dao.insert(mockData)(Some(conn))
      dao.endTransaction(conn)

      result.isRight mustBe true
      result.right.get.size mustBe 4
    }

    "unable to insert business_user_application_primary_contacts if transaction is not ended" in {

      val conn = dao.startTransaction.right.get

      val mockData = Seq(
        BUApplicPrimaryContactToInsert(
          applicationId = 3,
          contactType = "business owner",
          name = "Narek",
          middleName = None,
          surname = "Hakobyan",
          phoneNumber = "+97154445679",
          email = "n.hakobyan@pegb.tech",
          idType = "Emirates ID",
          isVelocityUser = true,
          velocityLevel = Some("admin"),
          isDefaultContact = Some(true),
          createdBy = "test_user",
          createdAt = LocalDateTime.now))

      val result = dao.insert(mockData)(Some(conn))
      //dao.endTransaction(conn)

      result.isRight mustBe true
      val confirm = dao.getByApplicationId(mockData.head.applicationId)
      confirm.right.get.isEmpty mustBe true
    }

    "get business_user_application_primary_contacts rows" in {
      val result = dao.getByApplicationId(1)
      result.isRight mustBe true
      result.right.get.size mustBe 3
      result.right.get.map(_.name).toSet mustBe Set("Lloyd", "George", "Ujali")
    }

    "delete business_user_application_primary_contacts rows" in {

      val conn = dao.startTransaction.right.get

      val result = dao.deleteByApplicationId(1)(Some(conn))
      dao.endTransaction(conn)
      val confirmed = dao.getByApplicationId(1)

      result.isRight mustBe true
      confirmed.right.get mustBe Nil
    }

    "unable to delete business_user_application_primary_contacts if transaction is not ended" in {
      val conn = dao.startTransaction.right.get

      val result = dao.deleteByApplicationId(2)(Some(conn))
      //dao.endTransaction(conn)
      val confirmed = dao.getByApplicationId(2)

      result.isRight mustBe true
      confirmed.right.get.isEmpty mustBe false
    }
  }
}
