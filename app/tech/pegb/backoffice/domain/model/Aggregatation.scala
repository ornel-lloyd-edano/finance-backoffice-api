package tech.pegb.backoffice.domain.model

import java.time.LocalDate

import tech.pegb.backoffice.domain.Identifiable

trait Aggregatation extends Identifiable {
  val uniqueId: String
  val date: Option[LocalDate] = None
  val day: Option[Int] = None
  val month: Option[Int] = None
  val year: Option[Int] = None
  val hour: Option[Int] = None
  val minute: Option[Int] = None
  val sum: Option[BigDecimal]
  val count: Option[Long]
}
