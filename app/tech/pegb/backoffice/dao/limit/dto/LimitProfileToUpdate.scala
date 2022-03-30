package tech.pegb.backoffice.dao.limit.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.GenericUpdateSql
import tech.pegb.backoffice.dao.limit.sql.LimitProfileSqlDao._

case class LimitProfileToUpdate(
    limitType: Option[String] = None,
    userType: Option[String] = None,
    tier: Option[String] = None,
    subscription: Option[String] = None,
    transactionType: Option[String] = None,
    channel: Option[String] = None,
    instrument: Option[String] = None,
    maxIntervalAmount: Option[BigDecimal] = None,
    maxAmount: Option[BigDecimal] = None,
    minAmount: Option[BigDecimal] = None,
    maxCount: Option[Int] = None,
    reason: Option[String] = None,
    deletedAt: Option[LocalDateTime] = None,
    updatedBy: String,
    updatedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime]) extends GenericUpdateSql {

  lastUpdatedAt.foreach(x ⇒ paramsBuilder += cLastUpdatedAt → x)
  append(cUpdatedBy → updatedBy)
  append(cUpdatedAt → updatedAt)

  deletedAt match {
    case None ⇒
      limitType.foreach(x ⇒ append(cLimitType → x))
      userType.foreach(x ⇒ append(cUserType → x))
      tier.foreach(x ⇒ append(cTier → x))
      subscription.foreach(x ⇒ append(cSubscription → x))
      transactionType.foreach(x ⇒ append(cTransactionType → x))
      channel.foreach(x ⇒ append(cChannel → x))
      instrument.foreach(x ⇒ append(cInstrument → x))

      append(cMaxIntervalAmount → maxIntervalAmount)
      append(cMaxAmount → maxAmount)
      append(cMinAmount → minAmount)
      append(cMaxCount → maxCount)

    case Some(d) ⇒
      append(cDeletedAt → d)
  }

}
