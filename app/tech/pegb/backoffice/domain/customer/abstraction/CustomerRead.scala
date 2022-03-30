package tech.pegb.backoffice.domain.customer.abstraction

import java.time.LocalDate
import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.model.GroupingField
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.customer.dto.{GenericUserCriteria, IndividualUserCriteria}
import tech.pegb.backoffice.domain.customer.implementation.CustomerReadService
import tech.pegb.backoffice.domain.customer.model.BusinessUsers._
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes._
import tech.pegb.backoffice.domain.customer.model.GenericUser
import tech.pegb.backoffice.domain.customer.model.IndividualUsers._
import tech.pegb.backoffice.domain.model.{CustomerAggregation, Ordering}
import tech.pegb.backoffice.domain.transaction.dto.TransactionCriteria

import scala.concurrent.Future

@ImplementedBy(classOf[CustomerReadService])
trait CustomerRead {
  type FSR[T] = Future[ServiceResponse[T]]

  def getIndividualUser(customerId: UUID): Future[ServiceResponse[IndividualUser]]

  def findIndividualUsersByCriteria(
    individualUserCriteria: IndividualUserCriteria,
    orderBy: Seq[Ordering],
    limit: Option[Int],
    offset: Option[Int]): FSR[Seq[IndividualUser]]

  def countIndividualUsersByCriteria(individualUserCriteria: IndividualUserCriteria): Future[ServiceResponse[Int]]

  def getBusinessUserSubmittedActivationRequirements(id: UUID): Future[ServiceResponse[Set[ActivationRequirement]]]

  def getActivatedBusinessUserById(id: UUID): Future[ServiceResponse[ActivatedBusinessUser]]

  def getWaitingForActivationBusinessUserById(id: UUID): Future[ServiceResponse[RegisteredButNotActivatedBusinessUser]]

  def getActivatedBusinessUserByName(username: LoginUsername): Future[ServiceResponse[ActivatedBusinessUser]]

  def getWaitingForActivationBusinessUserByName(username: LoginUsername): Future[ServiceResponse[RegisteredButNotActivatedBusinessUser]]

  def findWaitingForActivationBusinessUsersByCriteria(createdDateFrom: Option[LocalDate], createdDateTo: Option[LocalDate],
    createdByBackofficeUserId: Option[UUID], company: Option[NameAttribute],
    limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[RegisteredButNotActivatedBusinessUser]]]

  def countWaitingForActivationBusinessUsersByCriteria(createdDateFrom: Option[LocalDate], createdDateTo: Option[LocalDate],
    createdByBackofficeUserId: Option[UUID], company: Option[NameAttribute]): Future[ServiceResponse[Int]]

  def findActivatedBusinessUsersByCriteria(createdDateFrom: Option[LocalDate], createdDateTo: Option[LocalDate],
    createdByBackofficeUserId: Option[UUID], company: Option[NameAttribute],
    tier: Option[CustomerTier], segment: Option[CustomerSegment], subscription: Option[CustomerSubscription],
    limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[ActivatedBusinessUser]]]

  def countActivatedBusinessUsersByCriteria(createdDateFrom: Option[LocalDate], createdDateTo: Option[LocalDate],
    createdByBackofficeUserId: Option[UUID], company: Option[NameAttribute],
    tier: Option[CustomerTier], segment: Option[CustomerSegment], subscription: Option[CustomerSubscription]): Future[ServiceResponse[Int]]

  def aggregateCustomersByCriteriaAndPivots(
    criteria: IndividualUserCriteria,
    trxCriteria: TransactionCriteria,
    groupings: Seq[GroupingField]): Future[ServiceResponse[Seq[CustomerAggregation]]]

  def getUserByCriteria(
    criteria: GenericUserCriteria,
    orderBy: Seq[Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[GenericUser]]]

  def countUserByCriteria(criteria: GenericUserCriteria): Future[ServiceResponse[Int]]

  def getUser(customerId: UUID): Future[ServiceResponse[GenericUser]]

  def validateUser(userId: UUID, customerType: CustomerType): Future[ServiceResponse[GenericUser]]
}
