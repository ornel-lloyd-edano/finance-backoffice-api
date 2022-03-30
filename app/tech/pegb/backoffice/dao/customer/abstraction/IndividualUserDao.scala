package tech.pegb.backoffice.dao.customer.abstraction

import java.sql.Connection

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.customer.dto.{CustomerAggregation, IndividualUserCriteria, IndividualUserToUpdate}
import tech.pegb.backoffice.dao.customer.entity._
import tech.pegb.backoffice.dao.customer.sql.IndividualUserSqlDao
import tech.pegb.backoffice.dao.model.{GroupingField, Ordering}
import tech.pegb.backoffice.dao.transaction.dto.TransactionCriteria

@ImplementedBy(classOf[IndividualUserSqlDao])
trait IndividualUserDao extends Dao {

  def getIndividualUser(uuid: String): DaoResponse[Option[IndividualUser]]

  def getIndividualUsersByCriteria(
    individualUserCriteria: IndividualUserCriteria,
    orderBy: Seq[Ordering],
    limit: Option[Int],
    offset: Option[Int]): DaoResponse[Seq[IndividualUser]]

  def countIndividualUserByCriteria(individualUserCriteria: IndividualUserCriteria): DaoResponse[Int]

  def updateIndividualUser(uuid: String, individualUser: IndividualUserToUpdate)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Option[IndividualUser]]

  def updateStatusByMsisdn(msisdn: String, status: String): DaoResponse[Option[IndividualUser]]

  def aggregateCustomersByCriteriaAndPivots(
    criteria: IndividualUserCriteria,
    trxCriteria: TransactionCriteria,
    grouping: Seq[GroupingField]): DaoResponse[Seq[CustomerAggregation]]
}
