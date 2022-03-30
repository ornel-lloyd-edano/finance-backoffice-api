package tech.pegb.backoffice.domain.model

import tech.pegb.backoffice.util.Implicits._

trait Order

case class Ordering(field: String, order: Order)

object Ordering {
  case object ASCENDING extends Order {
    override def toString = "ASCENDING"
  }
  case object DESCENDING extends Order {
    override def toString = "DESCENDING"
  }

  implicit class ParseOrdering(arg: String) {
    def asDomain: Seq[Ordering] = {
      arg.split(",")
        .map(_.trim)
        .filterNot(_.isEmpty)
        .map(s ⇒
          if (s.startsWith("-"))
            Ordering(s.substring(1).toLowerCase, Ordering.DESCENDING)
          else
            Ordering(s.toLowerCase, Ordering.ASCENDING))
        .filterNot(_.field.trim.isEmpty)
        .map(s ⇒ Ordering(s.field, s.order))
    }
  }

  implicit class OrderingParamValidator(val orderingQueryParam: Option[String]) extends AnyVal {
    def validateOrdering(validOrderingFields: Set[String]): Either[Exception, Seq[Ordering]] = {
      orderingQueryParam match {
        case None ⇒
          Right(Nil)
        case Some(orderingFields) ⇒
          if (orderingFields.replace("-", "").toSeqByComma.containsOnly(validOrderingFields))
            Right(orderingFields.asDomain)
          else
            Left(new Exception(s"invalid field for order_by found. " +
              s"Valid fields: ${validOrderingFields.defaultMkString}"))

      }
    }
  }

}
