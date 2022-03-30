package tech.pegb.backoffice.dao.aggregations.dto

case class Entity(name: String, alias: Option[String] = None, joinColumns: Seq[JoinColumn] = Nil) {

}

case class JoinColumn(leftSideColumn: String, rightSideColumn: String)
