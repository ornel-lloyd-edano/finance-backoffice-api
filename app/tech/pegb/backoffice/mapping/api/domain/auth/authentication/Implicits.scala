package tech.pegb.backoffice.mapping.api.domain.auth.authentication

import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.LoginUsername

import scala.util.Try

object Implicits {

  implicit class StringToDomainWrapperAdapter(val arg: String) extends AnyVal {
    def asLoginUsername() = Try {
      LoginUsername(arg)
    }

    def asEmail() = Try {
      Email(arg)
    }
  }

}
