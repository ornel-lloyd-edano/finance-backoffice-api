package tech.pegb.backoffice.dao

import java.time.{Clock, Instant, LocalDateTime, ZoneId}

import cats.implicits._
import tech.pegb.backoffice.dao.DaoError.{ConstraintViolationError, PreconditionFailed}
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes, Ordering, OrderingSet}
import tech.pegb.backoffice.dao.settings.abstraction.SystemSettingsDao
import tech.pegb.backoffice.dao.settings.dto.{SystemSettingToInsert, SystemSettingToUpdate, SystemSettingsCriteria}
import tech.pegb.backoffice.dao.settings.entity._
import tech.pegb.backoffice.util.AppConfig
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.core.PegBTestApp

class SystemSettingsSqlDaoSpec extends PegBTestApp {

  val config = inject[AppConfig]

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  private val dao = inject[SystemSettingsDao]

  override def initSql =
    s"""
       |INSERT INTO system_settings
       |(id, `key`, `value`, created_at, updated_at, `type`, for_android, for_ios, for_backoffice, created_by, updated_by, explanation)
       |VALUES
       |(1, 'innovatrics_field_editable_if_below', '0.7', '$now', '$now', 'float', 0, 0, 0, 'pegbuser', null, 'innovatrics threshold'),
       |(2, 'saving_goal_reasons', '["vacation","marriage","birthday","gift","new_car"]', '$now', '$now', 'json', 1, 1, 0, 'pegbuser', null, null),
       |(3, 'saving_goal_max_amount', '12000', '$now', '$now', 'integer', 0, 0, 1, 'pegbuser', null, null),
       |(4, 'roundup_saving_tx_types', '["p2p_domestic"]', '$now', '$now', 'json', 0, 1, 0, 'pegbuser', null, null),
       |(5, 'roundup_saving_nearest', '10', '$now', '$now', 'integer', 1, 1, 0, 'pegbuser', null, null);
     """.stripMargin

