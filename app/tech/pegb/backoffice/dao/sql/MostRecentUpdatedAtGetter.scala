package tech.pegb.backoffice.dao.sql

import anorm._
import tech.pegb.backoffice.dao.Dao

//TODO I realize it is better to abstract the concept of getting the latest version of a resource on the domain
//So we have a trait in domain, HasLatestVersion with public method getLatestVersion
trait MostRecentUpdatedAtGetter[E, C] extends Dao {

  protected def getUpdatedAtColumn: String

  protected def getMainSelectQuery: String

  protected def getRowToEntityParser: Row ⇒ E

  protected def getWhereFilterFromCriteria(criteriaDto: Option[C]): String

  def getMostRecentUpdatedAt(maybeCriteria: Option[C]): DaoResponse[Option[E]] = {
    val rawQuery =
      s"""
         |${getMainSelectQuery}
         |${getWhereFilterFromCriteria(maybeCriteria)}
         |ORDER BY ${getUpdatedAtColumn} DESC
         |LIMIT 1;
         """.stripMargin
    withConnection({ implicit connection ⇒
      val anormQuery = SQL(rawQuery)

      anormQuery.as(anormQuery.defaultParser.singleOpt).map(getRowToEntityParser)

    }, s"Error in getMostRecentUpdatedAt, query: $rawQuery")
  }

}
