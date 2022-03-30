package tech.pegb.backoffice.dao.provider.sql

import java.sql.Connection
import java.time.LocalDateTime

import anorm._
import com.google.inject.{Inject, Singleton}
import play.api.db.DBApi
import tech.pegb.backoffice.dao.customer.sql.UserSqlDao
import tech.pegb.backoffice.dao.{SelectionBehavior, SqlDao}
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.provider.abstraction.ProviderDao
import tech.pegb.backoffice.dao.provider.dto.ProviderCriteria
import tech.pegb.backoffice.dao.provider.entity.Provider
import tech.pegb.backoffice.dao.util.Implicits._
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

@Singleton
class ProviderSqlDao @Inject() (val dbApi: DBApi) extends ProviderDao with SqlDao with SelectionBehavior[Provider, ProviderCriteria] {
  import SqlDao._

  def get(id: Int)(implicit txnConn: Option[Connection] = None) =
    withConnection({ conn ⇒
      implicit val actualConn = txnConn.getOrElse(conn)
      val sql = SQL(
        s"""
           |SELECT * FROM ${ProviderSqlDao.TableName} WHERE ${Provider.cId} = {${Provider.cId}}
         """.stripMargin).on(Provider.cId → id)

      sql.executeQuery().as(rowParser.singleOpt)

    }, s"Unexpected error in get($id)")

  def getByCriteria(
    criteria: ProviderCriteria,
    ordering: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int])(implicit txnConn: Option[Connection] = None) =
    withConnection({ conn ⇒
      implicit val actualConn = txnConn.getOrElse(conn)

      val filter = generateWhereClause(criteria)
      val orderBy = generateOrderByClause(ordering)
      val pagination = getPagination(limit, offset)
      val rawSql =
        s"""
           |SELECT ${ProviderSqlDao.TableName}.*,
           |${UserSqlDao.TableName}.${UserSqlDao.uuid}
           |FROM ${ProviderSqlDao.TableName}
           |
           |LEFT JOIN ${UserSqlDao.TableName}
           |ON  ${ProviderSqlDao.TableName}.${Provider.cUsrId} = ${UserSqlDao.TableName}.${UserSqlDao.id}
           |
           |$filter
           |$orderBy
           |$pagination
        """.stripMargin
      logger.info("query = " + rawSql)
      val sql = SQL(rawSql)

      sql.executeQuery().as(rowParser.*)

    }, s"Unexpected error in getByCriteria")

  def parseRow(row: Row): Try[Provider] = Try {
    Provider(
      id = row[Int](Provider.cId),
      userId = row[Int](Provider.cUsrId),
      serviceId = row[Option[Int]](Provider.cServiceId),
      name = row[String](Provider.cName),
      transactionType = row[String](Provider.cTransactionType),
      icon = row[String](Provider.cIcon),
      label = row[String](Provider.cLabel),
      pgInstitutionId = row[Int](Provider.cPgInstitutionId),
      utilityPaymentType = row[Option[String]](Provider.cUtilPayType),
      utilityMinPaymentAmount = row[Option[BigDecimal]](Provider.cMinUtilPayType),
      utilityMaxPaymentAmount = row[Option[BigDecimal]](Provider.cMaxUtilPayType),
      isActive = row[Int](Provider.cIsActive).toBoolean,
      createdBy = row[String](Provider.cCreatedBy),
      createdAt = row[LocalDateTime](Provider.cCreatedAt),
      updatedBy = row[Option[String]](Provider.cUpdatedBy),
      updatedAt = row[Option[LocalDateTime]](Provider.cUpdatedAt))
  }

  def generateWhereClause(criteria: ProviderCriteria): String = {
    Seq(
      criteria.id.map(_.toSql()),
      criteria.userUuid.map(_.toSql(Some(UserSqlDao.uuid), Some(UserSqlDao.TableName))),
      criteria.serviceId.map(_.toSql()),
      criteria.name.map(_.toSql()),
      criteria.transactionType.map(_.toSql()),
      criteria.pgInstitutionId.map(_.toSql()),
      criteria.utilityPaymentType.map(_.toSql()),
      criteria.utilityMinPaymentAmount.map(_.toSql()),
      criteria.utilityMaxPaymentAmount.map(_.toSql()),
      criteria.isActive.map(_.toSql()),
      criteria.createdBy.map(_.toSql()),
      criteria.createdAt.map(_.toSql()),
      criteria.updatedBy.map(_.toSql()),
      criteria.updatedAt.map(_.toSql())).flatten.toSql
  }

  def generateOrderByClause(maybeOrderBy: Option[OrderingSet]): String = {
    val defaultOrderBy = s" ORDER BY ${Provider.cId} ASC"

    maybeOrderBy.map(orderBy ⇒ {
      orderBy.underlying.map(_.toString).toSql
    }).getOrElse(defaultOrderBy)
  }
}

object ProviderSqlDao {
  val TableName = "providers"
  val TableAlias = "pr"

}
