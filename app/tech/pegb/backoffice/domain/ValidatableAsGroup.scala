package tech.pegb.backoffice.domain

import tech.pegb.backoffice.domain.BaseService.ServiceResponse

trait ValidatableAsGroup[T] {
  def validate(arg: Iterable[T]): ServiceResponse[Unit]
}
