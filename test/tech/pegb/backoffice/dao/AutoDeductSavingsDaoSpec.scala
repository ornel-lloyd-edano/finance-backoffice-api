package tech.pegb.backoffice.dao

import java.time.LocalDateTime

import org.scalamock.scalatest.MockFactory
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes, OrderingSet, Ordering}
import tech.pegb.backoffice.dao.savings.abstraction.AutoDeductSavingsDao
import tech.pegb.backoffice.dao.savings.dto.AutoDeductSavingsCriteria
import tech.pegb.backoffice.dao.savings.sql.AutoDeductSavingsSqlDao
import tech.pegb.core.PegBTestApp

class AutoDeductSavingsDaoSpec extends PegBTestApp with MockFactory {

  override def initSql =
    s"""
       |INSERT INTO account_types(id, account_type_name, description, created_at, updated_at, is_active)
       |VALUES('1', 'WALLET', 'standard account type for individual users', now(), null, 1);
       |
       |INSERT INTO currencies(id, currency_name, description, created_at, updated_at, is_active)
       |VALUES
       |('1', 'AED', 'default currency for UAE', now(), null, 1),
       |('2', 'KES', 'default currency for KENYA', now(), null, 1);
       |
       |INSERT INTO users(id, uuid, username, password, email, activated_at, password_updated_at, subscription, type, status, tier, segment, created_by, created_at, updated_by, updated_at)
       |VALUES
       |('1', '5744db00-2b50-4a34-8348-35f0030c3b1d', 'user01', 'pword', 'george@gmail.com',  '2018-01-01 00:00:00', null, 'sub_one',  'type_one', 'ACTIVE', null, null, 'SuperUser', '2018-01-01 00:00:00', null, null),
       |('2', '4634db00-2b61-4a23-8348-35f0030c3b1d', 'user02', 'pword', 'ujali@gmail.com',   '2018-01-01 00:00:00', null, 'sub_one',  'type_one', 'ACTIVE', null, null, 'SuperUser', '2018-01-01 00:00:00', null, null),
       |('3', '3be6a9e7-52ca-4e2c-a89c-35b480fdfdca', 'user03', 'pword', 'david@gmail.com',   '2018-01-01 00:00:00', null, 'sub_one',  'type_one', 'ACTIVE', null, null, 'SuperUser', '2018-01-01 00:00:00', null, null),
       |('4', '594c8e61-5e78-40ab-b85d-9556533b9c6f', 'user04', 'pword', 'alex@gmail.com',    '2018-01-01 00:00:00', null, 'sub_one',  'type_one', 'ACTIVE', null, null, 'SuperUser', '2018-01-01 00:00:00', null, null);
       |
       |INSERT INTO accounts(id, uuid, number, user_id, name, is_main_account, account_type_id, currency_id, balance, blocked_balance, status, closed_at, last_transaction_at, created_at, created_by, updated_at, updated_by, main_type)
       |VALUES
       |('1', '1c15bdb9-5d42-4a84-922a-0d01f8e5de71', '4716 4157 0907 3361', '1', 'George Ogalo', '1', '1', '2', '0.0', '0.0', 'INACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null, 'liability'),
       |('2', 'c86eb85f-70f8-4ccd-ae28-68bb08cc1fa5', '8912 3287 1209 3422', '2', 'Ujali Tyagi', '0', '1', '1', '20.0', '50.0', 'ACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null, 'liability'),
       |('3', '2c41cedf-b4ad-4daf-8019-35ff82c015cc', '8912 3287 1209 3423', '3', 'David Salgado', '0', '1', '1', '30.0', '60.0', 'ACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null, 'liability'),
       |('4', 'b370ef14-b219-42b3-8160-5f08a2bcc73a', '4716 4157 0907 3364', '4', 'Alex Kim', '1', '1', '1', '0.0', '0.0', 'INACTIVE', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null, 'asset');
       |
       |INSERT INTO auto_deduct_savings
       |(id, uuid, user_id, saving_account_id, current_amount, saving_percentage, min_income, status_updated_at, created_at, updated_at, is_active)
       |VALUES
       |(1, 'c86eb85f-2b61-5d42-ae28-0d01f8e5de71', '1', '1', '9999', '15', '15', '2019-07-06 08:13:00', '2019-07-04 09:13:00', '2019-07-11 07:36:46', '1'),
       |(2, 'fc3df811-adea-4e8d-9ae5-fbfc66c8b07d', '2', '2', '5000', '5', '100', '2019-07-06 08:13:00', '2019-05-10 09:13:00', '2019-07-11 07:36:46', '0'),
       |(3, 'b80f8522-9c58-4464-b4bc-0d6ef91b612e', '3', '3', '1500', '2', '200', '2019-07-06 08:13:00', '2019-03-06 09:13:00', '2019-07-11 07:36:46', '0'),
       |(4, 'd5698075-aa7a-40b4-834b-8f0e88f1a3b8', '4', '4', '2800', '8', '30', '2019-07-06 08:13:00', '2019-01-01 09:13:00', '2019-07-11 07:36:46', '1');
     """.stripMargin

