package tech.pegb.backoffice.dao.model

import java.time.LocalDate

trait Aggregation {
  val date: Option[LocalDate]
  val day: Option[Int]
  val month: Option[Int]
  val year: Option[Int]
  val hour: Option[Int]
  val minute: Option[Int]
  val sum: Option[BigDecimal]
  val count: Option[Long]
}
