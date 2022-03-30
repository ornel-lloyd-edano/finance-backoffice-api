package tech.pegb.backoffice.dao

import java.time.LocalDateTime

import org.scalamock.scalatest.MockFactory
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes}
import tech.pegb.backoffice.dao.savings.abstraction.SavingGoalsDao
import tech.pegb.backoffice.dao.savings.dto.SavingGoalsCriteria
import tech.pegb.core.PegBTestApp

class SavingGoalsDaoSpec extends PegBTestApp with MockFactory {

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
       |INSERT INTO saving_goals
       |(id, uuid,                                   user_id, saving_account_id, currency_id, goal_amount, current_amount, initial_amount, emi_amount, due_date,    name,       reason,      payment_type, status,   status_updated_at,     created_at,            updated_at)
       |VALUES
       |(1, 'c86eb85f-2b61-5d42-ae28-0d01f8e5de71', '1',      '1',              '2',          '9999',      '9999',         '9999',        '999',      '2019-10-01', 'anything', 'vacation',  'manual',    'active',  '2019-07-06 08:13:00', '2019-07-04 09:13:00', '2019-07-11 07:36:46'),
       |(2, 'fc3df811-adea-4e8d-9ae5-fbfc66c8b07d', '2',      '2',              '1',          '5000',      '5000',         '5000',        '500',      '2019-10-01', 'anything', 'education', 'manual',    'active',  '2019-07-06 08:13:00', '2019-05-10 09:13:00', '2019-07-11 07:36:46'),
       |(3, 'b80f8522-9c58-4464-b4bc-0d6ef91b612e', '3',      '3',              '1',          '1500',      '1500',         '1500',        '150',      '2019-10-01', 'anything', 'new car',   'automatic', 'active',  '2019-07-06 08:13:00', '2019-03-06 09:13:00', '2019-07-11 07:36:46'),
       |(4, 'd5698075-aa7a-40b4-834b-8f0e88f1a3b8', '4',      '4',              '1',          '2800',      '2800',         '2800',        '280',      '2019-10-01', 'anything', 'new house', 'automatic', 'inactive','2019-07-06 08:13:00', '2019-01-01 09:13:00', '2019-07-11 07:36:46');
     """.stripMargin

  override def cleanupSql: String =
    s"""
       |DELETE FROM saving_goals;
       |DELETE FROM accounts;
       |DELETE FROM currencies;
       |DELETE FROM account_types;
       |DELETE FROM users;
     """.stripMargin

  val savingGoalsDao = inject[SavingGoalsDao]

  "SavingGoalsDao" should {
    "get all SavingGoal records in getSavingOptionsByCriteria" in {
      val result = savingGoalsDao.getSavingOptionsByCriteria(None, None, None, None)

      val expected = Seq(
        1 → "c86eb85f-2b61-5d42-ae28-0d01f8e5de71",
        2 → "fc3df811-adea-4e8d-9ae5-fbfc66c8b07d",
        3 → "b80f8522-9c58-4464-b4bc-0d6ef91b612e",
        4 → "d5698075-aa7a-40b4-834b-8f0e88f1a3b8")

      result.right.get.map(i ⇒ (i.id → i.uuid)) mustBe expected
    }

    "count all SavingGoal records in getSavingOptionsByCriteria" in {
      val result = savingGoalsDao.countSavingOptionsByCriteria(None)
      result.right.get mustBe 4
    }

    "get filtered SavingGoal records in getSavingOptionsByCriteria" in {
      val filterById = SavingGoalsCriteria(id = Option(CriteriaField("", 3)))
      val result1 = savingGoalsDao.getSavingOptionsByCriteria(Option(filterById), None, None, None)

      val filterByUuid = SavingGoalsCriteria(uuid = Option(CriteriaField("", "c86eb85f-2b61-5d42-ae28-0d01f8e5de71")))
      val result2 = savingGoalsDao.getSavingOptionsByCriteria(Option(filterByUuid), None, None, None)

      val filterByCurrency = SavingGoalsCriteria(currency = Option(CriteriaField("", "AED")))
      val result3 = savingGoalsDao.getSavingOptionsByCriteria(Option(filterByCurrency), None, None, None)

      val filterByCreatedAt = SavingGoalsCriteria(createdAt = Option(CriteriaField("", LocalDateTime.of(2019, 3, 1, 0, 0, 0), MatchTypes.LesserOrEqual)))
      val result4 = savingGoalsDao.getSavingOptionsByCriteria(Option(filterByCreatedAt), None, None, None)

      val filterByStatus = SavingGoalsCriteria(isActive = Option(CriteriaField("", true)))
      val result5 = savingGoalsDao.getSavingOptionsByCriteria(Option(filterByStatus), None, None, None)

      val filterByUserUuid = SavingGoalsCriteria(userUuid = Option(CriteriaField("", "8348-35", MatchTypes.Partial)))
      val result6 = savingGoalsDao.getSavingOptionsByCriteria(Option(filterByUserUuid), None, None, None)

      val filterByAccountUuid = SavingGoalsCriteria(accountUuid = Option(CriteriaField("", "b370ef14-b219-42b3-8160-5f08a2bcc73a")))
      val result7 = savingGoalsDao.getSavingOptionsByCriteria(Option(filterByAccountUuid), None, None, None)

      val allResults = Seq(
        result1.right.get.map(_.id) == Seq(3),
        result2.right.get.map(_.uuid) == Seq("c86eb85f-2b61-5d42-ae28-0d01f8e5de71"),
        result3.right.get.map(r ⇒ (r.id → r.currency)).toSet == Set(2 → "AED", 3 → "AED", 4 → "AED"),
        result4.right.get.map(r ⇒ (r.id → r.createdAt)).toSet == Set(4 → LocalDateTime.of(2019, 1, 1, 9, 13, 0)),
        result5.right.get.map(r ⇒ (r.id → r.status)).toSet == Set(1 → "active", 2 → "active", 3 → "active"),
        result6.right.get.map(r ⇒ (r.id → r.userUuid)).toSet == Set(1 → "5744db00-2b50-4a34-8348-35f0030c3b1d", 2 → "4634db00-2b61-4a23-8348-35f0030c3b1d"),
        result7.right.get.map(r ⇒ (r.id → r.accountUuid)).toSet == Set(4 → "b370ef14-b219-42b3-8160-5f08a2bcc73a")).reduceOption(_ && _).getOrElse(false)

      allResults mustBe true
    }

    "count filtered SavingGoal records in getSavingOptionsByCriteria" in {
      val filterById = SavingGoalsCriteria(id = Option(CriteriaField("", 3)))
      val result1 = savingGoalsDao.countSavingOptionsByCriteria(Option(filterById))

      val filterByUuid = SavingGoalsCriteria(uuid = Option(CriteriaField("", "c86eb85f-2b61-5d42-ae28-0d01f8e5de71")))
      val result2 = savingGoalsDao.countSavingOptionsByCriteria(Option(filterByUuid))

      val filterByCurrency = SavingGoalsCriteria(currency = Option(CriteriaField("", "AED")))
      val result3 = savingGoalsDao.countSavingOptionsByCriteria(Option(filterByCurrency))

      val filterByCreatedAt = SavingGoalsCriteria(createdAt = Option(CriteriaField("", LocalDateTime.of(2019, 3, 1, 0, 0, 0), MatchTypes.LesserOrEqual)))
      val result4 = savingGoalsDao.countSavingOptionsByCriteria(Option(filterByCreatedAt))

      val filterByStatus = SavingGoalsCriteria(isActive = Option(CriteriaField("", true)))
      val result5 = savingGoalsDao.countSavingOptionsByCriteria(Option(filterByStatus))

      val filterByUserUuid = SavingGoalsCriteria(userUuid = Option(CriteriaField("", "8348-35", MatchTypes.Partial)))
      val result6 = savingGoalsDao.countSavingOptionsByCriteria(Option(filterByUserUuid))

      val filterByAccountUuid = SavingGoalsCriteria(accountUuid = Option(CriteriaField("", "b370ef14-b219-42b3-8160-5f08a2bcc73a")))
      val result7 = savingGoalsDao.countSavingOptionsByCriteria(Option(filterByAccountUuid))

      val allResults = Seq(
        result1.right.get,
        result2.right.get,
        result3.right.get,
        result4.right.get,
        result5.right.get,
        result6.right.get,
        result7.right.get)

      allResults mustBe Seq(1, 1, 3, 1, 3, 2, 1)
    }

    "get SavingGoal records arranged in specific order in getSavingOptionsByCriteria" in {

    }

    "get SavingGoal records paginated with limit and offset in getSavingOptionsByCriteria" in {

    }
  }
}
