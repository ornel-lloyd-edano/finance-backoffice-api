package tech.pegb.backoffice.dao

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import cats.implicits._
import tech.pegb.backoffice.dao.model.{CriteriaField, Ordering, OrderingSet}
import tech.pegb.backoffice.dao.notification.abstraction.NotificationTemplateDao
import tech.pegb.backoffice.dao.notification.dto.{NotificationTemplateCriteria, NotificationTemplateToInsert}
import tech.pegb.core.PegBTestApp

import scala.collection.mutable

class NotificationTemplatesSqlDaoSpec extends PegBTestApp {
  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  val UUID1 = UUID.randomUUID()
  val UUID2 = UUID.randomUUID()
  val UUID3 = UUID.randomUUID()

  override def initSql =
    s"""
       |
       |INSERT INTO notification_templates(id, uuid, name, title_resource, default_title,content_resource, default_content, channels,
       | description, created_at, created_by, updated_at, updated_by, is_active)
       |VALUES('1', '$UUID1','test_template_one', 'limitProfiles_delete_warning_msg', 'live, laugh and love','warning_msg', 'keep smiling',
       |'mobile','test description',  '$now', 'ujali', null, null, 1),
       |('2', '$UUID2','test_template_two', 'test_resource_two', 'peace comes from within','error_msg', 'keep smiling',
       |'mobile','test description',  '$now', 'ujali', null, null, 1),
       |('3', '$UUID3','test_template_three', 'test_resource_three', 'test_something','test_msg', 'keep smiling always',
       |'mobile','test description one',  '$now', 'david', null, null, 1);

    """.stripMargin

  override def cleanupSql =
    s"DELETE FROM notification_templates; "

  val notificationTemplateDao = inject[NotificationTemplateDao]

  "Notification Dao" should {
    "insert notification" in {
      val notificationTemplateToInsert = NotificationTemplateToInsert(
        createdAt = now,
        createdBy = "lloyd",
        name = "test_template_four",
        titleResource = "test_resource",
        defaultTitle = "test_title",
        contentResource = "test_resource",
        defaultContent = "test_default_resource",
        channels = "[sms, email]",
        description = none,
        isActive = false)

      val result = notificationTemplateDao.insertNotificationTemplate(notificationTemplateToInsert)

      result.right.map(_.name) mustBe Right("test_template_four")
    }

    "count all notifications records" in {
      val result = notificationTemplateDao.getCountByCriteria(none)
      result.right.get mustBe 4
    }

    "get notifications records by criteria" in {
      val criteriaField = CriteriaField("title_resource", "limitProfiles_delete_warning_msg")
      val criteria = NotificationTemplateCriteria(titleResource = criteriaField.some)

      val result = notificationTemplateDao.getNotificationTemplateByCriteria(criteria.some, none, none, none)

      result.right.get.map(_.defaultTitle) mustBe Seq("live, laugh and love")
      result.right.get.size mustBe 1
    }

    "get all notifications records with no criteria" in {
      val result = notificationTemplateDao.getNotificationTemplateByCriteria(none, none, none, none)

      result.right.get.map(_.defaultTitle) mustBe Seq("live, laugh and love", "peace comes from within", "test_something", "test_title")
      result.right.get.size mustBe 4
    }

    "get notifications records by extended criteria" in {
      val criteriaFieldOne = CriteriaField("created_by", "ujali")
      val criteriaFieldTwo = CriteriaField("channels", "mobile")
      val criteria = NotificationTemplateCriteria(createdBy = criteriaFieldOne.some, channels = criteriaFieldTwo.some)

      val result = notificationTemplateDao.getNotificationTemplateByCriteria(criteria.some, none, none, none)
      result.right.get.map(_.defaultTitle) mustBe Seq("live, laugh and love", "peace comes from within")
      result.right.get.size mustBe 2
    }

    "get notifications records by criteria with pagination and ordering" in {

      val orderBy = Option(OrderingSet(mutable.LinkedHashSet(Ordering("default_title", Ordering.DESC))))
      val result = notificationTemplateDao.getNotificationTemplateByCriteria(none, orderBy, 2.some, 1.some)

      result.right.get.map(_.defaultTitle) mustBe Seq("test_something", "peace comes from within")
      result.right.get.size mustBe 2
    }

  }
}