  override def cleanupSql: String =
    s"""
       |DELETE FROM auto_deduct_savings;
       |DELETE FROM accounts;
       |DELETE FROM currencies;
       |DELETE FROM account_types;
       |DELETE FROM users;
     """.stripMargin

  val autodeductSavingsDao = inject[AutoDeductSavingsDao]

  "AutoDeductSavingsDao" should {
    "get all AutoDeductSaving records in getSavingOptionsByCriteria" in {

      val result = autodeductSavingsDao.getSavingOptionsByCriteria(None, None, None, None)

      val expected = Seq(
        1 → "c86eb85f-2b61-5d42-ae28-0d01f8e5de71",
        2 → "fc3df811-adea-4e8d-9ae5-fbfc66c8b07d",
        3 → "b80f8522-9c58-4464-b4bc-0d6ef91b612e",
        4 → "d5698075-aa7a-40b4-834b-8f0e88f1a3b8")

      //println(result.left.get)

      result.right.get.map(i ⇒ (i.id → i.uuid)) mustBe expected
    }

    "count all AutoDeductSaving records in countSavingOptionsByCriteria" in {
      val result = autodeductSavingsDao.countSavingOptionsByCriteria(None)
      result.right.get mustBe 4
    }

    "get filtered AutoDeductSaving records in getSavingOptionsByCriteria" in {

      val filterById = AutoDeductSavingsCriteria(id = Option(CriteriaField(AutoDeductSavingsSqlDao.cId, 3)))
      val result1 = autodeductSavingsDao.getSavingOptionsByCriteria(Option(filterById), None, None, None)

      val filterByUuid = AutoDeductSavingsCriteria(uuid = Option(CriteriaField(AutoDeductSavingsSqlDao.cUuid, "c86eb85f-2b61-5d42-ae28-0d01f8e5de71")))
      val result2 = autodeductSavingsDao.getSavingOptionsByCriteria(Option(filterByUuid), None, None, None)

      val filterByMinIncome = AutoDeductSavingsCriteria(minIncome = Option(CriteriaField(AutoDeductSavingsSqlDao.cMinIncome, BigDecimal(100), MatchTypes.GreaterOrEqual)))
      val result3 = autodeductSavingsDao.getSavingOptionsByCriteria(Option(filterByMinIncome), None, None, None)

      val filterByCreatedAt = AutoDeductSavingsCriteria(createdAt = Option(CriteriaField(AutoDeductSavingsSqlDao.cCreatedAt, LocalDateTime.of(2019, 3, 1, 0, 0, 0), MatchTypes.LesserOrEqual)))
      val result4 = autodeductSavingsDao.getSavingOptionsByCriteria(Option(filterByCreatedAt), None, None, None)

      val filterByStatus = AutoDeductSavingsCriteria(isActive = Option(CriteriaField(AutoDeductSavingsSqlDao.cIsActive, true)))
      val result5 = autodeductSavingsDao.getSavingOptionsByCriteria(Option(filterByStatus), None, None, None)

      val allResults = Seq(
        result1.right.get.map(_.id) == Seq(3),
        result2.right.get.map(_.uuid) == Seq("c86eb85f-2b61-5d42-ae28-0d01f8e5de71"),
        result3.right.get.map(r ⇒ (r.id → r.minIncome)).toSet == Set(2 → BigDecimal(100), 3 → BigDecimal(200)),
        result4.right.get.map(r ⇒ (r.id → r.createdAt)).toSet == Set(4 → LocalDateTime.of(2019, 1, 1, 9, 13, 0)),
        result5.right.get.map(r ⇒ (r.id → r.isActive)).toSet == Set(1 → true, 4 → true)).reduceOption(_ && _).getOrElse(false)

      allResults mustBe true
    }

    "count filtered AutoDeductSaving records in getSavingOptionsByCriteria" in {
      val result1 = autodeductSavingsDao.countSavingOptionsByCriteria(None)

      val filterById = AutoDeductSavingsCriteria(id = Option(CriteriaField(AutoDeductSavingsSqlDao.cId, 3)))
      val result2 = autodeductSavingsDao.countSavingOptionsByCriteria(Option(filterById))

      val filterByStatus = AutoDeductSavingsCriteria(isActive = Option(CriteriaField(AutoDeductSavingsSqlDao.cIsActive, true)))
      val result3 = autodeductSavingsDao.countSavingOptionsByCriteria(Option(filterByStatus))

      val filterByCurrentAmt = AutoDeductSavingsCriteria(currentAmount = Option(CriteriaField(AutoDeductSavingsSqlDao.cCurrAmt, BigDecimal(9000), MatchTypes.LesserOrEqual)))
      val result4 = autodeductSavingsDao.countSavingOptionsByCriteria(Option(filterByCurrentAmt))

      val allResults = Seq(result1, result2, result3, result4)

      allResults.map(_.right.get) mustBe Seq(4, 1, 2, 3)
    }

    "get AutoDeductSaving records arranged in specific order in getSavingOptionsByCriteria" in {
      val result1 = autodeductSavingsDao.getSavingOptionsByCriteria(None, Option(OrderingSet(Ordering(AutoDeductSavingsSqlDao.cId, Ordering.DESC))), None, None)
      val expected1 = Seq(4, 3, 2, 1)

      result1.right.get.map(_.id) mustBe expected1

      val result2 = autodeductSavingsDao.getSavingOptionsByCriteria(None, Option(OrderingSet(Ordering(AutoDeductSavingsSqlDao.cCurrAmt, Ordering.ASC))), None, None)
      val expected2 = Seq(3, 4, 2, 1)

      result2.right.get.map(_.id) mustBe expected2

      val result3 = autodeductSavingsDao.getSavingOptionsByCriteria(
        None,
        Option(OrderingSet(
          Ordering(AutoDeductSavingsSqlDao.cIsActive, Ordering.DESC),
          Ordering(AutoDeductSavingsSqlDao.cCreatedAt, Ordering.ASC))), None, None)
      val expected3 = Seq(4, 1, 3, 2)

      result3.right.get.map(_.id) mustBe expected3
    }

    "get AutoDeductSaving records paginated with limit and offset in getSavingOptionsByCriteria" in {
      val result1 = autodeductSavingsDao.getSavingOptionsByCriteria(None, None, limit = Option(2), None)
      val expected1 = Seq(1, 2)

      result1.right.get.map(_.id) mustBe expected1

      val result2 = autodeductSavingsDao.getSavingOptionsByCriteria(None, None, limit = Option(2), offset = Option(2))
      val expected2 = Seq(3, 4)

      result2.right.get.map(_.id) mustBe expected2
    }
  }
}
