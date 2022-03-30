package tech.pegb.backoffice.dao

import java.sql.Connection
import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

import anorm.SQL
import play.api.db.Database
import tech.pegb.backoffice.dao.aggregations.abstraction.AggFunctions.{Avg, Count, Sum}
import tech.pegb.backoffice.dao.DaoError.RowParsingError
import tech.pegb.backoffice.dao.aggregations.abstraction.ScalarFunction
import tech.pegb.backoffice.dao.aggregations.abstraction.ScalarFunctions.{GetDate, GetMonth, GetWeek}
import tech.pegb.backoffice.dao.aggregations.dto.{AggregationInput, Entity, GroupByInput}
import tech.pegb.backoffice.dao.customer.abstraction.Transactional
import tech.pegb.backoffice.dao.model.MatchType
import tech.pegb.backoffice.dao.model.MatchTypes._
import tech.pegb.backoffice.util.{CriteriaWriter, TableToAliasLookups}

import scala.util.Try
import scala.util.control.NonFatal

trait PostgresDao extends SqlDao with Transactional {

  override def db: Database = dbApi.database("reports")

  private def modifyConnectionSetDefaultSchema = (cnx: Connection, schemaName: String) ⇒ {
    val defaultSchema = SQL(s"set search_path to $schemaName")
    defaultSchema.execute()(cnx)
    cnx
  }

  protected def withConnectionHasDefaultSchema[T](
    block: Connection ⇒ T,
    schemaName: String,
    errorMsg: ⇒ String,
    handlerPF: PartialFunction[Throwable, DaoError] = PartialFunction.empty): DaoResponse[T] = {
    Try(db.withConnection(
      cnx ⇒ {
        block(modifyConnectionSetDefaultSchema(cnx, schemaName))
      })).toEither.left.map(handlerPF.orElse(detailedNonFatalPF(errorMsg)))
  }

  protected def detailedNonFatalPF(errorMsg: ⇒ String): PartialFunction[Throwable, DaoError] = {
    case NonFatal(exc) ⇒
      val err = genericDbError(errorMsg)
      val detailedErr = genericDbError(errorMsg + ": " + exc.getStackTrace.mkString("\n"))
      logger.error(err.toString, exc)
      logger.error(detailedErr.toString, exc)
      err
  }
}

object PostgresDao extends CriteriaWriter {

  val dateTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

  // While extraction of columns from the Map, we use the convention that agg columns are first, then nonAgg columns
  // Anorm row.asMap has this behaviour that it adds either a leading dot(.) or <tablename>. in front of the map keys (the columns/alias in sql)

  // We are following the convention that all nonAgg columns are of the format table.column
  private def getAnormKeys(tableAndColumn: String, alias: Option[String]) = {
    val lookupTableAlias = tableAndColumn.substring(0, tableAndColumn.indexOf(".", 0))
    val actualTablename = TableToAliasLookups.tableAliasMap(lookupTableAlias)
    val columnName = alias.getOrElse(tableAndColumn.substring(tableAndColumn.indexOf(".", 0) + 1, tableAndColumn.length))
    val h2AnormKey = alias.map(cols ⇒ s".${cols}").getOrElse(s"${actualTablename}.${columnName}")
    val postgresKey = alias.map(cols ⇒ s"${actualTablename}.${cols}").getOrElse(s"${actualTablename}.${columnName}")
    // tuple 1 for H2 driver, tuple 2 for postgres
    //(.aliascolumn, tx.aliascolumn)
    (h2AnormKey, postgresKey)
  }

  //  private def doesAliasExists(alias: Option[String]): Boolean = {
  //    alias.nonEmpty
  //  }

