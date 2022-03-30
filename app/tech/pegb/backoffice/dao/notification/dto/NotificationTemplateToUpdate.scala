package tech.pegb.backoffice.dao.notification.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.GenericUpdateSql
import tech.pegb.backoffice.dao.notification.sql.NotificationTemplateSqlDao._

case class NotificationTemplateToUpdate(
    updatedAt: LocalDateTime,
    updatedBy: String,
    name: Option[String] = None,
    titleResource: Option[String] = None,
    defaultTitle: Option[String] = None,
    contentResource: Option[String] = None,
    defaultContent: Option[String] = None,
    description: Option[String] = None,
    channels: Option[String] = None,
    isActive: Option[Boolean] = None,
    lastUpdatedAt: Option[LocalDateTime]) extends GenericUpdateSql {

  append(cUpdatedAt → updatedAt)
  lastUpdatedAt.foreach(x ⇒ paramsBuilder += cLastUpdatedAt → x)

  name.foreach(x ⇒ append(cName → x))
  titleResource.foreach(x ⇒ append(cTitleResource → x))
  defaultTitle.foreach(x ⇒ append(cDefaultTitle → x))
  contentResource.foreach(x ⇒ append(cContentResource → x))
  defaultContent.foreach(x ⇒ append(cDefaultContent → x))
  description.foreach(x ⇒ append(cDescription → x))
  channels.foreach(x ⇒ append(cChannels → x))
  isActive.foreach(x ⇒ append(cIsActive → x))

}
