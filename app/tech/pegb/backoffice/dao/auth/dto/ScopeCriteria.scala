package tech.pegb.backoffice.dao.auth.dto

import tech.pegb.backoffice.dao.model.CriteriaField

case class ScopeCriteria(
    id: Option[CriteriaField[String]] = None,
    parentId: Option[CriteriaField[String]] = None,
    name: Option[CriteriaField[String]] = None,
    description: Option[CriteriaField[String]] = None,
    isActive: Option[CriteriaField[Int]] = None)
