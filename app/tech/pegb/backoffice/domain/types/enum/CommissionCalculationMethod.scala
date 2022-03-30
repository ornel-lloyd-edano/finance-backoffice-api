package tech.pegb.backoffice.domain.types.enum

import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.{ServiceError, Validatable}
import tech.pegb.backoffice.domain.types.abstraction.TypeEnum
import tech.pegb.backoffice.util.Implicits._

sealed trait CommissionCalculationMethod extends TypeEnum with Validatable[Unit] {
  override lazy val kind = CommissionCalculationMethods.toString
  override def isUnknown: Boolean = false
  def isFlatPercentage: Boolean = false
  def isStaircaseFlatAmount: Boolean = false
  def isStaircaseFlatPercentage: Boolean = false

  def isStaircaseType: Boolean = false
  def isPercentageType: Boolean = false

  override def validate: ServiceResponse[Unit] = {
    if (isUnknown)
      Left(ServiceError.validationError(s"Unknown commission calculation method [${this.toString}]. Valid calculation methods: ${CommissionCalculationMethods.toSeq.defaultMkString}"))
    else Right(())
  }
}

object CommissionCalculationMethods {
  override def toString = "commission_calculation_methods"

  lazy val toSeq = Seq(FlatPercentage, StaircaseFlatAmount, StaircaseFlatPercentage)
  lazy val toMap = toSeq.map(d ⇒ d.toString → d).toMap

  //Note: to be used when reading from dao layer
  def fromString(arg: String): CommissionCalculationMethod =
    toMap.get(arg.toLowerCase).getOrElse(UnknownCommissionCalculationMethod(arg))

  case object FlatPercentage extends CommissionCalculationMethod {
    override def toString: String = "flat_percentage"
    override def isFlatPercentage: Boolean = true

    override def isPercentageType: Boolean = true
  }

  case object StaircaseFlatAmount extends CommissionCalculationMethod {
    override def toString: String = "staircase_flat_amount"
    override def isStaircaseFlatAmount: Boolean = true

    override def isStaircaseType: Boolean = true
  }

  case object StaircaseFlatPercentage extends CommissionCalculationMethod {
    override def toString: String = "staircase_flat_percentage"
    override def isStaircaseFlatPercentage: Boolean = true

    override def isPercentageType: Boolean = true
    override def isStaircaseType: Boolean = true
  }

  case class UnknownCommissionCalculationMethod(underlying: String) extends CommissionCalculationMethod {
    override def toString = underlying
    override def isUnknown: Boolean = true
  }

}
