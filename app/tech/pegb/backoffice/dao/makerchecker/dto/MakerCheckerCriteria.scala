package tech.pegb.backoffice.dao.makerchecker.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.model.CriteriaField

case class MakerCheckerCriteria(
    id: Option[CriteriaField[String]] = None,
    uuid: Option[CriteriaField[String]] = None,
    status: Option[CriteriaField[String]] = None,
    module: Option[CriteriaField[String]] = None,
    createdAtFrom: Option[LocalDateTime] = None,
    createdAtTo: Option[LocalDateTime] = None,
    makerLevel: Option[CriteriaField[Int]] = None,
    makerBusinessUnit: Option[CriteriaField[String]] = None) {

}
