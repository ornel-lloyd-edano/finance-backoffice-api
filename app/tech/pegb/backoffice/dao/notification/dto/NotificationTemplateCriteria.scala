package tech.pegb.backoffice.dao.notification.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.model.CriteriaField

case class NotificationTemplateCriteria(
    id: Option[CriteriaField[Int]] = None,
    uuid: Option[CriteriaField[String]] = None,
    name: Option[CriteriaField[String]] = None,
    titleResource: Option[CriteriaField[String]] = None,
    contentResource: Option[CriteriaField[String]] = None,
    channels: Option[CriteriaField[String]] = None,
    createdAt: Option[CriteriaField[_]] = None,
    createdBy: Option[CriteriaField[String]] = None,
    updatedAt: Option[CriteriaField[LocalDateTime]] = None,
    updatedBy: Option[CriteriaField[String]] = None,
    isActive: Option[CriteriaField[Boolean]] = None) {

}
