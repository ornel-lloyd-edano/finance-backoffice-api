package tech.pegb.backoffice.dao.savings.sql

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}

import anorm._
import com.google.inject.Inject
import play.api.db.DBApi

import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.SqlDao.getPagination
import tech.pegb.backoffice.dao.account.sql.AccountSqlDao
import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao
import tech.pegb.backoffice.dao.customer.sql.UserSqlDao
import tech.pegb.backoffice.dao.model.{MatchTypes, OrderingSet}
import tech.pegb.backoffice.dao.savings.abstraction.SavingGoalsDao
import tech.pegb.backoffice.dao.savings.dto.SavingGoalsCriteria
import tech.pegb.backoffice.dao.savings.entity.SavingGoal
import tech.pegb.backoffice.dao.util.Implicits._
import tech.pegb.backoffice.util.AppConfig
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

class SavingGoalsSqlDao @Inject() (config: AppConfig, val dbApi: DBApi) extends SavingGoalsDao with SqlDao {

  import SavingGoalsSqlDao._

  def getSavingOptionsByCriteria(
    filter: Option[SavingGoalsCriteria],
    ordering: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int]): DaoResponse[List[SavingGoal]] = {

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

  def countSavingOptionsByCriteria(filter: Option[SavingGoalsCriteria]): DaoResponse[Int] = {

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

object SavingGoalsSqlDao {
  import SqlDao._

  val TableName = "saving_goals"
  val TableAlias = "sg"

  val cId = "id"
  val cUuid = "uuid"
  val cUsrId = "user_id"
  val cAccId = "saving_account_id"
  val cCurrAmt = "current_amount"
  val cInitAmt = "initial_amount"
  val cEmiAmt = "emi_amount"
  val cGoalAmt = "goal_amount"
  val cDueDate = "due_date"
  val cName = "name"
  val cReason = "reason"
  val cPaymentType = "payment_type"
  val cStatus = "status"
  val cStatusUpdAt = "status_updated_at"
  val cCreatedAt = "created_at"
  val cUpdAt = "updated_at"

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
    SavingGoal(
      id = row[Int](cId),
      uuid = row[String](s"$TableName.$cUuid"),
      userId = row[Int](cUsrId),
      userUuid = row[String](s"${UserSqlDao.TableName}.${UserSqlDao.uuid}"),
      accountId = row[Int](cAccId),
      accountUuid = row[String](s"${AccountSqlDao.TableName}.${AccountSqlDao.cUuid}"),
      currency = row[String](s"${CurrencySqlDao.TableName}.${CurrencySqlDao.cName}"),
      currentAmount = row[Option[BigDecimal]](cCurrAmt).getOrElse(BigDecimal(0)),
      goalAmount = row[BigDecimal](cGoalAmt),
      initialAmount = row[BigDecimal](cInitAmt),
      emiAmount = row[BigDecimal](cEmiAmt),

      name = row[String](cName),
      reason = row[Option[String]](cReason),
      status = row[String](cStatus),
      paymentType = row[String](cPaymentType),

      statusUpdatedAt = row[Option[LocalDateTime]](cStatusUpdAt),
      createdAt = row[LocalDateTime](cCreatedAt),
      dueDate = row[LocalDate](cDueDate),
      updatedAt = row[LocalDateTime](cUpdAt))
  }

  private def generateWhere(maybeCriteria: Option[SavingGoalsCriteria]): String = {
    maybeCriteria.map(criteria ⇒ {
      Seq(
        criteria.id.map(_.toSql(Some(cId), Some(TableAlias))),

        criteria.uuid.map(_.toSql(Some(cUuid), Some(TableAlias))),

        criteria.userId.map(_.toSql(Some(cUsrId), Some(TableAlias))),

        criteria.userUuid.map(_.toSql(Some(UserSqlDao.uuid), Some(UserSqlDao.TableAlias))),

        criteria.accountId.map(_.toSql(Some(cAccId), Some(TableAlias))),

        criteria.accountUuid.map(_.toSql(Some(AccountSqlDao.cUuid), Some(AccountSqlDao.TableAlias))),

        criteria.currency.map(_.toSql(Some(CurrencySqlDao.cName), Some(CurrencySqlDao.TableAlias))),

        criteria.currentAmount.map(_.toSql(Some(cCurrAmt), Some(TableAlias))),

        criteria.initialAmount.map(_.toSql(Some(cInitAmt), Some(TableAlias))),

        criteria.goalAmount.map(_.toSql(Some(cGoalAmt), Some(TableAlias))),

        criteria.emiAmount.map(_.toSql(Some(cEmiAmt), Some(TableAlias))),

        criteria.name.map(_.toSql(Some(cName), Some(TableAlias))),

        criteria.reason.map(_.toSql(Some(cReason), Some(TableAlias))),

        criteria.status.map(_.toSql(Some(cStatus), Some(TableAlias))),

        criteria.isActive.map(c ⇒ (
          if (c.value) {
            c.copy(value = "active", operator = MatchTypes.Exact)
          } else {
            c.copy(value = "active", operator = MatchTypes.NotSame)
          }).toSql(Some(cStatus), Some(TableAlias))),

        criteria.paymentType.map(_.toSql(Some(cPaymentType), Some(TableAlias))),

        criteria.statusUpdatedAt.map(c ⇒
          c.copy(value = c.value.format(DateTimeFormatter.ISO_DATE_TIME))
            .toSql(Some(cStatusUpdAt), Some(TableAlias))),

        criteria.createdAt.map(c ⇒ c.copy(value = c.value.format(DateTimeFormatter.ISO_DATE_TIME))
          .toSql(Some(cCreatedAt), Some(TableAlias)))).flatten.toSql
    }).getOrElse("")

  }
}
