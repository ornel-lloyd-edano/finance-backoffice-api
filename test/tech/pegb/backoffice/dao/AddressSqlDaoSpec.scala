package tech.pegb.backoffice.dao

import java.time.LocalDateTime
import java.time.temporal.{ChronoUnit}

import org.scalamock.scalatest.MockFactory
import tech.pegb.backoffice.dao.address.dto._
import tech.pegb.backoffice.dao.address.sql._
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes, OrderingSet}
import tech.pegb.core.PegBTestApp

class AddressSqlDaoSpec extends PegBTestApp with MockFactory {

  override def initSql =
    s"""
       |INSERT INTO users(id, uuid, username, password, email, subscription, type, status, tier, segment, activated_at, password_updated_at, created_at, created_by, updated_at, updated_by)
       |VALUES
       |('1', 'bcc32571-cf16-4abc-ac38-38d58f9cbab5', null, null, null, null, 'individual', null, null, null, now(), null, now(), 'SuperAdmin', null, null);
       |
       |INSERT INTO business_user_applications (id,uuid,business_name,brand_name,business_category,stage,status,user_tier, business_type, registration_number, tax_number, registration_date, created_by,updated_by,created_at,updated_at) VALUES
       |('1','fcad736b-a6d8-4b8e-845d-edb83489ac50','Universal Catering Co','Costa Coffee DSO','Restaurants - 5182','identity_info','ongoing', 'basic', 'merchant', '212/212EE', 'B12342M', '2019-01-01','system','system','2019-01-01 00:00:00','2019-01-01 00:00:00'),
       |('2','926551f7-84f6-459e-bf15-a76d07d1b712','Tindahan ni Aling Nena','Nenas Store','Store - 1111','application_documents','approved', 'basic', 'merchant', '111/212EE', 'A12342M', '1990-01-01','system','system','2019-01-01 00:00:00','2019-01-01 00:00:00');
       |
       |
       |INSERT INTO currencies
       |(id, currency_name, description, created_at, created_by, is_active, icon)
       |VALUES
       |('1', 'KES', 'Kenya Shillings', '2019-01-01 00:00:00', 'test_user', 1, 'icon_one'),
       |('2', 'USD', 'US Dollars', '2019-01-01 00:00:00', 'test_user', 1, 'icon_two'),
       |('3', 'YEN', 'Japanese Yen', '2019-01-01 00:00:00', 'test_user', 1, 'icon_3'),
       |('4', 'EUR', 'Euro', '2019-01-01 00:00:00', 'test_user', 1, 'icon_4'),
       |('5', 'PHP', 'Philippine Peso', '2019-01-01 00:00:00', 'test_user', 1, 'icon_5');
       |
       |CREATE TABLE `countries` (
       |  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
       |  `name` varchar(100) NOT NULL,
       |  `label` varchar(100) DEFAULT NULL,
       |  `icon` varchar(50) DEFAULT NULL,
       |  `is_active` tinyint(3) DEFAULT NULL,
       |  `created_at` datetime NOT NULL,
       |  `updated_at` datetime NOT NULL,
       |  `currency_id` tinyint(3) unsigned DEFAULT NULL,
       |  PRIMARY KEY (`id`),
       |  UNIQUE KEY `name` (`name`),
       |  KEY `FK_countries_currency_id_currencies_id` (`currency_id`),
       |  CONSTRAINT `FK_countries_currency_id_currencies_id` FOREIGN KEY (`currency_id`) REFERENCES `currencies` (`id`)
       |);
       |
       |INSERT INTO countries
       |(id, name, label, icon, currency_id, is_active, created_at, updated_at)
       |VALUES
       |('1', 'United Arab Emirates', null, null, '1', '1', '2019-01-01 00:00:00', '2019-01-01 00:00:00'),
       |('2', 'United States', null, null, '1', '1', '2019-01-01 00:00:00', '2019-01-01 00:00:00'),
       |('3', 'Philippines', null, null, null, '1', '2019-01-01 00:00:00', '2019-01-01 00:00:00');
       |
       |CREATE TABLE `addresses` (
       |  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
       |  `uuid` varchar(36) NOT NULL,
       |  `bu_application_id` int(10) unsigned DEFAULT NULL,
       |  `user_id` int(10) unsigned DEFAULT NULL,
       |  `address_type` varchar(30) NOT NULL,
       |  `country_id` int(10) unsigned NOT NULL,
       |  `city` varchar(50) NOT NULL,
       |  `postal_code` varchar(10) DEFAULT NULL,
       |  `address` varchar(500) NOT NULL,
       |  `coordinate_x` decimal(8,5) DEFAULT NULL,
       |  `coordinate_y` decimal(8,5) DEFAULT NULL,
       |  `created_by` varchar(50) NOT NULL,
       |  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
       |  `updated_by` varchar(50) DEFAULT NULL,
       |  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
       |  `is_active` tinyint unsigned DEFAULT '1',
       |  PRIMARY KEY (`id`),
       |  FOREIGN KEY (`country_id`) REFERENCES `countries` (`id`),
       |  FOREIGN KEY(bu_application_id) REFERENCES business_user_applications(id),
       |  FOREIGN KEY(user_id) REFERENCES users(id)
       |);
       |
       |INSERT INTO addresses
       |(id, uuid, bu_application_id, user_id, address_type, country_id, city, postal_code, address, coordinate_x,
       |coordinate_y, created_by, created_at, updated_by, updated_at, is_active)
       |VALUES
       |('1', 'c798925e-8db3-4002-a31d-9f5bae104a7b', '1', null, 'type1','1','Dubai',
       |'0000','Al Bafta Grand Deira Flat 102 Muraqabat Road', '101.20123', '34.21201', 'tester', now(), null, null, '1'),
       |
       |('2', 'f11bd8be-bda6-4a49-884d-755f93c6ec84', null, '1', 'type2', '3', 'Manila', '1234', 'Mother Ignacia Street Quezon City',
       |'-80.2122', '100.23111', 'tester', now(), 'tester', now(), '1');
     """.stripMargin

