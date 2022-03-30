package tech.pegb.backoffice.dao.model

import scala.collection.mutable
import tech.pegb.backoffice.util.Implicits._

case class OrderingSet(underlying: mutable.LinkedHashSet[Ordering]) {
  override def toString: String = {
    underlying.map(_.toString).mkStringOrEmpty("ORDER BY ", ",", "")
  }

  def toSql(tableAlias: Option[String]): String = {
    underlying.map(_.toSql(tableAlias = tableAlias)).mkStringOrEmpty("ORDER BY ", ",", "")
  }
}
object OrderingSet {
  //TODO remove this factory and replace third factory to appy(arg: (String, String)*)
  def apply(arg: Ordering*) = new OrderingSet(mutable.LinkedHashSet(arg.toSeq: _*))

  def apply(field: String, order: String) = new OrderingSet(mutable.LinkedHashSet(
    Ordering(field, if (order.trim.equalsIgnoreCase("ASC")) Ordering.ASC else Ordering.DESC)))

  def apply(arg: Iterable[(String, String)]) =
    new OrderingSet(mutable.LinkedHashSet(arg.toSeq.map(e ⇒ Ordering(e._1, if (e._2.trim.equalsIgnoreCase("ASC")) Ordering.ASC else Ordering.DESC)): _*))

}
