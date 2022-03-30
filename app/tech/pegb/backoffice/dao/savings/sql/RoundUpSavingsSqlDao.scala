package tech.pegb.backoffice.dao.savings.sql

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import anorm.{Row, SQL}
import com.google.inject.Inject
import play.api.db.DBApi

import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.SqlDao.getPagination
import tech.pegb.backoffice.dao.account.sql.AccountSqlDao
import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao
import tech.pegb.backoffice.dao.customer.sql.UserSqlDao
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.savings.abstraction.RoundUpSavingsDao
import tech.pegb.backoffice.dao.savings.dto.RoundUpSavingsCriteria
import tech.pegb.backoffice.dao.savings.entity.RoundUpSaving
import tech.pegb.backoffice.dao.util.Implicits._
import tech.pegb.backoffice.util.AppConfig
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

class RoundUpSavingsSqlDao @Inject() (config: AppConfig, val dbApi: DBApi) extends RoundUpSavingsDao with SqlDao {
  import RoundUpSavingsSqlDao._

  def getSavingOptionsByCriteria(
    filter: Option[RoundUpSavingsCriteria],
    ordering: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int]): DaoResponse[List[RoundUpSaving]] = {

    withConnection({ implicit connection ⇒

      val rawSelectQuery =
        s"""
           |SELECT $TableAlias.*,
           |${UserSqlDao.TableAlias}.${UserSqlDao.uuid},
           |${AccountSqlDao.TableAlias}.${AccountSqlDao.cUuid},
           |${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cName}
           |$commonJoin
     """.stripMargin

      val where = generateWhere(filter)

      val ord = ordering.map(_.toString).getOrElse(
        s"ORDER BY ${TableAlias}.${cId} ASC")

      val pagination = getPagination(limit, offset)

      val query = SQL(
        s"""
           |$rawSelectQuery
           |$where
           |$ord $pagination
         """.stripMargin)
      logger.info(s"getSavingOptionsByCriteria query = $query")
      query
        .as(query.defaultParser.*)
        .map(parseRowToEntity(_).get)

    }, s"Error while retrieving saving options by criteria: ${filter.map(_.toSmartString)}")
  }

  def countSavingOptionsByCriteria(filter: Option[RoundUpSavingsCriteria]): DaoResponse[Int] = {

    withConnection({ implicit connection ⇒

      val rawSelectQuery =
        s"""
           |SELECT COUNT(*) as n
           |$commonJoin
     """.stripMargin

      val where = generateWhere(filter)

      val query = SQL(
        s"""
           |$rawSelectQuery
           |$where
         """.stripMargin)
      logger.info(s"countSavingOptionsByCriteria query = $query")
      query
        .as(query.defaultParser.singleOpt)
        .map(row ⇒ row[Int]("n")).getOrElse(0)

    }, s"Error while counting saving options by criteria: ${filter.map(_.toSmartString)}")
  }
}

object RoundUpSavingsSqlDao {
  import SqlDao._

  val TableName = "roundup_savings"
  val TableAlias = "rus"

  val cId = "id"
  val cUuid = "uuid"
  val cUsrId = "user_id"
  val cAccId = "saving_account_id"
  val cCurrAmt = "current_amount"
  val cRoundNearest = "rounding_nearest"
  val cStatusUpdAt = "status_updated_at"
  val cCreatedAt = "created_at"
  val cUpdAt = "updated_at"
  val cIsActive = "is_active"

  val commonJoin =
    s"""
       |FROM $TableName $TableAlias
       |JOIN ${UserSqlDao.TableName} ${UserSqlDao.TableAlias}
       |ON ${UserSqlDao.TableAlias}.${UserSqlDao.id} = $TableAlias.$cUsrId
       |JOIN ${AccountSqlDao.TableName} ${AccountSqlDao.TableAlias}
       |ON ${AccountSqlDao.TableAlias}.${AccountSqlDao.cId} =  $TableAlias.$cAccId
       |JOIN ${CurrencySqlDao.TableName} ${CurrencySqlDao.TableAlias}
       |ON ${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cId} = ${AccountSqlDao.TableAlias}.${AccountSqlDao.cCurrencyId}
     """.stripMargin

  private def parseRowToEntity(row: Row) = Try {
    RoundUpSaving(
      id = row[Int](cId),
      uuid = row[String](s"$TableName.$cUuid"),
      userId = row[Int](cUsrId),
      userUuid = row[String](s"${UserSqlDao.TableName}.${UserSqlDao.uuid}"),
      accountId = row[Int](cAccId),
      accountUuid = row[String](s"${AccountSqlDao.TableName}.${AccountSqlDao.cUuid}"),
      currency = row[String](s"${CurrencySqlDao.TableName}.${CurrencySqlDao.cName}"),
      currentAmount = row[Option[BigDecimal]](cCurrAmt).getOrElse(BigDecimal(0)),
      roundingNearest = row[Int](cRoundNearest),
      statusUpdatedAt = row[Option[LocalDateTime]](cStatusUpdAt),
      createdAt = row[LocalDateTime](cCreatedAt),
      isActive = if (row[Int](cIsActive) == 0) false else true,
      updatedAt = row[LocalDateTime](cUpdAt))
  }

  private def generateWhere(maybeCriteria: Option[RoundUpSavingsCriteria]): String = {
    maybeCriteria.map(criteria ⇒ {
      Seq(
        criteria.id.map(_.toSql(Some(cId), Some(TableAlias))),

        criteria.uuid.map(_.toSql(Some(cUuid), Some(TableAlias))),

        criteria.userId.map(_.toSql(Some(cUsrId), Some(TableAlias))),

        criteria.userUuid.map(_.toSql(Some(UserSqlDao.uuid), Some(UserSqlDao.TableAlias))),

        criteria.accountId.map(_.toSql(Some(cAccId), Some(TableAlias))),

        criteria.accountUuid.map(_.toSql(Some(AccountSqlDao.cUuid), Some(TableAlias))),

        criteria.currency.map(_.toSql(Some(CurrencySqlDao.cName), Some(CurrencySqlDao.TableAlias))),

        criteria.currentAmount.map(_.toSql(Some(cCurrAmt), Some(TableAlias))),

        criteria.roundingNearest.map(_.toSql(Some(cRoundNearest), Some(TableAlias))),

        criteria.statusUpdatedAt.map(c ⇒
          c.copy(value = c.value.format(DateTimeFormatter.ISO_DATE_TIME))
            .toSql(Some(cStatusUpdAt), Some(TableAlias))),

        criteria.createdAt.map(c ⇒ c.copy(value = c.value.format(DateTimeFormatter.ISO_DATE_TIME))
          .toSql(Some(cCreatedAt), Some(TableAlias))),

        criteria.isActive.map(c ⇒ c.copy(value = if (c.value) 1 else 0)
          .toSql(Some(cIsActive), Some(TableAlias)))).flatten.toSql
    }).getOrElse("")

  }
}
