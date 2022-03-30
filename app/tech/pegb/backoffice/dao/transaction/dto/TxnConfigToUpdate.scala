package tech.pegb.backoffice.dao.transaction.dto

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.dao.GenericUpdateSql
import tech.pegb.backoffice.dao.transaction.entity.TxnConfig._

case class TxnConfigToUpdate(
    uuid: Option[UUID] = None,
    userId: Option[Int] = None,
    transactionType: Option[String] = None,
    currencyId: Option[Int] = None,
    createdBy: Option[String] = None,
    createdAt: Option[LocalDateTime] = None,
    updatedBy: String,
    updatedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime] = None) extends GenericUpdateSql {

  uuid.foreach(v ⇒ append(cUuid → v))
  userId.foreach(v ⇒ append(cUserId → v))
  transactionType.foreach(v ⇒ append(cTxnType → v))
  currencyId.foreach(v ⇒ append(cCurrencyId → v))
  createdBy.foreach(v ⇒ append(cCreatedBy → v))
  createdAt.foreach(v ⇒ append(cCreatedAt → v))

  append(cUpdatedAt → updatedAt)
  append(cUpdatedBy → updatedBy)

  lastUpdatedAt.foreach(paramsBuilder += cLastUpdatedAt → _)

}
