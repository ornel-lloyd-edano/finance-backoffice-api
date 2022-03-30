package tech.pegb.backoffice.dao

import org.scalamock.scalatest.MockFactory
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes, OrderingSet}
import tech.pegb.backoffice.dao.provider.dto.ProviderCriteria
import tech.pegb.backoffice.dao.provider.sql.ProviderSqlDao
import tech.pegb.core.PegBTestApp

class ProvidersSqlDaoSpec extends PegBTestApp with MockFactory {

  override def initSql =
    s"""
       |INSERT INTO users(id, uuid, username, password, email, subscription, type, status, tier, segment, activated_at, password_updated_at, created_at, created_by, updated_at, updated_by)
       |VALUES
       |('1', 'bcc32571-cf16-4abc-ac38-38d58f9cbab5', null, null, null, null, 'provider', null, null, null, now(), null, now(), 'SuperAdmin', null, null);
       |
       |CREATE TABLE IF NOT EXISTS `providers` (
       |  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
       |  `user_id` int(10) unsigned NOT NULL,
       |  `service_id` int(10) unsigned DEFAULT NULL,
       |  `name` varchar(50)  NOT NULL,
       |  `transaction_type` varchar(50)  NOT NULL,
       |  `icon` varchar(50)  NOT NULL,
       |  `label` varchar(50)   NOT NULL,
       |  `pg_institution_id` int(10) unsigned NOT NULL,
       |  `utility_payment_type` varchar(20) DEFAULT NULL,
       |  `utility_min_payment_amount` decimal(10,2) DEFAULT '0.01',
       |  `utility_max_payment_amount` decimal(10,2) DEFAULT NULL,
       |  `is_active` tinyint(3) NOT NULL,
       |  `created_by` varchar(36)  NOT NULL DEFAULT 'core',
       |  `updated_by` varchar(36)  NOT NULL DEFAULT 'core',
       |  `created_at` datetime NOT NULL,
       |  `updated_at` datetime NOT NULL,
       |  PRIMARY KEY (`id`),
       |  FOREIGN KEY (`user_id`) REFERENCES `users` (`id`)
       |);
       |
       |INSERT INTO `providers`
       |(id, user_id, service_id, name, transaction_type, icon, label, pg_institution_id,
       |utility_payment_type, utility_min_payment_amount, utility_max_payment_amount, is_active,
       |created_by, updated_by, created_at, updated_at)
       |VALUES
       |('1', '1', null, 'family provider', 'remittance', 'not available', 'not available', '1',
       | null, '0.01', null, '1', 'test user', 'test user', now(), now()),
       |
       |('2', '1', null, 'internet service provider', 'cashin', 'not available', 'not available', '1',
       | null, '0.01', null, '1', 'test user', 'test user', now(), now()),
       |
       |('3', '1', null, 'healthcare provider', 'cashout', 'not available', 'not available', '1',
       | null, '0.01', null, '1', 'test user', 'test user', now(), now());
     """.stripMargin

  "ProvidersSqlDao" should {
    val dao = inject[ProviderSqlDao]

    "get provider by id" in {
      val result = dao.get(2)(None)
      result.right.get.map(_.name).contains("internet service provider") mustBe true
      result.right.get.map(_.transactionType).contains("cashin") mustBe true
    }

    "get providers by user uuid and order by name" in {
      val criteria = ProviderCriteria(userUuid = Some(CriteriaField[String]("uuid", "bcc32571-cf16-4abc-ac38-38d58f9cbab5")))
      val results = dao.getByCriteria(criteria, Some(OrderingSet("name", "ASC")), None, None)
      val expected = Seq("family provider", "healthcare provider", "internet service provider")
      results.right.get.map(_.name) mustBe expected
    }

    "filter providers by partial match on transaction type and order by name descending" in {
      val criteria = ProviderCriteria(transactionType = Some(CriteriaField[String]("transaction_type", "cash", MatchTypes.Partial)))
      val ordering = OrderingSet("name", "DESC")
      val results = dao.getByCriteria(criteria, Some(ordering), None, None)

      val expected = Seq("internet service provider", "healthcare provider")

      results.right.get.map(_.name) mustBe expected
    }

    "get all providers and paginate with limit 2 and offset 1" in {
      val results = dao.getByCriteria(ProviderCriteria(), None, Some(2), Some(1))

      val expected = Seq("internet service provider", "healthcare provider")

      results.right.get.map(_.name) mustBe expected
    }
  }

}
