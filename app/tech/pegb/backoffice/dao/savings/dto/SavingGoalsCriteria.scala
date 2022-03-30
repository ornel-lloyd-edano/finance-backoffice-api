package tech.pegb.backoffice.dao.savings.dto

import java.time.{LocalDate, LocalDateTime}

import tech.pegb.backoffice.dao.model.CriteriaField

case class SavingGoalsCriteria(
    id: Option[CriteriaField[Int]] = None,
    uuid: Option[CriteriaField[String]] = None,
    userId: Option[CriteriaField[Int]] = None,
    userUuid: Option[CriteriaField[String]] = None,
    accountId: Option[CriteriaField[Int]] = None,
    accountUuid: Option[CriteriaField[String]] = None,
    currency: Option[CriteriaField[String]] = None,
    dueDate: Option[CriteriaField[LocalDate]] = None,
    statusUpdatedAt: Option[CriteriaField[LocalDateTime]] = None,
    createdAt: Option[CriteriaField[LocalDateTime]] = None,
    name: Option[CriteriaField[String]] = None,
    reason: Option[CriteriaField[String]] = None,
    status: Option[CriteriaField[String]] = None,
    isActive: Option[CriteriaField[Boolean]] = None,
    paymentType: Option[CriteriaField[String]] = None,
    goalAmount: Option[CriteriaField[BigDecimal]] = None,
    currentAmount: Option[CriteriaField[BigDecimal]] = None,
    initialAmount: Option[CriteriaField[BigDecimal]] = None,
    emiAmount: Option[CriteriaField[BigDecimal]] = None) extends SavingOptionCriteria {

}

