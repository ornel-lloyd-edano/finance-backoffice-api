package tech.pegb.backoffice.mapping.domain.dao.fee

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.fee.dto.{FeeProfileCriteria ⇒ DaoFeeProfileCriteria, FeeProfileRangeToInsert ⇒ DaoFeeProfileRangeToCreate, FeeProfileRangeToUpdate ⇒ DaoFeeProfileRangeToUpdate, FeeProfileToInsert ⇒ DaoFeeProfileToCreate, FeeProfileToUpdate ⇒ DaoFeeProfileToUpdate}
import tech.pegb.backoffice.dao.fee.sql._
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes}
import tech.pegb.backoffice.dao.provider.entity.Provider
import tech.pegb.backoffice.domain.fee.dto._
import tech.pegb.backoffice.domain.fee.model.FeeAttributes.TaxInclusionType
import tech.pegb.backoffice.domain.fee.model.FeeProfile
import tech.pegb.backoffice.util.Constants._

object Implicits {

  implicit class FeeProfileRangeToCreateAdapter(val arg: FeeProfileRangeToCreate) extends AnyVal {
    //TODO is there a use case that maybeFeeProfileId = None? Maybe change it to non option
    def asDao(maybeFeeProfileId: Option[Int], isAmountPercentage: Boolean) = DaoFeeProfileRangeToCreate(
      feeProfileId = maybeFeeProfileId,
      max = arg.to,
      min = arg.from,
      feeAmount = arg.flatAmount,
      feeRatio = arg.percentageAmount)
  }

  implicit class FeeProfileToCreateAdapter(val arg: FeeProfileToCreate) extends AnyVal {
    def asDao(currencyId: Int, maybeFeeProfileId: Option[Int]) = DaoFeeProfileToCreate(
      feeType = arg.feeType.underlying,
      userType = arg.userType.underlying,
      tier = arg.tier.toString,
      subscriptionType = arg.subscription.underlying,
      transactionType = arg.transactionType.underlying,
      channel = arg.channel.map(_.underlying),
      provider = arg.otherParty,
      instrument = arg.instrument,
      calculationMethod = arg.calculationMethod.underlying,
      currencyId = currencyId,
      feeMethod = arg.feeMethod.underlying,
      taxIncluded = arg.taxInclusion.toString,
      maxFee = arg.maxFee,
      minFee = arg.minFee,
      feeAmount = arg.flatAmount,
      feeRatio = arg.percentageAmount,
      ranges = arg.ranges.map(_.map(_.asDao(maybeFeeProfileId, arg.calculationMethod.isPercentageType))),
      createdAt = arg.createdAt,
      createdBy = arg.createdBy)
  }

  implicit class FeeProfileCriteriaByCreateAdapter(val arg: FeeProfileToCreate) extends AnyVal {
    def asDaoCriteria = DaoFeeProfileCriteria(
      feeType = Some(CriteriaField(FeeProfileSqlDao.cFeeType, arg.feeType.underlying)),
      userType = Some(CriteriaField(FeeProfileSqlDao.cUserType, arg.userType.underlying)),
      tier = Some(CriteriaField(FeeProfileSqlDao.cTier, arg.tier.toString)),
      subscriptionType = Some(CriteriaField(FeeProfileSqlDao.cSubscriptionType, arg.subscription.underlying)),
      transactionType = Some(CriteriaField(FeeProfileSqlDao.cTransactionType, arg.transactionType.underlying)),
      channel = arg.channel.map(c ⇒ CriteriaField(s"${FeeProfileSqlDao.cChannel}", c.underlying))
        .orElse(Some(CriteriaField(s"${FeeProfileSqlDao.cChannel}", empty, MatchTypes.IsNull))),
      provider = arg.otherParty.map(op ⇒ CriteriaField(Provider.cName, op))
        .orElse(Some(CriteriaField(Provider.cName, empty, MatchTypes.IsNull))),
      instrument = arg.instrument.map(CriteriaField(FeeProfileSqlDao.cInstrument, _))
        .orElse(Some(CriteriaField(FeeProfileSqlDao.cInstrument, empty, MatchTypes.IsNull))),
      currencyCode = Some(CriteriaField(FeeProfileSqlDao.cCurrencyName, arg.currencyCode.getCurrencyCode)),
      isDeleted = Some(CriteriaField(FeeProfileSqlDao.cDeletedAt, false)))
  }

