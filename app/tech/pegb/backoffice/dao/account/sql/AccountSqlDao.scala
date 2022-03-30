package tech.pegb.backoffice.dao.account.sql

import java.sql.{Connection, SQLException}
import java.time.LocalDateTime

import anorm._
import cats.implicits._
import com.google.inject.{Inject, Singleton}
import play.api.db.DBApi
import play.api.libs.json.Json
import tech.pegb.backoffice.application.KafkaDBSyncService
import tech.pegb.backoffice.dao.SqlDao.queryConditionClause
import tech.pegb.backoffice.dao.account.abstraction.AccountDao
import tech.pegb.backoffice.dao.account.dto.{AccountCriteria, AccountToInsert, AccountToUpdate}
import tech.pegb.backoffice.dao.account.entity.Account
import tech.pegb.backoffice.dao.account.entity.AccountAttributes.AccountStatus
import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao
import tech.pegb.backoffice.dao.customer.abstraction.UserDao
import tech.pegb.backoffice.dao.customer.sql.{BusinessUserSqlDao, UserSqlDao}
import tech.pegb.backoffice.dao.model.{CriteriaField, OrderingSet}
import tech.pegb.backoffice.dao.sql.MostRecentUpdatedAtGetter
import tech.pegb.backoffice.dao.{DaoError, SqlDao}
import tech.pegb.backoffice.dao.util.Implicits._
import tech.pegb.backoffice.util.Implicits._

@Singleton
class AccountSqlDao @Inject() (val dbApi: DBApi, userDao: UserDao, kafkaDBSyncService: KafkaDBSyncService) extends AccountDao with MostRecentUpdatedAtGetter[Account, AccountCriteria] with SqlDao {

  import AccountSqlDao._
  import SqlDao._

  protected def getUpdatedAtColumn: String = s"${AccountSqlDao.TableAlias}.${AccountSqlDao.cUpdatedAt}"

  protected def getMainSelectQuery: String = AccountSqlDao.qCommonSelect

  protected def getRowToEntityParser: Row ⇒ Account = (arg: Row) ⇒ AccountSqlDao.convertRowToAccount(arg)

  protected def getWhereFilterFromCriteria(criteriaDto: Option[AccountCriteria]): String = generateWhereFilter(criteriaDto)

  def getAccount(uuid: String): DaoResponse[Option[Account]] = {
    withConnection({ implicit connection: Connection ⇒
      findByIdInternal(uuid)
    }, s"Error while retrieving account by uuid:$uuid")
  }

  def getAccountsByInternalIds(ids: Set[Int]): DaoResponse[Set[Account]] = {
    withConnection({ implicit connection: Connection ⇒
      findByIdInternalSet(ids).toSet
    }, s"Error while retrieving account by ids :$ids")
  }

  def getAccountStatuses: DaoResponse[Set[AccountStatus]] = ???

  def getAccountsByUserId(userId: String): DaoResponse[Set[Account]] =
    withConnection({ implicit connection: Connection ⇒
      findByUserIdInternal(userId)

    }, s"Error while retrieving account by user id:$userId")

  def getAccountByAccNum(accountNumber: String): DaoResponse[Option[Account]] =
    withConnection({ implicit connection: Connection ⇒
      findByAccountNumberSql.on(cNumber → accountNumber)
        .as(findByAccountNumberSql.defaultParser.singleOpt)
        .map(row ⇒ convertRowToAccount(row))

    }, s"Error while retrieving account by account number:$accountNumber")

  def getAccountByAccountName(accountName: String): DaoResponse[Option[Account]] = ???

  def getMainAccountByUserId(userId: String): DaoResponse[Option[Account]] =
    withConnection({ implicit connection ⇒
      val whereFilter = generateWhereFilter(AccountCriteria(isMainAccount = CriteriaField("", true).some).some)
      val accountsByCriteriaSql = findAccountsByCriteria(whereFilter, None, None, None)

      accountsByCriteriaSql
        .as(accountsByCriteriaSql.defaultParser.*)
        .map(convertRowToAccount).headOption

    }, s"Error while retrieving main account by user id:$userId")

