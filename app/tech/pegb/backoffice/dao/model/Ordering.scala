package tech.pegb.backoffice.dao.model

trait Order

case class Ordering(field: String, order: Order, maybeTableNameOrAlias: Option[String] = None) {
  override def toString = maybeTableNameOrAlias.map(tblName ⇒ s"$tblName.$field $order").getOrElse(s"$field $order")

  def toSql(columnAlias: Option[String] = None, tableAlias: Option[String] = None): String = {
    val o = s"${columnAlias.getOrElse(field)} $order"
    tableAlias.map(t ⇒ s"$t.$o").getOrElse(o)
  }
}

object Ordering {
  case object ASC extends Order {
    override def toString = "ASC"
  }
  case object DESC extends Order {
    override def toString = "DESC"
  }
}
