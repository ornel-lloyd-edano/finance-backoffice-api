package tech.pegb.backoffice.mapping.domain.dao.types

import tech.pegb.backoffice.dao.types.entity.DescriptionToUpsert
import tech.pegb.backoffice.domain.types.abstraction.TypeEnum

object Implicits {
  implicit class DescriptionTypeAdapter(val arg: TypeEnum) extends AnyVal {
    def asDao = DescriptionToUpsert(None, arg.toString, Some(s"type of ${arg.kind}"))
  }
}
