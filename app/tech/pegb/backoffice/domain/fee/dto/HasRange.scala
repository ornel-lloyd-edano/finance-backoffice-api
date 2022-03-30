package tech.pegb.backoffice.domain.fee.dto

trait HasRange[T] {

  val from: T
  val to: Option[T]
  val flatAmount: Option[T]
  val percentageAmount: Option[T]

}
