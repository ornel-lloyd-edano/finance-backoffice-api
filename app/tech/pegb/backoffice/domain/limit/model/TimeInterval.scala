package tech.pegb.backoffice.domain.limit.model

import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

sealed trait TimeInterval

case class TimeIntervalWrapper(underlying: String) {
  assert(underlying.hasSomething, "empty interval")
  assert(underlying.matches("""[A-Za-z]+[A-Za-z0-9\-\_ ]*"""), s"invalid interval: ${underlying}")

  def asTimeInterval: TimeInterval = {
    TimeIntervals(underlying)
  }
}

object TimeIntervals {
  private val daily = "daily"
  private val weekly = "weekly"
  private val monthly = "monthly"
  private val yearly = "yearly"

  def apply(underlying: String): TimeInterval = {
    assert(underlying.hasSomething, "empty interval")
    val tryInterval = Try(fromString(underlying))
    tryInterval.get
  }

  case object Daily extends TimeInterval {
    override def toString: String = daily
  }

  case object Weekly extends TimeInterval {
    override def toString: String = weekly
  }

  case object Monthly extends TimeInterval {
    override def toString: String = monthly
  }

  case object Yearly extends TimeInterval {
    override def toString: String = yearly
  }

  def fromString(s: String): TimeInterval = s match {
    case `daily` ⇒ Daily
    case `weekly` ⇒ Weekly
    case `monthly` ⇒ Monthly
    case `yearly` ⇒ Yearly
  }
}
