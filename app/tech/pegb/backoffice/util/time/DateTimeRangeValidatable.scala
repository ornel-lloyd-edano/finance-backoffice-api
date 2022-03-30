package tech.pegb.backoffice.util.time

import java.time.LocalDateTime

//TODO apply this trait to all criteria with datetime start/end ranges
trait DateTimeRangeValidatable {
  val startDate: Option[LocalDateTime]
  val endDate: Option[LocalDateTime]

  def validateDateRange(implicit dateTimeRangeConfig: Option[DateTimeRangeConfig] = None): Either[Exception, Unit] = {
    DateTimeRangeUtil.validateDateTimeRange(startDate, endDate)(dateTimeRangeConfig)
  }

}
