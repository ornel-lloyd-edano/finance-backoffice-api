package tech.pegb.backoffice.domain.limit.implementation

import java.util.UUID

import com.google.inject.Inject
import tech.pegb.backoffice.domain.ServiceError
import tech.pegb.backoffice.domain.limit.abstraction.LimitManagementValidation
import tech.pegb.backoffice.domain.limit.model.TimeIntervals.{Daily, Monthly, Weekly, Yearly}
import tech.pegb.backoffice.domain.limit.model.{LimitProfile, TimeInterval}
import tech.pegb.backoffice.util.Implicits._

class LimitMgmtServiceValidation @Inject() extends LimitManagementValidation {

  def validateCurrentLimitWithExistingLimit(
    interval: Option[TimeInterval],
    maxIntervalAmount: Option[BigDecimal],
    maxIntervalCount: Option[Int],
    matchingLimitProfiles: Seq[LimitProfile])(implicit requestId: UUID): Option[ServiceError] = {

    if (interval.nonEmpty) {
      val intervalAmountValidation = maxIntervalAmount.flatMap { intervalAmount ⇒
        matchingLimitProfiles
          .filter(p ⇒ p.interval.isDefined && p.maxIntervalAmount.isDefined)
          .flatMap { foundProfile ⇒
            validateIntervals(
              interval.get,
              foundProfile.interval.get,
              intervalAmount,
              foundProfile.maxIntervalAmount.get,
              "interval amount")
          }.headOption
      }

      val intervalCountValidation = maxIntervalCount.flatMap { intervalCount ⇒
        matchingLimitProfiles
          .filter(p ⇒ p.interval.isDefined && p.maxCount.isDefined)
          .flatMap { foundProfile ⇒
            validateIntervals(
              interval.get,
              foundProfile.interval.get,
              intervalCount,
              foundProfile.maxCount.get,
              "interval count")
          }.headOption

      }
      Seq(intervalAmountValidation, intervalCountValidation).flatten.headOption
    } else matchingLimitProfiles
      .headOption
      .fold[Option[ServiceError]](None)(_ ⇒ Some(
        ServiceError.duplicateError(s"Similar Limit Profile already exists in db.", requestId.toOption)))
  }

  private def validateIntervals(
    newInterval: TimeInterval,
    existingInterval: TimeInterval,
    newIntervalAmount: BigDecimal,
    existingIntervalAmount: BigDecimal,
    fieldToValidate: String): Option[ServiceError] = {
    newInterval match {
      case Daily ⇒
        existingInterval match {
          case existing @ (Weekly | Monthly | Yearly) if newIntervalAmount >= existingIntervalAmount ⇒
            Some(ServiceError.validationError(
              s"max $fieldToValidate can not be more than or equal to $existingIntervalAmount for $newInterval interval"))
          case _ ⇒ None
        }
      case Weekly ⇒
        existingInterval match {
          case Daily if newIntervalAmount <= existingIntervalAmount ⇒
            Some(ServiceError.validationError(
              s"max $fieldToValidate can not be less than $existingIntervalAmount for $newInterval interval"))
          case existing @ (Monthly | Yearly) if newIntervalAmount >= existingIntervalAmount ⇒
            Some(ServiceError.validationError(
              s"max $fieldToValidate can not be more than or equal to $existingIntervalAmount for $newInterval interval"))
          case _ ⇒ None
        }
      case Monthly ⇒
        existingInterval match {
          case existing @ (Daily | Weekly) if newIntervalAmount <= existingIntervalAmount ⇒
            Some(ServiceError.validationError(
              s"max $fieldToValidate can not be less than or equal to $existingIntervalAmount for $newInterval interval"))
          case Yearly if newIntervalAmount >= existingIntervalAmount ⇒
            Some(ServiceError.validationError(
              s"max $fieldToValidate can not be more than or equal to $existingIntervalAmount for $newInterval interval"))
          case _ ⇒ None
        }
      case Yearly ⇒
        existingInterval match {
          case existing @ (Daily | Weekly | Monthly) if newIntervalAmount <= existingIntervalAmount ⇒
            Some(ServiceError.validationError(
              s"max $fieldToValidate can not be less than or equal to $existingIntervalAmount for $newInterval interval"))
          case _ ⇒ None
        }
    }
  }

}
