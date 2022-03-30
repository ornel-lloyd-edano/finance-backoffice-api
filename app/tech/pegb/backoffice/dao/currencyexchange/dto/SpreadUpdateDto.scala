package tech.pegb.backoffice.dao.currencyexchange.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.GenericUpdateSql
import tech.pegb.backoffice.dao.currencyexchange.sql.SpreadsSqlDao.{cSpread, cUpdatedAt, cUpdatedBy, cDeletedAt}

case class SpreadUpdateDto(
    spread: BigDecimal,
    updatedBy: String,
    updatedAt: LocalDateTime,
    deletedAt: Option[LocalDateTime],
    lastUpdatedAt: Option[LocalDateTime]) extends GenericUpdateSql {

  lastUpdatedAt.foreach(x ⇒ paramsBuilder += cLastUpdatedAt → x)
  append(cUpdatedAt → updatedAt)
  append(cUpdatedBy → updatedBy)

  deletedAt match {
    case Some(deletedTime) ⇒
      append(cDeletedAt → deletedTime)
    case None ⇒
      append(cSpread → spread)
  }

}
