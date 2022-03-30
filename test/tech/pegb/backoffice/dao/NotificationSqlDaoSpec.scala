package tech.pegb.backoffice.dao

import java.time.{Clock, Instant, LocalDateTime, ZoneId}
import java.util.UUID

import cats.implicits._
import tech.pegb.backoffice.dao.model.{CriteriaField, Ordering, OrderingSet}
import tech.pegb.backoffice.dao.notification.abstraction.NotificationDao
import tech.pegb.backoffice.dao.notification.dto.{NotificationCriteria, NotificationToInsert}
import tech.pegb.backoffice.dao.notification.entity.Notification
import tech.pegb.core.PegBTestApp

import scala.collection.mutable

class NotificationSqlDaoSpec extends PegBTestApp {

  val mockClock = Clock.fixed(Instant.ofEpochMilli(3600), ZoneId.systemDefault())
  val now = LocalDateTime.now(mockClock)

  val UUID1 = UUID.randomUUID()
  val UUID2 = UUID.randomUUID()
  val UUID3 = UUID.randomUUID()

  override def initSql =
    s"""
       |
       |INSERT INTO users(id, uuid, username, password, email, activated_at, password_updated_at, subscription, type,
       |status, tier, segment, created_by, created_at, updated_by, updated_at)
       |VALUES ('1', '5744db00-2b50-4a34-8348-35f0030c3b1d', 'user01', 'pword', 'george@gmail.com',
       |'2018-10-15 00:00:00', null, 'sub_one',  'type_one', 'ACTIVE', null, null, 'SuperUser', '2018-01-01 00:00:00',
       |null, null);
       |
       |INSERT INTO notification_templates(id, uuid, name, title_resource, default_title, default_content, channels,
       | description, created_at, created_by, updated_at, updated_by, is_active)
       |VALUES('1', '$UUID1','test_template', 'test_resource_one', 'live, laugh and love', 'keep smiling',
       |'mobile','test description',  '$now', 'ujali', null, null, 1);
       |
       |INSERT INTO notifications(id, uuid, template_id, channel, title, content, operation_id, address, user_id,
       | status, sent_at, error_message, retries, created_at, created_by, updated_at, updated_by,)
       |VALUES
       |('1', '$UUID1','1', 'Mobile', 'Rock On', 'keep rocking on', 'op_one','under the ground', 1,  'new',
       | '$now', null, '1',  '$now', 'ujali', null, null),
       |('2', '$UUID2','1', 'Mobile', 'Rock On Test', 'keep rocking on test', 'op_two','under the ground rule', 1,  'new',
       | '$now', null, '1',  '$now', 'dima', null, null),
       | ('3', '$UUID3','1', 'APP', 'Rock On Id 3', 'keep rocking on id 3', 'op_three','under the ground for new', 1,  'old',
       | '$now', null, '1',  '$now', 'david', null, null);
    """.stripMargin

  override def cleanupSql =
    s"""
       |DELETE FROM notifications;
       |DELETE FROM notification_templates;
       |DELETE FROM users;
     """.stripMargin

  val notificationDao = inject[NotificationDao]

  "Notification Dao" should {
    "insert notification" in {
      val notificationToInsert = NotificationToInsert(
        templateId = 1,
        channel = "email",
        title = "test_title",
        content = "test_content",
        address = "test_address",
        userId = 1.some,
        status = "new",
        operationId = "test_operation_id",
        sentAt = None,
        errorMsg = None,
        retries = None,
        createdBy = "lloyd",
        createdAt = now)

      val result = notificationDao.insertNotification(notificationToInsert)

      result.right.map(_.address) mustBe Right("test_address")
    }

    "count all notifications records" in {
      val result = notificationDao.getCountByCriteria(none)
      result.right.get mustBe 4
    }

    "get notifications records by criteria" in {
      val criteriaField = CriteriaField("uuid", UUID1.toString)
      val criteria = NotificationCriteria(uuid = criteriaField.some)

      val notification = Notification(
        id = 1,
        uuid = UUID1.toString,
        userId = 1.some,
        userUuid = "5744db00-2b50-4a34-8348-35f0030c3b1d".some,
        templateId = 1,
        templateUuid = UUID1.toString,
        channel = "Mobile",
        title = "Rock On",
        content = "keep rocking on",
        address = "under the ground",
        status = "new",
        errorMsg = none,
        retries = 1.some,
        operationId = "op_one",
        createdBy = "ujali",
        createdAt = now,
        updatedBy = none,
        updatedAt = none,
        sentAt = now.some)

      val result = notificationDao.getNotificationByCriteria(criteria.some, none, none, none)

      result.right.get mustBe Seq(notification)
      result.right.get.size mustBe 1
    }

    "get all notifications records by no criteria" in {

      val result = notificationDao.getNotificationByCriteria(none, none, none, none)

      result.right.get.map(_.address) mustBe Seq("under the ground", "under the ground rule", "under the ground for new", "test_address")
      result.right.get.size mustBe 4
    }

    "get all notifications records by extended criteria" in {
      val criteriaFieldOne = CriteriaField("channel", "Mobile")
      val criteriaFieldTwo = CriteriaField("status", "new")

      val criteria = NotificationCriteria(channel = criteriaFieldOne.some, status = criteriaFieldTwo.some)

      val result = notificationDao.getNotificationByCriteria(criteria.some, none, none, none)
      result.right.get.map(_.address) mustBe Seq("under the ground", "under the ground rule")
      result.right.get.size mustBe 2
    }

    "get notifications records by criteria with ordering and pagination" in {
      val orderBy = Option(OrderingSet(mutable.LinkedHashSet(Ordering("address", Ordering.DESC))))

      val result = notificationDao.getNotificationByCriteria(none, orderBy, 1.some, 1.some)
      result.right.get.map(_.address) mustBe Seq("under the ground for new")
    }

  }
}
