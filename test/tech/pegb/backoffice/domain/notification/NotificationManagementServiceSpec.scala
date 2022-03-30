package tech.pegb.backoffice.domain.notification

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import cats.implicits._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Seconds, Span}
import play.api.inject.bind
import tech.pegb.backoffice.dao.DaoError
import tech.pegb.backoffice.dao.i18n.abstraction.I18nStringDao
import tech.pegb.backoffice.dao.notification.abstraction.{NotificationDao, NotificationTemplateDao}
import tech.pegb.backoffice.dao.notification.dto.{NotificationTemplateToUpdate ⇒ DaoNotificationTemplateToUpdate}
import tech.pegb.backoffice.dao.notification.entity.{Notification, NotificationTemplate}
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.notification.abstraction.NotificationManagementService
import tech.pegb.backoffice.domain.notification.dto.{NotificationCriteria, NotificationTemplateCriteria, NotificationTemplateToCreate}
import tech.pegb.backoffice.mapping.dao.domain.notification.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.notification.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

class NotificationManagementServiceSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  implicit val defaultPatience = PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))
  private val notifTemplateDao = stub[NotificationTemplateDao]
  private val notifDao = stub[NotificationDao]
  private val i18nStringDao = stub[I18nStringDao]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[I18nStringDao].to(i18nStringDao),
      bind[NotificationDao].to(notifDao),
      bind[NotificationTemplateDao].to(notifTemplateDao),
      bind[WithExecutionContexts].to(TestExecutionContext))

  val notificationMgmtService = inject[NotificationManagementService]
  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  "NotificationManagementService createNotificationTemplate" should {
    "return created notificationTemplate" in {
      val dto = NotificationTemplateToCreate(
        createdAt = now,
        createdBy = "george",
        name = "template_1",
        titleResource = "title_resource_1",
        defaultTitle = "default_title_1",
        contentResource = "content_resource_1",
        defaultContent = "default_content_1",
        channels = Seq("push", "sms"),
        description = "description of template 1".some)

      val expected = NotificationTemplate(
        id = 1,
        uuid = UUID.randomUUID().toString,
        name = dto.name,
        titleResource = dto.titleResource,
        defaultTitle = dto.defaultTitle,
        contentResource = dto.contentResource,
        defaultContent = dto.defaultContent,
        description = dto.contentResource.some,
        channels = dto.channels.mkString(","),
        createdAt = dto.createdAt,
        createdBy = dto.createdBy,
        updatedAt = None,
        updatedBy = None,
        isActive = true)

      (typesDao.getCommunicationChannels _).when()
        .returns(Right(List((14, "push", "push".some), (15, "sms", "sms".some), (16, "email", "email".some))))

      (notifTemplateDao.insertNotificationTemplate _).when(dto.asDao)
        .returns(Right(expected))

      val res = notificationMgmtService.createNotificationTemplate(dto)
      whenReady(res) { actual ⇒
        actual mustBe Right(expected.asDomain)
      }
    }

    "return error when notificationTemplate has invalid channel" in {

      val dto = NotificationTemplateToCreate(
        createdAt = now,
        createdBy = "george",
        name = "template_1",
        titleResource = "title_resource_1",
        defaultTitle = "default_title_1",
        contentResource = "content_resource_1",
        defaultContent = "default_content_1",
        channels = Seq("push", "deadbeef"),
        description = "description of template 1".some)

      (typesDao.getCommunicationChannels _).when()
        .returns(Right(List((14, "push", "push".some), (15, "sms", "sms".some), (16, "email", "email".some))))

      val res = notificationMgmtService.createNotificationTemplate(dto)
      whenReady(res) { actual ⇒
        actual mustBe Left(ServiceError.validationError(s"provided element `deadbeef` is invalid for 'communication_channels'"))
      }
    }

  }

  "NotificationManagementService getNotificationTemplatesByCriteria" should {
    "return notification template lists by criteria" in {
      val t1 = NotificationTemplate(
        id = 1,
        uuid = UUID.randomUUID().toString,
        name = "template_1",
        titleResource = "title_resource_1",
        defaultTitle = "default_title_1",
        contentResource = "content_resource_1",
        defaultContent = "default_content_1",
        description = "description of template 1".some,
        channels = s"[push, sms]",
        createdAt = now,
        createdBy = "george",
        updatedAt = None,
        updatedBy = None,
        isActive = true)

      val t2 = NotificationTemplate(
        id = 2,
        uuid = UUID.randomUUID().toString,
        name = "template_2",
        titleResource = "title_resource_2",
        defaultTitle = "default_title_2",
        contentResource = "content_resource_2",
        defaultContent = "default_content_2",
        description = "description of template 2".some,
        channels = s"[push, email]",
        createdAt = now,
        createdBy = "george",
        updatedAt = None,
        updatedBy = None,
        isActive = true)

      val criteria = NotificationTemplateCriteria(
        isActive = true.some,
        channel = "push".some)

      (notifTemplateDao.getNotificationTemplateByCriteria _).when(criteria.asDao.some, None, None, None)
        .returns(Right(List(t1, t2)))

      val result = notificationMgmtService.getNotificationTemplatesByCriteria(criteria.some, Nil, none, none)

      whenReady(result) { actual ⇒
        actual mustBe Right(Seq(t1, t2).map(_.asDomain))
      }

    }

    "return count template by criteria" in {
      val criteria = NotificationTemplateCriteria(
        isActive = true.some,
        channel = "push".some)

      (notifTemplateDao.getCountByCriteria _).when(criteria.asDao.some)
        .returns(Right(2))

      val result = notificationMgmtService.countNotificationTemplatesByCriteria(criteria.some)

      whenReady(result) { actual ⇒
        actual mustBe Right(2)
      }
    }
  }

  "NotificationManagementService updatestatus" should {
    "set isActive to true for activateNotificationTemplate" in {

      val uuid = UUID.randomUUID()
      val updatedAt = now
      val updatedBy = "habibi"

      val t1 = NotificationTemplate(
        id = 1,
        uuid = uuid.toString,
        name = "template_1",
        titleResource = "title_resource_1",
        defaultTitle = "default_title_1",
        contentResource = "content_resource_1",
        defaultContent = "default_content_1",
        description = "description of template 1".some,
        channels = "[push, sms]",
        createdAt = now,
        createdBy = "george",
        updatedAt = None,
        updatedBy = None,
        isActive = true)

      (notifTemplateDao.updateNotificationTemplate _).when(uuid.toString, DaoNotificationTemplateToUpdate(
        updatedAt = updatedAt,
        updatedBy = updatedBy,
        isActive = true.some,
        lastUpdatedAt = none)).
        returns(Right(t1.copy(isActive = true, updatedAt = updatedAt.some, updatedBy = updatedBy.some)))

      val result = notificationMgmtService.activateNotificationTemplate(uuid, updatedBy, updatedAt, lastUpdatedAt = none)

      whenReady(result) { actual ⇒
        actual mustBe Right(())
      }

    }

    "set isActive to false for deactivateNotificationTemplate" in {

      val uuid = UUID.randomUUID()
      val updatedAt = now
      val updatedBy = "habibi"

      val t1 = NotificationTemplate(
        id = 1,
        uuid = uuid.toString,
        name = "template_1",
        titleResource = "title_resource_1",
        defaultTitle = "default_title_1",
        contentResource = "content_resource_1",
        defaultContent = "default_content_1",
        description = "description of template 1".some,
        channels = "[push, sms]",
        createdAt = now,
        createdBy = "george",
        updatedAt = None,
        updatedBy = None,
        isActive = true)

      (notifTemplateDao.updateNotificationTemplate _).when(uuid.toString, DaoNotificationTemplateToUpdate(
        updatedAt = updatedAt,
        updatedBy = updatedBy,
        isActive = false.some,
        lastUpdatedAt = none)).
        returns(Right(t1.copy(isActive = false, updatedAt = updatedAt.some, updatedBy = updatedBy.some)))

      val result = notificationMgmtService.deactivateNotificationTemplate(uuid, updatedBy, updatedAt, lastUpdatedAt = none)

      whenReady(result) { actual ⇒
        actual mustBe Right(())
      }

    }

  }

  "NotificationManagementService getNotificationsByCriteria" should {
    "return notification lists by criteria" in {
      val t1 = Notification(
        id = 1,
        uuid = UUID.randomUUID().toString,
        userId = 1.some,
        userUuid = UUID.randomUUID().toString.some,
        templateId = 1,
        templateUuid = UUID.randomUUID().toString,
        channel = "sms",
        title = "title_1",
        content = "content_1",
        address = "address_1",
        status = "active",
        errorMsg = None,
        retries = 5.some,
        operationId = UUID.randomUUID().toString,
        createdBy = "george",
        createdAt = now,
        updatedBy = none,
        updatedAt = none,
        sentAt = none)

      val t2 = Notification(
        id = 2,
        uuid = UUID.randomUUID().toString,
        userId = 2.some,
        userUuid = UUID.randomUUID().toString.some,
        templateId = 2,
        templateUuid = UUID.randomUUID().toString,
        channel = "push",
        title = "title_2",
        content = "content_2",
        address = "address_2",
        status = "active",
        errorMsg = None,
        retries = 5.some,
        operationId = UUID.randomUUID().toString,
        createdBy = "george",
        createdAt = now,
        updatedBy = none,
        updatedAt = none,
        sentAt = none)

      val criteria = NotificationCriteria(
        status = "active".some)

      (notifDao.getNotificationByCriteria _).when(criteria.asDao.some, None, None, None)
        .returns(Right(List(t1, t2)))

      val result = notificationMgmtService.getNotificationsByCriteria(criteria.some, Nil, none, none)

      whenReady(result) { actual ⇒
        actual mustBe Right(Seq(t1, t2).map(_.asDomain))
      }

    }

    "return count by criteria" in {
      val criteria = NotificationCriteria(
        status = "active".some)

      (notifDao.getCountByCriteria _).when(criteria.asDao.some)
        .returns(Right(2))

      val result = notificationMgmtService.countNotificationsByCriteria(criteria.some)

      whenReady(result) { actual ⇒
        actual mustBe Right(2)
      }
    }

    "return Right(Unit) if update successful" in {
      val mockId = UUID.randomUUID()
      val dto = tech.pegb.backoffice.domain.notification.dto.NotificationTemplateToUpdate(
        updatedAt = LocalDateTime.now(),
        updatedBy = "pegbuser",
        titleResource = "new title".toOption,
        description = "new description".toOption,
        channels = Seq("new channel").toOption,
        isActive = false.toOption)

      val mockResult = NotificationTemplate(
        id = 1,
        uuid = mockId.toString,
        name = "old name", titleResource = "new title", defaultTitle = "old default title",
        contentResource = "old content", defaultContent = "old content",
        description = None, channels = "[new channel]",
        createdAt = LocalDateTime.now, updatedAt = dto.updatedAt.toOption,
        createdBy = "admin", updatedBy = "pegbuser".toOption, isActive = false)

      (notifTemplateDao.updateNotificationTemplate _)
        .when(mockId.toString, dto.asDao).returns(Right(mockResult))

      val result = notificationMgmtService.updateNotificationTemplate(mockId, dto)

      whenReady(result) { actual ⇒
        actual mustBe Right(())
      }
    }

    "return Left(ServiceError) if update not successful" in {
      val mockId = UUID.randomUUID()
      val dto = tech.pegb.backoffice.domain.notification.dto.NotificationTemplateToUpdate(
        updatedAt = LocalDateTime.now(),
        updatedBy = "pegbuser",
        titleResource = "new title".toOption,
        description = "new description".toOption,
        channels = Seq("new channel").toOption,
        isActive = false.toOption)

      (notifTemplateDao.updateNotificationTemplate _)
        .when(mockId.toString, dto.asDao).returns(Left(DaoError.GenericDbError("some db error")))

      val result = notificationMgmtService.updateNotificationTemplate(mockId, dto)

      whenReady(result) { actual ⇒
        actual.left.get.message mustBe "some db error"
      }
    }

  }
}
