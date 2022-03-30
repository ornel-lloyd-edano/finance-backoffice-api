package tech.pegb.backoffice.util.time

import scala.concurrent.duration.Duration

case class DateTimeRangeConfig(
    startDateLimitFromCurrent: Option[Duration] = None,
    endDateLimitFromCurrent: Option[Duration] = None,
    rangeLimit: Option[Duration] = None)
