package tech.pegb.backoffice.dao.account.sql

import anorm._
import cats.implicits._
import java.sql.Connection
import java.time.LocalDateTime
import java.util.UUID

import com.google.inject.{Inject, Singleton}
import play.api.db.DBApi
import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.account.abstraction.ExternalAccountDao
import tech.pegb.backoffice.dao.account.dto.{ExternalAccountCriteria, ExternalAccountToCreate, ExternalAccountToUpdate}
import tech.pegb.backoffice.dao.account.entity.ExternalAccount
import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao
import tech.pegb.backoffice.dao.customer.abstraction.UserDao
import tech.pegb.backoffice.dao.customer.sql.UserSqlDao
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.util.Implicits._
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

@Singleton
class ExternalAccountSqlDao @Inject() (val dbApi: DBApi, userDao: UserDao) extends ExternalAccountDao with SqlDao {
  import ExternalAccountSqlDao._
  import SqlDao._

  def getExternalAccountById(id: Int)(implicit txnConn: Option[Connection] = None): DaoResponse[Option[ExternalAccount]] =
    withTransactionAndFlatten({ conn ⇒
      implicit val actualTxnConn = txnConn.orElse(Some(conn))

      getExternalAccountByCriteria(
        ExternalAccountCriteria(id = id),
        None, Some(1), None)(actualTxnConn).map(_.headOption)

    }, s"[getExternalAccountById] Unexpected error while fetching id [$id]")

