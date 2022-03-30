package tech.pegb.backoffice.domain.graphql

import java.time.LocalDate
import java.util.UUID

import com.google.inject.Inject
import tech.pegb.backoffice.dao.model.GroupingField
import tech.pegb.backoffice.domain.customer.abstraction.CustomerRead
import tech.pegb.backoffice.domain.customer.dto.IndividualUserCriteria
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes._
import tech.pegb.backoffice.domain.customer.model.IndividualUsers.{IndividualUser, IndividualUserType}
import tech.pegb.backoffice.domain.model.Ordering._
import tech.pegb.backoffice.domain.model.{CustomerAggregation}
import tech.pegb.backoffice.util.{Logging, WithExecutionContexts}
import tech.pegb.backoffice.util.Implicits._

import scala.concurrent.Future

case class CustomerQueryArgs(
    limit: Option[Int] = None,
    offset: Option[Int] = None,
    msisdn: Option[MsisdnLike] = None,
    tier: Option[CustomerTier] = None,
    segment: Option[CustomerSegment] = None,
    subscription: Option[CustomerSubscription] = None,
    status: Option[CustomerStatus] = None,
    individualUserType: Option[IndividualUserType] = None,
    name: Option[String] = None,
    fullName: Option[String] = None,
    gender: Option[String] = None,
    personId: Option[String] = None,
    documentNumber: Option[String] = None,
    documentType: Option[String] = None,
    birthDate: Option[LocalDate] = None,
    birthPlace: Option[String] = None,
    nationality: Option[String] = None,
    dateFrom: Option[String] = None,
    dateTo: Option[String] = None)

class CustomersRepo @Inject() (
    customerRead: CustomerRead,
    executionContexts: WithExecutionContexts) extends Logging {
  val DefaultOrdering = "-created_at,sequence"

  implicit val ec = executionContexts.genericOperations

  def customer(id: String): Future[Option[IndividualUser]] =
    customerRead.getIndividualUser(UUID.fromString(id)).map(_.toOption)

  def customers(args: CustomerQueryArgs): Future[Seq[IndividualUser]] = {
    val criteria = buildCriteria(args)
    val ordering = DefaultOrdering.asDomain
    customerRead
      .findIndividualUsersByCriteria(criteria, ordering, limit = args.limit, offset = args.offset)
      .map { serviceResponse ⇒ serviceResponse.fold(_ ⇒ List.empty, transactionsList ⇒ transactionsList) }
  }

  def aggregate(args: CustomerQueryArgs, trxArgs: TransactionQueryArgs, groupings: Seq[GroupingField]): Future[Seq[CustomerAggregation]] = {
    customerRead.aggregateCustomersByCriteriaAndPivots(
      buildCriteria(args),
      TransactionRepo.buildCriteria(trxArgs),
      groupings).map {
        serviceResponse ⇒
          serviceResponse.fold(
            error ⇒ {
              logger.error(s"Error in aggregation. $error")
              Seq()
            },
            aggregates ⇒ aggregates)
      }
  }

  private def buildCriteria(args: CustomerQueryArgs): IndividualUserCriteria = {
    IndividualUserCriteria(
      msisdnLike = args.msisdn,
      tier = args.tier,
      segment = args.segment,
      subscription = args.subscription,
      status = args.status,
      individualUserType = args.individualUserType,
      name = args.name.map(a ⇒ NameAttribute(a.sanitize)),
      fullName = args.fullName.map(a ⇒ NameAttribute(a.sanitize)),
      gender = args.gender.map(a ⇒ NameAttribute(a.sanitize)),
      documentType = args.documentType.map(a ⇒ NameAttribute(a.sanitize)),
      birthDate = args.birthDate,
      birthPlace = args.birthPlace.map(a ⇒ NameAttribute(a.sanitize)),
      nationality = args.nationality.map(a ⇒ NameAttribute(a.sanitize)),
      createdDateFrom = args.dateFrom.map(LocalDate.parse(_)),
      createdDateTo = args.dateTo.map(LocalDate.parse(_)),
    )
  }

}
