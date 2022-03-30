package tech.pegb.backoffice.dao.auth.dto

import tech.pegb.backoffice.dao.model.CriteriaField

case class PermissionCriteria(
    id: Option[CriteriaField[String]] = None,
    businessId: Option[CriteriaField[String]] = None,
    roleId: Option[CriteriaField[String]] = None,
    userId: Option[CriteriaField[String]] = None,
    scopeId: Option[CriteriaField[String]] = None,
    isActive: Option[CriteriaField[Int]] = None,
    createdAt: Option[CriteriaField[_]] = None)