  def getExternalAccountByCriteria(
    criteria: ExternalAccountCriteria,
    orderBy: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int])(implicit txnConn: Option[Connection] = None): DaoResponse[Seq[ExternalAccount]] = {
    withTransaction({ conn ⇒
      implicit val actualTxnConn = txnConn.getOrElse(conn)

      val filter = generateWhereClause(criteria)
      val ordering = generateOrderByClause(orderBy)
      val pagination = getPagination(limit, offset)

      val rawSql =
        s"""
           |SELECT ${TableAlias}.*,
           | ${UserSqlDao.TableAlias}.${UserSqlDao.uuid},
           | ${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cName} as ${ExternalAccount.cCurrencyName}
           |FROM ${TableName} ${TableAlias}
           |
           |JOIN ${UserSqlDao.TableName} ${UserSqlDao.TableAlias}
           |ON ${TableAlias}.${ExternalAccount.cUserId} = ${UserSqlDao.TableAlias}.${UserSqlDao.id}
           |
           |JOIN ${CurrencySqlDao.TableName} ${CurrencySqlDao.TableAlias}
           |ON ${TableAlias}.${ExternalAccount.cCurrencyId} = ${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cId}
           |
           |$filter
           |$ordering
           |$pagination
         """.stripMargin

      logger.info(s"[getExternalAccountByCriteria] query = $rawSql")

      SQL(rawSql).executeQuery().as(rowParser.*)
    }, s"[getExternalAccountByCriteria] Unexpected error using criteria [${criteria.toSmartString}]")
  }

  def countExternalAccount(criteria: ExternalAccountCriteria)(implicit txnConn: Option[Connection] = None): DaoResponse[Int] =
    withTransaction({ conn ⇒
      implicit val actualTxnConn = txnConn.getOrElse(conn)

      val filter = generateWhereClause(criteria)

      val rawSql =
        s"""
           |SELECT COUNT(${TableAlias}.${ExternalAccount.cId}) as n
           |FROM ${TableName} ${TableAlias}
           |
           |JOIN ${UserSqlDao.TableName} ${UserSqlDao.TableAlias}
           |ON ${TableAlias}.${ExternalAccount.cUserId} = ${UserSqlDao.TableAlias}.${UserSqlDao.id}
           |
           |JOIN ${CurrencySqlDao.TableName} ${CurrencySqlDao.TableAlias}
           |ON ${TableAlias}.${ExternalAccount.cCurrencyId} = ${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cId}
           |
           |$filter
         """.stripMargin

      logger.debug(s"[getExternalAccountByCriteria] query = $rawSql")

      SQL(rawSql).executeQuery().as(countParser.single)
    }, s"[count] Unexpected error using criteria [${criteria.toSmartString}]")

  def insertExternalAccount(dto: ExternalAccountToCreate)(implicit txnConn: Option[Connection] = None): DaoResponse[ExternalAccount] =
    withTransactionAndFlatten({ conn ⇒
      implicit val actualTxnConn = txnConn.getOrElse(conn)
      import ExternalAccount._

      val rawSql =
        s"""
           |INSERT INTO $TableName
           |($cUuid,   $cUserId,   $cProvider,   $cAccountNum,   $cAccountHolder,   $cCurrencyId,   $cCreatedBy,   $cCreatedAt,   $cUpdatedBy,   $cUpdatedAt)
           |VALUES
           |({$cUuid}, {$cUserId}, {$cProvider}, {$cAccountNum}, {$cAccountHolder}, {$cCurrencyId}, {$cCreatedBy}, {$cCreatedAt}, {$cUpdatedBy}, {$cUpdatedAt});
         """.stripMargin
      val parameters = Seq[NamedParameter](cUuid → dto.uuid, cUserId → dto.userId, cProvider → dto.provider,
        cAccountNum → dto.accountNumber, cAccountHolder → dto.accountHolder, cCurrencyId → dto.currencyId,
        cCreatedBy → dto.createdBy, cCreatedAt → dto.createdAt, cUpdatedBy → dto.updatedBy, cUpdatedAt → dto.updatedAt)
      val id = SQL(rawSql).on(parameters: _*).executeInsert(SqlParser.scalar[Long].single)
      getExternalAccountById(id.toInt)(Some(actualTxnConn)).map(_.get)

    }, s"[insertExternalAccount] Unexpected error using dto [${dto.toSmartString}]",
      {
        case error: java.util.NoSuchElementException ⇒
          entityNotFoundError("Entity created but was not found")
        case error: java.sql.SQLException if error.getMessage.toLowerCase.contains("unique") ⇒
          error.printStackTrace()
          constraintViolationError(s"Unique constraint on any of these columns ${ExternalAccount.uniqueConstraint.defaultMkString} was violated")
        case error: java.sql.SQLException if error.getMessage.toLowerCase.contains("referential integrity") ⇒
          constraintViolationError(s"Foreign key to any of these columns ${ExternalAccount.referentialConstraint.defaultMkString} was violated")
        case error: Exception ⇒
          error.printStackTrace()
          genericDbError("Unexpected error in creating external account entity")
      })

  def updateExternalAccount(criteria: ExternalAccountCriteria, dto: ExternalAccountToUpdate)(implicit txnConn: Option[Connection] = None): DaoResponse[Seq[ExternalAccount]] =
    withTransactionAndFlatten({ conn ⇒
      implicit val actualTxnConn = txnConn.getOrElse(conn)
      getExternalAccountByCriteria(criteria, None, None, None)(Some(actualTxnConn)).fold(
        _.toLeft,
        {
          case results if results.nonEmpty ⇒
            val flattenedCriteria = ExternalAccountCriteria(results.map(_.uuid))
            val filter = generateWhereClause(flattenedCriteria, addTableAlias = false)
            //TODO if criteria gets several records with different updated_at from each other this will be a problem
            val preQuery = dto.createSqlString(TableName, Some(filter))
            logger.info(s"[updateExternalAccount] query = $preQuery")
            val params = dto.paramsBuilder.result()
            val updateResult = SQL(preQuery).on(params: _*).executeUpdate()
            if (updateResult.isUpdated) {
              getExternalAccountByCriteria(flattenedCriteria, None, None, None)(Some(actualTxnConn))
            } else {
              Left(preconditionFailed(s"Failed to update stale external account. Entity has been updated recently."))
            }
          case _ ⇒
            Right(Nil)
        })
    }, s"[updateExternalAccount] Unexpected error using dto [${dto.toSmartString}]",
      {
        case error: java.sql.SQLException if error.getMessage.toLowerCase.contains("unique") ⇒
          error.printStackTrace()
          constraintViolationError(s"Unique constraint on any of these columns ${ExternalAccount.uniqueConstraint.defaultMkString} was violated")
        case error: java.sql.SQLException if error.getMessage.toLowerCase.contains("referential integrity") ⇒
          constraintViolationError(s"Foreign key to any of these columns ${ExternalAccount.referentialConstraint.defaultMkString} was violated")
        case error: Exception ⇒
          error.printStackTrace()
          genericDbError("Unexpected error in updating external account entity")
      })

  def deleteExternalAccount(criteria: ExternalAccountCriteria, lastUpdatedAt: Option[LocalDateTime])(implicit txnConn: Option[Connection] = None): DaoResponse[Option[Unit]] =
    withTransactionAndFlatten({ conn ⇒
      implicit val actualTxnConn = txnConn.getOrElse(conn)
      import ExternalAccount._
      getExternalAccountByCriteria(criteria, None, None, None)(Some(actualTxnConn)).fold(
        _.toLeft,
        rowsToBeDeleted ⇒ {
          if (rowsToBeDeleted.nonEmpty) {
            val rawSql =
              s"""DELETE FROM $TableName
               |WHERE id IN ({$cId})
               |AND ${lastUpdatedAt.fold("updated_at IS NULL")(updatedAt ⇒ s"updated_at = {$cUpdatedAt}")} """.stripMargin

            val numRowsDeleted = SQL(rawSql).on(cId → rowsToBeDeleted.map(_.id), cUpdatedAt → lastUpdatedAt).executeUpdate()
            if (numRowsDeleted == rowsToBeDeleted.size) {
              Right(Some(()))
            } else {
              Left(preconditionFailed(s"Failed to delete stale external account. Entity has been updated recently."))
            }

          } else {
            Right(None)
          }
        })

    }, s"[deleteExternalAccount] Unexpected error with this criteria [${criteria.toSmartString}]")

  def generateWhereClause(criteria: ExternalAccountCriteria, addTableAlias: Boolean = true): String = {
    val tblAlias = if (addTableAlias) TableAlias.some else None
    val currencyTblAlias = if (addTableAlias) CurrencySqlDao.TableAlias.some else None
    val usrTblAlias = if (addTableAlias) UserSqlDao.TableAlias.some else None
    Seq(
      criteria.id.map(_.toSql(None, tblAlias)),
      criteria.uuid.map(_.toSql(None, tblAlias)),
      criteria.anyUuid.map(_.toSql(None, tblAlias)),
      criteria.userId.map(_.toSql(None, tblAlias)),
      criteria.userUuid.map(_.toSql(Some(UserSqlDao.uuid), usrTblAlias)),
      criteria.provider.map(_.toSql(None, tblAlias)),
      criteria.accountNumber.map(_.toSql(None, tblAlias)),
      criteria.accountHolder.map(_.toSql(None, tblAlias)),
      criteria.currencyName.map(_.toSql(Some(CurrencySqlDao.cName), currencyTblAlias)),
      criteria.createdBy.map(_.toSql(None, tblAlias)),
      criteria.createdAt.map(_.toSql(None, tblAlias)),
      criteria.updatedBy.map(_.toSql(None, tblAlias)),
      criteria.updatedAt.map(_.toSql(None, tblAlias))).flatten.toSql
  }

  def generateOrderByClause(orderBy: Option[OrderingSet]): String = {
    orderBy.map(os ⇒ OrderingSet(os.underlying.map(o ⇒
      if (o.field == "external_account_id") o.copy(field = "id") else o)).toSql(None))
      .getOrElse(s"ORDER BY ${TableAlias}.${ExternalAccount.cId} ASC")
  }

  private val countParser: RowParser[Int] = row ⇒ Try {
    row[Int]("n")
  }.fold(
    exc ⇒ anorm.Error(SqlRequestError(new Exception(s"Failed to parse count external account result. Reason: ${exc.getMessage}"))),
    anorm.Success(_))

  private val rowParser: RowParser[ExternalAccount] = row ⇒ Try {
    parseRowToEntity(row)
  }.fold(
    exc ⇒ anorm.Error(SqlRequestError(new Exception(s"Failed to parse row to external account entity. Reason: ${exc.getMessage}"))),
    anorm.Success(_))

  def parseRowToEntity(row: Row): ExternalAccount = {
    ExternalAccount(
      id = row[Int](s"${TableName}.${ExternalAccount.cId}"),
      uuid = row[UUID](s"${TableName}.${ExternalAccount.cUuid}"),
      userId = row[Int](s"${TableName}.${ExternalAccount.cUserId}"),
      userUuid = row[UUID](s"${UserSqlDao.TableName}.${UserSqlDao.uuid}"),
      provider = row[String](s"${TableName}.${ExternalAccount.cProvider}"),
      accountNumber = row[String](s"${TableName}.${ExternalAccount.cAccountNum}"),
      accountHolder = row[String](s"${TableName}.${ExternalAccount.cAccountHolder}"),
      currencyId = row[Int](s"${TableName}.${ExternalAccount.cCurrencyId}"),
      currencyName = row[String](s"${CurrencySqlDao.TableName}.${CurrencySqlDao.cName}"),
      createdBy = row[String](s"${TableName}.${ExternalAccount.cCreatedBy}"),
      createdAt = row[LocalDateTime](s"${TableName}.${ExternalAccount.cCreatedAt}"),
      updatedBy = row[Option[String]](s"${TableName}.${ExternalAccount.cUpdatedBy}"),
      updatedAt = row[Option[LocalDateTime]](s"${TableName}.${ExternalAccount.cUpdatedAt}"))
  }
}

object ExternalAccountSqlDao {
  val TableName = "business_user_external_accounts"
  val TableAlias = "buea"
}
