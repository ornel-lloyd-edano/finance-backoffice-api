package tech.pegb.backoffice.dao

import java.sql.Connection
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import play.api.db.{DBApi, Database}
import tech.pegb.backoffice.dao.Dao.OldEntityId
import tech.pegb.backoffice.dao.model.{GroupingField, MatchType}
import tech.pegb.backoffice.dao.model.MatchTypes._
import tech.pegb.backoffice.util.CriteriaWriter
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

trait SqlDao extends Dao {

  protected val dbApi: DBApi

  def db: Database = dbApi.database("backoffice")

  def startTransaction: DaoResponse[Connection] = {
    Try {
      val conn = db.getConnection()
      conn.setAutoCommit(false)
      conn
    }.fold(
      error ⇒ {
        logger.error("Unable to get a db connection for a transaction", error)
        Left(genericDbError("Unable to get a db connection for a transaction"))
      },
      _.toRight)
  }

  def endTransaction(implicit txnConn: Connection): DaoResponse[Unit] = {
    Try {
      txnConn.commit()
      txnConn.setAutoCommit(true)
    }.fold(
      error ⇒ {
        txnConn.rollback()
        txnConn.close()
        logger.error("Unable to commit the transaction", error)
        Left(genericDbError("Unable to commit the transaction"))
      },
      _ ⇒ {
        txnConn.setAutoCommit(true)
        txnConn.close()
        Right(())
      })

  }

}

object SqlDao extends CriteriaWriter {
  def genId(): OldEntityId = UUID.randomUUID()

  val dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  final val cCount = "count"
  final val cSum = "sum"
  final val cDay = "day"
  final val cMonth = "month"
  final val cYear = "year"
  final val cHour = "hour"
  final val cMinute = "minute"
  final val cDate = "date"

  implicit def writeCriteria(aliasedField: String, realValue: String, operator: MatchType): String = {
    operator match {
      case GreaterOrEqual ⇒ s"$aliasedField >= $realValue"
      case LesserOrEqual ⇒ s"$aliasedField <= $realValue"
      case In ⇒ s"$aliasedField IN $realValue"
      case NotIn ⇒ s"$aliasedField NOT IN $realValue"
      case Exact ⇒ s"$aliasedField = $realValue"
      case NotSame ⇒ s"$aliasedField <> $realValue"
      case Partial ⇒ s"$aliasedField LIKE $realValue"
      case NotPartial ⇒ s"$aliasedField NOT LIKE $realValue"
      case InclusiveBetween ⇒ s"$aliasedField BETWEEN $realValue"
      case IsNull ⇒ s"$aliasedField IS NULL"
      case IsNotNull ⇒ s"$aliasedField IS NOT NULL"
    }
  }

  def fromDateTimeRange(
    alias: String,
    fieldName: String,
    startDate: Option[LocalDateTime],
    endDate: Option[LocalDateTime]): Option[String] = {
    (startDate, endDate) match {
      case (Some(start), None) ⇒ Some(s"$alias.$fieldName >= '${start.format(dateTimeFormat)}'")
      case (None, Some(end)) ⇒ Some(s"$alias.$fieldName <= '${end.format(dateTimeFormat)}'")
      case (Some(start), Some(end)) ⇒ Some(s"$alias.$fieldName BETWEEN '${start.format(dateTimeFormat)}' AND '${end.format(dateTimeFormat)}'")
      case (_, _) ⇒ None
    }
  }

  def formDateRange(
    alias: String,
    fieldName: String,
    startDate: Option[LocalDate],
    endDate: Option[LocalDate]): Option[String] = {
    (startDate, endDate) match {
      case (Some(start), None) ⇒ Some(s"$alias.$fieldName >= '$start 00:00:00'")
      case (None, Some(end)) ⇒ Some(s"$alias.$fieldName =< '$end 23:59:59'")
      case (Some(start), Some(end)) ⇒ Some(s"$alias.$fieldName BETWEEN '$start 00:00:00' AND '$end 23:59:59'")
      case (_, _) ⇒ None
    }
  }

  def getPagination(maybeLimit: Option[Int], maybeOffset: Option[Int]): String =
    (maybeLimit, maybeOffset) match {
      case (Some(limit), Some(offset)) ⇒
        s"LIMIT $limit OFFSET $offset"
      case (Some(limit), None) ⇒
        s"LIMIT $limit OFFSET 0"
      case (None, Some(offset)) ⇒
        s"LIMIT ${Int.MaxValue} OFFSET $offset"
      case _ ⇒
        ""
    }