  def writeScalarFunctions(column: String, scalarFunction: Option[ScalarFunction], alias: Option[String], mayBeTableAlias: Option[String]): (String, String, (String, String)) = {

    //val tableAlias = mayBeTableAlias.fold("")(ta ⇒ s"$ta.")
    val aliasOrColumn = alias.getOrElse(column)

    if (scalarFunction.nonEmpty) {
      scalarFunction.get match {
        // works only if column is a valid timestamp or date of the format ( yyyy-MM-dd HH:mm:ss )

        case GetMonth ⇒ {
          (s" extract(month from ${column}::date) || ', ' || extract(year from ${column}::date)  ${alias.map(" as " + _).getOrElse("")}", aliasOrColumn, getAnormKeys(column, alias))
        }
        case GetDate ⇒ (s" ${column}::date ${alias.map(" as " + _).getOrElse("")}", aliasOrColumn, getAnormKeys(column, alias))
        case GetWeek ⇒ (s" extract(week from ${column}::date) || ', ' || extract(year from ${column}::date) ${alias.map(" as " + _).getOrElse("")}", aliasOrColumn, getAnormKeys(column, alias))
        case _ ⇒ (column, aliasOrColumn, getAnormKeys(column, alias))
      }
    } else {
      //(s"${column} ${if (alias.exists(_.trim.nonEmpty)) "as " + alias.get else ""}", alias.getOrElse(column))
      (s"${column} ${alias.map(" as " + _).getOrElse("")}", aliasOrColumn, getAnormKeys(column, alias))
    }

  }

  implicit def writeSelect(
    mayBeTableAlias: Option[String],
    expressionsToAggregate: Seq[AggregationInput],
    groupBy: Seq[GroupByInput]) = {

    (expressionsToAggregate.isEmpty, groupBy.isEmpty) match {
      case (true, true) ⇒ "SELECT "
      case (true, false) ⇒ {
        val nonAggColumns = groupBy.map(
          eachColumn ⇒ {
            PostgresDao.writeScalarFunctions(eachColumn.column, eachColumn.scalarFunction, eachColumn.alias, mayBeTableAlias)
          })
        s"SELECT ${nonAggColumns.map(cols ⇒ cols._1).mkString(",")}"
      }
      case (false, true) ⇒ {
        "SELECT " + expressionsToAggregate.map(
          row ⇒ {
            row.function match {
              case Sum ⇒ s"sum(${row.columnOrExpression})::varchar(300) ${row.alias.map(someAlias ⇒ "as " + someAlias).getOrElse("")}"
              case Avg ⇒ s"avg(${row.columnOrExpression})::varchar(300) as ${row.alias}"
              case Count ⇒ s"count(${row.columnOrExpression})::varchar(300) as ${row.alias}"
              case _ ⇒ s"${row.columnOrExpression} as ${row.alias}"
            }
          }).mkString(",")

      }
      case (false, false) ⇒ {
        val nonAggColumns = groupBy.map(
          eachColumn ⇒ {
            PostgresDao.writeScalarFunctions(eachColumn.column, eachColumn.scalarFunction, eachColumn.alias, mayBeTableAlias)
          })

        // separate the select query and delete any extra commas in the end
        "SELECT " + expressionsToAggregate.map(
          row ⇒ {
            row.function match {
              case Sum ⇒ s"sum(${row.columnOrExpression})::varchar(300) ${row.alias.map(someAlias ⇒ "as " + someAlias).getOrElse("")}"
              case Avg ⇒ s"avg(${row.columnOrExpression})::varchar(300) as ${row.alias}"
              case Count ⇒ s"count(${row.columnOrExpression})::varchar(300) as ${row.alias}"
              case _ ⇒ s"${row.columnOrExpression} as ${row.alias}"
            }
          }).mkString(",") + "," + nonAggColumns.map(cols ⇒ cols._1).mkString(",")

      }
    }
  }

  /*
  val entity = Seq(Entity("transactions", Option("transactions")), Entity("accounts", Option("accounts"), Seq(JoinColumn("transactions.primary_account_id", "accounts.id"))))
  transactions as txn
  left outer join
  accounts as acct
  on
  txn.primary_account_id = acct.id
  and txn.status = acct.status
   */

  def writeEntityToSql(entity: Seq[Entity]): Either[DaoError, String] = {
    (entity.isEmpty, entity.size > 1) match {
      case (false, true) ⇒ {
        val multiTableJoin = entity.foldLeft[(String, String)](("", ""))(
          (result, each) ⇒ {
            (each.alias.getOrElse(each.name),
              // Generate the sql of the form : transactions as transactions left outer join accounts as accounts on transactions.primary_account_id = accounts.id
              result._2 + s" left outer join ${each.name} as ${each.alias.getOrElse(each.name)} ${

                val requiredJoins = each.joinColumns
                  .map(joinseq ⇒ {
                    s"${joinseq.leftSideColumn} = ${joinseq.rightSideColumn}"
                  })

                if (requiredJoins.isEmpty) "" else requiredJoins.mkString(" on ", "and ", "")

              }")
          })._2.replaceFirst("left outer join", "")

        Right(multiTableJoin)

      }
      case (false, false) ⇒ {
        val singleTable = entity(0)
        Right(s"${singleTable.name} ${singleTable.alias.map(a ⇒ s" as $a ").getOrElse(singleTable.name)}")

      }
      case (true, _) ⇒ {
        Left(RowParsingError("Requires atleast one table").asInstanceOf[DaoError])
      }
    }
  }

