package tech.pegb.backoffice.mapping.domain.dao.limit

import java.time.LocalDateTime

import cats.implicits._
import tech.pegb.backoffice.dao.limit.dto
import tech.pegb.backoffice.dao.limit.sql._
import tech.pegb.backoffice.util.Constants._
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes}
import tech.pegb.backoffice.dao.limit.dto.{LimitProfileToInsert, LimitProfileToUpdate ⇒ DaoLimitProfileToUpdate}
import tech.pegb.backoffice.dao.provider.entity.Provider
import tech.pegb.backoffice.domain.limit.dto.{LimitProfileCriteria, LimitProfileToCreate, LimitProfileToUpdate}
import tech.pegb.backoffice.domain.limit.model.{LimitProfile, LimitType}

object Implicits {

  implicit class LimitCriteriaAdapter(val arg: LimitProfileCriteria) extends AnyVal {
    def asDao = dto.LimitProfileCriteria(
      uuid = arg.uuid.map(id ⇒
        CriteriaField(s"${LimitProfileSqlDao.cUuid}", id,
          if (arg.partialMatchFields.contains("uuid")) MatchTypes.Partial else MatchTypes.Exact)),
      limitType = arg.limitType.map(lt ⇒ CriteriaField(s"${LimitProfileSqlDao.cLimitType}", lt.underlying)),
      userType = arg.userType.map(ut ⇒ CriteriaField(s"${LimitProfileSqlDao.cUserType}", ut.underlying)),
      tier = arg.tier.map(t ⇒ CriteriaField(s"${LimitProfileSqlDao.cTier}", t.underlying)),
      subscription = arg.subscription.map(s ⇒ CriteriaField(s"${LimitProfileSqlDao.cSubscription}", s.underlying)),
      transactionType = arg.transactionType.map(t ⇒ CriteriaField(s"${LimitProfileSqlDao.cTransactionType}", t.underlying)),
      channel = arg.channel.map(c ⇒ CriteriaField(s"${LimitProfileSqlDao.cChannel}", c.underlying)),
      provider = arg.otherParty.map(o ⇒
        CriteriaField(Provider.cName, o,
          if (arg.partialMatchFields.contains("other_party")) MatchTypes.Partial else MatchTypes.Exact)),
      instrument = arg.instrument.map(i ⇒ CriteriaField(s"${LimitProfileSqlDao.cInstrument}", i)),
      interval = arg.interval.map(i ⇒ CriteriaField(s"${LimitProfileSqlDao.cInterval}", i.toString)),
      currencyCode = arg.currencyCode.map(c ⇒ CriteriaField(s"${LimitProfileSqlDao.cCurrencyName}", c.getCurrencyCode)),
      isDeleted = arg.isDeleted.map(d ⇒ CriteriaField(s"${LimitProfileSqlDao.cDeletedAt}", d)))
  }

  implicit class UpdateDtoAdapter(val arg: LimitProfileToUpdate) extends AnyVal {
    def asDao(limitType: LimitType): DaoLimitProfileToUpdate = {
      val maxAmount = if (limitType == LimitType.BalanceBased) arg.maxBalanceAmount
      else arg.maxAmount

      DaoLimitProfileToUpdate(
        maxIntervalAmount = arg.maxIntervalAmount,
        maxAmount = maxAmount,
        minAmount = arg.minAmount,
        maxCount = arg.maxCount,
        updatedBy = arg.updatedBy,
        updatedAt = arg.updatedAt,
        lastUpdatedAt = arg.lastUpdatedAt)
    }
  }

  implicit class DeleteDtoAdapter(val arg: (String, LocalDateTime, Option[LocalDateTime])) extends AnyVal {
    def asDao = DaoLimitProfileToUpdate(
      deletedAt = Some(arg._2),
      updatedBy = arg._1,
      updatedAt = arg._2,
      lastUpdatedAt = arg._3)
  }