  def getAccountsByCriteria(criteria: Option[AccountCriteria], orderBy: Option[OrderingSet] = None, limit: Option[Int] = None, offset: Option[Int] = None): DaoResponse[Seq[Account]] =
    withConnection({ implicit connection ⇒
      val whereFilter = generateWhereFilter(criteria)
      val accountsByCriteriaSql = findAccountsByCriteria(whereFilter, orderBy, limit, offset)

      accountsByCriteriaSql
        .as(accountsByCriteriaSql.defaultParser.*)
        .map(convertRowToAccount)

    }, s"Error while retrieving account by criteria:$criteria")

  def countTotalAccountsByCriteria(criteria: AccountCriteria): DaoResponse[Int] = withConnection({ implicit connection ⇒
    val whereFilter = generateWhereFilter(criteria.toOption)
    val countByCriteriaSql = findAccountsCountByCriteria(whereFilter)

    //countSql.as(countSql.defaultParser.singleOpt.map(_.map(_[Long]("n")).getOrElse(0)))
    countByCriteriaSql
      .as(countByCriteriaSql.defaultParser.singleOpt)
      .map(convertRowToCount).getOrElse(0)

  }, s"Error while retrieving account by criteria:$criteria")

  def insertAccount(accountToInsert: AccountToInsert)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Account] = {
    for {
      foundInternalUserId ← userDao.getInternalUserId(accountToInsert.userId).fold(
        error ⇒ Left(error),
        {
          case Some(userInternalId) ⇒ Right(userInternalId)
          case _ ⇒ Left(DaoError.EntityNotFoundError(s"Could not insert account, user id [${accountToInsert.userId}] not found"))
        })
      accountTypeId ← findAccountTypeId(accountToInsert.accountType)
      currencyId ← findCurrencyId(accountToInsert.currency)
      newAccount ← withConnectionAndFlatten(
        { connection ⇒
          val generatedUuid = genId().toString
          val parameters = buildParametersForAccount(generatedUuid, foundInternalUserId, accountToInsert, accountTypeId, currencyId)
          insertSql(accountToInsert)
            .on(parameters: _*)
            .executeInsert(SqlParser.scalar[Long].single)(maybeTransaction.getOrElse(connection))
          findByIdInternal(generatedUuid)(maybeTransaction.getOrElse(connection))
            .toRight(DaoError.EntityNotFoundError(s"Couldn't find newly created account $generatedUuid"))
        },
        "Unknown error when insertAccount")
      _ = kafkaDBSyncService.sendInsert(TableName, Json.toJson(newAccount))
    } yield newAccount
  }

  private def findAccountTypeId(accountName: String): DaoResponse[Int] = {
    withConnectionAndFlatten({ implicit connection ⇒
      SQL(s"SELECT id FROM ${AccountTypesSqlDao.TableName} WHERE account_type_name = {accTypeName} LIMIT 1;")
        .on('accTypeName → accountName).as(SqlParser.scalar[Int].singleOpt)
        .toRight(DaoError.EntityNotFoundError(s"There is no account type called `$accountName`"))
    }, s"Couldn't fetch account type by name `$accountName`.")
  }

  private def findCurrencyId(currencyCode: String): DaoResponse[Int] = {
    withConnectionAndFlatten({ implicit connection ⇒
      SQL(s"SELECT id FROM currencies WHERE currency_name = {currName} LIMIT 1;")
        .on('currName → currencyCode).as(SqlParser.scalar[Int].singleOpt)
        .toRight(DaoError.EntityNotFoundError(s"There is no currency called `$currencyCode`"))
    }, s"Couldn't fetch currency by code `$currencyCode`.")
  }

  def updateAccount(uuid: String, accountToUpdate: AccountToUpdate)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Option[Account]] = {
    withTransaction(
      { connection: Connection ⇒
        val query = updateAccountSql(generateColumnsToSet(accountToUpdate), uuid)
        val result = query.executeUpdate()(maybeTransaction.getOrElse(connection))

        if (result > 0) {
          findByIdInternal(uuid)(maybeTransaction.getOrElse(connection)).map { account ⇒
            kafkaDBSyncService.sendUpdate(TableName, account)
            account
          }
        } else None
      },
      "Unexpected exception in accounts.update", {
        case e: SQLException ⇒
          val errorMessage = s"Could not update account with uuid ${uuid}"
          logger.error(errorMessage, e)
          constraintViolationError(errorMessage)
      })
  }

  def updateAccountByCriteria(criteria: AccountCriteria, account: AccountToUpdate)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Int] = ???

  def updateAccountByAccountNumber(accountNumber: String, accountToUpdate: AccountToUpdate)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Option[Account]] = ???

  def deleteAccount(uuid: String)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Boolean] = ???

  def deleteAccountByCriteria(criteria: AccountCriteria)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Int] = ???

}

