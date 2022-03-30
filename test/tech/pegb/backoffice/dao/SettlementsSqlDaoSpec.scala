package tech.pegb.backoffice.dao

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import org.scalamock.scalatest.MockFactory
import play.api.db.DBApi
import tech.pegb.backoffice.dao.Dao.DaoResponse
import tech.pegb.backoffice.dao.model.{Ordering, OrderingSet}
import tech.pegb.backoffice.dao.transaction.abstraction.SettlementDao
import tech.pegb.backoffice.dao.transaction.dto._
import tech.pegb.backoffice.dao.transaction.entity.{Settlement, SettlementFxHistory, SettlementLines, SettlementRecentAccount}
import tech.pegb.core.PegBTestApp

class SettlementsSqlDaoSpec extends PegBTestApp with MockFactory {

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  override def initSql =
    s"""
       |INSERT INTO account_types(id, account_type_name, description, created_at, updated_at, is_active)
       |VALUES('1', 'COLLECTION', 'collection account for pegb', now(), null, 1);
       |
       |INSERT INTO currencies(id, currency_name, description, created_at, updated_at, is_active, icon)
       |VALUES('1', 'AED', 'default currency for UAE', now(), null, 1, 'aed_icon'),
       |('2', 'KES', 'default currency for Kenya', now(), null, 1, 'kes_icon'),
       |('3', 'PHP', 'default currency for Philippines', now(), null, 1, 'php_icon');
       |
       |INSERT INTO users(id, uuid, username, password, email, subscription, type, status, tier, segment, activated_at, password_updated_at, created_at, created_by, updated_at, updated_by)
       |VALUES
       |('1', 'bcc32571-cf16-4abc-ac38-38d58f9cbab5', 'george_ogalo', null, null, null, null, null, null, null, now(), null, now(), 'SuperAdmin', null, null),
       |('2', '4634db00-2b61-4a23-8348-35f0030c3b1d', 'ujali_sharma', null, null, null, null, null, null, null, now(), null, now(), 'SuperAdmin', null, null);
       |
       |INSERT INTO individual_users(msisdn, user_id, type, name, fullname, gender, company, birthdate, birth_place, nationality, occupation, employer, created_at, created_by, updated_at, updated_by) VALUES
       |('971544465329', '2', 'type_one', 'Alice', 'Ujali Sharma', 'F', 'PegB', '1990-01-01', 'Dubai', 'Emirati', 'Manager', 'EMAAR', '2018-10-01 00:00:00', 'SuperUser', null, null);
       |
       |INSERT INTO accounts(id, uuid, number, user_id, name, is_main_account, account_type_id, currency_id, balance, blocked_balance, status, closed_at, last_transaction_at, created_at, created_by, updated_at, updated_by, main_type)
       |VALUES
       |('1', '1c15bdb9-5d42-4a84-922a-0d01f8e5de71', '4716 4157 0907 3361', '1', 'George Ogalo', '1', '1', '1', '0.0', '0.0', 'active', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null, 'liability'),
       |('2', 'c86eb85f-70f8-4ccd-ae28-68bb08cc1fa5', '8912 3287 1209 3422', '2', 'Ujali Tyagi', '0', '1', '1', '20.0', '50.0', 'active', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null, 'liability'),
       |('3', 'bcc32571-70f8-4ccd-ae28-35f0030c3b1d', '5001 3287 1209 6021', '2', 'Ujali Tyagi', '0', '1', '2', '20.0', '50.0', 'active', null, null, '2018-12-25 00:00:00', 'SuperAdmin', null, null, 'asset');
       |
       |INSERT INTO settlements(id, uuid, reason, status, created_at, created_by, checked_at, checked_by, updated_at)
       |VALUES
       |(1, '1b11adb9-5d52-4a84-922a-0d01f8e5de31', 'any reason', 'good status', '2018-10-10 00:00:00', 'some user 1', null, null, null),
       |(2, '1b11adb9-4ccd-4a84-922a-68bb08cc1fa5', 'any reason', 'bad status', '2018-10-11 00:00:00', 'some user 2', null, null, null);
       |
       |INSERT INTO settlement_lines(id, settlement_id, account_id, direction, currency_id, amount, explanation, updated_at)
       |VALUES
       |(1, 1, 2, 'credit', '1', '10000.50', 'no need of words just action', '2022-01-01 00:00:00'),
       |(2, 1, 3, 'debit', '2', '10000.50', 'no need of words just action', '2022-01-01 00:00:00'),
       |(3, 2, 1, 'credit', '1', '5000.10', 'no need of words just action', '2022-01-02 00:00:00'),
       |(4, 2, 3, 'debit', '2', '5000.10', 'no need of words just action', '2022-01-02 00:00:00');
     """.stripMargin

