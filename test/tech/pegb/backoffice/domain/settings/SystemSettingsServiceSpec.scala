package tech.pegb.backoffice.domain.settings

import java.time.{Clock, Instant, LocalDateTime, ZoneId}

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.inject.bind
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.settings.abstraction.SystemSettingsDao
import tech.pegb.backoffice.dao.settings.dto.SystemSettingsCriteria
import tech.pegb.backoffice.domain.model.Ordering
import tech.pegb.backoffice.domain.settings.abstraction.SystemSettingService
import tech.pegb.backoffice.domain.settings.dto.{SystemSettingsCriteria ⇒ DomainCriteria}
import tech.pegb.backoffice.domain.settings.model.SystemSetting
import tech.pegb.backoffice.mapping.domain.dao.setting.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

class SystemSettingsServiceSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  val mockClock = Clock.fixed(Instant.ofEpochMilli(1563288490000L), ZoneId.of("UTC"))
  val now = LocalDateTime.now(mockClock)

  val systemSettingsDao = stub[SystemSettingsDao]
  val systemSettingsService = inject[SystemSettingService]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[SystemSettingsDao].to(systemSettingsDao),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val systemSettingDomainOne = SystemSetting(
    id = 1,
    key = "float.account.numbers",
    value = "[acc_number_one, acc_number_two, acc_number_three]",
    `type` = "json",
    explanation = Some("List of accounts that should appear in float management"),
    forAndroid = false,
    forIOS = true,
    forBackoffice = true,
    createdAt = now,
    createdBy = "ujali",
    updatedAt = None,
    updatedBy = None)

  val systemSettingDomainTwo = SystemSetting(
    id = 2,
    key = "roundup_saving_tx_types",
    value = "[\"p2p_domestic\"]",
    `type` = "json",
    explanation = Some("The roundup value for saving accounts"),
    forAndroid = false,
    forIOS = true,
    forBackoffice = true,
    createdAt = now,
    createdBy = "david",
    updatedAt = None,
    updatedBy = None)

  val systemSettingDomainThree = SystemSetting(
    id = 3,
    key = "saving_goal_reasons",
    value = "[\"vacation\",\"marriage\",\"birthday\",\"gift\",\"new_car\"]",
    `type` = "json",
    explanation = None,
    forAndroid = true,
    forIOS = true,
    forBackoffice = true,
    createdAt = now,
    createdBy = "lloyd",
    updatedAt = None,
    updatedBy = None)
  val allSystemSettings = Seq(systemSettingDomainOne.asDao, systemSettingDomainTwo.asDao, systemSettingDomainThree.asDao)

  "SystemSettingsService" should {
    "get all system settings when no criteria" in {

      val criteria: Option[DomainCriteria] = None
      val orderBy: Seq[Ordering] = Seq.empty
      val limit: Option[Int] = None
      val offset: Option[Int] = None

      (systemSettingsDao.getSystemSettingsByCriteria(
        _: Option[SystemSettingsCriteria],
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])).when(None, None, None, None)
        .returns(Right(allSystemSettings))
      val expectedSystemSettings = Seq(systemSettingDomainOne, systemSettingDomainTwo, systemSettingDomainThree)

      val resultF = systemSettingsService.getSystemSettingsByCriteria(criteria, orderBy, limit, offset)

      whenReady(resultF) { result ⇒
        result.isRight mustBe true
        result.right.get mustBe expectedSystemSettings

      }
    }

    "get system settings by criteria" in {

      val criteria = Some(
        DomainCriteria(
          explanation = None,
          forAndroid = Some(true)))
      val orderBy: Seq[Ordering] = Seq.empty
      val limit: Option[Int] = None
      val offset: Option[Int] = None

      (systemSettingsDao.getSystemSettingsByCriteria(
        _: Option[SystemSettingsCriteria],
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])).when(criteria.map(_.asDao), None, None, None)
        .returns(Right(Seq(
          systemSettingDomainOne.asDao,
          systemSettingDomainTwo.asDao)))
      val expectedSystemSettings = Seq(systemSettingDomainOne, systemSettingDomainTwo)

      val resultF = systemSettingsService.getSystemSettingsByCriteria(criteria, orderBy, limit, offset)

      whenReady(resultF) { result ⇒
        result.isRight mustBe true
        result.right.get mustBe expectedSystemSettings

      }
    }

    "get system settings by criteria with limit and offset" in {

      val criteria = Some(
        DomainCriteria(
          explanation = None,
          forAndroid = Some(true)))
      val orderBy: Seq[Ordering] = Seq.empty
      val limit: Option[Int] = Some(1)
      val offset: Option[Int] = Some(2)

      (systemSettingsDao.getSystemSettingsByCriteria(
        _: Option[SystemSettingsCriteria],
        _: Option[OrderingSet],
        _: Option[Int],
        _: Option[Int])).when(criteria.map(_.asDao), None, limit, offset)
        .returns(Right(Seq(systemSettingDomainTwo.asDao)))
      val expectedSystemSettings = Seq(systemSettingDomainTwo)

      val resultF = systemSettingsService.getSystemSettingsByCriteria(criteria, orderBy, limit, offset)

      whenReady(resultF) { result ⇒
        result.isRight mustBe true
        result.right.get mustBe expectedSystemSettings

      }
    }

  }
}
