package tech.pegb.backoffice.util.time

import java.time.LocalDate
import tech.pegb.backoffice.util.Implicits._

//TODO apply this trait to all criteria with date start/end ranges
trait DateRangeValidatable {
  val startDate: Option[LocalDate]
  val endDate: Option[LocalDate]

  def validateDateRange(implicit dateTimeRangeConfig: Option[DateTimeRangeConfig] = None): Either[Exception, Unit] = {
    DateTimeRangeUtil.validateDateTimeRange(startDate.map(_.atStartOfDay()), endDate.map(_.atEndOfDay))(dateTimeRangeConfig)
  }

}
