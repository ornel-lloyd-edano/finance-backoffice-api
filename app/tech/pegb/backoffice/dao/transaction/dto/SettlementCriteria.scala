package tech.pegb.backoffice.dao.transaction.dto

import java.time.LocalDateTime

case class SettlementCriteria(
    id: Option[Int] = None,
    uuid: Option[String] = None,
    createdAtFrom: Option[LocalDateTime] = None,
    createdAtTo: Option[LocalDateTime] = None,
    accountNumber: Option[String] = None,
    direction: Option[String] = None,
    currency: Option[String] = None) {

}

object SettlementCriteria {
  val NO_CRITERIA = new SettlementCriteria(None, None, None, None, None, None, None)
}
