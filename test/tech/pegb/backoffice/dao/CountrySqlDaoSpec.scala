package tech.pegb.backoffice.dao

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.businessuserapplication.abstraction.CountryDao
import tech.pegb.backoffice.dao.businessuserapplication.dto.CountryToUpsert
import tech.pegb.core.PegBTestApp

class CountrySqlDaoSpec extends PegBTestApp {

  override def initSql =
    s"""
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
       |('1', 'UAE', null, null, '1', '1', '2019-01-01 00:00:00', '2019-01-01 00:00:00'),
       |('2', 'United States', null, null, '1', '1', '2019-01-01 00:00:00', '2019-01-01 00:00:00'),
       |('3', 'Philippines', null, null, null, '1', '2019-01-01 00:00:00', '2019-01-01 00:00:00');
     """.stripMargin

  private val dao = inject[CountryDao]

  "CountrySqlDao" should {
    "insert new country and update existing country in upsertCountry" in {
      val countries = Seq(
        CountryToUpsert(
          name = "UAE",
          label = Some("United Arab Emirates"),
          createdBy = "test_user",
          createdAt = LocalDateTime.now,
          updatedBy = Some("new_user"),
          updatedAt = Some(LocalDateTime.of(2019, 2, 28, 0, 0, 0))),

        CountryToUpsert(
          name = "United States",
          isActive = Some(false),
          createdBy = "test_user",
          createdAt = LocalDateTime.now,
          updatedBy = Some("new_user"),
          updatedAt = Some(LocalDateTime.of(2019, 2, 28, 0, 0, 0))),

        CountryToUpsert(
          name = "Philippines",
          label = Some("Republic of the Philippines"),
          currencyId = Some(5),
          createdBy = "test_user",
          createdAt = LocalDateTime.now,
          updatedBy = Some("new_user"),
          updatedAt = Some(LocalDateTime.of(2019, 2, 28, 0, 0, 0))),

        CountryToUpsert(
          name = "Japan",
          currencyId = Some(3),
          isActive = Some(true),
          createdBy = "test_user",
          createdAt = LocalDateTime.now,
          updatedBy = Some("new_user"),
          updatedAt = Some(LocalDateTime.of(2019, 2, 28, 0, 0, 0))))
      val result = dao.upsertCountry(countries)
      result.isRight mustBe true

      dao.getCountries.right.get.find(_.name == "UAE").get.label mustBe Some("United Arab Emirates")
      dao.getCountries.right.get.find(_.name == "United States").get.isActive mustBe Some(false)
      dao.getCountries.right.get.find(_.name == "Philippines").get.currencyCode mustBe Some("PHP")
      dao.getCountries.right.get.find(_.name == "Japan").isDefined mustBe true

    }

    "get all countries" in {
      val result = dao.getCountries
      result.right.get.size mustBe 4
    }

  }

}
