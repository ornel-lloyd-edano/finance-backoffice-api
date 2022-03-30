package tech.pegb.backoffice.domain

import tech.pegb.backoffice.domain.BaseService.{BatchValidatedServiceResponse, ServiceResponse}

trait Validatable[T] {

  def validate: ServiceResponse[T]

}

trait BatchValidatable[T] {

  def validate: BatchValidatedServiceResponse[T]

}
