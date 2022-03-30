package tech.pegb.backoffice.dao.businessuserapplication

import java.time._
import java.util.UUID

import cats.implicits._
import tech.pegb.backoffice.dao.Dao.UUIDEntityId
import tech.pegb.backoffice.dao.businessuserapplication.abstraction.BusinessUserApplicationDao
import tech.pegb.backoffice.dao.businessuserapplication.dto.{BusinessUserApplicationCriteria, BusinessUserApplicationToInsert, BusinessUserApplicationToUpdate}
import tech.pegb.backoffice.dao.businessuserapplication.entity.BusinessUserApplication
import tech.pegb.backoffice.dao.model
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes, Ordering, OrderingSet}
import tech.pegb.core.PegBTestApp

class BusinessUserApplicationDaoSpec extends PegBTestApp {

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  private val dao = inject[BusinessUserApplicationDao]

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
       |INSERT INTO business_user_applications (id,uuid,business_name,brand_name,business_category,stage,status,user_tier, business_type, registration_number, tax_number, registration_date, created_by,updated_by,created_at,updated_at) VALUES
       |('$buaId1','$buaUUID1','Universal Catering Co','Costa Coffee DSO','Restaurants - 5182','identity_info','ongoing', 'basic', 'merchant', '212/212EE', 'B12342M', '2019-01-01','system','system','$now','$now'),
       |('$buaId2','$buaUUID2','Tindahan ni Aling Nena','Nenas Store','Store - 1111','application_documents','rejected', 'basic', 'merchant', '111/212EE', 'A12342M', '1990-01-01','system','system','$now','$now'),
       |('$buaId3','$buaUUID3','Henry Sy and kids','SM Megamall','Department Store - 9999','application_documents','pending', 'basic', 'merchant', '999/212EE', 'C12342M', '1999-01-01','system','system','$now','$now');
       |
       |INSERT INTO business_user_application_primary_contacts
       |(id, uuid, application_id, contact_type, name, middle_name, surname, phone_number, email, id_type, is_velocity_user, velocity_level, is_default_contact, created_by,updated_by,created_at,updated_at)
       |VALUES
       |('1', uuid(), '1', 'type1', 'Salih', null, 'Dursuntas', '0542165308', 's.dursuntas@pegb.tech', 'type1', '1', 'admin', '1', 'system','system','$now','$now'),
       |('2', uuid(), '1', 'type2', 'George', null, 'Ogalo', '0544415630', 'g.ogalo@pegb.tech', 'type1', '1', 'admin', '1', 'system','system','$now','$now'),
       |('3', uuid(), '2', 'type3', 'Lloyd', null, 'Edano', '0564410974', 'o.lloyd@pegb.tech', 'type1', '1', 'admin', '1', 'system','system','$now','$now');
       |
       |""".stripMargin

  "BusinessUserApplicationDao " should {
    val bua1 = BusinessUserApplication(
      id = buaId1,
      uuid = buaUUID1.toString,
      businessName = "Universal Catering Co",
      brandName = "Costa Coffee DSO",
      businessCategory = "Restaurants - 5182",
      stage = "identity_info",
      status = "ongoing",
      userTier = "basic",
      businessType = "merchant",
      registrationNumber = "212/212EE",
      taxNumber = "B12342M".some,
      registrationDate = LocalDate.of(2019, 1, 1).some,
      explanation = None,
      userId = None,
      submittedBy = None,
      submittedAt = None,
      checkedBy = None,
      checkedAt = None,
      createdBy = "system",
      createdAt = now,
      updatedBy = "system".some,
      updatedAt = now.some)

    val bua2 = BusinessUserApplication(
      id = buaId2,
      uuid = buaUUID2.toString,
      businessName = "Tindahan ni Aling Nena",
      brandName = "Nenas Store",
      businessCategory = "Store - 1111",
      stage = "application_documents",
      status = "rejected",
      userTier = "basic",
      businessType = "merchant",
      registrationNumber = "111/212EE",
      taxNumber = "A12342M".some,
      registrationDate = LocalDate.of(1990, 1, 1).some,
      explanation = None,
      userId = None,
      submittedBy = None,
      submittedAt = None,
      checkedBy = None,
      checkedAt = None,
      createdBy = "system",
      createdAt = now,
      updatedBy = "system".some,
      updatedAt = now.some)

    val bua3 = BusinessUserApplication(
      id = buaId3,
      uuid = buaUUID3.toString,
      businessName = "Henry Sy and kids",
      brandName = "SM Megamall",
      businessCategory = "Department Store - 9999",
      stage = "application_documents",
      status = "pending",
      userTier = "basic",
      businessType = "merchant",
      registrationNumber = "999/212EE",
      taxNumber = "C12342M".some,
      registrationDate = LocalDate.of(1999, 1, 1).some,
      explanation = None,
      userId = None,
      submittedBy = None,
      submittedAt = None,
      checkedBy = None,
      checkedAt = None,
      createdBy = "system",
      createdAt = now,
      updatedBy = "system".some,
      updatedAt = now.some)

    "return all business user application in countBusinessUserApplicationByCriteria all" in {
      val criteria = BusinessUserApplicationCriteria()

      val orderingSet = OrderingSet(Ordering("business_name", Ordering.DESC)).some
      val resp = dao.countBusinessUserApplicationByCriteria(criteria)

      resp mustBe Right(3)
    }
    "return all business user application in countBusinessUserApplicationByCriteria stage" in {
      val criteria = BusinessUserApplicationCriteria(
        stage = model.CriteriaField("", "application_documents").some)

      val resp = dao.countBusinessUserApplicationByCriteria(criteria)

      resp mustBe Right(2)
    }
    "return all business user application in getBusinessUserApplicationByCriteria all" in {
      val criteria = BusinessUserApplicationCriteria()

      val orderingSet = OrderingSet(Ordering("business_name", Ordering.DESC)).some
      val resp = dao.getBusinessUserApplicationByCriteria(criteria, orderingSet, none, none)

      resp mustBe Right(Seq(bua1, bua2, bua3))
    }
    "return all business user application in getBusinessUserApplicationByCriteria stage" in {
      val criteria = BusinessUserApplicationCriteria(
        stage = model.CriteriaField("", "application_documents").some)

      val orderingSet = OrderingSet(Ordering("registration_date", Ordering.ASC)).some
      val resp = dao.getBusinessUserApplicationByCriteria(criteria, orderingSet, none, none)

      resp mustBe Right(Seq(bua2, bua3))
    }
    "return all business user application in getBusinessUserApplicationByCriteria status" in {
      val criteria = BusinessUserApplicationCriteria(
        status = model.CriteriaField("", "pending").some)

      val orderingSet = OrderingSet(Ordering("registration_date", Ordering.ASC)).some
      val resp = dao.getBusinessUserApplicationByCriteria(criteria, orderingSet, none, none)

      resp mustBe Right(Seq(bua3))
    }
    "return all business user application in getBusinessUserApplicationByCriteria business name" in {
      val criteria = BusinessUserApplicationCriteria(
        businessName = model.CriteriaField("", "nena", MatchTypes.Partial).some)

      val orderingSet = OrderingSet(Ordering("registration_date", Ordering.ASC)).some
      val resp = dao.getBusinessUserApplicationByCriteria(criteria, orderingSet, none, none)

      resp mustBe Right(Seq(bua2))
    }
    "return all business user application in getBusinessUserApplicationByCriteria brand name" in {
      val criteria = BusinessUserApplicationCriteria(
        brandName = model.CriteriaField("", "costa", MatchTypes.Partial).some)

      val orderingSet = OrderingSet(Ordering("registration_date", Ordering.ASC)).some
      val resp = dao.getBusinessUserApplicationByCriteria(criteria, orderingSet, none, none)

      resp mustBe Right(Seq(bua1))
    }
    "return all business user application in getBusinessUserApplicationByCriteria isActive" in {
      val criteria = BusinessUserApplicationCriteria(
        isActive = model.CriteriaField("", true).some)

      val orderingSet = OrderingSet(Ordering("registration_date", Ordering.ASC)).some
      val resp = dao.getBusinessUserApplicationByCriteria(criteria, orderingSet, none, none)

      resp mustBe Right(Seq(bua3, bua1))
    }
    "return all business user application in getBusinessUserApplicationByCriteria by businessCategory" in {
      val criteria = BusinessUserApplicationCriteria(
        businessCategory = model.CriteriaField("", "Restaurants - 5182").some)

      val resp = dao.getBusinessUserApplicationByCriteria(criteria, none, none, none)

      resp mustBe Right(Seq(bua1))
    }
    "return all business user application in getBusinessUserApplicationByCriteria by uuid" in {
      val criteria = BusinessUserApplicationCriteria(
        uuid = model.CriteriaField("", buaUUID1.toString).some)

      val resp = dao.getBusinessUserApplicationByCriteria(criteria, none, none, none)

      resp mustBe Right(Seq(bua1))
    }
    "return all business user application in getBusinessUserApplicationByCriteria registration_date" in {
      val criteria = BusinessUserApplicationCriteria(
        registrationDate = model.CriteriaField[(LocalDate, LocalDate)](
          "", (LocalDate.of(1998, 1, 1), LocalDate.of(2020, 1, 1)), MatchTypes.InclusiveBetween).some)

      val orderingSet = OrderingSet(Ordering("registration_date", Ordering.ASC)).some
      val resp = dao.getBusinessUserApplicationByCriteria(criteria, orderingSet, none, none)

      resp mustBe Right(Seq(bua3, bua1))
    }

    "return business user application by partial match in phone_number" in {
      val criteria = BusinessUserApplicationCriteria(
        contactsPhoneNumber = Some(CriteriaField[String]("phone_number", "054", MatchTypes.Partial)))
      val resp = dao.getBusinessUserApplicationByCriteria(criteria, none, none, none)
      resp.right.get.map(b ⇒ (b.businessName, b.brandName)) mustBe Seq(("Universal Catering Co", "Costa Coffee DSO"))
    }

    "return business user application by partial match in email" in {
      val criteria = BusinessUserApplicationCriteria(
        contactsEmail = Some(CriteriaField[String]("email", "pegb.tech", MatchTypes.Partial)))
      val resp = dao.getBusinessUserApplicationByCriteria(criteria, none, none, none)
      resp.right.get.map(b ⇒ (b.businessName, b.brandName)) mustBe Seq(("Universal Catering Co", "Costa Coffee DSO"), ("Tindahan ni Aling Nena", "Nenas Store"))
    }

    "return created BusinessUserApplication in insertBusinessUserApplication" in {
      val dto = BusinessUserApplicationToInsert(
        uuid = UUID.randomUUID().toString,
        businessName = "Cupaghawa",
        brandName = "Cupaghawa DSO",
        businessCategory = "Restautrants - 5182",
        stage = "Identity Info",
        status = "Ongoing",
        userTier = "Basic",
        businessType = "Merchant",
        registrationNumber = "213/564654EE",
        taxNumber = "A213546468977M".some,
        registrationDate = LocalDate.of(1996, 1, 1).some,
        createdBy = "pegbuser",
        createdAt = now)

      val resp = dao.insertBusinessUserApplication(dto)

      val actual = resp.right.get

      actual.uuid mustBe dto.uuid
      actual.businessName mustBe dto.businessName
      actual.brandName mustBe dto.brandName
      actual.businessCategory mustBe dto.businessCategory
      actual.stage mustBe dto.stage
      actual.status mustBe dto.status
      actual.userTier mustBe dto.userTier
      actual.businessType mustBe dto.businessType
      actual.registrationNumber mustBe dto.registrationNumber
      actual.taxNumber mustBe dto.taxNumber
      actual.registrationDate mustBe dto.registrationDate
      actual.createdAt mustBe dto.createdAt
      actual.createdBy mustBe dto.createdBy
      actual.updatedAt mustBe dto.createdAt.some
      actual.updatedBy mustBe dto.createdBy.some
    }

    "return updated BusinessUserApplication in updateBusinessUserApplication all" in {
      val dto = BusinessUserApplicationToUpdate(
        businessName = "Hermes".some,
        brandName = "Hermes Dubai Mall".some,
        businessCategory = "Store - 1111".some,
        userTier = "Basic".some,
        businessType = "Merchant".some,
        registrationNumber = "188/1111EF".some,
        taxNumber = "B999999M".some,
        registrationDate = LocalDate.of(1980, 1, 1).some,
        explanation = "just explanation".some,
        stage = "Application Documents".some,
        status = "submitted".some,
        submittedBy = "pegbuser".some,
        submittedAt = now.some,
        checkedBy = "checker".some,
        checkedAt = now.some,
        userId = 1.some,
        updatedBy = "checker",
        updatedAt = now,
        lastUpdatedAt = now.some)

      val resp = dao.updateBusinessUserApplication(UUIDEntityId(buaUUID1), dto)(None)

      val actual = resp.right.get.get

      actual.businessName mustBe dto.businessName.get
      actual.brandName mustBe dto.brandName.get
      actual.businessCategory mustBe dto.businessCategory.get
      actual.stage mustBe dto.stage.get
      actual.status mustBe dto.status.get
      actual.userTier mustBe dto.userTier.get
      actual.businessType mustBe dto.businessType.get
      actual.registrationNumber mustBe dto.registrationNumber.get
      actual.taxNumber mustBe dto.taxNumber
      actual.registrationDate mustBe dto.registrationDate
      actual.explanation mustBe dto.explanation
      actual.submittedBy mustBe dto.submittedBy
      actual.submittedAt mustBe dto.submittedAt
      actual.checkedBy mustBe dto.checkedBy
      actual.checkedAt mustBe dto.checkedAt
      actual.userId mustBe dto.userId
      actual.updatedAt mustBe dto.updatedAt.some
      actual.updatedBy mustBe dto.updatedBy.some
    }

    "return updated BusinessUserApplication in updateBusinessUserApplication update stage and status" in {
      val dto = BusinessUserApplicationToUpdate(
        stage = "Application Documents".some,
        status = "rejected".some,
        checkedBy = "martin".some,
        checkedAt = now.some,
        updatedBy = "martin",
        updatedAt = now,
        lastUpdatedAt = now.some)

      val resp = dao.updateBusinessUserApplication(UUIDEntityId(buaUUID1), dto)(None)

      val actual = resp.right.get.get

      actual.businessName mustBe "Hermes"
      actual.stage mustBe dto.stage.get
      actual.status mustBe dto.status.get
      actual.checkedBy mustBe dto.checkedBy
      actual.checkedAt mustBe dto.checkedAt
      actual.updatedAt mustBe dto.updatedAt.some
      actual.updatedBy mustBe dto.updatedBy.some
    }

    "return updated BusinessUserApplication in updateBusinessUserApplication update submitted_by and submitted_at" in {
      val dto = BusinessUserApplicationToUpdate(
        status = "submitted".some,
        submittedBy = "odersky".some,
        submittedAt = now.some,
        updatedBy = "odersky",
        updatedAt = now,
        lastUpdatedAt = now.some)

      val resp = dao.updateBusinessUserApplication(UUIDEntityId(buaUUID1), dto)(None)

      val actual = resp.right.get.get

      actual.businessName mustBe "Hermes"
      actual.status mustBe dto.status.get
      actual.submittedBy mustBe dto.submittedBy
      actual.submittedAt mustBe dto.submittedAt
      actual.updatedAt mustBe dto.updatedAt.some
      actual.updatedBy mustBe dto.updatedBy.some
    }

  }

}