  implicit class FeeProfileToUpdateAdapter(val arg: FeeProfileToUpdate) extends AnyVal {
    def asDao(maybeFeeProfileId: Option[Int]) = DaoFeeProfileToUpdate(
      calculationMethod = arg.calculationMethod.underlying,
      feeMethod = arg.feeMethod.underlying,
      taxIncluded = arg.taxInclusion.toString,
      maxFee = arg.maxFee.orElse(Some(null)),
      minFee = arg.minFee.orElse(Some(null)),
      feeAmount = arg.flatAmount.orElse(Some(null)),
      feeRatio = arg.percentageAmount.orElse(Some(null)),
      ranges = arg.ranges.map(_.map(_.asDao(maybeFeeProfileId, arg.calculationMethod.isPercentageType))).orElse(Some(null)),
      deletedAt = None,
      updatedAt = arg.updatedAt,
      updatedBy = arg.updatedBy,
      lastUpdatedAt = arg.lastUpdatedAt)
  }

  implicit class FeeProfileRangeToUpdateAdapter(val arg: FeeProfileRangeToUpdate) extends AnyVal {
    def asDao = DaoFeeProfileRangeToUpdate(
      max = arg.to,
      min = arg.from,
      feeAmount = arg.flatAmount,
      feeRatio = arg.percentageAmount)
  }

  implicit class FeeProfileToDeleteAdapter(val arg: LocalDateTime) extends AnyVal {
    def asDao(
      calculationMethod: String,
      feeMethod: String,
      mbTaxIncluded: TaxInclusionType,
      updatedAt: LocalDateTime,
      updatedBy: String,
      lastUpdatedAt: Option[LocalDateTime]) = DaoFeeProfileToUpdate(
      calculationMethod = calculationMethod,
      feeMethod = feeMethod,
      taxIncluded = mbTaxIncluded.toString,
      ranges = None,
      deletedAt = Some(arg),
      updatedAt = updatedAt,
      updatedBy = updatedBy,
      lastUpdatedAt = lastUpdatedAt)
  }

  implicit class FeeProfileCriteriaAdapter(val arg: FeeProfileCriteria) extends AnyVal {
    def asDao = DaoFeeProfileCriteria(
      id = arg.id.map(id ⇒ CriteriaField(FeeProfileSqlDao.cId, id.underlying,
        if (arg.partialMatchFields.contains("id")) MatchTypes.Partial else MatchTypes.Exact)),
      feeType = arg.feeType.map(f ⇒ CriteriaField(FeeProfileSqlDao.cFeeType, f.underlying)),
      userType = arg.userType.map(u ⇒ CriteriaField(FeeProfileSqlDao.cUserType, u.underlying)),
      tier = arg.tier.map(u ⇒ CriteriaField(FeeProfileSqlDao.cTier, u.toString)),
      subscriptionType = arg.subscription.map(s ⇒ CriteriaField(FeeProfileSqlDao.cSubscriptionType, s.underlying)),
      transactionType = arg.transactionType.map(t ⇒ CriteriaField(FeeProfileSqlDao.cTransactionType, t.underlying)),
      channel = arg.channel.map(c ⇒ CriteriaField(FeeProfileSqlDao.cChannel, c.underlying)),
      provider = arg.otherParty.map(otherParty ⇒ CriteriaField(Provider.cName, otherParty,
        if (arg.partialMatchFields.contains("other_party")) MatchTypes.Partial else MatchTypes.Exact)),
      instrument = arg.instrument.map(CriteriaField(FeeProfileSqlDao.cInstrument, _)),
      calculationMethod = arg.calculationMethod.map(cm ⇒ CriteriaField(FeeProfileSqlDao.cCalculationMethod, cm.underlying)),
      currencyCode = arg.currencyCode.map(c ⇒ CriteriaField(FeeProfileSqlDao.cCurrencyName, c.getCurrencyCode)),
      feeMethod = arg.feeMethod.map(fm ⇒ CriteriaField(FeeProfileSqlDao.cFeeMethod, fm.underlying)),
      taxIncluded = arg.taxInclusion.map(ti ⇒ CriteriaField(FeeProfileSqlDao.cTaxIncluded, ti.toString)),
      isDeleted = arg.isDeleted.map(d ⇒ CriteriaField(FeeProfileSqlDao.cDeletedAt, d)))
  }

  implicit class FeeProfileModelToUpdateDtoAdapter(val arg: FeeProfile) extends AnyVal {
    def asDaoUpdateDto(updatedBy: String, updatedAt: LocalDateTime, lastUpdatedAt: Option[LocalDateTime]) = {
      DaoFeeProfileToUpdate(
        calculationMethod = arg.calculationMethod.underlying,
        feeMethod = arg.feeMethod.underlying,
        taxIncluded = arg.taxInclusion.toString,
        maxFee = arg.maxFee,
        minFee = arg.minFee,
        feeAmount = arg.flatAmount,
        feeRatio = arg.percentageAmount,
        ranges = None,
        deletedAt = None,
        updatedAt = updatedAt,
        updatedBy = updatedBy,
        lastUpdatedAt = lastUpdatedAt)
    }
  }

}
