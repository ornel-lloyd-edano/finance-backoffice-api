package tech.pegb.backoffice.dao.fee.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.GenericUpdateSql
import tech.pegb.backoffice.dao.fee.sql.FeeProfileSqlDao._

case class FeeProfileToUpdate(
    calculationMethod: String,
    feeMethod: String,
    taxIncluded: String,
    maxFee: Option[BigDecimal] = None,
    minFee: Option[BigDecimal] = None,
    feeAmount: Option[BigDecimal] = None,
    feeRatio: Option[BigDecimal] = None,
    ranges: Option[Seq[FeeProfileRangeToInsert]],
    deletedAt: Option[LocalDateTime] = None,
    updatedAt: LocalDateTime,
    updatedBy: String,
    lastUpdatedAt: Option[LocalDateTime]) extends GenericUpdateSql {

  lastUpdatedAt.foreach(x ⇒ paramsBuilder += cLastUpdatedAt → x)
  append(cUpdatedAt → updatedAt)
  append(cUpdatedBy → updatedBy)

  deletedAt match {
    case Some(deletedTime) ⇒
      append(cDeletedAt → deletedTime)
    case None ⇒
      append(cCalculationMethod → calculationMethod)
      append(cFeeMethod → feeMethod)
      append(cTaxIncluded → taxIncluded)
      maxFee.foreach(x ⇒ append(cMaxFee → x))
      minFee.foreach(x ⇒ append(cMinFee → x))
      feeAmount.foreach(x ⇒ append(cFeeAmount → x))
      feeRatio.foreach(x ⇒ append(cFeeRatio → x))
      feeRatio.foreach(x ⇒ append(cFeeRatio → x))
  }

}
