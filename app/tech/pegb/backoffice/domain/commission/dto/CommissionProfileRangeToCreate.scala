package tech.pegb.backoffice.domain.commission.dto

import cats.implicits._
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.{ServiceError, Validatable}

import scala.util.Try

case class CommissionProfileRangeToCreate(
    from: BigDecimal,
    to: Option[BigDecimal],
    flatAmount: Option[BigDecimal],
    percentageAmount: Option[BigDecimal]) extends Validatable[Unit] {

  override def validate: ServiceResponse[Unit] = {
    for {
      _ ← Try(CommissionProfileRangeToCreate.assertFeeProfileRangeAmounts(
        from, to, flatAmount, percentageAmount)).toEither.leftMap(t ⇒ ServiceError.validationError(t.getMessage))
    } yield {
      ()
    }
  }

}

case object CommissionProfileRangeToCreate {
  private val Zero = BigDecimal(0)

  def assertFeeProfileRangeAmounts(
    from: BigDecimal,
    toOption: Option[BigDecimal],
    flatAmount: Option[BigDecimal],
    percentageAmount: Option[BigDecimal]): Unit = { //TODO: return ServiceResponse

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

