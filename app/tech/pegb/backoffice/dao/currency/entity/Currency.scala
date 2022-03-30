package tech.pegb.backoffice.dao.currency.entity

import java.time.LocalDateTime

import org.coursera.autoschema.annotations._
import play.api.libs.json.Json

case class Currency(
    id: Int,
    name: String,
    description: Option[String],
    isActive: Boolean,
    icon: Option[String],
    @Term.Hide createdAt: LocalDateTime,
    @Term.Hide createdBy: String,
    @Term.Hide updatedAt: Option[LocalDateTime],
    @Term.Hide updatedBy: Option[String]) {

  override def equals(obj: scala.Any): Boolean = {
    //there is unique constraint in table currency for this column
    //so if two entity have the same name they are practically the same regardless
    obj.asInstanceOf[Currency].name == name
  }
}

object Currency {
  implicit val f = Json.format[Currency]
}