  "AddressSqlDao" should {
    val dao = inject[AddressSqlDao]

    "get address by uuid" in {
      val result = dao.get("f11bd8be-bda6-4a49-884d-755f93c6ec84")(None)
      result.right.get.map(_.countryName).contains("Philippines") mustBe true
      result.right.get.map(_.city).contains("Manila") mustBe true
      result.right.get.flatMap(_.address).contains("Mother Ignacia Street Quezon City") mustBe true
      result.right.get.flatMap(_.postalCode).contains("1234") mustBe true
    }

    "insert address" in {
      val dto = AddressToInsert(
        uuid = "c7e0f14c-4904-4abd-8811-c4ef2ab24a12",
        userUuid = "bcc32571-cf16-4abc-ac38-38d58f9cbab5",
        addressType = "type AB",
        country = "Philippines",
        city = "Quezon City",
        postalCode = Some("0200"),
        address = Some("EDSA corner Timog Avenue Diliman"),
        coordinateX = Some(BigDecimal(123.1234)),
        coordinateY = Some(BigDecimal(29.00112)),
        createdBy = "test_user",
        createdAt = LocalDateTime.now.truncatedTo(ChronoUnit.DAYS),
        isActive = true)

      val result = dao.insert(dto)(None)
      result.right.get.uuid == dto.uuid
      result.right.get.countryName == "Philippines"
      result.right.get.city == "Quezon City"
      result.right.get.address == Some("EDSA corner Timog Avenue Diliman")
    }

    "update address" in {
      val dto = AddressToUpdate(
        countryId = Some(1),
        address = Some("Maligawkasana Street Sito Walangpatutunguhan"),
        coordinateX = Some(BigDecimal(99.9999)),
        coordinateY = Some(BigDecimal(55.5555)),
        updatedBy = "unit_test",
        updatedAt = LocalDateTime.now,
        lastUpdatedAt = Some(LocalDateTime.now.truncatedTo(ChronoUnit.DAYS)))
      val result = dao.update("c7e0f14c-4904-4abd-8811-c4ef2ab24a12", dto)

      result.right.map(_.map(_.countryName)) mustBe Right(Some("United Arab Emirates"))
      result.right.get.map(_.uuid) mustBe Some("c7e0f14c-4904-4abd-8811-c4ef2ab24a12")
      result.right.get.flatMap(_.address) mustBe Some("Maligawkasana Street Sito Walangpatutunguhan")
      result.right.get.flatMap(_.coordinateX) mustBe Some(BigDecimal(99.9999))
      result.right.get.flatMap(_.coordinateY) mustBe Some(BigDecimal(55.5555))
    }

    "fail to update address if last_updated_at is not equal to updated_at" in {
      val dto = AddressToUpdate(
        city = Some("Kahit Saan City"),
        updatedBy = "unit_test",
        updatedAt = LocalDateTime.now,
        lastUpdatedAt = Some(LocalDateTime.now.truncatedTo(ChronoUnit.DAYS)))
      val result = dao.update("c7e0f14c-4904-4abd-8811-c4ef2ab24a12", dto)

      result.left.get.message mustBe "Update failed. Address with id [c7e0f14c-4904-4abd-8811-c4ef2ab24a12] has been modified by another process."
    }

    "get address by user uuid" in {
      val criteria = AddressCriteria(userUuid = Some(CriteriaField[String]("users.uuid", "bcc32571-cf16-4abc-ac38-38d58f9cbab5")))
      val results = dao.getByCriteria(criteria, None, None, None)
      val expected = Seq(
        ("f11bd8be-bda6-4a49-884d-755f93c6ec84", "Manila"),
        ("c7e0f14c-4904-4abd-8811-c4ef2ab24a12", "Quezon City"))
      results.right.get.map(c ⇒ (c.uuid, c.city)) mustBe expected
    }

    "get address by business application uuid" in {
      val criteria = AddressCriteria(buApplicationUuid = Some(CriteriaField[String]("business_user_applications.uuid", "fcad736b-a6d8-4b8e-845d-edb83489ac50")))
      val results = dao.getByCriteria(criteria, None, None, None)
      val expected = Seq(
        ("c798925e-8db3-4002-a31d-9f5bae104a7b", Some("Al Bafta Grand Deira Flat 102 Muraqabat Road")))
      results.right.get.map(c ⇒ (c.uuid, c.address)) mustBe expected
    }

    "filter address by partial match on address and order by country ascending" in {
      val criteria = AddressCriteria(address = Some(CriteriaField[String]("address", "Street", MatchTypes.Partial)))
      val ordering = OrderingSet(Seq("country_name" → "ASC"))
      val results = dao.getByCriteria(criteria, Some(ordering), None, None)

      val expected = Seq(
        ("f11bd8be-bda6-4a49-884d-755f93c6ec84", "Manila"),
        ("c7e0f14c-4904-4abd-8811-c4ef2ab24a12", "Quezon City"))

      results.right.get.map(c ⇒ (c.uuid, c.city)) mustBe expected
    }

    "get all address and paginate with limit 1 and offset 1" in {
      val results = dao.getByCriteria(AddressCriteria(), None, Some(1), Some(1))

      val expected = Seq(
        ("f11bd8be-bda6-4a49-884d-755f93c6ec84", "Manila"))

      results.right.get.map(c ⇒ (c.uuid, c.city)) mustBe expected
    }
  }

}
