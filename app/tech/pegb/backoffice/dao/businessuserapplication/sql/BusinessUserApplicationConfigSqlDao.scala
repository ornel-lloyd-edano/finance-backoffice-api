package tech.pegb.backoffice.dao.businessuserapplication.sql

import java.sql.{Connection, SQLException}
import java.time.LocalDateTime

import anorm.{BatchSql, NamedParameter, Row, RowParser, SQL, SimpleSql, SqlRequestError}
import cats.data.NonEmptyList
import com.google.inject.Inject
import play.api.db.DBApi
import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.SqlDao.genId
import tech.pegb.backoffice.dao.businessuserapplication.abstraction.BusinessUserApplicationConfigDao
import tech.pegb.backoffice.dao.businessuserapplication.dto.{AccountConfigToInsert, ExternalAccountToInsert, TransactionConfigToInsert}
import tech.pegb.backoffice.dao.businessuserapplication.entity.{AccountConfig, ExternalAccount, TransactionConfig}
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

class BusinessUserApplicationConfigSqlDao @Inject() (
    val dbApi: DBApi)
  extends BusinessUserApplicationConfigDao with SqlDao {
  import BusinessUserApplicationConfigSqlDao._

  def insertTxnConfig(
    applicationId: Int,
    txnConfigToInsert: NonEmptyList[TransactionConfigToInsert],
    createdBy: String,
    createdAt: LocalDateTime)(implicit maybeConnection: Option[Connection]): DaoResponse[Seq[TransactionConfig]] = {

    logger.debug(s"[insertTxnConfig] applicationId: ${applicationId}")
    logger.debug(s"[insertTxnConfig] txnConfigToInsert: ${txnConfigToInsert}")

    withTransaction({ cxn: Connection ⇒

      implicit val connection = maybeConnection.getOrElse(cxn)

      import TransactionConfigMeta._

      val txnConfigNamedParameterSeq = txnConfigToInsert.map { dto ⇒
        Seq(
          NamedParameter(colUuid, genId()),
          NamedParameter(colApplicationId, applicationId),
          NamedParameter(colTxnType, dto.transactionType),
          NamedParameter(colCurrencyId, dto.currencyId),
          NamedParameter(colCreatedAt, createdAt),
          NamedParameter(colCreatedBy, createdBy),
          NamedParameter(colUpdatedAt, createdAt),
          NamedParameter(colUpdatedBy, createdBy))
      }

      batchInsertQuery(insertTransactionConfigQuery, txnConfigNamedParameterSeq.head, txnConfigNamedParameterSeq.tail: _*).execute()
      getInternal[TransactionConfig](applicationId, tableName, tableAlias, txnConfigParser)

    }, s"[insertTxnConfig] Failed to create TxnConfig: {id = $applicationId} $txnConfigToInsert",
      handlerPF = {
        case e: SQLException ⇒
          val errorMessage = s"[insertTxnConfig] Could not create TxnConfig {id = $applicationId} $txnConfigToInsert"
          logger.error(errorMessage, e)
          constraintViolationError(errorMessage)
        case generic ⇒
          logger.error(s"[insertTxnConfig]", generic)
          genericDbError(s"error encountered while inserting application $applicationId")
      })
  }

  def insertAccountConfig(
    applicationId: Int,
    accountConfigToInsert: NonEmptyList[AccountConfigToInsert],
    createdBy: String,
    createdAt: LocalDateTime)(implicit maybeConnection: Option[Connection]): DaoResponse[Seq[AccountConfig]] = {
    logger.debug(s"[insertTxnConfig] applicationId: ${applicationId}")
    logger.debug(s"[insertTxnConfig] accountConfigToInsert: ${accountConfigToInsert}")

    withTransaction({ cxn: Connection ⇒

      implicit val connection = maybeConnection.getOrElse(cxn)

      import AccountConfigMeta._

      val accountConfigNamedParameterSeq = accountConfigToInsert.map { dto ⇒
        Seq(
          NamedParameter(colUuid, genId()),
          NamedParameter(colApplicationId, applicationId),
          NamedParameter(colAccountType, dto.accountType),
          NamedParameter(colAccountName, dto.accountName),
          NamedParameter(colCurrencyId, dto.currencyId),
          NamedParameter(colIsDefault, dto.isDefault),
          NamedParameter(colCreatedAt, createdAt),
          NamedParameter(colCreatedBy, createdBy),
          NamedParameter(colUpdatedAt, createdAt),
          NamedParameter(colUpdatedBy, createdBy))
      }

      batchInsertQuery(insertAccountConfigQuery, accountConfigNamedParameterSeq.head, accountConfigNamedParameterSeq.tail: _*).execute()
      getInternal[AccountConfig](applicationId, tableName, tableAlias, accountConfigParser)

    }, s"[insertAccountConfig] Failed to create AccountConfig: {id = $applicationId} $accountConfigToInsert",
      handlerPF = {
        case e: SQLException ⇒
          val errorMessage = s"[insertAccountConfig] Could not create AccountConfig {id = $applicationId} $accountConfigToInsert"
          logger.error(errorMessage, e)
          constraintViolationError(errorMessage)
        case generic ⇒
          logger.error(s"[insertTxnConfig]", generic)
          genericDbError(s"Error encountered while inserting txn config $applicationId")
      })

  }

  def insertExternalAccount(
    applicationId: Int,
    externalAccountToInsert: NonEmptyList[ExternalAccountToInsert],
    createdBy: String,
    createdAt: LocalDateTime)(implicit maybeConnection: Option[Connection]): DaoResponse[Seq[ExternalAccount]] = {

    logger.debug(s"[insertTxnConfig] applicationId: ${applicationId}")
    logger.debug(s"[insertTxnConfig] externalAccountToInsert: ${externalAccountToInsert}")

    withTransaction({ cxn: Connection ⇒

      implicit val connection = maybeConnection.getOrElse(cxn)

      import ExternalAccountMeta._

      val externalAccountNamedParameterSeq = externalAccountToInsert.map { dto ⇒
        import ExternalAccountMeta._
        Seq(
          NamedParameter(colUuid, genId()),
          NamedParameter(colApplicationId, applicationId),
          NamedParameter(colProvider, dto.provider),
          NamedParameter(colAccountNumber, dto.accountNumber),
          NamedParameter(colAccountHolder, dto.accountHolder),
          NamedParameter(colCurrencyId, dto.currencyId),
          NamedParameter(colCreatedAt, createdAt),
          NamedParameter(colCreatedBy, createdBy),
          NamedParameter(colUpdatedAt, createdAt),
          NamedParameter(colUpdatedBy, createdBy))
      }

      batchInsertQuery(insertExternalAccountQuery, externalAccountNamedParameterSeq.head, externalAccountNamedParameterSeq.tail: _*).execute()
      getInternal[ExternalAccount](applicationId, tableName, tableAlias, externalAccountParser)

    }, s"[insertAccountConfig] Failed to create ExternalAccount: {id = $applicationId} $externalAccountToInsert",
      handlerPF = {
        case e: SQLException ⇒
          val errorMessage = s"[insertAccountConfig] Could not create ExternalAccount {id = $applicationId} $externalAccountToInsert"
          logger.error(errorMessage, e)
          constraintViolationError(errorMessage)
        case generic ⇒
          logger.error(s"[insertAccountConfig]", generic)
          genericDbError(s"Error encountered while inserting account config $applicationId")
      })
  }

  def deleteTxnConfig(applicationId: Int)(implicit maybeConnection: Option[Connection]): DaoResponse[Unit] = {
    withTransaction({ cn ⇒
      implicit val connection = maybeConnection.getOrElse(cn)

      deleteConfig(applicationId, TransactionConfigMeta.tableName).execute()
    }, s"[deleteTxnConfig] Unexpected error in delete TxnConfig for applicationId ${applicationId}")
  }

  def deleteAccountConfig(applicationId: Int)(implicit maybeConnection: Option[Connection]): DaoResponse[Unit] = {
    withTransaction({ cn ⇒
      implicit val connection = maybeConnection.getOrElse(cn)

      deleteConfig(applicationId, AccountConfigMeta.tableName).execute()
    }, s"[deleteAccountConfig] Unexpected error in delete AccountConfig for applicationId ${applicationId}")
  }

  def deleteExternalAccount(applicationId: Int)(implicit maybeConnection: Option[Connection]): DaoResponse[Unit] = {
    withTransaction({ cn ⇒
      implicit val connection = maybeConnection.getOrElse(cn)

      deleteConfig(applicationId, ExternalAccountMeta.tableName).execute()
    }, s"[deleteExternalAccount] Unexpected error in delete ExternalAccount for applicationId ${applicationId}")
  }

  def getTxnConfig(applicationId: Int): DaoResponse[Seq[TransactionConfig]] = withConnection({ implicit connection ⇒
    import TransactionConfigMeta._
    getInternal[TransactionConfig](applicationId, tableName, tableAlias, txnConfigParser)
  }, s"[getTxnConfig] Error while retrieving business user application config id = $applicationId")

  def getAccountConfig(applicationId: Int): DaoResponse[Seq[AccountConfig]] = withConnection({ implicit connection ⇒
    import AccountConfigMeta._
    getInternal[AccountConfig](applicationId, tableName, tableAlias, accountConfigParser)
  }, s"[getAccountConfig] Error while retrieving business user application config id = $applicationId")

  def getExternalAccount(applicationId: Int): DaoResponse[Seq[ExternalAccount]] = withConnection({ implicit connection ⇒
    import ExternalAccountMeta._
    getInternal[ExternalAccount](applicationId, tableName, tableAlias, externalAccountParser)
  }, s"[getExternalAccount] Error while retrieving business user application config id = $applicationId")

  private[sql] def getInternal[T](
    applicationId: Int,
    tableName: String,
    tableAlias: String,
    parser: RowParser[T])(implicit cxn: Connection): Seq[T] = {
    val filters = s"""WHERE $colApplicationId = {$colApplicationId}"""

    SQL(s"${baseFindConfigByApplicationId(tableName, tableAlias)} $filters".stripMargin)
      .on(colApplicationId → applicationId)
      .executeQuery().as(parser.*)
  }

}

