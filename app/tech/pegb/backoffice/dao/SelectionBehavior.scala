package tech.pegb.backoffice.dao

import anorm.{Row, RowParser, SqlRequestError}
import tech.pegb.backoffice.dao.model.OrderingSet

import scala.util.Try

trait SelectionBehavior[E, C] {

  lazy val rowParser: RowParser[E] = (row: Row) ⇒ {
    parseRow(row).fold(
      exc ⇒ anorm.Error(SqlRequestError(new Exception(s"Error in parsing row to entity. Reason: ${exc.getMessage}"))),
      anorm.Success(_))
  }

  def parseRow(arg: Row): Try[E]

  def generateWhereClause(criteria: C): String

  def generateOrderByClause(ordering: Option[OrderingSet]): String

}
