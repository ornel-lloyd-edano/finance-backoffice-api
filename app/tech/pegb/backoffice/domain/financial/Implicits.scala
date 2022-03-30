package tech.pegb.backoffice.domain.financial

import scala.math.BigDecimal.RoundingMode

object Implicits {

  implicit class AccountingStandard(arg: BigDecimal) {
    def toFinancial: BigDecimal = arg.setScale(2, RoundingMode.HALF_EVEN)

    def toFinancialFxRate: BigDecimal = arg.setScale(6, RoundingMode.HALF_EVEN)
  }
}
