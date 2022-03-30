package tech.pegb.backoffice.dao.account.dto

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.dao.GenericUpdateSql
import tech.pegb.backoffice.dao.account.entity.ExternalAccount._

case class ExternalAccountToUpdate(
    id: Option[Int] = None,
    uuid: Option[UUID] = None,
    userId: Option[Int] = None,
    provider: Option[String] = None,
    accountNumber: Option[String] = None,
    accountHolder: Option[String] = None,
    currencyId: Option[Int] = None,
    createdBy: Option[String] = None,
    createdAt: Option[LocalDateTime] = None,
    updatedBy: Option[String] = None,
    updatedAt: Option[LocalDateTime] = None,
    lastUpdatedAt: Option[LocalDateTime] = None) extends GenericUpdateSql {

  id.foreach(v ⇒ append(cId → v))
  uuid.foreach(v ⇒ append(cUuid → v))
  userId.foreach(v ⇒ append(cUserId → v))
  provider.foreach(v ⇒ append(cProvider → v))
  accountNumber.foreach(v ⇒ append(cAccountNum → v))
  accountHolder.foreach(v ⇒ append(cAccountHolder → v))
  currencyId.foreach(v ⇒ append(cCurrencyId → v))
  createdBy.foreach(v ⇒ append(cCreatedBy → v))
  createdAt.foreach(v ⇒ append(cCreatedAt → v))
  updatedAt.foreach(v ⇒ append(cUpdatedAt → v))
  updatedBy.foreach(v ⇒ append(cUpdatedBy → v))

  lastUpdatedAt.foreach(paramsBuilder += cLastUpdatedAt → _)
}
