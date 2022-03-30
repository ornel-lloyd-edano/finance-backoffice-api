package tech.pegb.backoffice.mapping.domain.api

trait AsApiAdapter[T1, T2] {
  def asApi(arg: T1): T2
}
