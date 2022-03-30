package tech.pegb.backoffice.domain.fee.model

import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

object FeeAttributes {
  case class FeeType(underlying: String) {
    assert(underlying.hasSomething, "empty fee type")
    assert(underlying.matches("""[A-Za-z]+[A-Za-z0-9\-\_ ]*"""), s"invalid fee type [${underlying}]")
  }

  case class FeeMethod(underlying: String) {
    assert(underlying.hasSomething, "empty fee method")
    assert(underlying.matches("""[A-Za-z]+[A-Za-z0-9\-\_ ]*"""), s"invalid fee method [${underlying}]")
  }

  case class FeeCalculationMethod(underlying: String) {
    assert(underlying.hasSomething, "empty fee calculation method")
    assert(underlying.matches("""[A-Za-z]+[A-Za-z0-9\-\_ ]*"""), s"invalid fee calculation method [${underlying}]")

    def isPercentageType: Boolean = underlying.toLowerCase.contains("percentage")

    def isStairCaseType: Boolean = underlying.toLowerCase.contains("staircase")
  }
  object FeeCalculationMethod {
    implicit class RichString(val arg: String) extends AnyVal {
      def isPercentageType: Boolean = arg.toLowerCase.contains("percentage")

      def isStairCaseType: Boolean = arg.toLowerCase.contains("staircase")
    }
  }

  sealed trait TaxInclusionType
  object TaxInclusionTypes {

    def apply(underlying: String): TaxInclusionType = {
      assert(underlying.hasSomething, "tax_included is empty")
      val tryTaxInc = Try(underlying.toTaxInclusionType)
      assert(tryTaxInc.isSuccess, s"invalid tax included [$underlying]")
      tryTaxInc.get
    }

    case object TaxIncluded extends TaxInclusionType {
      override def toString = "tax_included"
    }
    case object TaxNotIncluded extends TaxInclusionType {
      override def toString = "tax_not_included"
    }
    case object NoTax extends TaxInclusionType {
      override def toString = "no_tax"
    }

    implicit class TaxInclusionAdapter(val arg: TaxInclusionType) extends AnyVal {
      def toOptBoolean = arg match {
        case TaxIncluded ⇒ Option(true)
        case TaxNotIncluded ⇒ Option(false)
        case NoTax ⇒ None
      }
    }

    implicit class TaxInclusionBooleanAdapter(val arg: Option[Boolean]) extends AnyVal {
      def toTaxInclusionType = arg match {
        case Some(true) ⇒ TaxIncluded
        case Some(false) ⇒ TaxNotIncluded
        case _ ⇒ NoTax
      }
    }

    implicit class TaxInclusionStringAdapter(val arg: String) extends AnyVal {
      def toTaxInclusionType = arg match {
        case "tax_included" ⇒ TaxIncluded
        case "tax_not_included" ⇒ TaxNotIncluded
        case "no_tax" ⇒ NoTax
      }
    }

  }

}