  override def cleanupSql: String =
    s"""
       |DELETE FROM accounts;
       |DELETE FROM users;
       |DELETE FROM currencies;
       |DELETE FROM account_types;
       |DELETE FROM settlement_lines;
       |DELETE FROM settlements;
     """.stripMargin

  val dbApi: DBApi = inject[DBApi]
  val settlementDao = inject[SettlementDao]

  "SettlementsSqlDao" should {
    val firstUuid = UUID.randomUUID().toString
    val dto = SettlementToInsert(
      uuid = firstUuid,
      createdBy = "unit test",
      createdAt = LocalDateTime.of(2018, 10, 12, 0, 0, 0),
      checkedBy = Option("backoffice user"),
      checkedAt = Option(LocalDateTime.of(2018, 10, 12, 0, 0, 0)),
      fxProvider = None,
      fromCurrencyId = None,
      toCurrencyId = None,
      fxRate = None,
      status = "approved",
      reason = "top up account",
      settlementLines = Seq(
        SettlementLinesToInsert(
          accountId = 1,
          direction = "debit",
          currencyId = 1,
          amount = BigDecimal(999.99),
          explanation = "put 999.99AED in this account"),
        SettlementLinesToInsert(
          accountId = 2,
          direction = "credit",
          currencyId = 1,
          amount = BigDecimal(999.99),
          explanation = "get 999.99AED from this account")))

    "save settlement along with associated settlement_lines" in {
      val result: DaoResponse[Settlement] = settlementDao.insertSettlement(dto)

      val expected = Settlement(
        id = 3,
        uuid = dto.uuid,
        transactionReason = dto.reason,
        createdBy = dto.createdBy,
        createdAt = dto.createdAt,
        checkedBy = dto.checkedBy,
        checkedAt = dto.checkedAt,
        status = dto.status,
        fxProvider = None,
        fromCurrencyId = None,
        toCurrencyId = None,
        fxRate = None,
        lines = Seq(
          SettlementLines(
            id = 5, manualSettlementId = 3,
            accountId = 1, accountNumber = "4716 4157 0907 3361", direction = "debit",
            currencyId = 1, currency = "AED", amount = BigDecimal(999.99),
            explanation = "put 999.99AED in this account"),
          SettlementLines(
            id = 6, manualSettlementId = 3,
            accountId = 2, accountNumber = "8912 3287 1209 3422", direction = "credit",
            currencyId = 1, currency = "AED", amount = BigDecimal(999.99),
            explanation = "get 999.99AED from this account")))

      result.isRight mustBe true
      result.right.get mustBe expected
    }

    "unable to save settlement because uuid already exists" in {
      val dto = SettlementToInsert(
        uuid = firstUuid,
        createdBy = "unit test",
        createdAt = LocalDateTime.now,
        checkedBy = Option("backoffice user"),
        checkedAt = Option(LocalDateTime.now),
        status = "approved",
        reason = "top up account",
        fxProvider = None,
        fromCurrencyId = None,
        toCurrencyId = None,
        fxRate = None,
        settlementLines = Seq(
          SettlementLinesToInsert(
            accountId = 1,
            direction = "debit",
            currencyId = 1,
            amount = BigDecimal(999.99),
            explanation = "put 999.99AED in this account"),
          SettlementLinesToInsert(
            accountId = 2,
            direction = "credit",
            currencyId = 1,
            amount = BigDecimal(999.99),
            explanation = "get 999.99AED from this account")))

      val result: DaoResponse[Settlement] = settlementDao.insertSettlement(dto)

      result.isLeft mustBe true
    }

    "unable to save settlement because currency id does not exist" in {
      val dto = SettlementToInsert(
        uuid = firstUuid,
        createdBy = "unit test",
        createdAt = LocalDateTime.now,
        checkedBy = Option("backoffice user"),
        checkedAt = Option(LocalDateTime.now),
        status = "approved",
        reason = "top up account",
        fxProvider = None,
        fromCurrencyId = None,
        toCurrencyId = None,
        fxRate = None,
        settlementLines = Seq(
          SettlementLinesToInsert(
            accountId = 1,
            direction = "debit",
            currencyId = 1,
            amount = BigDecimal(999.99),
            explanation = "put 999.99AED in this account"),
          SettlementLinesToInsert(
            accountId = 2,
            direction = "credit",
            currencyId = 99, //no currencyId 99
            amount = BigDecimal(999.99),
            explanation = "get 999.99AED from this account")))

      val result: DaoResponse[Settlement] = settlementDao.insertSettlement(dto)

      result.isLeft mustBe true
    }

    "unable to save settlement because acount id does not exist" in {
      val dto = SettlementToInsert(
        uuid = firstUuid,
        createdBy = "unit test",
        createdAt = LocalDateTime.now,
        checkedBy = Option("backoffice user"),
        checkedAt = Option(LocalDateTime.now),
        status = "approved",
        reason = "top up account",
        fxProvider = None,
        fromCurrencyId = None,
        toCurrencyId = None,
        fxRate = None,
        settlementLines = Seq(
          SettlementLinesToInsert(
            accountId = 1,
            direction = "debit",
            currencyId = 1,
            amount = BigDecimal(999.99),
            explanation = "put 999.99AED in this account"),
          SettlementLinesToInsert(
            accountId = 99, //no accountId 99
            direction = "credit",
            currencyId = 1,
            amount = BigDecimal(999.99),
            explanation = "get 999.99AED from this account")))

      val result: DaoResponse[Settlement] = settlementDao.insertSettlement(dto)

      result.isLeft mustBe true
    }

    "get all settlements along with associated settlement_lines" in {
      val result: DaoResponse[Seq[Settlement]] = settlementDao.getSettlementsByCriteria(criteria = SettlementCriteria.NO_CRITERIA)

      val expected = Seq(
        Settlement(
          id = 1,
          uuid = "1b11adb9-5d52-4a84-922a-0d01f8e5de31",
          transactionReason = "any reason",
          createdBy = "some user 1",
          createdAt = LocalDateTime.of(2018, 10, 10, 0, 0),
          checkedBy = None,
          checkedAt = None,
          status = "good status",
          fxProvider = None,
          fromCurrencyId = None,
          toCurrencyId = None,
          fxRate = None,
          lines = Seq(
            SettlementLines(
              id = 1, manualSettlementId = 1,
              accountId = 2, accountNumber = "8912 3287 1209 3422", direction = "credit",
              currencyId = 1, currency = "AED", amount = BigDecimal(10000.50),
              explanation = "no need of words just action"),
            SettlementLines(
              id = 2, manualSettlementId = 1,
              accountId = 3, accountNumber = "5001 3287 1209 6021", direction = "debit",
              currencyId = 2, currency = "KES", amount = BigDecimal(10000.50),
              explanation = "no need of words just action"))),
        Settlement(
          id = 2,
          uuid = "1b11adb9-4ccd-4a84-922a-68bb08cc1fa5",
          transactionReason = "any reason",
          createdBy = "some user 2",
          createdAt = LocalDateTime.of(2018, 10, 11, 0, 0),
          checkedBy = None,
          checkedAt = None,
          status = "bad status",
          fxProvider = None,
          fromCurrencyId = None,
          toCurrencyId = None,
          fxRate = None,
          lines = Seq(
            SettlementLines(
              id = 3, manualSettlementId = 2,
              accountId = 1, accountNumber = "4716 4157 0907 3361", direction = "credit",
              currencyId = 1, currency = "AED", amount = BigDecimal(5000.10),
              explanation = "no need of words just action"),
            SettlementLines(
              id = 4, manualSettlementId = 2,
              accountId = 3, accountNumber = "5001 3287 1209 6021", direction = "debit",
              currencyId = 2, currency = "KES", amount = BigDecimal(5000.10),
              explanation = "no need of words just action"))),
        Settlement(
          id = 3,
          uuid = dto.uuid,
          transactionReason = dto.reason,
          createdBy = dto.createdBy,
          createdAt = dto.createdAt,
          checkedBy = dto.checkedBy,
          checkedAt = dto.checkedAt,
          status = dto.status,
          fxProvider = None,
          fromCurrencyId = None,
          toCurrencyId = None,
          fxRate = None,
          lines = Seq(
            SettlementLines(
              id = 5, manualSettlementId = 3,
              accountId = 1, accountNumber = "4716 4157 0907 3361", direction = "debit",
              currencyId = 1, currency = "AED", amount = BigDecimal(999.99),
              explanation = "put 999.99AED in this account"),
            SettlementLines(
              id = 6, manualSettlementId = 3,
              accountId = 2, accountNumber = "8912 3287 1209 3422", direction = "credit",
              currencyId = 1, currency = "AED", amount = BigDecimal(999.99),
              explanation = "get 999.99AED from this account"))))

      result.isRight mustBe true

      result.right.get mustBe expected
    }

    "get all settlements along with associated settlement_lines which meets criteria" in {
      val result: DaoResponse[Seq[Settlement]] = settlementDao.getSettlementsByCriteria(
        criteria = SettlementCriteria(
          createdAtFrom = Some(LocalDateTime.of(2018, 10, 11, 0, 0)),
          createdAtTo = Some(LocalDateTime.of(2018, 10, 12, 0, 0)),
          currency = Some("KES")))

      val expected = Seq(
        Settlement(
          id = 2,
          uuid = "1b11adb9-4ccd-4a84-922a-68bb08cc1fa5",
          transactionReason = "any reason",
          createdBy = "some user 2",
          createdAt = LocalDateTime.of(2018, 10, 11, 0, 0),
          checkedBy = None,
          checkedAt = None,
          status = "bad status",
          fxProvider = None,
          fromCurrencyId = None,
          toCurrencyId = None,
          fxRate = None,
          lines = Seq(
            SettlementLines(
              id = 4, manualSettlementId = 2,
              accountId = 3, accountNumber = "5001 3287 1209 6021", direction = "debit",
              currencyId = 2, currency = "KES", amount = BigDecimal(5000.10),
              explanation = "no need of words just action"))))

      result.isRight mustBe true
      result.right.get mustBe expected
    }

    "count all settlements" in {
      val result = settlementDao.countSettlementsByCriteria(criteria = SettlementCriteria.NO_CRITERIA)
      result.isRight mustBe true
      result.right.get mustBe 3
    }

    "count all settlements which meets criteria" in {
      val result = settlementDao.countSettlementsByCriteria(
        criteria = SettlementCriteria(
          createdAtFrom = Some(LocalDateTime.of(2018, 10, 11, 0, 0)),
          createdAtTo = Some(LocalDateTime.of(2018, 10, 12, 0, 0)),
          currency = Some("KES")))
      result.isRight mustBe true
      result.right.get mustBe 1
    }

  }

