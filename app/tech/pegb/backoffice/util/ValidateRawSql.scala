package tech.pegb.backoffice.util

import java.sql.{Connection, PreparedStatement}

import play.api.db.Database

class ValidateRawSql(greenplumDb: Database) {

  def validateGreenplumQuery(rawSql: String): Either[Boolean, String] = {
    val conn = PostgresqlComponent.db
    val gpQuery = rawSql.stripSuffix(";") + " LIMIT 1;"
    val prepStmt: PreparedStatement = conn.prepareStatement(gpQuery)
    val result: Either[Boolean, String] =
      try Left(prepStmt.execute())
      catch {
        case ex: Exception ⇒
          Right(ex.getMessage)
      }
    //conn.close()
    result
  }

}

object ValidateRawSql {
  def validateGreenplumQuery(conn: Connection, rawSql: String): Either[Boolean, String] = {
    //val conn = gpdb.getConnection()
    val gpQuery = rawSql.stripSuffix(";") + " LIMIT 1;"
    val prepStmt: PreparedStatement = conn.prepareStatement(gpQuery)
    val result: Either[Boolean, String] =
      try Left(prepStmt.execute())
      catch {
        case ex: Exception ⇒
          Right(ex.getMessage)
      }
    //conn.close()
    result
  }
}