  def formWhereInClause[T](
    fieldValue: Iterable[T],
    fieldName: String,
    mayBeTableName: Option[String] = None): Option[String] = {

    if (fieldValue.nonEmpty) {
      Some(mayBeTableName.fold(s"$fieldName IN ${fieldValue.map(value ⇒ s"'$value'").mkString("(", ",", ")")} ") {
        tableName ⇒ s"$tableName.$fieldName IN ${fieldValue.map(value ⇒ s"'$value'").mkString("(", ",", ")")} "
      })
    } else {
      None
    }
  }

  def queryConditionClause[T](
    fieldValue: T,
    fieldName: String,
    mayBeTableName: Option[String] = None,
    isPartialMatch: Boolean = false): String = {

    val assignment = (fieldValue, isPartialMatch) match {
      case (time: LocalDateTime, false) ⇒ s"= '${time.toSqlString}'"
      case (time: LocalDateTime, true) ⇒ s"LIKE '%${time.toSqlString}%'"
      case (_, false) ⇒ s"= '$fieldValue'"
      case (_, _) ⇒ s"LIKE '%$fieldValue%'"
    }

    mayBeTableName.fold(s"$fieldName $assignment") { tableName ⇒ s"$tableName.$fieldName $assignment" }
  }

  def toNullSql(
    column: String,
    isNull: Boolean,
    tableAlias: Option[String] = None): String = {
    val isOrIsNot = if (isNull) "IS" else "IS NOT"

    tableAlias.fold(s"$column $isOrIsNot NULL")(tableName ⇒ s"$tableName.$column $isOrIsNot NULL")
  }

  def queryWithIfColIsEmptyStatementsClause(
    fieldValue: String,
    fieldNames: Seq[String],
    defaultFieldName: String,
    mayBeTableName: Option[String] = None): String = {

    mayBeTableName.fold {
      fieldNames.foldRight(s"$defaultFieldName LIKE '%$fieldValue%'") { (fieldOne, fieldTwo) ⇒
        s"CASE WHEN $fieldOne IS NOT NULL THEN $fieldOne LIKE '%$fieldValue%' ELSE $fieldTwo"
      } ++ "END"
    } { tableName ⇒

      fieldNames.foldRight(s"$tableName.$defaultFieldName LIKE '%$fieldValue%'") {
        (fieldOne, fieldTwo) ⇒ s"CASE WHEN $tableName.$fieldOne IS NOT NULL THEN $tableName.$fieldOne LIKE '%$fieldValue%' ELSE $fieldTwo"
      } ++ "END"
    }
  }

  def queryConditionClauseForFilter[T](fieldValue: T, fieldName: String, mayBeTableName: Option[String] = None): String =
    mayBeTableName.fold(s"$fieldName != '$fieldValue'") { tableName ⇒ s"$tableName.$fieldName != '$fieldValue'" }

  //TODO it is used for queries with foreign key reference
  def subQueryConditionClause[T](
    fieldValue: T,
    fieldName: String,
    tableName: String,
    refFieldName: String,
    conditionalFieldName: String,
    refTableName: String): String =
    s"$tableName.$fieldName = (SELECT $refTableName.$refFieldName FROM $refTableName WHERE $refTableName.$conditionalFieldName='$fieldValue')"

  def selectWithGroups(tableAlias: String, groupingSeq: Seq[GroupingField]) = {
    groupingSeq.map { grouping ⇒
      s"""${grouping.toSql(defaultTableAlias = Some(tableAlias))} as ${grouping.projectionAlias.getOrElse(grouping.field)}"""
    }.mkStringOrEmpty("", ",", "")
  }

  def groupBy(tableAlias: String, groupingSeq: Seq[GroupingField]) = {
    groupingSeq.map { grouping ⇒
      grouping.toSql(defaultTableAlias = Some(tableAlias))
    }.mkStringOrEmpty(" GROUP BY ", ", ", "")
  }

  implicit class OptionInQueryStringAdapter(val arg: Option[String]) extends AnyVal {
    def toSql = arg.getOrElse("")
  }

  implicit class LocalDateTimeFormatter(arg: LocalDateTime) {
    def toSqlString: String = {
      val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

      arg.format(formatter)

    }
  }

  val countAlias = "n"

}