  "Insert settlements with currency_exchange " should {
    "save settlement along with associated settlement_lines" in {
      val firstUuid = UUID.randomUUID().toString
      val dto = SettlementToInsert(
        uuid = firstUuid,
        createdBy = "unit test",
        createdAt = LocalDateTime.of(2018, 10, 12, 0, 0, 0),
        checkedBy = Option("backoffice user"),
        checkedAt = Option(LocalDateTime.of(2018, 10, 12, 0, 0, 0)),
        fxProvider = Some("Central bank of Kenya: CBK"),
        fromCurrencyId = Some(1),
        toCurrencyId = Some(2),
        fxRate = Some(BigDecimal(0.00983)),
        status = "approved",
        reason = "top up account",
        settlementLines = Seq(
          SettlementLinesToInsert(
            accountId = 1,
            direction = "debit",
            currencyId = 1,
            amount = BigDecimal(43261.00),
            explanation = "get 1250.01AED in this account"),
          SettlementLinesToInsert(
            accountId = 2,
            direction = "credit",
            currencyId = 2,
            amount = BigDecimal(425.46),
            explanation = "put 425.46KES from this account")))

      val result: DaoResponse[Settlement] = settlementDao.insertSettlement(dto)

      val expected = Settlement(
        id = 7,
        uuid = dto.uuid,
        transactionReason = dto.reason,
        createdBy = dto.createdBy,
        createdAt = dto.createdAt,
        checkedBy = dto.checkedBy,
        checkedAt = dto.checkedAt,
        fxProvider = Some("Central bank of Kenya: CBK"),
        fromCurrencyId = Some(1),
        toCurrencyId = Some(2),
        fxRate = Some(BigDecimal(0.00983)),
        status = dto.status,
        lines = Seq(
          SettlementLines(
            id = 7, manualSettlementId = 7,
            accountId = 1, accountNumber = "4716 4157 0907 3361", direction = "debit",
            currencyId = 1, currency = "AED", amount = BigDecimal(43261.00),
            explanation = "get 1250.01AED in this account"),
          SettlementLines(
            id = 8, manualSettlementId = 7,
            accountId = 2, accountNumber = "8912 3287 1209 3422", direction = "credit",
            currencyId = 2, currency = "KES", amount = BigDecimal(425.46),
            explanation = "put 425.46KES from this account")))

      result.isRight mustBe true
      result.right.get mustBe expected
    }

    "save another settlement with associated settlement_lines" in {
      val firstUuid = UUID.randomUUID().toString
      val dto = SettlementToInsert(
        uuid = firstUuid,
        createdBy = "unit test",
        createdAt = LocalDateTime.of(2019, 1, 12, 0, 0, 0),
        checkedBy = Option("backoffice user"),
        checkedAt = Option(LocalDateTime.of(2019, 1, 12, 0, 0, 0)),
        fxProvider = Some("Central bank of Philippines: BSP"),
        fromCurrencyId = Some(1),
        toCurrencyId = Some(3),
        fxRate = Some(BigDecimal(14.02)),
        status = "approved",
        reason = "top up account",
        settlementLines = Seq(
          SettlementLinesToInsert(
            accountId = 1,
            direction = "debit",
            currencyId = 1,
            amount = BigDecimal(1000.00),
            explanation = "get 1000AED in this account"),
          SettlementLinesToInsert(
            accountId = 2,
            direction = "credit",
            currencyId = 3,
            amount = BigDecimal(14020),
            explanation = "put 14020PHP from this account")))

      val result: DaoResponse[Settlement] = settlementDao.insertSettlement(dto)

      val expected = Settlement(
        id = 8,
        uuid = dto.uuid,
        transactionReason = dto.reason,
        createdBy = dto.createdBy,
        createdAt = dto.createdAt,
        checkedBy = dto.checkedBy,
        checkedAt = dto.checkedAt,
        fxProvider = Some("Central bank of Philippines: BSP"),
        fromCurrencyId = Some(1),
        toCurrencyId = Some(3),
        fxRate = Some(BigDecimal(14.02)),
        status = dto.status,
        lines = Seq(
          SettlementLines(
            id = 9, manualSettlementId = 8,
            accountId = 1, accountNumber = "4716 4157 0907 3361", direction = "debit",
            currencyId = 1, currency = "AED", amount = BigDecimal(1000.00),
            explanation = "get 1000AED in this account"),
          SettlementLines(
            id = 10, manualSettlementId = 8,
            accountId = 2, accountNumber = "8912 3287 1209 3422", direction = "credit",
            currencyId = 3, currency = "PHP", amount = BigDecimal(14020.00),
            explanation = "put 14020PHP from this account")))

      result.isRight mustBe true
      result.right.get mustBe expected
    }

    "count settlement fx history " in {
      val criteria = SettlementFxHistoryCriteria()

      val result = settlementDao.countSettlementFxHistory(criteria)

      result mustBe Right(2)

    }

    "get settlement fx history " in {
      val criteria = SettlementFxHistoryCriteria()
      val orderingSet = Some(OrderingSet(Ordering("created_at", Ordering.DESC)))

      val result = settlementDao.getSettlementFxHistory(criteria, orderingSet, None, None)

      val cbk = SettlementFxHistory(
        fxProvider = "Central bank of Kenya: CBK",
        fromCurrencyId = 1,
        fromCurrency = "AED",
        fromIcon = "aed_icon",
        toCurrencyId = 2,
        toCurrency = "KES",
        toIcon = "kes_icon",
        fxRate = BigDecimal(0.009830),
        createdAt = LocalDateTime.of(2018, 10, 12, 0, 0, 0))

      val bsp = SettlementFxHistory(
        fxProvider = "Central bank of Philippines: BSP",
        fromCurrencyId = 1,
        fromCurrency = "AED",
        fromIcon = "aed_icon",
        toCurrencyId = 3,
        toCurrency = "PHP",
        toIcon = "php_icon",
        fxRate = BigDecimal(14.020),
        createdAt = LocalDateTime.of(2019, 1, 12, 0, 0, 0))

      result mustBe Right(Seq(bsp, cbk))

    }

    "get settlement fx history filter by fxProvider" in {
      val criteria = SettlementFxHistoryCriteria(fxProvider = Some("Central bank of Philippines: BSP"))
      val orderingSet = Some(OrderingSet(Ordering("created_at", Ordering.DESC)))

      val result = settlementDao.getSettlementFxHistory(criteria, orderingSet, None, None)

      val bsp = SettlementFxHistory(
        fxProvider = "Central bank of Philippines: BSP",
        fromCurrencyId = 1,
        fromCurrency = "AED",
        fromIcon = "aed_icon",
        toCurrencyId = 3,
        toCurrency = "PHP",
        toIcon = "php_icon",
        fxRate = BigDecimal(14.020),
        createdAt = LocalDateTime.of(2019, 1, 12, 0, 0, 0))

      result mustBe Right(Seq(bsp))

    }

    "get settlement fx history filter by to_currency" in {
      val criteria = SettlementFxHistoryCriteria(toCurrency = Some("PHP"))
      val orderingSet = Some(OrderingSet(Ordering("created_at", Ordering.DESC)))

      val result = settlementDao.getSettlementFxHistory(criteria, orderingSet, None, None)

      val bsp = SettlementFxHistory(
        fxProvider = "Central bank of Philippines: BSP",
        fromCurrencyId = 1,
        fromCurrency = "AED",
        fromIcon = "aed_icon",
        toCurrencyId = 3,
        toCurrency = "PHP",
        toIcon = "php_icon",
        fxRate = BigDecimal(14.020),
        createdAt = LocalDateTime.of(2019, 1, 12, 0, 0, 0))

      result mustBe Right(Seq(bsp))

    }

    "get settlement fx history filter by from_currency" in {
      val criteria = SettlementFxHistoryCriteria(fromCurrency = Some("AED"))
      val orderingSet = Some(OrderingSet(Ordering("created_at", Ordering.ASC)))

      val result = settlementDao.getSettlementFxHistory(criteria, orderingSet, None, None)

      val cbk = SettlementFxHistory(
        fxProvider = "Central bank of Kenya: CBK",
        fromCurrencyId = 1,
        fromCurrency = "AED",
        fromIcon = "aed_icon",
        toCurrencyId = 2,
        toCurrency = "KES",
        toIcon = "kes_icon",
        fxRate = BigDecimal(0.009830),
        createdAt = LocalDateTime.of(2018, 10, 12, 0, 0, 0))

      val bsp = SettlementFxHistory(
        fxProvider = "Central bank of Philippines: BSP",
        fromCurrencyId = 1,
        fromCurrency = "AED",
        fromIcon = "aed_icon",
        toCurrencyId = 3,
        toCurrency = "PHP",
        toIcon = "php_icon",
        fxRate = BigDecimal(14.020),
        createdAt = LocalDateTime.of(2019, 1, 12, 0, 0, 0))

      result mustBe Right(Seq(cbk, bsp))

    }

    "get settlement fx history filter by created_at from and to" in {
      val criteria = SettlementFxHistoryCriteria(
        createdAtFrom = Some(LocalDateTime.of(2018, 1, 1, 0, 0, 0)),
        createdAtTo = Some(LocalDateTime.of(2018, 12, 30, 0, 0, 0)),
      )
      val orderingSet = Some(OrderingSet(Ordering("created_at", Ordering.ASC)))

      val result = settlementDao.getSettlementFxHistory(criteria, orderingSet, None, None)

      val cbk = SettlementFxHistory(
        fxProvider = "Central bank of Kenya: CBK",
        fromCurrencyId = 1,
        fromCurrency = "AED",
        fromIcon = "aed_icon",
        toCurrencyId = 2,
        toCurrency = "KES",
        toIcon = "kes_icon",
        fxRate = BigDecimal(0.009830),
        createdAt = LocalDateTime.of(2018, 10, 12, 0, 0, 0))

      result mustBe Right(Seq(cbk))
    }

    "get settlement fx history filter by created_at to" in {
      val criteria = SettlementFxHistoryCriteria(
        createdAtTo = Some(LocalDateTime.of(2018, 12, 30, 0, 0, 0))
      )
      val orderingSet = Some(OrderingSet(Ordering("created_at", Ordering.ASC)))

      val result = settlementDao.getSettlementFxHistory(criteria, orderingSet, None, None)

      val cbk = SettlementFxHistory(
        fxProvider = "Central bank of Kenya: CBK",
        fromCurrencyId = 1,
        fromCurrency = "AED",
        fromIcon = "aed_icon",
        toCurrencyId = 2,
        toCurrency = "KES",
        toIcon = "kes_icon",
        fxRate = BigDecimal(0.009830),
        createdAt = LocalDateTime.of(2018, 10, 12, 0, 0, 0))

      result mustBe Right(Seq(cbk))
    }

    "get settlement fx history filter by created_at from" in {
      val criteria = SettlementFxHistoryCriteria(createdAtFrom = Some(LocalDateTime.of(2019, 1, 1, 0,0,0)))
      val orderingSet = Some(OrderingSet(Ordering("created_at", Ordering.ASC)))

      val result = settlementDao.getSettlementFxHistory(criteria, orderingSet, None, None)

      val bsp = SettlementFxHistory(
        fxProvider = "Central bank of Philippines: BSP",
        fromCurrencyId = 1,
        fromCurrency = "AED",
        fromIcon = "aed_icon",
        toCurrencyId = 3,
        toCurrency = "PHP",
        toIcon = "php_icon",
        fxRate = BigDecimal(14.020),
        createdAt = LocalDateTime.of(2019, 1, 12, 0, 0, 0))

      result mustBe Right(Seq(bsp))

    }

    "get settlemet recent accounts " in {
      val criteria = SettlementRecentAccountCriteria()
      val result = settlementDao.getSettlementRecentAccounts(criteria, None, None)

      val recent1 =SettlementRecentAccount(
        accountId = 1,
        accountUUID = "1c15bdb9-5d42-4a84-922a-0d01f8e5de71",
        customerName = Some("george_ogalo"),
        accountNumber = "4716 4157 0907 3361",
        accountName = Some("George Ogalo"),
        balance = BigDecimal(0),
        currency = "AED")

      val recent2 = SettlementRecentAccount(
        accountId = 2,
        accountUUID = "c86eb85f-70f8-4ccd-ae28-68bb08cc1fa5",
        customerName = Some("Ujali Sharma"),
        accountNumber = "8912 3287 1209 3422",
        accountName = Some("Ujali Tyagi"),
        balance = BigDecimal(20.00),
        currency = "AED")

      val recent3 = SettlementRecentAccount(
        accountId = 3,
        accountUUID = "bcc32571-70f8-4ccd-ae28-35f0030c3b1d",
        customerName = Some("Ujali Sharma"),
        accountNumber = "5001 3287 1209 6021",
        accountName = Some("Ujali Tyagi"),
        balance = BigDecimal(20.00),
        currency = "KES")

      result.map(_.toSet) mustBe Right(Set(recent2, recent1))

    }
  }
}