  // TODO How can we catch that a function implementation is missing at compile time while using sqlCriteriaWriter ?
  implicit def writeCriteria(aliasedField: String, realValue: String, operator: MatchType): String = {
    operator match {
      case GreaterOrEqual ⇒ s"upper(cast($aliasedField as varchar)) >= ${realValue.toUpperCase}"
      case LesserOrEqual ⇒ s"upper(cast($aliasedField as varchar)) <= ${realValue.toUpperCase}"
      case In ⇒ s"upper(cast($aliasedField as varchar)) IN ${realValue.toUpperCase}"
      case NotIn ⇒ s"upper(cast($aliasedField as varchar)) NOT IN ${realValue.toUpperCase}"
      case Exact ⇒ s"upper(cast($aliasedField as varchar)) = ${realValue.toUpperCase}"
      case NotSame ⇒ s"upper(cast($aliasedField as varchar)) <> ${realValue.toUpperCase}"
      case Partial ⇒ s"upper(cast($aliasedField as varchar)) LIKE ${realValue.toUpperCase}"
      // implement a sql not like for FilterNotPartial
      case NotPartial ⇒ s"upper(cast($aliasedField as varchar)) NOT LIKE ${realValue.toUpperCase}"
      case InclusiveBetween ⇒ s"upper(cast($aliasedField as varchar)) BETWEEN ${realValue.toUpperCase}"
      case IsNotNull ⇒ s"$aliasedField IS NOT NULL"
      case IsNull ⇒ s"$aliasedField IS NULL"
    }
  }

  def fromDateTimeRange(
    alias: String,
    fieldName: String,
    startDate: Option[LocalDateTime],
    endDate: Option[LocalDateTime]): Option[String] = {
    (startDate, endDate) match {
      case (Some(start), None) ⇒ Some(s"$alias.$fieldName >= '${start.format(dateTimeFormat)}'")
      case (None, Some(end)) ⇒ Some(s"$alias.$fieldName =< '${end.format(dateTimeFormat)}'")
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

  def sorter(mayBeOrdering: Option[model.Ordering]): String = mayBeOrdering.map(o ⇒ s" ORDER BY ${o.field} ${o.order} ").getOrElse("")

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

  def queryNullableConditionClause[T](
    fieldValueOption: Option[T],
    fieldName: String,
    mayBeTableName: Option[String] = None): Option[String] = {
    fieldValueOption match {
      case Some(null) ⇒ Some(queryNullConditionClause(fieldName, true, mayBeTableName))
      case fieldValueOpt ⇒ fieldValueOpt.map(queryConditionClause(_, fieldName, mayBeTableName))
    }
  }

  def queryConditionClause[T](
    fieldValue: T,
    fieldName: String,
    mayBeTableName: Option[String] = None,
    isPartialMatch: Boolean = false): String = {

    val assignment = if (!isPartialMatch) {
      s"= '$fieldValue'"
    } else {
      s"LIKE '%$fieldValue%'"
    }
    mayBeTableName.fold(s"$fieldName $assignment") { tableName ⇒ s"$tableName.$fieldName $assignment" }
  }

  def queryNullConditionClause(fieldName: String, isNull: Boolean, mayBeTableName: Option[String] = None): String = {
    val isOrIsNot = if (isNull) "IS" else "IS NOT"
    mayBeTableName.fold {
      s"$fieldName $isOrIsNot NULL"
    } { tableName ⇒
      s"$tableName.$fieldName $isOrIsNot NULL"
    }
  }

  def queryLikeClause(fieldValue: String, fieldName: String, mayBeTableName: Option[String] = None): String =
    mayBeTableName.fold {
      s"$fieldName LIKE '%$fieldValue%'"
    } { tableName ⇒
      s"$tableName.$fieldName LIKE '%$fieldValue%'"
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

  implicit class OptionInQueryStringAdapter(val arg: Option[String]) extends AnyVal {
    def toSql = arg.getOrElse("")
  }

  val countAlias = "n"

}
