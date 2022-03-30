package tech.pegb.backoffice.dao.notification.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.GenericUpdateSql
import tech.pegb.backoffice.dao.notification.sql.NotificationSqlDao._

case class NotificationToUpdate(
    updatedAt: LocalDateTime,
    updatedBy: String,
    templateId: Option[Int] = None,
    templateUuid: Option[String] = None,
    channel: Option[String] = None,
    operationId: Option[String] = None,
    title: Option[String] = None,
    content: Option[String] = None,
    address: Option[String] = None,
    userId: Option[Int] = None,
    userUuid: Option[String] = None,
    status: Option[String] = None,
    sentAt: Option[LocalDateTime] = None,
    errorMsg: Option[String] = None,
    retries: Option[Int] = None,
    lastUpdatedAt: Option[LocalDateTime]) extends GenericUpdateSql {

  append(cUpdatedAt → updatedAt)
  lastUpdatedAt.foreach(x ⇒ paramsBuilder += cLastUpdatedAt → x)

  templateId.foreach(x ⇒ append(cTemplateId → x))
  templateUuid.foreach(x ⇒ append(cTemplateUuid → x))
  channel.foreach(x ⇒ append(cChannel → x))
  operationId.foreach(x ⇒ append(cOperationId → x))
  title.foreach(x ⇒ append(cTitle → x))
  content.foreach(x ⇒ append(cContent → x))
  address.foreach(x ⇒ append(cAddress → x))
  userId.foreach(x ⇒ append(cUserId → x))
  userUuid.foreach(x ⇒ append(cUserUuid → x))
  status.foreach(x ⇒ append(cStatus → x))
  sentAt.foreach(x ⇒ append(cSentAt → x))
  errorMsg.foreach(x ⇒ append(cErrorMessage → x))
  retries.foreach(x ⇒ append(cRetries → x))
}
