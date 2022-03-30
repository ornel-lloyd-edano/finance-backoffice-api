package tech.pegb.backoffice.dao.notification.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.model.CriteriaField

case class NotificationCriteria(
    id: Option[CriteriaField[Int]] = None,
    uuid: Option[CriteriaField[String]] = None,
    templateId: Option[CriteriaField[Int]] = None,
    templateUuid: Option[CriteriaField[String]] = None,
    channel: Option[CriteriaField[String]] = None,
    title: Option[CriteriaField[String]] = None,
    content: Option[CriteriaField[String]] = None,
    operationId: Option[CriteriaField[String]] = None,
    address: Option[CriteriaField[String]] = None,
    userId: Option[CriteriaField[Int]] = None,
    userUuid: Option[CriteriaField[String]] = None,
    status: Option[CriteriaField[String]] = None,
    createdAt: Option[CriteriaField[_]] = None,
    createdBy: Option[CriteriaField[String]] = None,
    updatedAt: Option[CriteriaField[(LocalDateTime, LocalDateTime)]] = None,
    updatedBy: Option[CriteriaField[String]] = None,
    errorMsg: Option[CriteriaField[String]] = None,
    retries: Option[CriteriaField[Int]] = None) {

}
