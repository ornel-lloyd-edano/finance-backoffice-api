package tech.pegb.backoffice.mapping.domain.dao

trait AsDaoAdapter[T1, T2] {
  def asDao(arg: T1): T2
}
