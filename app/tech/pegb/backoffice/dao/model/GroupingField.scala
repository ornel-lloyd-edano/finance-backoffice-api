package tech.pegb.backoffice.dao.model

import tech.pegb.backoffice.dao.model.GroupOperationTypes._

sealed trait GroupOperationType
object GroupOperationTypes {
  case object Date extends GroupOperationType
  case object Day extends GroupOperationType
  case object Month extends GroupOperationType
  case object Year extends GroupOperationType
  case object Hour extends GroupOperationType
  case object Minute extends GroupOperationType
  case object DateHour extends GroupOperationType
  case object IsNotNull extends GroupOperationType
}

case class GroupingField(
    field: String,
    operator: Option[GroupOperationType] = None,
    tableAlias: Option[String] = None,
    columnAlias: Option[String] = None,
    projectionAlias: Option[String] = None) {
  def toSql(actualColumn: Option[String] = None, defaultTableAlias: Option[String] = None) = {
    val safeColumnAlias = columnAlias.orElse(actualColumn)
    val actualField = safeColumnAlias.getOrElse(field)
    val safeTableAlias = tableAlias.orElse(defaultTableAlias)
    val aliasedField = safeTableAlias.map(t ⇒ s"$t.$actualField").getOrElse(actualField)

    operator.fold(
      s"$aliasedField")(
        value ⇒ value match {
          case Date ⇒ s"date($aliasedField)"
          case Day ⇒ s"day($aliasedField)"
          case Month ⇒ s"month($aliasedField)"
          case Year ⇒ s"year($aliasedField)"
          case Hour ⇒ s"hour($aliasedField)"
          case Minute ⇒ s"minute($aliasedField)"
          case DateHour ⇒ s"concat(date($aliasedField),'T',hour($aliasedField),':00:00'))"
          case IsNotNull ⇒ s"($aliasedField is not null)"
          case _ ⇒ s"$aliasedField"
        })

  }
}
