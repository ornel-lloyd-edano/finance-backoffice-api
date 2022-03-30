package tech.pegb.backoffice.mapping.dao.domain.types

import tech.pegb.backoffice.dao.types.entity.{Description, DescriptionType}
import tech.pegb.backoffice.domain.types.model.TypeDescription

object Implicits {
  implicit class TupleConverter(val arg: (Int, String, Option[String])) extends AnyVal {
    def asDomain = TypeDescription(id = arg._1, name = arg._2, description = arg._3)
  }

  implicit class TypesDaoToDomainAdapter(val arg: Map[DescriptionType, Seq[Description]]) extends AnyVal {
    def asDomain = arg.map(entry ⇒ entry._1.`type` → entry._2.map(e ⇒ TypeDescription(e.id, e.name, e.description)))
  }
}