  "SystemSettingsSqlDao retrieve" should {
    "return SystemSetting in getSystemSettingById if exist" in {

      val resp = dao.getSystemSettingById(1)

      resp.map(_.get.id) mustBe Right(1)
      resp.map(_.get.key) mustBe Right("innovatrics_field_editable_if_below")
      resp.map(_.get.value) mustBe Right("0.7")
      resp.map(_.get.`type`) mustBe Right("float")
      resp.map(_.get.explanation) mustBe Right("innovatrics threshold".some)
      resp.map(_.get.forAndroid) mustBe Right(false)
      resp.map(_.get.forIOS) mustBe Right(false)
      resp.map(_.get.forBackoffice) mustBe Right(false)
      resp.map(_.get.createdAt) mustBe Right(now)
      resp.map(_.get.createdBy) mustBe Right("pegbuser")
    }

    "return None in getSystemSettingById if NOT exist" in {

      val resp = dao.getSystemSettingById(10000)

      resp mustBe Right(None)
    }

    "return systemSettings seq in getSystemSettingCriteria - id filter" in {
      val criteria = SystemSettingsCriteria(
        id = Option(CriteriaField("", 1)),
        key = None,
        explanation = None,
        forAndroid = None,
        forIOS = None,
        forBackoffice = None)

      val resp = dao.getSystemSettingsByCriteria(Some(criteria), None, None, None)

      val expected = SystemSetting(
        id = 1,
        key = "innovatrics_field_editable_if_below",
        value = "0.7",
        `type` = "float",
        explanation = "innovatrics threshold".some,
        forAndroid = false,
        forIOS = false,
        forBackoffice = false,
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = now.some,
        updatedBy = none)

      resp mustBe Right(Seq(expected))

    }

    "return systemSettings seq in getSystemSettingCriteria - key filter" in {
      val criteria = SystemSettingsCriteria(
        id = None,
        key = Option(CriteriaField("", "innovatrics_field_editable_if_below")),
        explanation = None,
        forAndroid = None,
        forIOS = None,
        forBackoffice = None)

      val resp = dao.getSystemSettingsByCriteria(Some(criteria), None, None, None)

      val expected = SystemSetting(
        id = 1,
        key = "innovatrics_field_editable_if_below",
        value = "0.7",
        `type` = "float",
        explanation = "innovatrics threshold".some,
        forAndroid = false,
        forIOS = false,
        forBackoffice = false,
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = now.some,
        updatedBy = none)

      resp mustBe Right(Seq(expected))

    }

    "return systemSettings seq in getSystemSettingCriteria - explanation filter" in {
      val criteria = SystemSettingsCriteria(
        id = None,
        key = None,
        explanation = Option(CriteriaField("", "innovatrics", MatchTypes.Partial)),
        forAndroid = None,
        forIOS = None,
        forBackoffice = None)

      val resp = dao.getSystemSettingsByCriteria(Some(criteria), None, None, None)

      val expected = SystemSetting(
        id = 1,
        key = "innovatrics_field_editable_if_below",
        value = "0.7",
        `type` = "float",
        explanation = "innovatrics threshold".some,
        forAndroid = false,
        forIOS = false,
        forBackoffice = false,
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = now.some,
        updatedBy = none)

      resp mustBe Right(Seq(expected))

    }

    "return systemSettings seq in getSystemSettingCriteria - forAndroid filter" in {
      val criteria = SystemSettingsCriteria(
        id = None,
        key = None,
        explanation = None,
        forAndroid = CriteriaField("", true).some,
        forIOS = None,
        forBackoffice = None)

      val resp = dao.getSystemSettingsByCriteria(Some(criteria), None, None, None)

      val s1 = SystemSetting(
        id = 2,
        key = "saving_goal_reasons",
        value = """["vacation","marriage","birthday","gift","new_car"]""",
        `type` = "json",
        explanation = None,
        forAndroid = true,
        forIOS = true,
        forBackoffice = false,
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = now.some,
        updatedBy = none)

      val s3 = SystemSetting(
        id = 5,
        key = "roundup_saving_nearest",
        value = "10",
        `type` = "integer",
        explanation = None,
        forAndroid = true,
        forIOS = true,
        forBackoffice = false,
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = now.some,
        updatedBy = none)

      resp mustBe Right(Seq(s1, s3))

    }
    "return systemSettings seq in getSystemSettingCriteria - forIOS filter" in {
      val criteria = SystemSettingsCriteria(
        id = None,
        key = None,
        explanation = None,
        forAndroid = None,
        forIOS = CriteriaField("", true).some,
        forBackoffice = None)
      val orderingSet = OrderingSet(Ordering("id", Ordering.DESC)).some

      val resp = dao.getSystemSettingsByCriteria(Some(criteria), orderingSet, None, None)

      val s1 = SystemSetting(
        id = 2,
        key = "saving_goal_reasons",
        value = """["vacation","marriage","birthday","gift","new_car"]""",
        `type` = "json",
        explanation = None,
        forAndroid = true,
        forIOS = true,
        forBackoffice = false,
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = now.some,
        updatedBy = none)

      val s2 = SystemSetting(
        id = 4,
        key = "roundup_saving_tx_types",
        value = """["p2p_domestic"]""",
        `type` = "json",
        explanation = None,
        forAndroid = false,
        forIOS = true,
        forBackoffice = false,
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = now.some,
        updatedBy = none)

      val s3 = SystemSetting(
        id = 5,
        key = "roundup_saving_nearest",
        value = "10",
        `type` = "integer",
        explanation = None,
        forAndroid = true,
        forIOS = true,
        forBackoffice = false,
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = now.some,
        updatedBy = none)

      resp mustBe Right(Seq(s3, s2, s1))

    }

    "return systemSettings seq in getSystemSettingCriteria - forBackoffice filter" in {
      val criteria = SystemSettingsCriteria(
        id = None,
        key = None,
        explanation = None,
        forAndroid = None,
        forIOS = None,
        forBackoffice = CriteriaField("", true).some)

      val resp = dao.getSystemSettingsByCriteria(Some(criteria), None, None, None)

      val s1 = SystemSetting(
        id = 3,
        key = "saving_goal_max_amount",
        value = """12000""",
        `type` = "integer",
        explanation = None,
        forAndroid = false,
        forIOS = false,
        forBackoffice = true,
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = now.some,
        updatedBy = none)

      resp mustBe Right(Seq(s1))

    }

    "return systemSettings seq in getSystemSettingCriteria - multiple filter" in {
      val criteria = SystemSettingsCriteria(
        id = None,
        key = None,
        explanation = None,
        forAndroid = CriteriaField("", false).some,
        forIOS = CriteriaField("", true).some,
        forBackoffice = None)
      val orderingSet = OrderingSet(Ordering("id", Ordering.ASC)).some

      val resp = dao.getSystemSettingsByCriteria(Some(criteria), None, None, None)

      val s2 = SystemSetting(
        id = 4,
        key = "roundup_saving_tx_types",
        value = """["p2p_domestic"]""",
        `type` = "json",
        explanation = None,
        forAndroid = false,
        forIOS = true,
        forBackoffice = false,
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = now.some,
        updatedBy = none)

      resp mustBe Right(Seq(s2))

    }

    "return list of types for getSystemSettingsTypes" in {
      val resp = dao.getSystemSettingsTypes()
      resp mustBe Right(Seq("float", "integer", "json"))
    }

    "return count in countSystemSettingsByCriteria - no filter" in {
      val criteria = SystemSettingsCriteria()

      val resp = dao.countSystemSettingsByCriteria(criteria)

      resp mustBe Right(5)

    }

    "return count in countSystemSettingsByCriteria - platform filter" in {
      val criteria = SystemSettingsCriteria(
        id = None,
        key = None,
        explanation = None,
        forAndroid = None,
        forIOS = CriteriaField("", true).some,
        forBackoffice = None)

      val resp = dao.countSystemSettingsByCriteria(criteria)

      resp mustBe Right(3)

    }
  }

