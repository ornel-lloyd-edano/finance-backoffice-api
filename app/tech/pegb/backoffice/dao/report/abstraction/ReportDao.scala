package tech.pegb.backoffice.dao.report.abstraction

import com.google.inject.ImplementedBy
import play.api.libs.json.JsArray
import tech.pegb.backoffice.dao.Dao.DaoResponse
import tech.pegb.backoffice.dao.report.entity.Report
import tech.pegb.backoffice.dao.report.sql.ReportSqlDao

@ImplementedBy(classOf[ReportSqlDao])
trait ReportDao {

  def executeRawSql(rawSql: String, reportDefinitionParams: JsArray, queryParams: Map[String, String]): DaoResponse[Report]

}
