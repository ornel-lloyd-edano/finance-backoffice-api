package tech.pegb.backoffice.dao.report.sql

import java.sql.PreparedStatement

import com.google.inject.Inject
import play.api.db.DBApi
import play.api.libs.json
import play.api.libs.json._
import tech.pegb.backoffice.dao.PostgresDao
import tech.pegb.backoffice.dao.report.abstraction.ReportDao
import tech.pegb.backoffice.dao.report.entity.Report
import tech.pegb.backoffice.util.AppConfig

import scala.collection.mutable

class ReportSqlDao @Inject() (val dbApi: DBApi, appConfig: AppConfig) extends ReportDao with PostgresDao {
  import ReportSqlDao._

  val defaultSchema = appConfig.Reports.dbSchema
  val setDefaultSchema = s"""set search_path to $defaultSchema""".stripMargin

  def executeRawSql(rawSql: String, reportDefinitionParam: JsArray, queryParams: Map[String, String]): DaoResponse[Report] = {
    logger.info("From the query string  --> " + Json.toJson(queryParams))
    logger.info("Raw SQL --> " + rawSql)

    val modifiedQueryParams = limitOffsetUppercaseQueryParams(queryParams)

    val queryWithoutLimitOffset = reportDefinitionParam.as[List[json.JsValue]].foldLeft(rawSql)((acc, head) ⇒ {
      val name: String = (head \ "name").as[JsString].value
      val `type` = (head \ "type").as[JsString].value
      val default = (head \ "default").getOrElse(JsString("")).as[JsString].value
      val value = queryParams.getOrElse(name, default)
      val paramValue = mapParamValueToQuery(value, `type`)
      acc.replaceAll(s"\\{$name\\}", s"$paramValue")
    })

    val countQuery = "select count(1) as cnt from " + queryWithoutLimitOffset.split("(?i)from", 2)(1)

    logger.info("!!!!! ---- !!!!")
    logger.info(queryWithoutLimitOffset)

    val result = withConnectionHasDefaultSchema(
      block = { implicit cxn ⇒
        logger.info("!!! Count Query --> " + countQuery)
        val prepCountQuery = cxn.prepareStatement(countQuery)
        val resultSetCount = prepCountQuery.executeQuery()

        val total = if (resultSetCount.next().equals(true)) resultSetCount.getLong("cnt") else 0

        if (total > 0) {
          val (newSql, newParams) = maybeModifiedSql(queryWithoutLimitOffset, queryParams)
          val queryWithParams = newParams.foldLeft(newSql)((acc, head) ⇒ {
            // Only converts limit and offset to upper case
            val name: String = (head \ "name").as[JsString].value
            val `type` = (head \ "type").as[JsString].value
            val value = modifiedQueryParams.get(name)
            val paramValue = mapParamValueToQuery(value.get, `type`)
            logger.info(name + "---" + paramValue)
            acc.replaceAll(s"\\{${name.toLowerCase}\\}", s"$paramValue")
          })

          val sqlQuery: PreparedStatement = cxn.prepareStatement(s"""$queryWithParams """)
          logger.info("!!! SQL : " + sqlQuery)
          val resultSet = sqlQuery.executeQuery()
          val rsmd = resultSet.getMetaData
          val resultBuffer = mutable.ListBuffer[JsValue]()
          // TODO : Use immutable maps later. This works for generic query results

          while (resultSet.next()) {
            val rowData = (1 to rsmd.getColumnCount).map { i ⇒
              val result: Object = resultSet.getObject(i)
              val dbColumn = if (result == null) {
                null
              } else {
                result.toString
              }
              (rsmd.getColumnName(i) → dbColumn)
            }.toMap

            resultBuffer += Json.toJson(rowData)
          }
          val finalResult = resultBuffer.toList
          Report(total, finalResult)
        } else {
          Report(total, Seq.empty[JsValue])
        }

      },
      schemaName = defaultSchema,
      errorMsg = ("Error while fetching data for the report"))

    result
  }

  private def maybeModifiedSql(sql: String, queryParams: Map[String, String]): (String, Seq[JsValue]) = {
    val modifiedQueryParams = limitOffsetUppercaseQueryParams(queryParams)

    (modifiedQueryParams.get(vLimit), modifiedQueryParams.get(vOffset)) match {
      case (Some(_), Some(_)) ⇒ {
        logger.info(" Modifying the SQL ---> ")
        val limitOffset = Seq(
          Json.obj("name" → vLimit, "type" → "number"),
          Json.obj("name" → vOffset, "type" → "number"))
        logger.info("******************* " + limitOffset)

        //val modifiedParameters = Json.toJson(parametersSpec.as[List[JsValue]] ::: limitOffset).asInstanceOf[JsArray]
        (sql + " LIMIT {limit} OFFSET {offset}", limitOffset)
      }
      case (_, _) ⇒ {
        (sql, List.empty)
      }
    }
  }

}

object ReportSqlDao {

  val vLimit = "LIMIT"
  val vOffset = "OFFSET"

  def limitOffsetUppercaseQueryParams(queryParams: Map[String, String]): Map[String, String] = {
    queryParams.map {
      case (k, v) if (k.toUpperCase.contains(vLimit) || k.toUpperCase.contains(vOffset)) ⇒ k.toUpperCase → v
      case default ⇒ default
    }
  }

  def mapParamValueToQuery(value: String, `type`: String): String = {
    `type` match {
      case "boolean" | "amount" | "number" | "percentage" | "ratio" ⇒ value
      case _ ⇒ s"'$value'"
    }
  }
}
