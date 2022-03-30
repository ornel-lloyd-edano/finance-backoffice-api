package tech.pegb.backoffice.dao.savings.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.model.CriteriaField

case class RoundUpSavingsCriteria(
    id: Option[CriteriaField[Int]] = None,
    uuid: Option[CriteriaField[String]] = None,
    userId: Option[CriteriaField[Int]] = None,
    userUuid: Option[CriteriaField[String]] = None,
    accountId: Option[CriteriaField[Int]] = None,
    accountUuid: Option[CriteriaField[String]] = None,
    currency: Option[CriteriaField[String]] = None,
    currentAmount: Option[CriteriaField[BigDecimal]] = None,
    roundingNearest: Option[CriteriaField[Int]] = None,
    statusUpdatedAt: Option[CriteriaField[LocalDateTime]] = None,
    createdAt: Option[CriteriaField[LocalDateTime]] = None,
    isActive: Option[CriteriaField[Boolean]] = None) extends SavingOptionCriteria {

}
