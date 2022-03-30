package tech.pegb.backoffice.mapping.api.domain.commission

import java.time.ZonedDateTime
import java.util.{Currency, UUID}

import tech.pegb.backoffice.api.commission.dto.{CommissionProfileRangeToCreate, CommissionProfileToCreate}
import tech.pegb.backoffice.domain.businessuserapplication.model.{BusinessTypes, BusinessUserTiers}
import tech.pegb.backoffice.domain.commission.dto
import tech.pegb.backoffice.domain.commission.dto.CommissionProfileCriteria
import tech.pegb.backoffice.domain.transaction.model.TransactionType
import tech.pegb.backoffice.domain.types.enum.CommissionCalculationMethods
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.UUIDLike

import scala.util.Try

object Implicits {

  implicit class CommissionProfileCriteriaAdapter(val arg: (Option[UUIDLike], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Option[String], Set[String])) extends AnyVal {
    def asDomain: Try[CommissionProfileCriteria] =
      Try(
        CommissionProfileCriteria(
          uuid = arg._1,
          businessType = arg._2.map(BusinessTypes.fromString(_)),
          tier = arg._3.map(BusinessUserTiers.fromString(_)),
          subscriptionType = arg._4,
          transactionType = arg._5.map(TransactionType(_)),
          currency = arg._6.map(Currency.getInstance(_)), //TODO: sanitize
          channel = arg._7,
          instrument = arg._8,
          calculationMethod = arg._9.map(CommissionCalculationMethods.fromString(_)),
          partialMatchFields = arg._10))
  }

  implicit class CommissionProfileToCreateToDomain(val arg: CommissionProfileToCreate) extends AnyVal {
    def asDomain(id: UUID, doneAt: ZonedDateTime, doneBy: String) = Try {
      dto.CommissionProfileToCreate(
        uuid = id,
        businessType = BusinessTypes.fromString(arg.businessType),
        tier = BusinessUserTiers.fromString(arg.tier),
        subscriptionType = arg.subscriptionType,
        transactionType = TransactionType(arg.transactionType),
        currencyCode = Currency.getInstance(arg.currencyCode), //TODO: perform in domain validation
        channel = arg.channel,
        instrument = arg.instrument,
        calculationMethod = CommissionCalculationMethods.fromString(arg.calculationMethod),
        maxCommission = arg.maxCommission,
        minCommission = arg.minCommission,
        flatAmount = arg.commissionAmount,
        percentageAmount = arg.commissionRatio,
        ranges = arg.ranges.map(_.map(_.asDomain)),
        createdBy = doneBy,
        createdAt = doneAt.toLocalDateTimeUTC)
    }
  }

  implicit class CommissionProfileRangeToCreateToDomain(val arg: CommissionProfileRangeToCreate) extends AnyVal {
    def asDomain = dto.CommissionProfileRangeToCreate(
      from = arg.min,
      to = arg.max,
      flatAmount = arg.commissionAmount,
      percentageAmount = arg.commissionRatio)
  }

}
