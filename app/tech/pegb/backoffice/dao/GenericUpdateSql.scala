package tech.pegb.backoffice.dao

import anorm.NamedParameter
import tech.pegb.backoffice.util.LastUpdatedAt

import scala.collection.mutable.ArrayBuffer

//TODO improve append operation by just providing list of fields and using implicit writer to invoke for diff Dao(i.e same as CriteriaField)
trait GenericUpdateSql extends LastUpdatedAt {

  val cLastUpdatedAt = "last_updated_at"

  val queryPartsBuilder = ArrayBuffer.newBuilder[String]

  val paramsBuilder = ArrayBuffer.newBuilder[NamedParameter]

  def append(param: NamedParameter): Unit = {
    queryPartsBuilder += s"`${param.name}` = {${param.name}}"
    paramsBuilder += param
  }

  def appendForGreenPlum(param: NamedParameter): Unit = {
    queryPartsBuilder += s"${param.name} = {${param.name}}"
    paramsBuilder += param
  }

  def createSqlString(tableName: String, filters: Option[String], disableOptimisticLock: Boolean = false): String = {
    val setValuesAsString = queryPartsBuilder.result().mkString(", ")

    val whereClause = if (disableOptimisticLock) {
      filters.getOrElse("")
    } else {
      val lastUpdatedAtQuery = lastUpdatedAt.fold("updated_at is NULL")(_ ⇒ s"updated_at = {$cLastUpdatedAt}")
      filters.fold(s"WHERE $lastUpdatedAtQuery")(where ⇒ s"$where AND $lastUpdatedAtQuery")
    }
    s"UPDATE $tableName SET $setValuesAsString $whereClause;"
  }

}
