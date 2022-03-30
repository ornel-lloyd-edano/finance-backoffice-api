package tech.pegb.backoffice.dao.transaction.dto

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.dao.model.{CriteriaField}
import tech.pegb.backoffice.dao.transaction.entity.TxnConfig

case class TxnConfigCriteria(
    id: Option[CriteriaField[Int]] = None,
    uuid: Option[CriteriaField[String]] = None,
    anyUuid: Option[CriteriaField[Set[String]]] = None,
    userId: Option[CriteriaField[Int]] = None,
    userUuid: Option[CriteriaField[String]] = None,
    transactionType: Option[CriteriaField[String]] = None,
    currencyName: Option[CriteriaField[String]] = None,
    currencyId: Option[CriteriaField[Int]] = None,
    createdBy: Option[CriteriaField[String]] = None,
    createdAt: Option[CriteriaField[(LocalDateTime, LocalDateTime)]] = None,
    updatedBy: Option[CriteriaField[String]] = None,
    updatedAt: Option[CriteriaField[(LocalDateTime, LocalDateTime)]] = None) {
}

object TxnConfigCriteria {
  def apply(id: Int) = new TxnConfigCriteria(id = Some(CriteriaField(TxnConfig.cId, id)))
  def apply(uuids: Iterable[UUID]) = new TxnConfigCriteria(anyUuid = Some(CriteriaField(TxnConfig.cUuid, uuids.map(_.toString).toSet)))
}
