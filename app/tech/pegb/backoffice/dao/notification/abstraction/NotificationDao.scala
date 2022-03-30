package tech.pegb.backoffice.dao.notification.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.notification.dto.{NotificationCriteria, NotificationToInsert, NotificationToUpdate}
import tech.pegb.backoffice.dao.notification.entity.Notification
import tech.pegb.backoffice.dao.notification.sql.NotificationSqlDao

@ImplementedBy(classOf[NotificationSqlDao])
trait NotificationDao extends Dao {

  def getNotificationByCriteria(
    criteria: Option[NotificationCriteria],
    ordering: Option[OrderingSet],
    limit: Option[Int], offset: Option[Int]): DaoResponse[Seq[Notification]]

  def getCountByCriteria(criteria: Option[NotificationCriteria]): DaoResponse[Int]

  def insertNotification(dto: NotificationToInsert): DaoResponse[Notification]

  def updateNotification(uuid: String, dto: NotificationToUpdate): DaoResponse[Notification]
}
