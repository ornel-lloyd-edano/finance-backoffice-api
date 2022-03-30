package tech.pegb.backoffice.dao.util

import tech.pegb.backoffice.util.Implicits._

import scala.collection.mutable

object Implicits {

  implicit class IntToBoolean(arg: Int) {
    def isInserted: Boolean = arg.toBoolean

    def isUpdated: Boolean = arg.toBoolean
  }

  implicit class StringifiedCriteriaFieldList(val arg: Seq[String]) extends AnyVal {
    def toSql = arg.mkStringOrEmpty(" WHERE ", " AND ", "")
    def toSqlAppend = arg.mkStringOrEmpty(" AND ", " AND ", "")
  }

  implicit class StringifiedOrderingList(val arg: mutable.LinkedHashSet[String]) extends AnyVal {
    def toSql = arg.mkStringOrEmpty(" ORDER BY ", ", ", "")
  }

}