object BusinessUserApplicationConfigSqlDao {
  val colId = "id"
  val colUuid = "uuid"
  val colApplicationId = "application_id"
  val colCurrencyId = "currency_id"
  val colCreatedBy = "created_by"
  val colCreatedAt = "created_at"
  val colUpdatedBy = "updated_by"
  val colUpdatedAt = "updated_at"

  object TransactionConfigMeta {
    val tableName = "business_user_application_txn_configs"
    val tableAlias = "tx_c"
    val colTxnType = "txn_type"
  }

  object AccountConfigMeta {
    val tableName = "business_user_application_account_configs"
    val tableAlias = "acc_c"
    val colAccountType = "account_type"
    val colAccountName = "account_name"
    val colIsDefault = "is_default"
  }

  object ExternalAccountMeta {
    val tableName = "business_user_application_external_accounts"
    val tableAlias = "ext_acc"
    val colProvider = "provider"
    val colAccountNumber = "account_number"
    val colAccountHolder = "account_holder"
  }

  object Currency {
    val tableName = "currencies"
    val tableAlias = "cur"
    val colCurrencyName = "currency_name"
  }

  private def deleteConfig(applicationId: Int, table: String): SimpleSql[Row] = {
    SQL(s"DELETE FROM $table WHERE $colApplicationId = {$colApplicationId}")
      .on(colApplicationId → applicationId)
  }

