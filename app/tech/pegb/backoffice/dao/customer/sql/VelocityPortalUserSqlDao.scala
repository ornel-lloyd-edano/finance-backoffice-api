package tech.pegb.backoffice.dao.customer.sql

import java.time.LocalDateTime

import anorm.{Row, RowParser, SQL, SqlRequestError}
import cats.implicits._
import com.google.inject.Inject
import play.api.db.DBApi
import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.customer.abstraction.VelocityPortalUserDao
import tech.pegb.backoffice.dao.customer.dto.VelocityPortalUsersCriteria
import tech.pegb.backoffice.dao.customer.entity.VelocityPortalUser
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.util.Implicits._

import scala.util.Try
import tech.pegb.backoffice.dao.customer.entity.VelocityPortalUser._

class VelocityPortalUserSqlDao @Inject() (
    val dbApi: DBApi)
  extends VelocityPortalUserDao with SqlDao {

  import VelocityPortalUserSqlDao._

  def getVelocityPortalUsersByCriteria(
    criteria: VelocityPortalUsersCriteria,
    ordering: Option[OrderingSet],
    limit: Option[Int], offset: Option[Int]): DaoResponse[Seq[VelocityPortalUser]] = withConnection({ implicit connection ⇒

    val whereFilter = generateVelocityPortalUsersWhereFilter(criteria.some)

    val order = ordering.fold(s"ORDER BY $TableAlias.$cId")(_.toString)
    val pagination = SqlDao.getPagination(limit, offset)

    val columns = s"$TableAlias.*, ${UserSqlDao.TableAlias}.*"

    val velocityPortalUsersByCriteriaSql = SQL(s"""${baseFindByCriteria(columns, whereFilter)} $order $pagination""".stripMargin)

    velocityPortalUsersByCriteriaSql.as(velocityPortalUserParser.*)

  }, s"Error while retrieving velocity portal users by criteria: $criteria")

  def countVelocityPortalUserByCriteria(criteria: VelocityPortalUsersCriteria): DaoResponse[Int] = withConnection({ implicit connection ⇒
    val whereFilter = generateVelocityPortalUsersWhereFilter(criteria.some)
    val column = "COUNT(*) as n"
    val countByCriteriaSql = SQL(s"""${baseFindByCriteria(column, whereFilter)}""")

    countByCriteriaSql
      .as(countByCriteriaSql.defaultParser.singleOpt)
      .map(row ⇒ convertRowToCount(row)).getOrElse(0)

  }, s"Error while retrieving count by criteria:$criteria")

}

object VelocityPortalUserSqlDao {
  val TableName = "vp_users"
  val TableAlias = "vpu"

  private def baseFindByCriteria(selectColumns: String, filters: String): String = {
    s"""SELECT $selectColumns
       |FROM $TableName $TableAlias
       |JOIN ${UserSqlDao.TableName} ${UserSqlDao.TableAlias}
       |ON $TableAlias.$cUserId = ${UserSqlDao.TableAlias}.${UserSqlDao.id}
       |$filters""".stripMargin
  }

  private def generateVelocityPortalUsersWhereFilter(mayBeCriteria: Option[VelocityPortalUsersCriteria]): String = {
    import SqlDao._
    mayBeCriteria.map { criteria ⇒

      Seq(
        criteria.uuid.map(_.toSql(cUuid.some, TableAlias.some)),
        criteria.userId.map(_.toSql(cUserId.some, TableAlias.some)),
        criteria.userUUID.map(_.toSql(UserSqlDao.uuid.some, UserSqlDao.TableAlias.some)),
        criteria.name.map { cf ⇒
          s"(${cf.toSql(cName.some, TableAlias.some)} OR " +
            s"${cf.toSql(cMiddleName.some, TableAlias.some)} OR " +
            s"${cf.toSql(cSurname.some, TableAlias.some)})"
        },
        criteria.role.map(_.toSql(cRole.some, TableAlias.some)),
        criteria.username.map(_.toSql(cUsername.some, TableAlias.some)),
        criteria.email.map(_.toSql(cEmail.some, TableAlias.some)),
        criteria.msisdn.map(_.toSql(cMsisdn.some, TableAlias.some)),
        criteria.status.map(_.toSql(cStatus.some, TableAlias.some)),
        criteria.createdBy.map(_.toSql(cCreatedBy.some, TableAlias.some)),
        criteria.createdAt.map(_.toFormattedDateTime.toSql(cCreatedAt.some, TableAlias.some)),
        criteria.updatedBy.map(_.toSql(cUpdatedBy.some, TableAlias.some)),
        criteria.updatedAt.map(_.toFormattedDateTime.toSql(cUpdatedAt.some, TableAlias.some)),
        criteria.lastLoginAt.map(_.toFormattedDateTime.toSql(cLastLoginAt.some, TableAlias.some))).flatten.toSql
    }.getOrElse("")
  }

  def rowToVelocityPortalUser(row: Row): VelocityPortalUser = {
    VelocityPortalUser(
      id = row[Int](s"$TableName.$cId"),
      uuid = row[String](s"$TableName.$cUuid"),
      userId = row[Int](s"$TableName.$cUserId"),
      userUUID = row[String](s"${UserSqlDao.TableName}.${UserSqlDao.uuid}"),
      name = row[String](s"$TableName.$cName"),
      middleName = row[Option[String]](s"$TableName.$cMiddleName"),
      surname = row[String](s"$TableName.$cSurname"),
      msisdn = row[String](s"$TableName.$cMsisdn"),
      email = row[String](s"$TableName.$cEmail"),
      username = row[String](s"$TableName.$cUsername"),
      role = row[String](s"$TableName.$cRole"),
      status = row[String](s"$TableName.$cStatus"),
      lastLoginAt = row[Option[LocalDateTime]](s"$TableName.$cLastLoginAt"),
      createdBy = row[String](s"$TableName.$cCreatedBy"),
      createdAt = row[LocalDateTime](s"$TableName.$cCreatedAt"),
      updatedBy = row[Option[String]](s"$TableName.$cUpdatedBy"),
      updatedAt = row[Option[LocalDateTime]](s"$TableName.$cUpdatedAt"))
  }

  private val velocityPortalUserParser: RowParser[VelocityPortalUser] = row ⇒ {
    Try {
      rowToVelocityPortalUser(row)
    }.fold(
      exc ⇒ anorm.Error(SqlRequestError(exc)),
      anorm.Success(_))
  }

  private def convertRowToCount(row: Row): Int = row[Int]("n")

}
