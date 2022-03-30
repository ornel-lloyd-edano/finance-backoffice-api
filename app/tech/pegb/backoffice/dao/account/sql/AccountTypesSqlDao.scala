package tech.pegb.backoffice.dao.account.sql

import java.sql.Connection
import java.time.LocalDateTime

import anorm._
import com.google.inject.{Inject, Singleton}
import play.api.db.DBApi
import tech.pegb.backoffice.application.KafkaDBSyncService
import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.SqlDao.queryConditionClause
import tech.pegb.backoffice.dao.account.abstraction.AccountTypesDao
import tech.pegb.backoffice.dao.account.dto.{AccountTypeToUpdate, AccountTypeToUpsert}
import tech.pegb.backoffice.dao.account.entity.AccountAttributes.AccountType
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.dao.customer.abstraction.UserDao
import tech.pegb.backoffice.util.Implicits._

@Singleton
class AccountTypesSqlDao @Inject() (val dbApi: DBApi, userDao: UserDao, kafkaDBSyncService: KafkaDBSyncService) extends AccountTypesDao with SqlDao {

  import AccountTypesSqlDao._

  def bulkUpsert(
    dto: Seq[AccountTypeToUpsert],
    createdAt: LocalDateTime,
    createdBy: String): DaoResponse[Seq[AccountType]] = {
    withTransaction({ implicit connection ⇒
      assert(dto.nonEmpty, "cannot upsert empty list of accountTypes")

      val queryResult = dto.map { accType ⇒
        accType.id.fold {
          SQL(
            s"""
               |INSERT INTO $TableName
               |($cAccountType, $cDescription, $cCreatedAt, $cCreatedBy, $cUpdatedAt, $cUpdatedBy, $cIsActive)
               |VALUES
               |${
              s"('${accType.accountTypeName}', ${accType.description.map(d ⇒ s"'$d'").getOrElse("null")}, " +
                s"{$cCreatedAt}, {$cCreatedBy}, null, null, " +
                s"'${accType.isActive.toInt}')"
            }
           ;""".stripMargin).on(
              cCreatedAt → createdAt,
              cCreatedBy → createdBy).execute()

        } { existingId ⇒
          SQL(
            s"""
               |INSERT INTO $TableName
               |($cId, $cAccountType, $cDescription, $cCreatedAt, $cCreatedBy, $cUpdatedAt, $cUpdatedBy, $cIsActive)
               |VALUES
               |${
              s"('$existingId','${accType.accountTypeName}', ${accType.description.map(d ⇒ s"'$d'").getOrElse("null")}, " +
                s"{$cCreatedAt}, {$cCreatedBy}, null, null, " +
                s"'${accType.isActive.toInt}')"
            }
               |ON DUPLICATE KEY UPDATE
               |$cAccountType = VALUES($cAccountType),
               |$cDescription = VALUES($cDescription),
               |$cUpdatedAt   = VALUES($cCreatedAt),
               |$cUpdatedBy   = VALUES($cCreatedBy),
               |$cIsActive    = VALUES($cIsActive)
           ;""".stripMargin).on(
              cCreatedAt → createdAt,
              cCreatedBy → createdBy).execute()
        }
      }
      if (queryResult.nonEmpty) {
        val accountTypesList = findAllInternal
        accountTypesList.foreach { accountType ⇒
          kafkaDBSyncService.sendUpsert(TableName, accountType)
        }
        accountTypesList
      } else {
        throw new Throwable("could not perform upsert for account types")
      }

    }, s"could not bulk upsert to account_types")
  }

  def getAll: DaoResponse[Set[AccountType]] =
    withConnection({ implicit connection: Connection ⇒
      getAllTypesSql.as(getAllTypesSql.defaultParser.*)
        .map(convertRowToAccountType).toSet
    }, "Error while retrieving getting all account types")

  def update(id: Int, accountTypeToUpdate: AccountTypeToUpdate): DaoResponse[Option[AccountType]] =
    withConnection({ implicit connection: Connection ⇒
      val setClause = updateClause(accountTypeToUpdate)

      val result = updateSql(setClause, id).executeUpdate()
      if (result > 0) {
        findByIdInternal(id).map { accountType ⇒
          kafkaDBSyncService.sendUpdate(TableName, accountType)
          accountType
        }
      } else None
    }, "Error while updating account types")

  private def findByIdInternal(id: Int)(implicit connection: Connection): Option[AccountType] = {
    findById.on('id → id)
      .as(findById.defaultParser.singleOpt)
      .map(row ⇒ convertRowToAccountType(row))
  }

  private def findAllInternal(implicit connection: Connection): Seq[AccountType] = {
    getAllTypesSql
      .as(getAllTypesSql.defaultParser.*)
      .map(row ⇒ convertRowToAccountType(row))
  }
}

object AccountTypesSqlDao {
  final val TableName = "account_types"
  final val TableAlias = "at"

  final val cId = "id"
  final val cAccountType = "account_type_name"
  final val cDescription = "description"
  final val cIsActive = "is_active"
  final val cCreatedAt = "created_at"
  final val cCreatedBy = "created_by"
  final val cUpdatedAt = "updated_at"
  private[dao] final val cUpdatedBy = "updated_by"

  private[dao] final val TableFields: Seq[String] =
    Seq(cAccountType, cDescription, cIsActive, cCreatedAt, cCreatedBy, cUpdatedAt, cUpdatedBy)
  private[dao] final val TableFieldsStr = TableFields.mkString(",")

  private[dao] final val findById = SQL(s"SELECT * FROM $TableName where id={id}")

  private[dao] def insertSql(accountTypeToInsert: AccountTypeToUpsert) =
    SQL(
      s"INSERT INTO $TableName ($TableFieldsStr) VALUES ({$cAccountType}," +
        s" {$cDescription}, {$cIsActive}, {$cCreatedAt}, {$cCreatedBy}, {$cUpdatedAt}, {$cUpdatedBy});")

  private[dao] def updateSql(set: String, id: Int) =
    SQL(s"UPDATE $TableName $TableAlias SET $set WHERE $TableAlias.${AccountTypesSqlDao.cId}=$id")

  private final val getAllTypesSql = SQL(s"SELECT * FROM $TableName")

  private def updateClause(accountTypeToUpdate: AccountTypeToUpdate): String =
    Seq(
      accountTypeToUpdate.accountTypeName.map(queryConditionClause(_, cAccountType, Some(TableAlias))),
      accountTypeToUpdate.description.map(queryConditionClause(_, cDescription, Some(TableAlias))),
      accountTypeToUpdate.isActive.map(queryConditionClause(_, cIsActive, Some(TableAlias))),
      Some(queryConditionClause(accountTypeToUpdate.updatedAt, cUpdatedAt, Some(TableAlias))),
      Some(queryConditionClause(accountTypeToUpdate.updatedBy, cUpdatedBy, Some(TableAlias)))).flatten.mkString(",")

  private def convertRowToAccountType(row: Row): AccountType = {
    AccountType(
      id = row[Int](cId),
      accountTypeName = row[String](cAccountType),
      description = row[Option[String]](cDescription),
      isActive = row[Int](cIsActive).toBoolean,
      createdAt = row[LocalDateTime](cCreatedAt),
      createdBy = row[String](cCreatedBy),
      updatedAt = row[Option[LocalDateTime]](cUpdatedAt),
      updatedBy = row[Option[String]](cUpdatedBy))
  }
}
