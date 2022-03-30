package tech.pegb.backoffice.dao.commission.dto

case class CommissionProfileRangeToInsert(
    min: Option[BigDecimal],
    max: Option[BigDecimal],
    commissionAmount: Option[BigDecimal],
    commissionRatio: Option[BigDecimal])
