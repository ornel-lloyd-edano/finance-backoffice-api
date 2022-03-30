package tech.pegb.backoffice.dao.auth.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.model.CriteriaField

case class RoleCriteria(
    id: Option[CriteriaField[String]] = None,
    name: Option[CriteriaField[String]] = None,
    level: Option[CriteriaField[Int]] = None,
    isActive: Option[CriteriaField[Int]] = None,
    createdBy: Option[CriteriaField[String]] = None,
    createdAt: Option[CriteriaField[(LocalDateTime, LocalDateTime)]] = None,
    updatedBy: Option[CriteriaField[String]] = None,
    updatedAt: Option[CriteriaField[(LocalDateTime, LocalDateTime)]] = None) {

}
