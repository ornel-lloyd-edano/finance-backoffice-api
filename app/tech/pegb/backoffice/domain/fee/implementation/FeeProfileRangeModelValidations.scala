package tech.pegb.backoffice.domain.fee.implementation

import tech.pegb.backoffice.domain.fee

trait FeeProfileRangeModelValidations extends fee.abstraction.FeeProfileRangeModelValidations {
  private val Zero = BigDecimal(0)

  def assertFeeProfileRangeAmounts(
    from: BigDecimal,
    toOption: Option[BigDecimal],
    flatAmount: Option[BigDecimal],
    percentageAmount: Option[BigDecimal]): Unit = {

    toOption.foreach { to ⇒
      assert(to >= Zero, "to amount can not be a negative value")
      assert(to.scale <= 2, "to amount can not have more than 2 decimal digits")
      assert(to >= from, "to amount can not be less than from amount")
    }

    assert(from >= Zero, "from amount can not be a negative value")
    assert(from.scale <= 2, "from amount can not have more than 2 decimal digits")

    flatAmount.foreach { fAmount ⇒
      assert(fAmount >= Zero, "flat amount can not be a negative value")
      assert(fAmount.scale <= 2, "flat amount can not have more than 2 decimal digits")
    }
    percentageAmount.foreach { percentage ⇒
      assert(percentage.scale <= 4, "percentage amount can not have more than 2 decimal digits")
    }

  }
}
