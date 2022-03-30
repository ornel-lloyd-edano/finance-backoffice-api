package tech.pegb.backoffice.dao.customer.abstraction

import java.sql.Connection

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao

import tech.pegb.backoffice.dao.customer.dto.{UserToInsert, UserToUpdate}
import tech.pegb.backoffice.dao.customer.entity.User
import tech.pegb.backoffice.dao.customer.dto.{GenericUserCriteria, UserToInsert, UserToUpdate}
import tech.pegb.backoffice.dao.customer.entity.{GenericUser, User}
import tech.pegb.backoffice.dao.customer.sql.UserSqlDao
import tech.pegb.backoffice.dao.model.OrderingSet

@ImplementedBy(classOf[UserSqlDao])
trait UserDao extends Dao {
  def insertUser(user: UserToInsert): DaoResponse[(Int, String)]

  def getUser(uuid: String): DaoResponse[Option[User]]

  def updateUser(uuid: String, user: UserToUpdate)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Option[User]]

  def getInternalUserId(uuid: String): DaoResponse[Option[Int]]

  def getUUIDByInternalUserId(userId: Int): DaoResponse[Option[String]]

  def getUserByUserId(userId: Int): DaoResponse[Option[User]]

  def getUserByCriteria(
    criteria: GenericUserCriteria,
    orderBy: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int]): DaoResponse[Seq[GenericUser]]

  def countUserByCriteria(criteria: GenericUserCriteria): DaoResponse[Int]

}