object AccountSqlDao {

  import tech.pegb.backoffice.dao.customer.sql.IndividualUserSqlDao

  final val TableName = "accounts"
  final val CurrencyTblName = "currencies"

  final val TableAlias = "a"
  final val CurrencyTblAlias = "c"

  final val cId = "id"
  final val cUuid = "uuid"
  final val cNumber = "number"
  final val cUserId = "user_id"
  final val cName = "name"
  final val cAccountTypeId = "account_type_id"
  final val cIsMainAccount = "is_main_account"

  final val cCurrencyId = "currency_id"
  final val CurrencyTblCurrencyCode = "currency_name"
  final val CurrencyTblId = "id"

  final val cBalance = "balance"
  final val cBlockedBalance = "blocked_balance"
  final val cStatus = "status"
  final val cClosedAt = "closed_at"
  final val cLastTransactionAt = "last_transaction_at"
  final val cMainType = "main_type"
  final val cCreatedAt = "created_at"
  final val cCreatedBy = "created_by"
  final val cUpdatedAt = "updated_at"
  final val cUpdatedBy = "updated_by"
  final val cIsActive = "is_active"
  //join columns
  private[dao] final val userUuid = "user_uuid"

  private[dao] final val TableFields: Seq[String] =
    Seq(cUuid, cNumber, cUserId, cName, cAccountTypeId, cIsMainAccount, cCurrencyId, cBalance, cBlockedBalance, cStatus,
      cClosedAt, cLastTransactionAt, cMainType, cCreatedAt, cCreatedBy, cUpdatedAt, cUpdatedBy)
  private[dao] final val TableFieldsStr = TableFields.mkString(",")

  private[dao] def insertSql(accountToInsert: AccountToInsert) = {
    SQL(
      s"""INSERT INTO $TableName ($TableFieldsStr) VALUES ({$cUuid}, {$cNumber}, {$cUserId}, {$cName}, {$cAccountTypeId}, {$cIsMainAccount},
         | {$cCurrencyId}, {$cBalance},{$cBlockedBalance}, {$cStatus}, {$cClosedAt}, {$cLastTransactionAt}, {$cMainType}, {$cCreatedAt},
         |{$cCreatedBy}, {$cUpdatedAt}, {$cUpdatedBy});""".stripMargin)
  }

  private final def findByInternalIdSet =
    SQL(s"$qCommonSelect WHERE ${AccountSqlDao.TableAlias}.$cId in ({ids});")

  private final def findByIdSql = SQL(s"$qCommonSelect WHERE ${AccountSqlDao.TableAlias}.$cUuid = {uuid};")

  private final def findByUserIdSql =
    SQL(s"$qCommonSelect WHERE ${UserSqlDao.TableAlias}.${UserSqlDao.uuid} = {user_id};")

  private final def findByAccountNumberSql =
    SQL(s"$qCommonSelect WHERE $TableAlias.$cNumber = {$cNumber};")

