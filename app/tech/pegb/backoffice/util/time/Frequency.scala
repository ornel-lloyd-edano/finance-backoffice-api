package tech.pegb.backoffice.util.time

sealed trait Frequency

object Frequency {
  def fromString(value: String): Frequency = {
    value match {
      case "daily" ⇒ Daily
      case "weekly" ⇒ Weekly
      case "monthly" ⇒ Monthly
      case _ ⇒ Daily
    }
  }
}

case object Daily extends Frequency
case object Weekly extends Frequency
case object Monthly extends Frequency
