package tech.pegb.backoffice.dao.transaction.sql

import java.sql.Connection
import java.time.LocalDateTime
import java.util.UUID

import anorm._
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import play.api.db.DBApi
import tech.pegb.backoffice.application.KafkaDBSyncService
import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao
import tech.pegb.backoffice.dao.customer.sql.UserSqlDao
import tech.pegb.backoffice.dao.model.{OrderingSet}
import tech.pegb.backoffice.dao.transaction.abstraction.TransactionConfigDao
import tech.pegb.backoffice.dao.transaction.dto.{TxnConfigCriteria, TxnConfigToCreate, TxnConfigToUpdate}
import tech.pegb.backoffice.dao.transaction.entity.TxnConfig
import tech.pegb.backoffice.dao.util.Implicits._
import tech.pegb.backoffice.util.AppConfig
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

@Singleton
class TransactionConfigSqlDao @Inject() (
    val dbApi: DBApi,
    config: AppConfig,
    kafkaDBSyncService: KafkaDBSyncService)
  extends TransactionConfigDao with SqlDao {
  import SqlDao._
  import TxnConfig._
  import TransactionConfigSqlDao._

  def getTxnConfigById(id: Int)(implicit txnConn: Option[Connection]): DaoResponse[Option[TxnConfig]] =
    withTransactionAndFlatten({ conn ⇒
      implicit val actualTxnConn = txnConn.orElse(Some(conn))

      getTxnConfigByCriteria(
        TxnConfigCriteria(id),
        None, Some(1), None)(actualTxnConn).map(_.headOption)

    }, s"[getTxnConfigById] Unexpected error while fetching id [$id]")

  def getTxnConfigByCriteria(
    criteria: TxnConfigCriteria,
    orderBy: Option[OrderingSet],
    limit: Option[Int], offset: Option[Int])(implicit txnConn: Option[Connection]): DaoResponse[Seq[TxnConfig]] = {
    withTransaction({ conn ⇒
      implicit val actualTxnConn = txnConn.getOrElse(conn)

      val filter = generateWhereClause(criteria)
      val ordering = generateOrderByClause(orderBy)
      val pagination = getPagination(limit, offset)

      val rawSql =
        s"""
           |SELECT ${TableAlias}.*,
           | ${UserSqlDao.TableAlias}.${UserSqlDao.uuid},
           | ${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cName}
           |FROM ${TableName} ${TableAlias}
           |
           |JOIN ${UserSqlDao.TableName} ${UserSqlDao.TableAlias}
           |ON ${TableAlias}.${cUserId} = ${UserSqlDao.TableAlias}.${UserSqlDao.id}
           |
           |JOIN ${CurrencySqlDao.TableName} ${CurrencySqlDao.TableAlias}
           |ON ${TableAlias}.${cCurrencyId} = ${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cId}
           |
           |$filter
           |$ordering
           |$pagination
         """.stripMargin

      logger.debug(s"[getTxnConfigByCriteria] query = $rawSql")

      SQL(rawSql).executeQuery().as(rowParser.*)
    }, s"[getTxnConfigByCriteria] Unexpected error using criteria [${criteria.toSmartString}]")
  }

  def countTxnConfig(criteria: TxnConfigCriteria)(implicit txnConn: Option[Connection]): DaoResponse[Int] = withTransaction({ conn ⇒
    implicit val actualTxnConn = txnConn.getOrElse(conn)

    val filter = generateWhereClause(criteria)

    val rawSql =
      s"""
         |SELECT COUNT(${TableAlias}.${cId}) as n
         |FROM ${TableName} ${TableAlias}
         |
         |JOIN ${UserSqlDao.TableName} ${UserSqlDao.TableAlias}
         |ON ${TableAlias}.${cUserId} = ${UserSqlDao.TableAlias}.${UserSqlDao.id}
         |
         |JOIN ${CurrencySqlDao.TableName} ${CurrencySqlDao.TableAlias}
         |ON ${TableAlias}.${cCurrencyId} = ${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cId}
         |
         |$filter
         """.stripMargin

    logger.debug(s"[countTxnConfig] query = $rawSql")

    SQL(rawSql).executeQuery().as(countParser.single)
  }, s"[countTxnConfig] Unexpected error using criteria [${criteria.toSmartString}]")

  def insertTxnConfig(dto: TxnConfigToCreate)(implicit txnConn: Option[Connection]): DaoResponse[TxnConfig] = withTransactionAndFlatten({ conn ⇒
    implicit val actualTxnConn = txnConn.getOrElse(conn)

    val rawSql =
      s"""
         |INSERT INTO $TableName
         |($cUuid,   $cUserId,   $cTxnType,  $cCurrencyId,   $cCreatedBy,   $cCreatedAt,   $cUpdatedBy,   $cUpdatedAt)
         |VALUES
         |({$cUuid}, {$cUserId}, {$cTxnType}, {$cCurrencyId}, {$cCreatedBy}, {$cCreatedAt}, {$cUpdatedBy}, {$cUpdatedAt});
         """.stripMargin
    val parameters = Seq[NamedParameter](cUuid → dto.uuid, cUserId → dto.userId,
      cTxnType → dto.transactionType, cCurrencyId → dto.currencyId,
      cCreatedBy → dto.createdBy, cCreatedAt → dto.createdAt,
      cUpdatedBy → dto.updatedBy, cUpdatedAt → dto.updatedAt)
    val id = SQL(rawSql).on(parameters: _*).executeInsert(SqlParser.scalar[Long].single)
    getTxnConfigById(id.toInt)(Some(actualTxnConn)).map(_.get)

  }, s"[insertTxnConfig] Unexpected error using dto [${dto.toSmartString}]",
    {
      case error: java.util.NoSuchElementException ⇒
        entityNotFoundError("Entity created but was not found")
      case error: java.sql.SQLException if error.getMessage.toLowerCase.contains("unique") ⇒
        error.printStackTrace()
        constraintViolationError(s"Unique constraint on any of these columns ${uniqueConstraint.defaultMkString} was violated")
      case error: java.sql.SQLException if error.getMessage.toLowerCase.contains("referential integrity") ⇒
        constraintViolationError(s"Foreign key to any of these columns ${referentialConstraint.defaultMkString} was violated")
      case error: Exception ⇒
        error.printStackTrace()
        genericDbError("Unexpected error in creating txn config entity")
    })

  def updateTxnConfig(criteria: TxnConfigCriteria, dto: TxnConfigToUpdate)(implicit txnConn: Option[Connection]): DaoResponse[Seq[TxnConfig]] = withTransactionAndFlatten({ conn ⇒
    implicit val actualTxnConn = txnConn.getOrElse(conn)
    getTxnConfigByCriteria(criteria, None, None, None)(Some(actualTxnConn)).fold(
      _.toLeft,
      {
        case results if results.nonEmpty ⇒
          val flattenedCriteria = TxnConfigCriteria(results.map(_.uuid))
          val filter = generateWhereClause(flattenedCriteria, addTableAlias = false)
          //TODO if criteria gets several records with different updated_at from each other this will be a problem
          val preQuery = dto.createSqlString(TableName, Some(filter))
          logger.info(s"[updateTxnConfig] query = $preQuery")
          val params = dto.paramsBuilder.result()
          val updateResult = SQL(preQuery).on(params: _*).executeUpdate()
          if (updateResult.isUpdated) {
            getTxnConfigByCriteria(flattenedCriteria, None, None, None)(Some(actualTxnConn))
          } else {
            Left(preconditionFailed(s"Failed to update stale txn config. Entity has been updated recently."))
          }
        case _ ⇒
          Right(Nil)
      })
  }, s"[updateTxnConfig] Unexpected error using dto [${dto.toSmartString}]",
    {
      case error: java.sql.SQLException if error.getMessage.toLowerCase.contains("unique") ⇒
        constraintViolationError(s"Unique constraint on any of these columns ${uniqueConstraint.defaultMkString} was violated")
      case error: java.sql.SQLException if error.getMessage.toLowerCase.contains("referential integrity") ⇒
        constraintViolationError(s"Foreign key to any of these columns ${referentialConstraint.defaultMkString} was violated")
      case error: Exception ⇒
        error.printStackTrace()
        genericDbError("Unexpected error in updating txn config entity")
    })

  def deleteTxnConfig(criteria: TxnConfigCriteria, lastUpdatedAt: Option[LocalDateTime])(implicit txnConn: Option[Connection]): DaoResponse[Option[Unit]] =
    withTransactionAndFlatten({ conn ⇒
      implicit val actualTxnConn = txnConn.getOrElse(conn)
      getTxnConfigByCriteria(criteria, None, None, None)(Some(actualTxnConn)).fold(
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
              Left(preconditionFailed(s"Failed to delete stale txn config. Entity has been updated recently."))
            }

          } else {
            Right(None)
          }
        })

    }, s"[deleteTxnConfig] Unexpected error with this criteria [${criteria.toSmartString}]")

  def generateWhereClause(criteria: TxnConfigCriteria, addTableAlias: Boolean = true): String = {
    val tblAlias = if (addTableAlias) TableAlias.some else None
    val currencyTblAlias = if (addTableAlias) CurrencySqlDao.TableAlias.some else None
    val usrTblAlias = if (addTableAlias) UserSqlDao.TableAlias.some else None
    Seq(
      criteria.id.map(_.toSql(None, tblAlias)),
      criteria.uuid.map(_.toSql(None, tblAlias)),
      criteria.anyUuid.map(_.toSql(None, tblAlias)),
      criteria.userId.map(_.toSql(None, tblAlias)),
      criteria.userUuid.map(_.toSql(Some(UserSqlDao.uuid), usrTblAlias)),
      criteria.transactionType.map(_.toSql(None, tblAlias)),
      criteria.currencyName.map(_.toSql(Some(CurrencySqlDao.cName), currencyTblAlias)),
      criteria.createdBy.map(_.toSql(None, tblAlias)),
      criteria.createdAt.map(_.toSql(None, tblAlias)),
      criteria.updatedBy.map(_.toSql(None, tblAlias)),
      criteria.updatedAt.map(_.toSql(None, tblAlias))).flatten.toSql
  }

  def generateOrderByClause(orderBy: Option[OrderingSet]): String = {
    orderBy.map(os ⇒ OrderingSet(os.underlying.map(o ⇒
      if (o.field == "txn_config_id") o.copy(field = "id") else o)).toSql(None))
      .getOrElse(s"ORDER BY ${TableAlias}.${cId} ASC")
  }

  private val countParser: RowParser[Int] = row ⇒ Try {
    row[Int]("n")
  }.fold(
    exc ⇒ anorm.Error(SqlRequestError(new Exception(s"Failed to parse count txn config result. Reason: ${exc.getMessage}"))),
    anorm.Success(_))

  private val rowParser: RowParser[TxnConfig] = row ⇒ Try {
    parseRowToEntity(row)
  }.fold(
    exc ⇒ anorm.Error(SqlRequestError(new Exception(s"Failed to parse row to txn config entity. Reason: ${exc.getMessage}"))),
    anorm.Success(_))

  def parseRowToEntity(row: Row): TxnConfig = {
    TxnConfig(
      id = row[Int](s"${TableName}.${cId}"),
      uuid = row[UUID](s"${TableName}.${cUuid}"),
      userId = row[Int](s"${TableName}.${cUserId}"),
      userUuid = row[UUID](s"${UserSqlDao.TableName}.${UserSqlDao.uuid}"),
      transactionType = row[String](s"${TableName}.${cTxnType}"),
      currencyId = row[Int](s"${TableName}.${cCurrencyId}"),
      currencyName = row[String](s"${CurrencySqlDao.TableName}.${CurrencySqlDao.cName}"),
      createdBy = row[String](s"${TableName}.${cCreatedBy}"),
      createdAt = row[LocalDateTime](s"${TableName}.${cCreatedAt}"),
      updatedBy = row[Option[String]](s"${TableName}.${cUpdatedBy}"),
      updatedAt = row[Option[LocalDateTime]](s"${TableName}.${cUpdatedAt}"))
  }
}

object TransactionConfigSqlDao {
  val TableName = "business_user_txn_configs"
  val TableAlias = "butc"
}