  "SystemSettingsSqlDao create" should {
    "return newly created systemSettings" in {
      val dto = SystemSettingToInsert(
        key = "default_currency",
        value = "KES",
        `type` = "string",
        explanation = None,
        forAndroid = true,
        forIOS = true,
        forBackoffice = false,
        createdAt = now,
        createdBy = "pegbuser")

      val resp = dao.insertSystemSetting(dto)

      resp.map(_.key) mustBe Right(dto.key)
      resp.map(_.value) mustBe Right(dto.value)
      resp.map(_.`type`) mustBe Right(dto.`type`)
      resp.map(_.explanation) mustBe Right(dto.explanation)
      resp.map(_.forAndroid) mustBe Right(dto.forAndroid)
      resp.map(_.forIOS) mustBe Right(dto.forIOS)
      resp.map(_.forBackoffice) mustBe Right(dto.forBackoffice)
      resp.map(_.createdAt) mustBe Right(dto.createdAt)
      resp.map(_.createdBy) mustBe Right(dto.createdBy)

    }

    "return exception when trying to insert same key" in {
      val dto = SystemSettingToInsert(
        key = "default_currency",
        value = "KES",
        `type` = "string",
        explanation = None,
        forAndroid = true,
        forIOS = true,
        forBackoffice = false,
        createdAt = now,
        createdBy = "pegbuser")

      val resp = dao.insertSystemSetting(dto)

      resp mustBe ConstraintViolationError(s"Could not create system setting ${dto.toSmartString}").asLeft[SystemSetting]

    }
  }

  "SystemSettings update" should {
    "successfully update system settings if correct data (value)" in {
      val dto = SystemSettingToUpdate(
        value = "0.8".some,
        updatedAt = now,
        updatedBy = "george",
        lastUpdatedAt = now.some)

      val resp = dao.updateSystemSettings(1, dto)
      val expected = SystemSetting(
        id = 1,
        key = "innovatrics_field_editable_if_below",
        value = "0.8",
        `type` = "float",
        explanation = "innovatrics threshold".some,
        forAndroid = false,
        forIOS = false,
        forBackoffice = false,
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = now.some,
        updatedBy = "george".some)

      resp mustBe Right(expected)
    }

    "successfully update system settings if correct data (all data)" in {
      val dto = SystemSettingToUpdate(
        key = "default_institution".some,
        value = "pesalink".some,
        `type` = "string".some,
        explanation = "default institution name".some,
        forAndroid = true.some,
        forIOS = true.some,
        forBackoffice = true.some,
        updatedAt = now,
        updatedBy = "george",
        lastUpdatedAt = now.some)

      val resp = dao.updateSystemSettings(1, dto)
      val expected = SystemSetting(
        id = 1,
        key = "default_institution",
        value = "pesalink",
        `type` = "string",
        explanation = "default institution name".some,
        forAndroid = true,
        forIOS = true,
        forBackoffice = true,
        createdAt = now,
        createdBy = "pegbuser",
        updatedAt = now.some,
        updatedBy = "george".some)

      resp mustBe Right(expected)
    }

    "fail on update system settings when stale data" in {
      val dto = SystemSettingToUpdate(
        key = "default_institution".some,
        value = "pesalink".some,
        `type` = "string".some,
        explanation = "default institution name".some,
        forAndroid = true.some,
        forIOS = true.some,
        forBackoffice = true.some,
        updatedAt = now,
        updatedBy = "george",
        lastUpdatedAt = LocalDateTime.now().some)

      val resp = dao.updateSystemSettings(1, dto)

      resp mustBe Left(PreconditionFailed(s"Update failed. System Settings id: 1 has been modified by another process."))
    }
  }

  "get system setting by key" in {
    val expected = SystemSetting(
      id = 1,
      key = "default_institution",
      value = "pesalink",
      `type` = "string",
      explanation = "default institution name".some,
      forAndroid = true,
      forIOS = true,
      forBackoffice = true,
      createdAt = now,
      createdBy = "pegbuser",
      updatedAt = now.some,
      updatedBy = "george".some)

    val resp = dao.getSystemSettingByKey("default_institution")

    resp mustBe Right(Some(expected))
  }

}
