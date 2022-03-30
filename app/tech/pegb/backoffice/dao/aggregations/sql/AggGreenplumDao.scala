package tech.pegb.backoffice.dao.aggregations.sql

import com.google.inject.{Inject, Singleton}
import play.api.db.DBApi
import tech.pegb.backoffice.dao.{PostgresDao}
import tech.pegb.backoffice.dao.aggregations.abstraction.GenericAggregationDao
import tech.pegb.backoffice.dao.aggregations.dto.{AggregatedValue, AggregationInput, AggregationResult, Entity, GroupByInput, Grouping}
import tech.pegb.backoffice.dao.model.{CriteriaField, OrderingSet}
import anorm._
import tech.pegb.backoffice.util.AppConfig

@Singleton
class AggGreenplumDao @Inject() (val dbApi: DBApi, val conf: AppConfig) extends GenericAggregationDao with PostgresDao {

  val defaultSchema = conf.SchemaName

  def aggregate(
    entity: Seq[Entity],
    expressionsToAggregate: Seq[AggregationInput],
    criteria: Seq[CriteriaField[_]],
    groupBy: Seq[GroupByInput],
    orderBy: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int]): DaoResponse[Seq[AggregationResult]] = {
    // TODO: segregate between mysql and greenplum , after refactor implement sql logic in domain class,
    // Extend the same class with SqlDao also
    import PostgresDao._

    logger.info(s"Print Greenplum Dao Args : entity =>  ${entity.toString()}  -|- expressionsToAggregate => ${expressionsToAggregate.toString()}  -|- criteria => ${criteria.toString()}  -|- groupBy => ${groupBy.toString()} -|- orderBy => ${orderBy.toString}")

    // use the implict writeSelect defined in PostgresDao
    val select = writeSelect(entity.headOption.flatMap(_.alias), expressionsToAggregate, groupBy)

    val nonAggColumns = groupBy.map(
      eachColumn ⇒ {
        PostgresDao.writeScalarFunctions(eachColumn.column, eachColumn.scalarFunction, eachColumn.alias, entity.headOption.flatMap(_.alias))
      })

    logger.info(" ++++++++++++++++++++++++ " + nonAggColumns.toString())

    // generate the final column names or aliases as per anorm
    val doesColumnHaveAlias = expressionsToAggregate.map(column ⇒
      column.alias.getOrElse(column.columnOrExpression) → (s".${column.alias.getOrElse("UnknownAlias")}", s".${column.alias.getOrElse("UnknownAlias")}")).toMap.++(
      nonAggColumns.map(column ⇒ column._2 → column._3).toMap)

    logger.info("Printing all columns : " + doesColumnHaveAlias)

    // generate a select 1 from joined columns for validation
    val fromClause = PostgresDao.writeEntityToSql(entity)

    val validationResult = fromClause.fold(
      left ⇒ Left(left), right ⇒ {
        val validationQuery = SQL(s"select 1 from ${right} limit 1")
        logger.info("Validaton Query : " + validationQuery)
        withConnectionHasDefaultSchema(
          implicit cxn ⇒ {
            validationQuery.execute()
          }, defaultSchema, (s"SQL error while executing JOIN query for ${entity.mkString(",")}")).fold(
            left ⇒ Left(left), right ⇒ Right(right))
      })

    // TODO : Fail if validation query has DaoError
    validationResult.fold(

      err ⇒ Left(err), right ⇒ {
        withConnectionHasDefaultSchema(
          implicit conn ⇒ {
            // separate the select query and delete any extra commas in the end

            val query = select +
              s""" FROM ${fromClause.right.get} WHERE ${
                criteria.map(each ⇒ {
                  each.toSql(Option(each.actualColumn), None)
                }).mkString(" AND ")
              } ${if (nonAggColumns.nonEmpty) "GROUP BY " + nonAggColumns.map(cols ⇒ cols._2).mkString(",") else ""} """

            logger.info("aggregate query = " + query)

            val anormQuery = SQL(query)
            // map the anorm Row as per the input params, differentiate into aggregated and group cols
            val tmp = anormQuery.as(anormQuery.defaultParser.*)

            logger.info(" Printing the result set object " + tmp.toString())

            val finalSqlResultset = tmp.map(row ⇒ {

              // While extraction of columns from the Map, we use the convention that agg columns are first, then nonAgg columns
              // Anorm row.asMap has this behaviour that it adds either a leading dot(.) or <tablename>. in front of the map keys (the columns/alias in sql)

              val rowMap = row.asMap.mapValues {
                case Some(value) ⇒ Some(value)
                case None ⇒ None
                case any ⇒ Some(any)
              }.withDefaultValue(None)

              logger.info("Print the resultSet as map : " + rowMap.toString())

              //logger.info("## Line 87 ##" + rowMap.get("." + expressionsToAggregate.seq(0).alias.get))

              val aggregates = expressionsToAggregate.map(input ⇒ {
                val (anormKey, postgresKey) = doesColumnHaveAlias.get(input.alias.getOrElse("UnknownAlias")).getOrElse(("Unknown", "Unknown"))
                //
                val aggResult = AggregatedValue(
                  input.alias.get,
                  (rowMap(postgresKey), rowMap(anormKey)) match {
                    case (Some(value: String), _) if (value.trim.nonEmpty) ⇒ value
                    case (None, Some(value: String)) if (value.trim.nonEmpty) ⇒ value
                    case (_, _) ⇒ "0.0"
                  })
                logger.info("## Aggregation Result ##" + aggResult)
                aggResult
              })

              // Even though we provide an alias, anorm generic asMap ignores and creates the key to be <tablename>.<columnName>
              // We were expecting <tableAlias>

              val nonAggs = nonAggColumns.map(input ⇒ {
                logger.info("Fetching non-agg :" + input.toString())
                val (anormKey, postgresKey) = doesColumnHaveAlias.get(input._2).getOrElse(("Unknown", "Unknown"))

                val nonAggResult = Grouping(
                  input._2,
                  (rowMap(postgresKey), rowMap(anormKey)) match {
                    case (Some(sqlValue), _) ⇒ sqlValue.toString
                    case (None, Some(sqlValue)) ⇒ sqlValue.toString
                    case (None, None) ⇒ " "
                  })
                nonAggResult
              })

              logger.info(AggregationResult(aggregates, nonAggs).toString)

              AggregationResult(aggregates, nonAggs)

            }).toSeq

            finalSqlResultset
            //Seq(AggregationResult(Seq(AggregatedValue("None", "None")), Seq(Grouping("None", "None"))))
          }, schemaName = conf.SchemaName, ("Error encountered while executing aggregation for dashboard"))
      })

  }
}

object AggGreenplumDao {
}

