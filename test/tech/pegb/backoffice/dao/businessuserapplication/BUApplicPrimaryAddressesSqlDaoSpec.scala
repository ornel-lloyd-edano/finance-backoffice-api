package tech.pegb.backoffice.dao.businessuserapplication

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.businessuserapplication.dto.BUApplicPrimaryAddressToInsert
import tech.pegb.backoffice.dao.businessuserapplication.sql.BUApplicPrimaryAddressesSqlDao
import tech.pegb.core.PegBTestApp

class BUApplicPrimaryAddressesSqlDaoSpec extends PegBTestApp {

  val dao = inject[BUApplicPrimaryAddressesSqlDao]

  override def initSql =
    s"""
       |INSERT INTO business_user_applications (id,uuid,business_name,brand_name,business_category,stage,status,user_tier, business_type, registration_number, tax_number, registration_date, created_by,updated_by,created_at,updated_at) VALUES
       |('1','b6239cf9-9a07-4b7f-974f-4f130d7927df','Universal Catering Co','Costa Coffee DSO','Restaurants - 5182','identity_info','ongoing', 'basic', 'merchant', '212/212EE', 'B12342M', '2019-01-01','system','system','2019-01-01 00:00:00','2019-01-01 00:00:00'),
       |('2','4483d05f-5ed0-43bb-ad6d-7bd6555d77ce','Tindahan ni Aling Nena','Nenas Store','Store - 1111','application_documents','approved', 'basic', 'merchant', '111/212EE', 'A12342M', '1990-01-01','system','system','2019-01-01 00:00:00','2019-01-01 00:00:00'),
       |('3','3f832e6c-3c50-44b4-8dca-4978308239e3','Henry Sy and kids','SM Megamall','Department Store - 9999','application_documents','pending', 'basic', 'merchant', '999/212EE', 'C12342M', '1999-01-01','system','system','2019-01-01 00:00:00','2019-01-01 00:00:00');
       |
       |create table business_user_application_primary_addresses
       |(
       |	id             int unsigned                       auto_increment,
       |	uuid           varchar(36)                        not null,
       |	application_id int unsigned                       not null,
       |	address_type   varchar(30)                        not null,
       |	country_id     int unsigned                       not null,
       |	city           varchar(50)                        not null,
       |	postal_code    varchar(10)                        null,
       |	address        varchar(500)                       null,
       |	coordinate_x   decimal(8,5)                       null,
       |	coordinate_y   decimal(8,5)                       null,
       |	created_by     varchar(50)                        not null,
       |	created_at     datetime default CURRENT_TIMESTAMP null,
       |	updated_by     varchar(50)                        null,
       |	updated_at datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
       |  PRIMARY KEY (`id`),
       |  UNIQUE KEY (`uuid`),
       |  KEY `FK_bu_app_primary_addresses_application_id_bu_applications_id` (`application_id`),
       |  CONSTRAINT `FK_bu_app_primary_addresses_application_id_bu_applications_id` FOREIGN KEY (`application_id`) REFERENCES `business_user_applications` (`id`) ON DELETE NO ACTION ON UPDATE NO ACTION
       |
       |);
       |""".stripMargin

  "BUApplicPrimaryAddressesSqlDao" should {
    "insert business_user_application_primary_addresses rows" in {

      val conn = dao.startTransaction.right.get

      val mockData = Seq(
        BUApplicPrimaryAddressToInsert(
          applicationId = 1,
          addressType = "Primary Address",
          countryId = 1,
          city = "Nairobi",
          postalCode = Some("10100"),
          address = Some("Business center No. 17"),
          coordinateX = Some(-1.29044),
          coordinateY = Some(36.816472),
          createdBy = "test_user",
          createdAt = LocalDateTime.now),
        BUApplicPrimaryAddressToInsert(
          applicationId = 2,
          addressType = "Primary Address",
          countryId = 2,
          city = "Delhi",
          postalCode = Some("201301"),
          address = Some("Business center No. 17"),
          coordinateX = None,
          coordinateY = None,
          createdBy = "ujali",
          createdAt = LocalDateTime.now),
        BUApplicPrimaryAddressToInsert(
          applicationId = 1,
          addressType = "Secondary Address",
          countryId = 2,
          city = "Bangalore",
          postalCode = Some("10105"),
          address = Some("Business center No. 17"),
          coordinateX = None,
          coordinateY = None,
          createdBy = "test_user",
          createdAt = LocalDateTime.now))

      val result = dao.insert(mockData)(Some(conn))
      dao.endTransaction(conn)
      result.isRight mustBe true
      result.right.get.size mustBe 3
      result.right.get.map(_.city).toSet mustBe Set("Nairobi", "Delhi", "Bangalore")

    }

    "unable to insert business_user_application_primary_addresses if transaction is not ended" in {

      val conn = dao.startTransaction.right.get

      val mockData = Seq(
        BUApplicPrimaryAddressToInsert(
          applicationId = 3,
          addressType = "Primary Address",
          countryId = 1,
          city = "Manila",
          postalCode = Some("10223"),
          address = Some("Business center No. 19"),
          coordinateX = Some(-1.29044),
          coordinateY = Some(36.816472),
          createdBy = "test_user",
          createdAt = LocalDateTime.now))

      val result = dao.insert(mockData)(Some(conn))
      //dao.endTransaction(conn)
      result.isRight mustBe true
      val confirm = dao.getByApplicationId(mockData.head.applicationId)
      confirm.right.get.isEmpty mustBe true
    }

    "get business_user_application_primary_addresses rows" in {
      val result = dao.getByApplicationId(1)

      result.isRight mustBe true
      result.right.get.size mustBe 2
      result.right.get.map(_.city).toSet mustBe Set("Nairobi", "Bangalore")
    }

    "delete business_user_application_primary_addresses rows" in {

      val conn = dao.startTransaction.right.get

      val result = dao.deleteByApplicationId(1)(Some(conn))
      dao.endTransaction(conn)
      val confirmed = dao.getByApplicationId(1)

      result.isRight mustBe true
      confirmed.right.get mustBe Nil
    }

    "unable to delete business_user_application_primary_addresses if transaction is not ended" in {
      val conn = dao.startTransaction.right.get

      val result = dao.deleteByApplicationId(2)(Some(conn))
      //dao.endTransaction(conn)
      val confirmed = dao.getByApplicationId(2)

      result.isRight mustBe true
      confirmed.right.get.isEmpty mustBe false
    }
  }
}
