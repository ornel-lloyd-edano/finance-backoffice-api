package tech.pegb.backoffice.dao

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import cats.data.NonEmptyList
import tech.pegb.backoffice.dao.businessuserapplication.abstraction.BusinessUserApplicationConfigDao
import tech.pegb.backoffice.dao.businessuserapplication.dto.{AccountConfigToInsert, ExternalAccountToInsert, TransactionConfigToInsert}
import tech.pegb.backoffice.util.AppConfig
import tech.pegb.core.PegBTestApp

class BusinessUserApplicationConfigDaoSpec extends PegBTestApp {

  val config = inject[AppConfig]

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  private val dao = inject[BusinessUserApplicationConfigDao]

  val buaId1 = 1
  val buaId2 = 2
  val buaId3 = 3
  val buaUUID1 = UUID.randomUUID()
  val buaUUID2 = UUID.randomUUID()
  val buaUUID3 = UUID.randomUUID()

  override def initSql =
    s"""
       |INSERT INTO currencies(id, currency_name, description, created_at, updated_at, is_active)
       |VALUES
       |('1', 'AED', 'default currency for UAE', now(), null, 1),
       |('2', 'KES', 'default currency for KENYA', now(), null, 1);
       |
       |INSERT INTO business_user_applications (id,uuid,business_name,brand_name,business_category,stage,status,user_tier, business_type, registration_number, tax_number, registration_date, created_by,updated_by,created_at,updated_at) VALUES
       |('$buaId1','$buaUUID1','Universal Catering Co','Costa Coffee DSO','Restaurants - 5182','identity_info','ongoing', 'basic', 'merchant', '212/212EE', 'B12342M', '2019-01-01','system','system','$now','$now'),
       |('$buaId2','$buaUUID2','Tindahan ni Aling Nena','Nenas Store','Store - 1111','application_documents','approved', 'basic', 'merchant', '111/212EE', 'A12342M', '1990-01-01','system','system','$now','$now'),
       |('$buaId3','$buaUUID3','Henry Sy and kids','SM Megamall','Department Store - 9999','application_documents','pending', 'basic', 'merchant', '999/212EE', 'C12342M', '1999-01-01','system','system','$now','$now');
       |
       |INSERT INTO business_user_application_txn_configs (id, uuid, application_id, txn_type, currency_id, created_by, created_at, updated_by, updated_at)
       |VALUES
       |('1', 'e0b16794-32c0-4975-b1e4-1448fe5dd9fb', '$buaId1', 'merchant_payment', 1, 'pegbuser', '$now', 'pegbuser', '$now'),
       |('2', 'bd1c49f6-00d7-4bb4-8ca8-66c9d6158ab0', '$buaId1', 'cashout', 1, 'pegbuser', '$now', 'pegbuser', '$now');
       |
       |INSERT INTO business_user_application_account_configs (id, uuid, application_id, account_type, account_name, currency_id, is_default,  created_by, created_at, updated_by, updated_at)
       |VALUES
       |('1', '467e57ab-cf2d-4aff-b16b-edd71e9b39fd', '$buaId1', 'collection', 'Default Collection', 1, 1, 'pegbuser', '$now', 'pegbuser', '$now'),
       |('2', 'e3569831-5efb-483d-ab51-f3eb99eab317', '$buaId1', 'distribution', 'Default Distribution', 1, 1, 'pegbuser', '$now', 'pegbuser', '$now');
       |
       |INSERT INTO business_user_application_external_accounts (id, uuid, application_id, provider, account_number, account_holder, currency_id, created_by, created_at, updated_by, updated_at)
       |VALUES
       |('1', '11a3b118-26ec-43b2-8a58-5245255500d3', '$buaId1', 'mPesa', '955100', 'Coste Coffee FZE', 1, 'pegbuser', '$now', 'pegbuser', '$now'),
       |('2', 'd02deeb2-0d7c-4535-bff5-aec2f93f041a', '$buaId1', 'Pesalink', '+254701258963', 'George Ogalo', 1, 'pegbuser', '$now', 'pegbuser', '$now');
     """.stripMargin

  "BusinessUserApplicationConfigDao " should {

    "get txn config" in {
      val res = dao.getTxnConfig(buaId1)

      val actual = res.right.get.map(t ⇒ (t.transactionType, t.currencyId, t.currencyCode, t.createdBy)).toSet

      actual mustBe Set(("merchant_payment", 1, "AED", "pegbuser"), ("cashout", 1, "AED", "pegbuser"))
    }

    "get account config" in {
      val res = dao.getAccountConfig(buaId1)

      val actual = res.right.get.map(t ⇒ (t.accountType, t.accountName, t.currencyId, t.isDefault, t.currencyCode, t.createdBy)).toSet

      actual mustBe Set(("collection", "Default Collection", 1, true, "AED", "pegbuser"), ("distribution", "Default Distribution", 1, true, "AED", "pegbuser"))
    }

    "insert txnConfig" in {
      val txnConfig = NonEmptyList.of(
        TransactionConfigToInsert("merchant_payment", 2),
        TransactionConfigToInsert("cashin", 2))

      val res = dao.insertTxnConfig(buaId2, txnConfig, "alice", now)

      val actual = res.right.get.map(t ⇒ (t.transactionType, t.currencyId, t.currencyCode, t.createdBy)).toSet

      actual mustBe Set(("merchant_payment", 2, "KES", "alice"), ("cashin", 2, "KES", "alice"))
    }

    "insert accountConfig" in {
      val accountConfig = NonEmptyList.of(
        AccountConfigToInsert("collection", "default collection", 2, 1),
        AccountConfigToInsert("distribution", "default distribution", 2, 1))

      val res = dao.insertAccountConfig(buaId2, accountConfig, "alice", now)

      val actual = res.right.get.map(t ⇒ (t.accountType, t.accountName, t.currencyId, t.isDefault, t.currencyCode, t.createdBy)).toSet

      actual mustBe Set(("collection", "default collection", 2, true, "KES", "alice"), ("distribution", "default distribution", 2, true, "KES", "alice"))
    }

    "insert externalAccounts" in {
      val externalAccount = NonEmptyList.of(
        ExternalAccountToInsert("mPesa", "955100", "Coste Coffee FZE", 2),
        ExternalAccountToInsert("Pesalink", "+254701258963", "George Ogalo", 2))

      val res = dao.insertExternalAccount(buaId2, externalAccount, "alice", now)

      val actual = res.right.get.map(t ⇒ (t.provider, t.accountNumber, t.accountHolder, t.currencyId, t.currencyCode, t.createdBy)).toSet

      actual mustBe Set(
        ("mPesa", "955100", "Coste Coffee FZE", 2, "KES", "alice"),
        ("Pesalink", "+254701258963", "George Ogalo", 2, "KES", "alice"))
    }

  }
}
