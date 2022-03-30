package tech.pegb.backoffice.dao.customer.sql

import java.sql.Connection
import java.time.LocalDateTime

import anorm.{Row, SQL}
import com.google.inject.Inject
import play.api.db.DBApi

import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.account.entity.Account
import tech.pegb.backoffice.dao.customer.abstraction.CustomerAttributesDao
import tech.pegb.backoffice.dao.customer.entity.CustomerAttributes._
import tech.pegb.backoffice.dao.customer.entity._

import scala.util.Try

//TODO cache Attribute data
class CustomerAttributesSqlDao @Inject() (val dbApi: DBApi) extends CustomerAttributesDao with SqlDao {

  import CustomerAttributesSqlDao._

  def getUserAndBusinessUserJoinWithMainAccountJoin(id: String): DaoResponse[UserAndBusinessUserJoinWithAccountJoin] = ???

  def getUserAndBusinessUserJoinWithAllAccountsJoin(id: String): DaoResponse[(UserAndBusinessUserJoinWithAccountJoin, Set[Account])] = ???

  def getCustomerSegments: DaoResponse[Set[CustomerSegment]] = ???

  def getCustomerSubscriptions: DaoResponse[Set[CustomerSubscription]] = ???

  def getCustomerTiers: DaoResponse[Set[CustomerTier]] = ???

  def getCustomerStatuses: DaoResponse[Set[CustomerStatus]] = withConnection({
    implicit connection: Connection ⇒ findAllStatusesInternal
  }, s"failed to get customer status from ${this.getClass}.getCustomerStatuses")

  def getCustomerTypes: DaoResponse[Set[CustomerType]] = ???

  def getBusinessUserTypes: DaoResponse[Set[BusinessUserType]] = ???
}

object CustomerAttributesSqlDao {

  private[dao] final val UserStatusTable = "user_status"

  private[dao] final val Fields: Seq[String] =
    Seq("id", "status_name", "description", "created_at", "updated_at", "is_active")

  private[dao] final val insertSql = SQL(
    s"""INSERT INTO $UserStatusTable $Fields VALUES ({"id"}, {"status_name"}, {"description"}, {"created_at"}, {"updated_at"},{"is_active"});""".stripMargin)
  private final val selectAllUserStatusRawSql = s"SELECT * FROM $UserStatusTable"
  private final val findAllStatusSql = SQL(selectAllUserStatusRawSql)

  private def convertRowToUserStatus(row: Row) = Try {
    CustomerStatus(
      statusName = row[String]("status_name"),
      description = row[Option[String]]("description"),
      isActive = row[Boolean]("is_active"),
      createdAt = row[LocalDateTime]("created_at"),
      createdBy = row[String]("created_by"),
      updatedAt = row[Option[LocalDateTime]]("updated_at"),
      updatedBy = row[Option[String]]("updated_by"))
  }

  def findAllStatusesInternal(implicit connection: Connection): Set[CustomerStatus] = {
    findAllStatusSql.as(findAllStatusSql.defaultParser.*)
      .map(row ⇒ convertRowToUserStatus(row).get).toSet
  }

}
