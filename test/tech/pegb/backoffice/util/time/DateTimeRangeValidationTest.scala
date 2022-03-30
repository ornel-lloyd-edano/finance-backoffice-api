package tech.pegb.backoffice.util.time

import java.time.LocalDateTime

import org.scalamock.scalatest.MockFactory
import tech.pegb.core.PegBNoDbTestApp

import scala.concurrent.duration._

class DateTimeRangeValidationTest extends PegBNoDbTestApp with MockFactory {

  val validator = DateTimeRangeUtil

  "DateTimeRangeValidation" should {
    "validate that starting date/time must be earlier than some configured limit from the current date/time -positive" in {
      val startDateOneHundredDaysFromCurrent = LocalDateTime.now.minusDays(100)
      implicit val oneHundredOneDaysLimit = Option(DateTimeRangeConfig(startDateLimitFromCurrent = Some(101.days)))
      val result = validator.validateDateTimeRange(Some(startDateOneHundredDaysFromCurrent), None)

      result mustBe Right(())
    }

    "validate that starting date/time must be earlier than a configured limit from the current date/time -negative" in {
      val startDateOneHundredDaysFromCurrent = LocalDateTime.now.minusDays(100)
      implicit val ninetyNineDaysLimit = Option(DateTimeRangeConfig(startDateLimitFromCurrent = Some(99.days)))
      val result = validator.validateDateTimeRange(Some(startDateOneHundredDaysFromCurrent), None)

      val expected = s"Left(java.lang.Exception: Starting date/time [$startDateOneHundredDaysFromCurrent] must not be earlier than 99 days from current date/time.)"
      result.toString mustBe expected
    }

    "validate that starting date/time cannot be later than the current date/time -positive" in {
      val startDateOneDayAgo = LocalDateTime.now.minusDays(1)
      val result = validator.validateDateTimeRange(Some(startDateOneDayAgo), None)

      result mustBe Right(())
    }

    "validate that starting date/time cannot be later than the current date/time -negative" in {
      val startDateTomorrow = LocalDateTime.now.plusDays(1)
      val result = validator.validateDateTimeRange(Some(startDateTomorrow), None)

      val expected = s"Left(java.lang.Exception: Start date/time [$startDateTomorrow] cannot be later than the current date/time.)"
      result.toString mustBe expected
    }

    "validate that starting date/time cannot be later than the end date/time -positive" in {
      val startDateOneDayAgo = LocalDateTime.now.minusDays(1)
      val endDateTwoDaysFromNow = LocalDateTime.now.plusDays(2)
      val result = validator.validateDateTimeRange(Some(startDateOneDayAgo), Some(endDateTwoDaysFromNow))

      result mustBe Right(())
    }

    "validate that starting date/time cannot be later than the end date/time -negative" in {
      val startDateTwoDaysAgo = LocalDateTime.now.minusDays(2)
      val endDateThreeDaysAgo = LocalDateTime.now.minusDays(3)
      val result = validator.validateDateTimeRange(Some(startDateTwoDaysAgo), Some(endDateThreeDaysAgo))

      val expected = s"Left(java.lang.Exception: Start date/time [$startDateTwoDaysAgo] " +
        s"cannot be later than end date/time [$endDateThreeDaysAgo].)"
      result.toString mustBe expected
    }

    "validate that end date/time cannot be later than some configured limit from the current date/time -positive" in {
      val endDateNinetyNineDaysFromNow = LocalDateTime.now.plusDays(99)
      implicit val oneHundredOneDaysLimit = Option(DateTimeRangeConfig(endDateLimitFromCurrent = Some(100.days)))
      val result = validator.validateDateTimeRange(None, Some(endDateNinetyNineDaysFromNow))

      result mustBe Right(())
    }

    "validate that end date/time cannot be later than some configured limit from the current date/time -negative" in {
      val endDateOneHundredOneDaysFromNow = LocalDateTime.now.plusDays(101)
      implicit val oneHundredOneDaysLimit = Option(DateTimeRangeConfig(endDateLimitFromCurrent = Some(100.days)))
      val result = validator.validateDateTimeRange(None, Some(endDateOneHundredOneDaysFromNow))

      val expected = s"Left(java.lang.Exception: End date/time [$endDateOneHundredOneDaysFromNow] must not be later than 100 days from current date/time.)"
      result.toString mustBe expected
    }

    "validate that the span between start date/time to end date/time cannot be more than some configured limit -positive" in {
      val startDate = LocalDateTime.now.minusDays(50)
      val endDate = LocalDateTime.now.plusDays(49)
      implicit val oneHundredOneDaysLimit = Option(DateTimeRangeConfig(rangeLimit = Some(100.days)))
      val result = validator.validateDateTimeRange(Some(startDate), Some(endDate))

      result mustBe Right(())
    }

    "validate that the span between start date/time to end date/time cannot be more than some configured limit -negative" in {
      val startDate = LocalDateTime.now.minusDays(50)
      val endDate = LocalDateTime.now.plusDays(51)
      implicit val oneHundredOneDaysLimit = Option(DateTimeRangeConfig(rangeLimit = Some(100.days)))
      val result = validator.validateDateTimeRange(Some(startDate), Some(endDate))

      val expected = s"Left(java.lang.Exception: Span between start date/time [$startDate] to end date/time [$endDate] cannot exceed 100 days.)"
      result.toString mustBe expected
    }
  }

}
