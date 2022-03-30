package tech.pegb.backoffice.dao.model

import cats.implicits._
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import tech.pegb.backoffice.dao.SqlDao._
import tech.pegb.backoffice.util.Implicits._

sealed trait MatchType

object MatchTypes {

  case object GreaterOrEqual extends MatchType

  case object LesserOrEqual extends MatchType

  case object InclusiveBetween extends MatchType

  case object Exact extends MatchType

  case object NotSame extends MatchType

  case object Partial extends MatchType

  case object In extends MatchType

  case object NotIn extends MatchType

  case object IsNull extends MatchType

  case object IsNotNull extends MatchType
  case object NotPartial extends MatchType
}
//TODO move sql syntax from real value creation
case class CriteriaField[T](actualColumn: String, value: T, operator: MatchType = MatchTypes.Exact) {

  //todo remove column from toSql as it is same as actual column and needs to be provided in mapping from entity columns
  def toSql(column: Option[String] = none, tableAlias: Option[String] = none)(implicit criteriaSqlWriter: (String, String, MatchType) ⇒ String): String = {
    val actualField = column.getOrElse(actualColumn)
    val aliasedField = tableAlias.map(t ⇒ s"$t.$actualField").getOrElse(actualField)

    val (realValue, finalOperator) = (value match {
      case (t1: LocalDateTime, t2: LocalDateTime) ⇒ (s"'${t1.toSqlString}' AND '${t2.toSqlString}'", MatchTypes.InclusiveBetween)
      case (t1, t2) ⇒ (s"'$t1' AND '$t2'", MatchTypes.InclusiveBetween)
      case data: Iterable[_] if data.nonEmpty ⇒ (data.map(d ⇒ s"'$d'").mkStringOrEmpty("(", ", ", ")"), MatchTypes.In)
      case data: Iterable[_] if data.isEmpty ⇒ ("('')", MatchTypes.In)
      case data: Boolean ⇒ (s"$data", operator)
      case data: LocalDateTime ⇒ (s"'${data.toSqlString}'", operator)
      case data if operator == MatchTypes.Partial || operator == MatchTypes.NotPartial ⇒ (s"'%$data%'", operator)
      case data ⇒ (s"'$data'", operator)
    })

    criteriaSqlWriter(aliasedField, realValue, finalOperator)
  }

  //TODO move this method to some other appropriate place
  def toFormattedDateTime: CriteriaField[Any] = {
    val formattedTime = value match {
      case (from: LocalDateTime, to: LocalDateTime) ⇒
        (from.format(DateTimeFormatter.ISO_DATE_TIME), to.format(DateTimeFormatter.ISO_DATE_TIME))
      case (dateTime: LocalDateTime) ⇒
        dateTime.format(DateTimeFormatter.ISO_DATE_TIME)
      case any ⇒ any
    }

    this.copy(value = formattedTime)
  }
}

object CriteriaField {
  def apply(actualColumn: String, value: String, partialMatchFields: Set[String]) = {
    val actualOperator = if (partialMatchFields.contains(actualColumn)) MatchTypes.Partial else MatchTypes.Exact
    new CriteriaField[String](actualColumn, value, actualOperator)
  }

}