  private def findByIdInternalSet(ids: Set[Int])(implicit connection: Connection) =
    findByInternalIdSet.on('ids → ids)
      .as(findByInternalIdSet.defaultParser.*)
      .map(row ⇒ convertRowToAccount(row))

  private def findByIdInternal(uuid: String)(implicit connection: Connection) = {
    findByIdSql.on('uuid → uuid)
      .as(findByIdSql.defaultParser.*)
      .map(row ⇒ convertRowToAccount(row)).headOption
  }

  private def findByUserIdInternal(userId: String)(implicit connection: Connection) =
    findByUserIdSql.on('user_id → userId)
      .as(findByUserIdSql.defaultParser.*)
      .map(row ⇒ convertRowToAccount(row)).toSet

  private val qCommonSelect =
    s"""SELECT $TableAlias.*,
       |${UserSqlDao.TableAlias}.${UserSqlDao.uuid} AS $userUuid,
       |${UserSqlDao.TableAlias}.${UserSqlDao.typeName},
       |${UserSqlDao.TableAlias}.${UserSqlDao.username},
       |${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cName},
       |${AccountTypesSqlDao.TableAlias}.${AccountTypesSqlDao.cAccountType},
       |${IndividualUserSqlDao.TableAlias}.${IndividualUserSqlDao.name},
       |${IndividualUserSqlDao.TableAlias}.${IndividualUserSqlDao.fullName},
       |${IndividualUserSqlDao.TableAlias}.${IndividualUserSqlDao.msisdn},
       |${BusinessUserSqlDao.TableAlias}.${BusinessUserSqlDao.cBusinessName},
       |${BusinessUserSqlDao.TableAlias}.${BusinessUserSqlDao.cBrandName}
       |$qCommonJoin
     """.stripMargin

  private def findAccountsByCriteria(
    filters: String,
    maybeOrderBy: Option[OrderingSet],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): SqlQuery = {
    val pagination = SqlDao.getPagination(maybeLimit, maybeOffset)

    val ordering = maybeOrderBy.fold("")(_.toString)

    val query = SQL(
      s"""$qCommonSelect
         |$filters $ordering $pagination""".stripMargin)

    query
  }

  lazy val qCommonJoin: String =
    s"""
       |FROM $TableName $TableAlias
       |JOIN ${UserSqlDao.TableName} ${UserSqlDao.TableAlias} ON $TableAlias.$cUserId=${UserSqlDao.TableAlias}.${UserSqlDao.id}
       |LEFT JOIN ${IndividualUserSqlDao.TableName} ${IndividualUserSqlDao.TableAlias}
       |ON ${IndividualUserSqlDao.TableAlias}.${IndividualUserSqlDao.userId} = ${UserSqlDao.TableAlias}.${UserSqlDao.id}
       |LEFT JOIN ${BusinessUserSqlDao.TableName} ${BusinessUserSqlDao.TableAlias}
       |ON ${BusinessUserSqlDao.TableAlias}.${BusinessUserSqlDao.cUserId} = ${UserSqlDao.TableAlias}.${UserSqlDao.id}
       |LEFT JOIN ${AccountTypesSqlDao.TableName} ${AccountTypesSqlDao.TableAlias}
       |ON $TableAlias.account_type_id = ${AccountTypesSqlDao.TableAlias}.id
       |INNER JOIN $CurrencyTblName $CurrencyTblAlias ON $TableAlias.currency_id = $CurrencyTblAlias.id
     """.stripMargin

  private def findAccountsCountByCriteria(filters: String): SqlQuery = {
    SQL(
      s"""SELECT COUNT(*) as n $qCommonJoin
            $filters;""".stripMargin)
  }

  private def updateAccountSql(setValues: String, uuid: String) = {
    SQL(s"UPDATE $TableName $TableAlias SET $setValues WHERE $TableAlias.$cUuid='$uuid'")

  }

  private def convertRowToAccount(row: Row) = {

    val username = row[Option[String]](s"${UserSqlDao.TableName}.${UserSqlDao.username}")
    val individualUserNameOption = row[Option[String]](s"${IndividualUserSqlDao.TableName}.${IndividualUserSqlDao.name}")
    val individualUserFullNameOption = row[Option[String]](s"${IndividualUserSqlDao.TableName}.${IndividualUserSqlDao.fullName}")
    val businessUserBusinessNameOption = row[Option[String]](s"${BusinessUserSqlDao.TableName}.${BusinessUserSqlDao.cBusinessName}")
    val businessUserBrandNameOption = row[Option[String]](s"${BusinessUserSqlDao.TableName}.${BusinessUserSqlDao.cBrandName}")

    //TODO transfer this to dao.domain.mapping
    val customerName =
      (businessUserBusinessNameOption, individualUserFullNameOption.orElse(individualUserNameOption)) match {
        case (Some(_), None) ⇒ businessUserBusinessNameOption
        case (None, Some(_)) ⇒ individualUserFullNameOption.orElse(individualUserNameOption)
        case _ ⇒ username
      }

    Account(
      id = row[Int](cId),
      uuid = row[String](s"${AccountSqlDao.TableName}.${AccountSqlDao.cUuid}"),
      accountNumber = row[String](s"${AccountSqlDao.TableName}.${AccountSqlDao.cNumber}"),
      userId = row[Int](s"${AccountSqlDao.TableName}.${AccountSqlDao.cUserId}"),
      userUuid = row[String](s"${UserSqlDao.TableName}.${UserSqlDao.uuid}"),
      userType = row[String](s"${UserSqlDao.TableName}.${UserSqlDao.typeName}"),
      anyCustomerName = customerName,
      userName = username,
      brandName = businessUserBrandNameOption,
      individualUserName = individualUserNameOption,
      individualUserFullName = individualUserFullNameOption,
      msisdn = row[Option[String]](s"${IndividualUserSqlDao.TableName}.${IndividualUserSqlDao.msisdn}"),
      accountName = row[String](s"${AccountSqlDao.TableName}.${AccountSqlDao.cName}"),
      accountType = row[String](s"${AccountTypesSqlDao.TableName}.${AccountTypesSqlDao.cAccountType}"),
      isMainAccount = row[Option[Int]](s"${AccountSqlDao.TableName}.${AccountSqlDao.cIsMainAccount}").map(_.toBoolean),
      currency = row[String](s"$CurrencyTblName.${CurrencySqlDao.cName}"),
      balance = row[Option[BigDecimal]](s"${AccountSqlDao.TableName}.${AccountSqlDao.cBalance}"),
      blockedBalance = row[Option[BigDecimal]](s"${AccountSqlDao.TableName}.${AccountSqlDao.cBlockedBalance}"),
      status = row[Option[String]](s"${AccountSqlDao.TableName}.${AccountSqlDao.cStatus}"),
      closedAt = row[Option[LocalDateTime]](s"${AccountSqlDao.TableName}.${AccountSqlDao.cClosedAt}"),
      lastTransactionAt = row[Option[LocalDateTime]](s"${AccountSqlDao.TableName}.${AccountSqlDao.cLastTransactionAt}"),
      mainType = row[String](s"${AccountSqlDao.TableName}.${AccountSqlDao.cMainType}"),
      createdAt = row[LocalDateTime](s"${AccountSqlDao.TableName}.${AccountSqlDao.cCreatedAt}"),
      createdBy = row[String](s"${AccountSqlDao.TableName}.${AccountSqlDao.cCreatedBy}"),
      updatedAt = row[Option[LocalDateTime]](s"${AccountSqlDao.TableName}.${AccountSqlDao.cUpdatedAt}"),
      updatedBy = row[Option[String]](s"${AccountSqlDao.TableName}.${AccountSqlDao.cUpdatedBy}"))
  }

  private def convertRowToCount(row: Row): Int = row[Int]("n")

  private def buildParametersForAccount(
    accountUUID: String,
    accountUserId: Int,
    accountToInsert: AccountToInsert,
    accountTypeIdValue: Int,
    currencyIdValue: Int): Seq[NamedParameter] =
    Seq[NamedParameter](
      cUuid → accountUUID,
      cNumber → accountToInsert.accountNumber,
      cUserId → accountUserId,
      cName → accountToInsert.accountName,
      cAccountTypeId → accountTypeIdValue,
      cIsMainAccount → accountToInsert.isMainAccount,
      cCurrencyId → currencyIdValue,
      cBalance → accountToInsert.balance,
      cBlockedBalance → accountToInsert.blockedBalance,
      cStatus → accountToInsert.status,
      cClosedAt → Option.empty[LocalDateTime],
      cLastTransactionAt → Option.empty[LocalDateTime],
      cMainType → accountToInsert.mainType,
      cCreatedAt → accountToInsert.createdAt,
      cCreatedBy → accountToInsert.createdBy,
      cUpdatedAt → accountToInsert.createdAt, //not nullable in db and same as created at on insertion
      cUpdatedBy → accountToInsert.createdBy) //not nullable in db and same as created by on insertion

  private def generateWhereFilter(maybeCriteria: Option[AccountCriteria]) = {
    import SqlDao._

    maybeCriteria match {
      case Some(criteria) ⇒
        val anyCustomerName = criteria.anyCustomerName.map { name ⇒
          s"(${name.toSql(UserSqlDao.username.some, UserSqlDao.TableAlias.some)} OR " +
            s"${name.toSql(IndividualUserSqlDao.name.some, IndividualUserSqlDao.TableAlias.some)} OR " +
            s"${name.toSql(IndividualUserSqlDao.fullName.some, IndividualUserSqlDao.TableAlias.some)} OR " +
            s"${name.toSql(BusinessUserSqlDao.cBusinessName.some, BusinessUserSqlDao.TableAlias.some)} OR " +
            s"${name.toSql(BusinessUserSqlDao.cBrandName.some, BusinessUserSqlDao.TableAlias.some)} )"
        }
        Seq(
          anyCustomerName,
          criteria.userId.map(_.toSql(UserSqlDao.uuid.some, UserSqlDao.TableAlias.some)),
          criteria.individualUserFullName.map(_.toSql(IndividualUserSqlDao.fullName.some, IndividualUserSqlDao.TableAlias.some)),
          criteria.msisdn.map(_.toSql(IndividualUserSqlDao.msisdn.some, IndividualUserSqlDao.TableAlias.some)),
          criteria.isMainAccount.map(_.toSql(AccountSqlDao.cIsMainAccount.some, AccountSqlDao.TableAlias.some)),
          criteria.status.map(_.toSql(AccountSqlDao.cStatus.some, AccountSqlDao.TableAlias.some)),
          criteria.currency.map(_.toSql(CurrencySqlDao.cName.some, CurrencySqlDao.TableAlias.some)),
          criteria.accountType.map(_.toSql(AccountTypesSqlDao.cAccountType.some, AccountTypesSqlDao.TableAlias.some)),
          criteria.accountNumber.map(_.toSql(AccountSqlDao.cNumber.some, AccountSqlDao.TableAlias.some)),
          criteria.accountNumbers.map(_.toSql(AccountSqlDao.cNumber.some, AccountSqlDao.TableAlias.some)),
          criteria.accountIds.map(_.toSql(AccountSqlDao.cId.some, AccountSqlDao.TableAlias.some)),
          criteria.createdDateRange.map(_.toSql(AccountSqlDao.cCreatedAt.some, AccountSqlDao.TableAlias.some)),
          criteria.updatedDateRange.map(_.toSql(AccountSqlDao.cUpdatedAt.some, AccountSqlDao.TableAlias.some)),
          criteria.createdBy.map(_.toSql(AccountSqlDao.cCreatedBy.some, AccountSqlDao.TableAlias.some)),
          criteria.updatedBy.map(_.toSql(AccountSqlDao.cUpdatedBy.some, AccountSqlDao.TableAlias.some))).flatten
          .toSql

      case None ⇒ ""
    }
  }

  private def generateColumnsToSet(accountToUpdate: AccountToUpdate): String = {
    Seq(
      accountToUpdate.accountName.map(queryConditionClause(_, cName, Some(TableAlias))),
      accountToUpdate.accountNumber.map(queryConditionClause(_, cNumber, Some(TableAlias))),
      accountToUpdate.accountType.map { accountTypeValue ⇒
        SqlDao.subQueryConditionClause(
          fieldValue = accountTypeValue,
          fieldName = cAccountTypeId,
          tableName = TableAlias,
          refFieldName = cId,
          conditionalFieldName = AccountTypesSqlDao.cAccountType,
          refTableName = AccountTypesSqlDao.TableName)
      },
      accountToUpdate.balance.map(queryConditionClause(_, cBalance, Some(TableAlias))),
      accountToUpdate.blockedBalance.map(queryConditionClause(_, cBlockedBalance, Some(TableAlias))),
      accountToUpdate.currency.map { currencyValue ⇒

        SqlDao.subQueryConditionClause(
          fieldValue = currencyValue,
          fieldName = cCurrencyId,
          tableName = TableAlias,
          refFieldName = cId,
          conditionalFieldName = CurrencySqlDao.cName,
          refTableName = CurrencyTblName)
      },
      accountToUpdate.isMainAccount.map(queryConditionClause(_, cIsMainAccount, Some(TableAlias))),
      accountToUpdate.status.map(queryConditionClause(_, cStatus, Some(TableAlias))),
      Some(queryConditionClause(accountToUpdate.updatedAt, cUpdatedAt, Some(TableAlias))),
      Some(queryConditionClause(accountToUpdate.updatedBy, cUpdatedBy, Some(TableAlias))))
      .flatten.mkString(", ")
  }

}