  private def batchInsertQuery(insertQuery: String, firstRow: Seq[NamedParameter], tail: Seq[NamedParameter]*) = {
    BatchSql(insertQuery, firstRow, tail: _*)
  }

  private val insertTransactionConfigQuery = {
    import TransactionConfigMeta._
    s"INSERT INTO $tableName ($colUuid, $colApplicationId, $colTxnType, $colCurrencyId, $colCreatedAt, $colCreatedBy, $colUpdatedAt, $colUpdatedBy) " +
      s"VALUES ({$colUuid}, {$colApplicationId}, {$colTxnType}, {$colCurrencyId}, {$colCreatedAt}, {$colCreatedBy}, {$colUpdatedAt}, {$colUpdatedBy})"
  }

  private val insertAccountConfigQuery = {
    import AccountConfigMeta._
    s"INSERT INTO $tableName ($colUuid, $colApplicationId, $colAccountType, $colAccountName, $colCurrencyId, $colIsDefault, $colCreatedAt, $colCreatedBy, $colUpdatedAt, $colUpdatedBy) " +
      s"VALUES ({$colUuid}, {$colApplicationId}, {$colAccountType}, {$colAccountName}, {$colCurrencyId}, {$colIsDefault}, {$colCreatedAt}, {$colCreatedBy}, {$colUpdatedAt}, {$colUpdatedBy})"
  }

