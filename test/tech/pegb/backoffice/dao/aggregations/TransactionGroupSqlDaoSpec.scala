package tech.pegb.backoffice.dao.aggregations

import org.scalamock.scalatest.MockFactory
import tech.pegb.backoffice.dao.transaction.dto.{TransactionCriteria, TransactionGroup, TransactionGroupings}
import tech.pegb.backoffice.dao.transaction.sql.TransactionGroupSqlDao
import tech.pegb.core.PegBTestApp

class TransactionGroupSqlDaoSpec extends PegBTestApp with MockFactory {

  override def initSql =
    s"""
       |INSERT INTO account_types(id, account_type_name, description, created_at, updated_at, is_active)
       |VALUES('1', 'WALLET', 'standard account type for individual users', now(), null, 1);
       |
       |INSERT INTO currencies(id, currency_name, description, created_at, updated_at, is_active)
       |VALUES
       |('1', 'AED', 'United Arab Emirates Dirhams', now(), null, 1),
       |('2', 'KES', 'Kenyan Shilling', now(), null, 1),
       |('3', 'USD', 'United States Dollar', now(), null, 1);
       |
       |INSERT INTO users(id, uuid, username, password, email, subscription, type, status, tier, segment, activated_at, password_updated_at, created_at, created_by, updated_at, updated_by)
       |VALUES
       |('1', 'bcc32571-cf16-4abc-ac38-38d58f9cbab5', null, null, null, null, 'individual', null, null, null, now(), null, now(), 'SuperAdmin', null, null);
       |
       |INSERT INTO users(id, uuid, username, password, email, activated_at, password_updated_at, subscription, type, status, tier, segment, created_by, created_at, updated_by, updated_at)
       |VALUES(2, 'a57291af-c840-4ab1-bb4d-4baed930ed58', 'provider-user', 'pword', 'provider@gmail.com',  '2018-10-01 00:00:00', null, 'sub_one',  'type_one', 'ACTIVE', null, null, 'SuperUser', '2018-10-01 00:00:00', null, null);
       |
       |INSERT INTO accounts(id, uuid, number, user_id, name, is_main_account, account_type_id, currency_id, balance, blocked_balance, status, closed_at, last_transaction_at, created_at, created_by, updated_at, updated_by, main_type)
       |VALUES
       |('1', '1c15bdb9-5d42-4a84-922a-0d01f8e5de71', '4716 4157 0907 3361', '1', 'George Ogalo', '1', '1', '1', '0.0', '0.0', 'INACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null, 'liability');
       |
       |
       |INSERT INTO `providers`
       |(id, user_id, service_id, name, transaction_type, icon, label, pg_institution_id,
       |utility_payment_type, utility_min_payment_amount, utility_max_payment_amount, is_active,
       |created_by, updated_by, created_at, updated_at)
       |VALUES
       |('1', '2', null, 'pesalink', 'remittance', 'not available', 'not available', '1',
       | null, '0.01', null, '1', 'test user', 'test user', now(), now()),
       |
       |('2', '2', null, 'mpesa', 'cashin', 'not available', 'not available', '1',
       | null, '0.01', null, '1', 'test user', 'test user', now(), now()),
       |
       |('3', '2', null, 'airtel', 'cashout', 'not available', 'not available', '1',
       | null, '0.01', null, '1', 'test user', 'test user', now(), now());
       |
       |INSERT INTO transactions(unique_id, id, sequence, primary_account_id, secondary_account_id, direction, type, amount, currency_id, channel, provider_id, explanation, status, created_at, updated_at)
       |VALUES
       |(1,'1549449579', 1, 1, 1, 'debit', 'p2p_domestic', 1250.000, 1, 'IOS_APP', null, 'some explanation', 'success', '2018-12-25 00:00:00', '2019-01-01 00:00:00'),
       |(2,'1549449579', 2, 1, 1, 'debit', 'fee',          1250.000, 2, 'IOS_APP', '1', 'some explanation', 'success', '2018-12-25 00:00:00', '2019-01-01 00:00:00'),
       |(3,'1549449579', 3, 1, 1, 'debit', 'cash_in',      1250.000, 3, 'IOS_APP', '2', 'some explanation', 'success', '2018-12-25 00:00:00', '2019-01-01 00:00:00'),
       |(4,'1549449579', 4, 1, 1, 'debit', 'cash_in',      1250.000, 1, 'IOS_APP', '3', 'some explanation', 'success', '2018-12-25 00:00:00', '2019-01-01 00:00:00'),
       |(5,'1549449579', 5, 1, 1, 'debit', 'fee',          1250.000, 2, 'IOS_APP', '1', 'some explanation', 'success', '2018-12-25 00:00:00', '2019-01-01 00:00:00');
    """.stripMargin

  override def cleanupSql: String =
    """
      |DELETE FROM transactions;
      |DELETE FROM accounts;
      |DELETE FROM providers;
      |DELETE FROM users;
      |DELETE FROM account_types;
      |DELETE FROM currencies;
    """.stripMargin

  lazy val txnGrpSqlDao = fakeApplication().injector.instanceOf[TransactionGroupSqlDao]

  "TransactionGroupSqlDao" should {
    "get currency groups" in {
      val result = txnGrpSqlDao.getTransactionGroups(TransactionCriteria(), TransactionGroupings(currencyCode = true))
      val expected = Set(TransactionGroup(currencyCode = Some("AED")), TransactionGroup(currencyCode = Some("KES")), TransactionGroup(currencyCode = Some("USD")))
      result.right.get.toSet mustBe expected
    }

    "get transaction type and currency groups" in {
      val result = txnGrpSqlDao.getTransactionGroups(TransactionCriteria(), TransactionGroupings(currencyCode = true, transactionType = true))
      val expected = Set(
        TransactionGroup(transactionType = Some("cash_in"), currencyCode = Some("AED")),
        TransactionGroup(transactionType = Some("p2p_domestic"), currencyCode = Some("AED")),
        TransactionGroup(transactionType = Some("fee"), currencyCode = Some("KES")),
        TransactionGroup(transactionType = Some("cash_in"), currencyCode = Some("USD")))
      result.right.get.toSet mustBe expected
    }

    "get other_party, transaction type and currency groups" in {
      val result = txnGrpSqlDao.getTransactionGroups(TransactionCriteria(), TransactionGroupings(
        provider = true,
        currencyCode = true, transactionType = true))
      val expected = Set(
        TransactionGroup(transactionType = Some("cash_in"), currencyCode = Some("AED"), provider = Some("airtel")),
        TransactionGroup(transactionType = Some("p2p_domestic"), currencyCode = Some("AED")),
        TransactionGroup(transactionType = Some("fee"), currencyCode = Some("KES"), provider = Some("pesalink")),
        TransactionGroup(transactionType = Some("cash_in"), currencyCode = Some("USD"), provider = Some("mpesa")))
      result.right.get.toSet mustBe expected
    }
  }

}