  implicit class LimitCriteriaByCreateAdapter(val arg: LimitProfileToCreate) extends AnyVal {

    def asDaoCriteria = dto.LimitProfileCriteria(
      uuid = None,
      limitType = CriteriaField(s"${LimitProfileSqlDao.cLimitType}", arg.limitType.underlying).some,
      userType = CriteriaField(s"${LimitProfileSqlDao.cUserType}", arg.userType.underlying).some,
      tier = CriteriaField(s"${LimitProfileSqlDao.cTier}", arg.tier.underlying).some,
      subscription = CriteriaField(s"${LimitProfileSqlDao.cSubscription}", arg.subscription.underlying).some,
      currencyCode = CriteriaField(s"${LimitProfileSqlDao.cCurrencyName}", arg.currencyCode.getCurrencyCode).some,
      channel = arg.channel.map(c ⇒ CriteriaField(s"${LimitProfileSqlDao.cChannel}", c.underlying))
        .orElse(CriteriaField(s"${LimitProfileSqlDao.cChannel}", empty, MatchTypes.IsNull).some),
      transactionType = arg.transactionType.map(t ⇒ CriteriaField(s"${LimitProfileSqlDao.cTransactionType}", t.underlying))
        .orElse(CriteriaField(s"${LimitProfileSqlDao.cTransactionType}", empty, MatchTypes.IsNull).some),
      instrument = arg.instrument.map(CriteriaField(s"${LimitProfileSqlDao.cInstrument}", _))
        .orElse(CriteriaField(s"${LimitProfileSqlDao.cInstrument}", empty, MatchTypes.IsNull).some),
      interval = arg.interval.map(i ⇒ CriteriaField(s"${LimitProfileSqlDao.cInterval}", i.underlying))
        .orElse(CriteriaField(s"${LimitProfileSqlDao.cInterval}", empty, MatchTypes.IsNull).some),
      provider = arg.otherParty.map(o ⇒ CriteriaField(Provider.cName, o, MatchTypes.Exact))
        .orElse(CriteriaField(Provider.cName, empty, MatchTypes.IsNull).some),
      isDeleted = CriteriaField(s"${LimitProfileSqlDao.cDeletedAt}", false).some)

    def asDaoCriteriaWithoutInterval: dto.LimitProfileCriteria = asDaoCriteria.copy(interval = None)

  }

  implicit class LimitCriteriaByLimitProfileAdapter(val arg: LimitProfile) extends AnyVal {
    def asDaoCriteriaWithoutInterval = dto.LimitProfileCriteria(
      uuid = None,
      limitType = CriteriaField(s"${LimitProfileSqlDao.cLimitType}", arg.limitType.underlying).some,
      userType = CriteriaField(s"${LimitProfileSqlDao.cUserType}", arg.userType.underlying).some,
      tier = CriteriaField(s"${LimitProfileSqlDao.cTier}", arg.tier.underlying).some,
      subscription = CriteriaField(s"${LimitProfileSqlDao.cSubscription}", arg.subscription.underlying).some,
      currencyCode = CriteriaField(s"${LimitProfileSqlDao.cCurrencyName}", arg.currencyCode.getCurrencyCode).some,
      channel = arg.channel.map(c ⇒ CriteriaField(s"${LimitProfileSqlDao.cChannel}", c.underlying))
        .orElse(CriteriaField(s"${LimitProfileSqlDao.cChannel}", empty, MatchTypes.IsNull).some),
      transactionType = arg.transactionType.map(t ⇒ CriteriaField(s"${LimitProfileSqlDao.cTransactionType}", t.underlying))
        .orElse(CriteriaField(s"${LimitProfileSqlDao.cTransactionType}", empty, MatchTypes.IsNull).some),
      instrument = arg.instrument.map(CriteriaField(s"${LimitProfileSqlDao.cInstrument}", _))
        .orElse(CriteriaField(s"${LimitProfileSqlDao.cInstrument}", empty, MatchTypes.IsNull).some),
      provider = arg.otherParty.map(o ⇒ CriteriaField(Provider.cName, o, MatchTypes.Exact))
        .orElse(CriteriaField(Provider.cName, empty, MatchTypes.IsNull).some),
      isDeleted = CriteriaField(s"${LimitProfileSqlDao.cDeletedAt}", false).some)
  }

  implicit class LimitCreateMapper(val arg: LimitProfileToCreate) extends AnyVal {
    def asDao(currencyId: Int): LimitProfileToInsert = {
      val maxAmount = if (LimitType.isBalanceBased(arg.limitType)) arg.maxBalance else arg.maxAmount

      LimitProfileToInsert(
        limitType = arg.limitType.underlying,
        userType = Some(arg.userType.underlying),
        tier = Some(arg.tier.underlying),
        subscription = Some(arg.subscription.underlying),
        transactionType = arg.transactionType.map(_.underlying),
        channel = arg.channel.map(_.underlying),
        provider = arg.otherParty,
        instrument = arg.instrument,
        interval = arg.interval.map(_.underlying),
        maxIntervalAmount = arg.maxIntervalAmount,
        maxAmount = maxAmount,
        minAmount = arg.minAmount,
        maxCount = arg.maxCount,
        currencyId = currencyId,
        createdBy = arg.createdBy,
        createdAt = arg.createdAt)
    }
  }

}
