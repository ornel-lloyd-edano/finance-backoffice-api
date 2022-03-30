package tech.pegb.backoffice.domain.customer.implementation

import java.time.LocalDateTime
import java.util.UUID

import cats.data._
import cats.implicits._
import com.google.inject.Inject
import tech.pegb.backoffice.core.integration.abstraction.SavingOptionsCoreApiClient
import tech.pegb.backoffice.dao.account.abstraction.AccountDao
import tech.pegb.backoffice.dao.customer.abstraction.IndividualUserDao
import tech.pegb.backoffice.dao.savings.abstraction.{AutoDeductSavingsDao, RoundUpSavingsDao, SavingGoalsDao}
import tech.pegb.backoffice.dao.savings.entity.{AutoDeductSaving, RoundUpSaving, SavingGoal}
import tech.pegb.backoffice.domain.customer.abstraction
import tech.pegb.backoffice.domain.customer.abstraction.CustomerRead
import tech.pegb.backoffice.domain.customer.dto._
import tech.pegb.backoffice.domain.customer.model.{GenericSavingOption, SavingOptionTypes}
import tech.pegb.backoffice.domain.{ServiceError}
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.customer.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.saving.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.backoffice.util.Implicits._

import scala.concurrent.Future

class SavingOptionsMgmtService @Inject() (
    executionContext: WithExecutionContexts,
    config: AppConfig,
    customerReadService: CustomerRead,
    savingGoalsDao: SavingGoalsDao,
    autoDeductSavingDao: AutoDeductSavingsDao,
    roundUpSavingsDao: RoundUpSavingsDao,
    individualUserDao: IndividualUserDao,
    accountDao: AccountDao,
    savingOptionsApiClient: SavingOptionsCoreApiClient) extends abstraction.SavingOptionsMgmtService {

  implicit val ec = executionContext.blockingIoOperations
  implicit val localDateOrdering: Ordering[LocalDateTime] = _ compareTo _

  def getCustomerSavingOptions(
    customerId: UUID,
    criteria: Option[SavingOptionCriteria]): Future[ServiceResponse[Seq[GenericSavingOption]]] = {

    (for {
      _ ← EitherT(customerReadService.getIndividualUser(customerId))
      savingGoals ← EitherT.fromEither[Future](savingGoalsDao.getSavingOptionsByCriteria(
        criteria.map(_.asSavingGoalCriteriaDao))
        .asServiceResponse)

      autoDeductSavings ← EitherT.fromEither[Future](
        autoDeductSavingDao.getSavingOptionsByCriteria(
          criteria.map(_.asAutoDeductCriteriaDao))
          .asServiceResponse)

      roundUpSavings ← EitherT.fromEither[Future](
        roundUpSavingsDao.getSavingOptionsByCriteria(
          criteria.map(_.asRoundUpCriteriaDao)).asServiceResponse)
    } yield {

      val savingGoalGenericSaving = savingGoals.flatMap(goal ⇒ goal.asGenericSavingOption(
        customerId = customerId,
        savingOptionType = SavingOptionTypes.SavingGoals,
        savingGoalName = goal.name.some,
        goalAmount = goal.goalAmount.some,
        reason = goal.reason,
        dueDate = goal.dueDate.some).toOption)
      val autoDeductGenericSavings = autoDeductSavings.flatMap(savingOption ⇒ savingOption.asGenericSavingOption(
        customerId = customerId,
        savingOptionType = SavingOptionTypes.AutoDeduct,
        goalAmount = none,
        reason = none,
        dueDate = none).toOption)
      val roundUpGenericSaving = roundUpSavings.flatMap(savingOption ⇒ savingOption.asGenericSavingOption(
        customerId = customerId,
        savingOptionType = SavingOptionTypes.RoundUp,
        goalAmount = none,
        reason = none,
        dueDate = none).toOption)

      savingGoalGenericSaving ++ autoDeductGenericSavings ++ roundUpGenericSaving
    }).value
  }

  def getLatestVersion(savingOptions: Seq[GenericSavingOption]): Future[ServiceResponse[Option[String]]] =
    Future.successful(Right(savingOptions.sortWith((a, b) ⇒ Ordering[LocalDateTime].lt(a.updatedAt, b.updatedAt))
      .lastOption.map(_.updatedAt.toString)))

  def deactivateSavingOption(
    id: UUID,
    customerId: UUID,
    updatedBy: String,
    updatedAt: LocalDateTime,
    lastUpdatedAt: Option[LocalDateTime])(implicit requestId: UUID): Future[ServiceResponse[GenericSavingOption]] = {
    (for {
      _ ← EitherT(customerReadService.getIndividualUser(customerId))
      savingGoals ← EitherT.fromEither[Future](savingGoalsDao.getSavingOptionsByCriteria(
        SavingOptionCriteria(
          uuid = id.some,
          userUuid = customerId.some, isActive = true.some).asSavingGoalCriteriaDao.some).asServiceResponse)
      autoDeductSavings ← EitherT.fromEither[Future](autoDeductSavingDao.getSavingOptionsByCriteria(
        SavingOptionCriteria(
          uuid = id.some,
          userUuid = customerId.some, isActive = true.some).asAutoDeductCriteriaDao.some).asServiceResponse)
      roundUpSavings ← EitherT.fromEither[Future](roundUpSavingsDao.getSavingOptionsByCriteria(
        SavingOptionCriteria(
          uuid = id.some,
          userUuid = customerId.some, isActive = true.some).asRoundUpCriteriaDao.some).asServiceResponse)
      goal ← EitherT(deactivateViaCore(id, customerId, savingGoals.headOption, autoDeductSavings.headOption, roundUpSavings.headOption))
    } yield {
      goal
    }).value
  }

  private def deactivateViaCore(
    id: UUID,
    customerId: UUID,
    savingGoalOption: Option[SavingGoal],
    autoDeductOption: Option[AutoDeductSaving],
    roundUpOption: Option[RoundUpSaving])(implicit requestId: UUID): Future[ServiceResponse[GenericSavingOption]] = {

    (savingGoalOption, autoDeductOption, roundUpOption) match {
      case (Some(savingGoal), None, None) ⇒
        (for {
          _ ← EitherT(savingOptionsApiClient.deactivateSavingGoal(savingGoal.id))
          savingGoals ← EitherT.fromEither[Future](savingGoalsDao.getSavingOptionsByCriteria(
            SavingOptionCriteria(
              uuid = id.some,
              userUuid = customerId.some, isActive = false.some).asSavingGoalCriteriaDao.some).asServiceResponse)
          goal ← EitherT.fromOption[Future](savingGoals.headOption, ServiceError.notFoundError(s"Deactivated Saving Goal $id for customer $customerId not found", requestId.toOption))
          genericGoal ← EitherT.fromEither[Future](
            goal.asGenericSavingOption(
              customerId = customerId,
              savingOptionType = SavingOptionTypes.SavingGoals,
              savingGoalName = goal.name.some,
              goalAmount = goal.goalAmount.some,
              reason = goal.reason,
              dueDate = goal.dueDate.some).toEither.leftMap(ex ⇒ ServiceError.dtoMappingError(s"Failed to create GenericSavingOption - error: ${ex.getMessage}", requestId.toOption)))
        } yield {
          genericGoal
        }).value
      case (None, Some(autoDeduct), None) ⇒
        (for {
          _ ← EitherT(savingOptionsApiClient.deactivateAutoDeductSaving(autoDeduct.id))
          autoDeductSavings ← EitherT.fromEither[Future](autoDeductSavingDao.getSavingOptionsByCriteria(
            SavingOptionCriteria(
              uuid = id.some,
              userUuid = customerId.some, isActive = false.some).asAutoDeductCriteriaDao.some).asServiceResponse)
          goal ← EitherT.fromOption[Future](autoDeductSavings.headOption, ServiceError.notFoundError(s"Deactivated Auto Deduct Saving $id for customer $customerId not found", requestId.toOption))
          genericGoal ← EitherT.fromEither[Future](
            goal.asGenericSavingOption(
              customerId = customerId,
              savingOptionType = SavingOptionTypes.AutoDeduct,
              goalAmount = none,
              reason = none,
              dueDate = none).toEither.leftMap(ex ⇒ ServiceError.dtoMappingError(s"Failed to create GenericSavingOption - error: ${ex.getMessage}", requestId.toOption)))
        } yield {
          genericGoal
        }).value
      case (None, None, Some(roundUp)) ⇒
        (for {
          _ ← EitherT(savingOptionsApiClient.deactivateRoundUpSaving(roundUp.id))
          roundUpSavings ← EitherT.fromEither[Future](roundUpSavingsDao.getSavingOptionsByCriteria(
            SavingOptionCriteria(
              uuid = id.some,
              userUuid = customerId.some, isActive = false.some).asRoundUpCriteriaDao.some).asServiceResponse)
          goal ← EitherT.fromOption[Future](roundUpSavings.headOption, ServiceError.notFoundError(s"Deactivated Round Up Saving $id for customer $customerId not found", requestId.toOption))
          genericGoal ← EitherT.fromEither[Future](
            goal.asGenericSavingOption(
              customerId = customerId,
              savingOptionType = SavingOptionTypes.RoundUp,
              goalAmount = none,
              reason = none,
              dueDate = none).toEither.leftMap(ex ⇒ ServiceError.dtoMappingError(s"Failed to create GenericSavingOption - error: ${ex.getMessage}", requestId.toOption)))
        } yield {
          genericGoal
        }).value
      case _ ⇒
        Future.successful(ServiceError.validationError(s"No active saving option matches id: $id for customer $customerId", requestId.toOption).asLeft[GenericSavingOption])
    }
  }
}
