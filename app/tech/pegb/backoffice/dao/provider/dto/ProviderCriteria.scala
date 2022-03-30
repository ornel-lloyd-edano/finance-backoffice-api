package tech.pegb.backoffice.dao.provider.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.model.CriteriaField
import tech.pegb.backoffice.dao.provider.entity.Provider

case class ProviderCriteria(
    id: Option[CriteriaField[Int]] = None,
    userUuid: Option[CriteriaField[String]] = None,
    serviceId: Option[CriteriaField[Int]] = None,
    name: Option[CriteriaField[String]] = None,
    transactionType: Option[CriteriaField[String]] = None,
    pgInstitutionId: Option[CriteriaField[Int]] = None,
    utilityPaymentType: Option[CriteriaField[String]] = None,
    utilityMinPaymentAmount: Option[CriteriaField[(BigDecimal, BigDecimal)]] = None,
    utilityMaxPaymentAmount: Option[CriteriaField[(BigDecimal, BigDecimal)]] = None,
    isActive: Option[CriteriaField[Boolean]] = None,
    createdBy: Option[CriteriaField[String]] = None,
    createdAt: Option[CriteriaField[(LocalDateTime, LocalDateTime)]] = None,
    updatedBy: Option[CriteriaField[String]] = None,
    updatedAt: Option[CriteriaField[(LocalDateTime, LocalDateTime)]] = None) {

}

object ProviderCriteria {
  def apply(name: String) = new ProviderCriteria(name = Some(CriteriaField[String](Provider.cName, name)))
}
