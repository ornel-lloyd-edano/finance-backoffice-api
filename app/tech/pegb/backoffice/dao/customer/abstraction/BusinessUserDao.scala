package tech.pegb.backoffice.dao.customer.abstraction

import java.sql.Connection

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.customer.dto._
import tech.pegb.backoffice.dao.customer.entity._
import tech.pegb.backoffice.dao.customer.sql.BusinessUserSqlDao
import tech.pegb.backoffice.dao.model.OrderingSet

@ImplementedBy(classOf[BusinessUserSqlDao])
trait BusinessUserDao extends Dao {

  def getBusinessUser(uuid: String): DaoResponse[Option[BusinessUser]]

  def getBusinessUserByCriteria(criteria: BusinessUserCriteria, ordering: Option[OrderingSet], limit: Option[Int], offset: Option[Int]): DaoResponse[Seq[BusinessUser]]

  def getUserAndBusinessUserJoinByCriteria(criteria: UserAndBusinessUserJoinGetCriteria, limit: Option[Int], offset: Option[Int]): DaoResponse[Seq[UserAndBusinessUserJoin]]

  def countTotalUserAndBusinessUserJoinByCriteria(criteria: UserAndBusinessUserJoinGetCriteria): DaoResponse[Int]

  def updateBusinessUser(uuid: String, businessUser: BusinessUserToUpdate)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Option[BusinessUser]]

  def insertBusinessUser(user: UserToInsert, businessUser: BusinessUserToInsert): DaoResponse[UserAndBusinessUserJoin]

  def deleteBusinessUser(uuid: String)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Unit]

}
