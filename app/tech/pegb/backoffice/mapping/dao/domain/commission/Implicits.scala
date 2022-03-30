package tech.pegb.backoffice.mapping.dao.domain.commission

import java.util.{Currency, UUID}

import tech.pegb.backoffice.dao.commission.entity.{CommissionProfile, CommissionProfileRange}
import tech.pegb.backoffice.domain.businessuserapplication.model.{BusinessTypes, BusinessUserTiers}
import tech.pegb.backoffice.domain.commission.model
import tech.pegb.backoffice.domain.types.enum.CommissionCalculationMethods

import scala.util.Try

object Implicits {

  implicit class CommissionProfileAdapter(val arg: CommissionProfile) extends AnyVal {
    def asDomain = Try {
      model.CommissionProfile(
        id = arg.id,
        uuid = UUID.fromString(arg.uuid),
        businessType = BusinessTypes.fromString(arg.businessType),
        tier = BusinessUserTiers.fromString(arg.tier),
        subscriptionType = arg.subscriptionType,
        transactionType = arg.transactionType,
        currencyId = arg.currencyId,
        currencyCode = Currency.getInstance(arg.currencyCode),
        channel = arg.channel,
        instrument = arg.instrument,
        calculationMethod = CommissionCalculationMethods.fromString(arg.calculationMethod),
        maxCommission = arg.maxCommission,
        minCommission = arg.minCommission,
        commissionAmount = arg.commissionAmount,
        commissionRatio = arg.commissionRatio,
        ranges = arg.ranges.map(_.map(_.asDomain)),
        createdBy = arg.createdBy,
        updatedBy = arg.updatedBy,
        createdAt = arg.createdAt,
        updatedAt = arg.updatedAt,
        deletedAt = arg.deletedAt)
    }
  }

  implicit class CommissionProfileRangeAdapter(val arg: CommissionProfileRange) extends AnyVal {
    def asDomain: model.CommissionProfileRange = {
      model.CommissionProfileRange(
        id = arg.id,
        commissionProfileId = arg.commissionProfileId,
        min = arg.min,
        max = arg.max,
        flatAmount = arg.commissionAmount,
        percentageAmount = arg.commissionRatio,
        createdAt = arg.createdAt,
        updatedAt = arg.updatedAt)
    }
  }
}
