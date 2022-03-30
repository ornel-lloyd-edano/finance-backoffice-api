package tech.pegb.backoffice.util.time

import java.time.{Instant, LocalDate, LocalDateTime, ZoneOffset}
import java.time.temporal.WeekFields
import java.util.Locale

object DateTimeRangeUtil {

  val weekFields = WeekFields.of(Locale.getDefault)

  def createDateRange(from: Option[LocalDate], to: Option[LocalDate], frequency: Frequency): Seq[String] = {

    val defaultEndDate = to.getOrElse(LocalDate.now())
    val defaultStartDate = from.getOrElse(defaultEndDate.minusMonths(12))

    val dateRange = Iterator.iterate(defaultStartDate)(_.plusDays(1L)).takeWhile(!_.isAfter(defaultEndDate)).toList

    dateRange.map { d ⇒
      frequency match {
        case Daily ⇒
          d.toString
        case Weekly ⇒
          Week(d.get(weekFields.weekOfYear()), d.getYear).toString
        case Monthly ⇒
          Month(d.getMonthValue, d.getYear).toString
      }
    }.distinct
  }

  def validateDateTimeRange(
    startDateTime: Option[LocalDateTime],
    endDateTime: Option[LocalDateTime])(implicit dateTimeRangeConfig: Option[DateTimeRangeConfig] = None): Either[Exception, Unit] = {

    for {
      _ ← (startDateTime, dateTimeRangeConfig.flatMap(_.startDateLimitFromCurrent)) match {
        case (Some(startDateTime), Some(limit)) ⇒
          val deltaInMillisFromCurrentToStartDateTime =
            Math.abs(Instant.now().toEpochMilli - startDateTime.toInstant(ZoneOffset.UTC).toEpochMilli)
          if (deltaInMillisFromCurrentToStartDateTime > limit.toMillis) {
            Left(new Exception(s"Starting date/time [$startDateTime] must not be earlier than $limit from current date/time."))
          } else {
            Right(())
          }
        case _ ⇒ Right(())
      }

      _ ← (startDateTime, LocalDateTime.now()) match {
        case (Some(startDateTime), now) if startDateTime.isAfter(now) ⇒
          Left(new Exception(s"Start date/time [$startDateTime] cannot be later than the current date/time."))
        case _ ⇒ Right(())
      }

      _ ← (startDateTime, endDateTime) match {
        case (Some(startDateTime), Some(endDateTime)) if startDateTime.isAfter(endDateTime) ⇒
          Left(new Exception(s"Start date/time [$startDateTime] cannot be later than end date/time [$endDateTime]."))
        case _ ⇒ Right(())
      }

      _ ← (startDateTime, endDateTime, dateTimeRangeConfig.flatMap(_.rangeLimit)) match {
        case (Some(startDateTime), Some(endDateTime), Some(limit)) ⇒
          val deltaInMillisFromStartDateTimeToEndDateTime =
            Math.abs(startDateTime.toInstant(ZoneOffset.UTC).toEpochMilli - endDateTime.toInstant(ZoneOffset.UTC).toEpochMilli)
          if (deltaInMillisFromStartDateTimeToEndDateTime > limit.toMillis) {
            Left(new Exception(s"Span between start date/time [$startDateTime] to end date/time [$endDateTime] cannot exceed $limit."))
          } else {
            Right(())
          }
        case _ ⇒ Right(())
      }

      _ ← (endDateTime, dateTimeRangeConfig.flatMap(_.endDateLimitFromCurrent)) match {
        case (Some(endDateTime), Some(limit)) ⇒
          val deltaInMillisFromCurrentToEndDateTime =
            Math.abs(Instant.now().toEpochMilli - endDateTime.toInstant(ZoneOffset.UTC).toEpochMilli)
          if (deltaInMillisFromCurrentToEndDateTime > limit.toMillis) {
            Left(new Exception(s"End date/time [$endDateTime] must not be later than $limit from current date/time."))
          } else {
            Right(())
          }
        case _ ⇒ Right(())
      }
    } yield {
      ()
    }
  }
}
