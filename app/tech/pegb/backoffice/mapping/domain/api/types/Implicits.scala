package tech.pegb.backoffice.mapping.domain.api.types

import tech.pegb.backoffice.api.types.dto.TypeToRead
import tech.pegb.backoffice.domain.types.model.TypeDescription

object Implicits {
  implicit class TypeAdapter(val arg: TypeDescription) extends AnyVal {
    def asApi: TypeToRead = {
      TypeToRead(
        id = arg.id,
        name = arg.name,
        description = arg.description)
    }
  }
}
