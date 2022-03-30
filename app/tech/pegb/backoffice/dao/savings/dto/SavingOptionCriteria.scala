package tech.pegb.backoffice.dao.savings.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.model.CriteriaField

trait SavingOptionCriteria {
  val id: Option[CriteriaField[Int]]
  val uuid: Option[CriteriaField[String]]
  val userId: Option[CriteriaField[Int]]
  val userUuid: Option[CriteriaField[String]]
  val accountId: Option[CriteriaField[Int]]
  val accountUuid: Option[CriteriaField[String]]
  val currency: Option[CriteriaField[String]]
  val isActive: Option[CriteriaField[Boolean]]
  val currentAmount: Option[CriteriaField[BigDecimal]]
  val statusUpdatedAt: Option[CriteriaField[LocalDateTime]]
  val createdAt: Option[CriteriaField[LocalDateTime]]
}