  private val insertExternalAccountQuery = {
    import ExternalAccountMeta._
    s"INSERT INTO $tableName ($colUuid, $colApplicationId, $colProvider, $colAccountNumber, $colAccountHolder, $colCurrencyId, $colCreatedAt, $colCreatedBy, $colUpdatedAt, $colUpdatedBy) " +
      s"VALUES ({$colUuid}, {$colApplicationId}, {$colProvider}, {$colAccountNumber}, {$colAccountHolder}, {$colCurrencyId}, {$colCreatedAt}, {$colCreatedBy}, {$colUpdatedAt}, {$colUpdatedBy})"
  }

  private def baseFindConfigByApplicationId(tableName: String, tableAlias: String): String = {
    s"""SELECT $tableAlias.*, ${Currency.tableAlias}.${Currency.colCurrencyName}
       |FROM $tableName $tableAlias
       |JOIN ${Currency.tableName} ${Currency.tableAlias}
       |ON $tableAlias.$colCurrencyId = ${Currency.tableAlias}.$colId
       |""".stripMargin
  }

  private val txnConfigParser: RowParser[TransactionConfig] = row ⇒ {
    import TransactionConfigMeta._
    Try {
      TransactionConfig(
        id = row[Int](s"$tableName.$colId"),
        uuid = row[String](s"$tableName.$colUuid"),
        applicationId = row[Int](s"$tableName.$colApplicationId"),
        transactionType = row[String](s"$tableName.$colTxnType"),
        currencyId = row[Int](s"$tableName.$colCurrencyId"),
        currencyCode = row[String](s"${Currency.tableName}.${Currency.colCurrencyName}"),
        createdAt = row[LocalDateTime](s"$tableName.$colCreatedAt"),
        createdBy = row[String](s"$tableName.$colCreatedBy"),
        updatedAt = row[Option[LocalDateTime]](s"$tableName.$colUpdatedAt"),
        updatedBy = row[Option[String]](s"$tableName.$colUpdatedBy"))
    }.fold(
      exc ⇒ anorm.Error(SqlRequestError(exc)),
      anorm.Success(_))
  }

  private val accountConfigParser: RowParser[AccountConfig] = row ⇒ {
    import AccountConfigMeta._
    Try {
      AccountConfig(
        id = row[Int](s"$tableName.$colId"),
        uuid = row[String](s"$tableName.$colUuid"),
        applicationId = row[Int](s"$tableName.$colApplicationId"),
        accountType = row[String](s"$tableName.$colAccountType"),
        accountName = row[String](s"$tableName.$colAccountName"),
        currencyId = row[Int](s"$tableName.$colCurrencyId"),
        currencyCode = row[String](s"${Currency.tableName}.${Currency.colCurrencyName}"),
        isDefault = row[Int](s"$tableName.${colIsDefault}").toBoolean,
        createdAt = row[LocalDateTime](s"$tableName.$colCreatedAt"),
        createdBy = row[String](s"$tableName.$colCreatedBy"),
        updatedAt = row[Option[LocalDateTime]](s"$tableName.$colUpdatedAt"),
        updatedBy = row[Option[String]](s"$tableName.$colUpdatedBy"))
    }.fold(
      exc ⇒ anorm.Error(SqlRequestError(exc)),
      anorm.Success(_))
  }

  private val externalAccountParser: RowParser[ExternalAccount] = row ⇒ {
    import ExternalAccountMeta._
    Try {
      ExternalAccount(
        id = row[Int](s"$tableName.$colId"),
        uuid = row[String](s"$tableName.$colUuid"),
        applicationId = row[Int](s"$tableName.$colApplicationId"),
        provider = row[String](s"$tableName.$colProvider"),
        accountNumber = row[String](s"$tableName.$colAccountNumber"),
        accountHolder = row[String](s"$tableName.$colAccountHolder"),
        currencyId = row[Int](s"$tableName.$colCurrencyId"),
        currencyCode = row[String](s"${Currency.tableName}.${Currency.colCurrencyName}"),
        createdAt = row[LocalDateTime](s"$tableName.$colCreatedAt"),
        createdBy = row[String](s"$tableName.$colCreatedBy"),
        updatedAt = row[Option[LocalDateTime]](s"$tableName.$colUpdatedAt"),
        updatedBy = row[Option[String]](s"$tableName.$colUpdatedBy"))
    }.fold(
      exc ⇒ anorm.Error(SqlRequestError(exc)),
      anorm.Success(_))
  }

}

