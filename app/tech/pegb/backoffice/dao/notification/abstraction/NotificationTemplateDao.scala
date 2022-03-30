package tech.pegb.backoffice.dao.notification.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.notification.dto.{NotificationTemplateCriteria, NotificationTemplateToInsert, NotificationTemplateToUpdate}
import tech.pegb.backoffice.dao.notification.entity.NotificationTemplate
import tech.pegb.backoffice.dao.notification.sql.NotificationTemplateSqlDao

@ImplementedBy(classOf[NotificationTemplateSqlDao])
trait NotificationTemplateDao extends Dao {

  def getNotificationTemplateByCriteria(
    criteria: Option[NotificationTemplateCriteria],
    ordering: Option[OrderingSet],
    limit: Option[Int], offset: Option[Int]): DaoResponse[Seq[NotificationTemplate]]

  def getCountByCriteria(criteria: Option[NotificationTemplateCriteria]): DaoResponse[Int]

  def insertNotificationTemplate(dto: NotificationTemplateToInsert): DaoResponse[NotificationTemplate]

  def updateNotificationTemplate(uuid: String, dto: NotificationTemplateToUpdate): DaoResponse[NotificationTemplate]

}
