package tech.pegb.backoffice.domain

import tech.pegb.backoffice.domain.BaseService.ServiceResponse

object Implicits {

  implicit class GroupValidation[T](val arg: Iterable[T]) extends AnyVal {
    def validate(implicit groupValidator: ValidatableAsGroup[T]): ServiceResponse[Unit] = {
      groupValidator.validate(arg)
    }
  }

}
