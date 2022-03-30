package tech.pegb.backoffice.mapping.domain.dao.commission

import cats.data.NonEmptyList
import cats.implicits._
import tech.pegb.backoffice.dao.commission.dto
import tech.pegb.backoffice.dao.commission.dto.{CommissionProfileRangeToInsert, CommissionProfileToInsert}
import tech.pegb.backoffice.dao.commission.entity.CommissionProfile
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes}
import tech.pegb.backoffice.domain.commission.dto.{CommissionProfileCriteria, CommissionProfileRangeToCreate, CommissionProfileToCreate}

object Implicits {

  import CommissionProfile._

  implicit class CommissionProfileCriteriaAdapter(val arg: CommissionProfileCriteria) extends AnyVal {
    def asDao = dto.CommissionProfileCriteria(
      uuid = arg.uuid.map(f ⇒ CriteriaField(cUuid, f.toString,
        if (arg.partialMatchFields.contains("id")) MatchTypes.Partial else MatchTypes.Exact)),
      businessType = arg.businessType.map(f ⇒ CriteriaField(cBusinessType, f.toString)),
      tier = arg.tier.map(f ⇒ CriteriaField(cTier, f.toString)),
      subscriptionType = arg.subscriptionType.map(f ⇒ CriteriaField(cSubscriptionType, f)),
      transactionType = arg.transactionType.map(f ⇒ CriteriaField(cTransactionType, f.underlying)),
      currency = arg.currency.map(f ⇒ CriteriaField(cCurrencyCode, f.getCurrencyCode)),
      channel = arg.channel.map(f ⇒ CriteriaField(cChannel, f)),
      instrument = arg.instrument.map(f ⇒ CriteriaField(cInstrument, f)),
      calculationMethod = arg.calculationMethod.map(f ⇒ CriteriaField(cCalculationMethod, f.toString)),
      isDeleted = CriteriaField("", false).some)
  }

  implicit class CommissionToCreateToDaoAdapter(val arg: CommissionProfileToCreate) extends AnyVal {
    def asDao(currencyId: Int) = CommissionProfileToInsert(
      uuid = arg.uuid.toString,
      businessType = arg.businessType.toString,
      tier = arg.tier.toString,
      subscriptionType = arg.subscriptionType,
      transactionType = arg.transactionType.underlying,
      currencyId = currencyId,
      channel = arg.channel,
      instrument = arg.instrument,
      calculationMethod = arg.calculationMethod.toString,
      maxCommission = arg.maxCommission,
      minCommission = arg.minCommission,
      commissionAmount = arg.flatAmount,
      commissionRatio = arg.percentageAmount,
      ranges = arg.ranges.map(r ⇒ NonEmptyList(r.head.asDao, r.tail.map(_.asDao).toList)),
      createdBy = arg.createdBy,
      createdAt = arg.createdAt)

    def asDaoCriteria = dto.CommissionProfileCriteria(
      businessType = CriteriaField(cBusinessType, arg.businessType.toString).some,
      tier = CriteriaField(cTier, arg.tier.toString).some,
      subscriptionType = CriteriaField(cSubscriptionType, arg.subscriptionType).some,
      transactionType = CriteriaField(cTransactionType, arg.transactionType.underlying).some,
      currency = CriteriaField(cCurrencyCode, arg.currencyCode.toString).some,
      channel = arg.channel.map(f ⇒ CriteriaField(cChannel, f)),
      instrument = arg.instrument.map(f ⇒ CriteriaField(cInstrument, f)),
      isDeleted = CriteriaField("", false).some)
  }

  implicit class CommissionRangesToDao(val arg: CommissionProfileRangeToCreate) extends AnyVal {
    def asDao = CommissionProfileRangeToInsert(
      max = arg.to,
      min = arg.from.some,
      commissionAmount = arg.flatAmount,
      commissionRatio = arg.percentageAmount)
  }
}
