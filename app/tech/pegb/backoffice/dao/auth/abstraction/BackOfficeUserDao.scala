package tech.pegb.backoffice.dao.auth.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.auth.dto._
import tech.pegb.backoffice.dao.auth.entity.BackOfficeUser
import tech.pegb.backoffice.dao.auth.sql.BackOfficeUserSqlDao
import tech.pegb.backoffice.dao.model.OrderingSet

@ImplementedBy(classOf[BackOfficeUserSqlDao])
trait BackOfficeUserDao extends Dao {
  def createBackOfficeUser(dto: BackOfficeUserToInsert): DaoResponse[BackOfficeUser]

  def updateBackOfficeUser(id: String, dto: BackOfficeUserToUpdate): DaoResponse[Option[BackOfficeUser]]

  def countBackOfficeUsersByCriteria(criteria: Option[BackOfficeUserCriteria]): DaoResponse[Int]

  def getBackOfficeUsersByCriteria(
    criteria: Option[BackOfficeUserCriteria],
    orderBy: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int]): DaoResponse[Seq[BackOfficeUser]]

  def updateLastLoginTimestamp(id: String): DaoResponse[Option[BackOfficeUser]]
}
