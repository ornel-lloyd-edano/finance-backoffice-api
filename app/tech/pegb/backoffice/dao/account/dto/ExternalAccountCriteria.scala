package tech.pegb.backoffice.dao.account.dto

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.dao.account.entity.ExternalAccount
import tech.pegb.backoffice.dao.model.CriteriaField

case class ExternalAccountCriteria(
    id: Option[CriteriaField[Int]] = None,
    uuid: Option[CriteriaField[String]] = None,
    anyUuid: Option[CriteriaField[Set[String]]] = None,
    userId: Option[CriteriaField[Int]] = None,
    userUuid: Option[CriteriaField[String]] = None,
    provider: Option[CriteriaField[String]] = None,
    accountNumber: Option[CriteriaField[String]] = None,
    accountHolder: Option[CriteriaField[String]] = None,
    currencyName: Option[CriteriaField[String]] = None,
    createdBy: Option[CriteriaField[String]] = None,
    createdAt: Option[CriteriaField[(LocalDateTime, LocalDateTime)]] = None,
    updatedBy: Option[CriteriaField[String]] = None,
    updatedAt: Option[CriteriaField[(LocalDateTime, LocalDateTime)]] = None) {

}

object ExternalAccountCriteria {
  def apply(uuids: Iterable[UUID]) = new ExternalAccountCriteria(anyUuid = Some(CriteriaField(ExternalAccount.cUuid, uuids.map(_.toString).toSet)))
  def apply(id: Int) = new ExternalAccountCriteria(id = Some(CriteriaField(ExternalAccount.cId, id)))
}
