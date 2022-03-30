package tech.pegb.backoffice.dao.transaction.sql

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import anorm.{Row, SQL, SqlQuery}
import com.google.inject.Inject
import play.api.db.DBApi
import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.account.sql.AccountSqlDao.{CurrencyTblAlias, CurrencyTblName}
import tech.pegb.backoffice.dao.account.sql.{AccountSqlDao, AccountTypesSqlDao}
import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao
import tech.pegb.backoffice.dao.customer.sql.{BusinessUserSqlDao, IndividualUserSqlDao, UserSqlDao}
import tech.pegb.backoffice.dao.model.{GroupingField, Ordering}
import tech.pegb.backoffice.dao.provider.entity.Provider
import tech.pegb.backoffice.dao.provider.sql.ProviderSqlDao
import tech.pegb.backoffice.dao.sql.MostRecentUpdatedAtGetter
import tech.pegb.backoffice.dao.transaction.abstraction.TransactionDao
import tech.pegb.backoffice.dao.transaction.dto.{TransactionAggregation, TransactionCriteria}
import tech.pegb.backoffice.dao.transaction.entity
import tech.pegb.backoffice.dao.transaction.entity.Transaction
import tech.pegb.backoffice.util.AppConfig
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

class TransactionSqlDao @Inject() (
    val dbApi: DBApi,
    config: AppConfig)
  extends TransactionDao with MostRecentUpdatedAtGetter[Transaction, TransactionCriteria] with SqlDao {

  import SqlDao._
  import TransactionSqlDao._

  protected def getUpdatedAtColumn: String = s"${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cUpdatedAt}"

  protected def getMainSelectQuery: String = TransactionSqlDao.qCommonSelectJoin

  protected def getRowToEntityParser: Row ⇒ Transaction = (arg: Row) ⇒ TransactionSqlDao.convertRowToTransactionNew(arg)

  protected def getWhereFilterFromCriteria(criteriaDto: Option[TransactionCriteria]): String = where(criteriaDto)

  def countTotalTransactionsByCriteria(criteria: TransactionCriteria): DaoResponse[Int] = withConnection({ implicit connection ⇒
    val whereFilter = where(criteria.toOption)
    val countByCriteriaSql = countTransactionsByCriteriaNew(whereFilter)

    countByCriteriaSql
      .as(countByCriteriaSql.defaultParser.singleOpt)
      .map(row ⇒ math.min(convertRowToCount(row), config.PaginationMaxLimit)).getOrElse(0)
  }, s"Error while retrieving count by criteria:$criteria")

  def sumTotalTransactionsByCriteria(criteria: TransactionCriteria): DaoResponse[BigDecimal] = withConnection({ implicit connection ⇒
    val whereFilter = where(criteria.toOption)
    val sumByCriteriaSql = sumTransactionsByCriteria(whereFilter)

    sumByCriteriaSql
      .as(sumByCriteriaSql.defaultParser.singleOpt)
      .map(row ⇒ convertRowToSum(row)).getOrElse(0)
  }, s"Error while retrieving count by criteria:$criteria")

  def getTransactionsByCriteria(
    criteria: TransactionCriteria,
    orderBy: Seq[Ordering] = Nil,
    limit: Option[Int] = None, offset: Option[Int] = None): DaoResponse[Seq[Transaction]] = withConnection({ implicit connection ⇒
    val whereFilter = where(criteria.toOption)
    val controlledLimit = limit.map(lim ⇒ math.min(lim, config.PaginationMaxLimit))
    val transactionsByCriteriaSql = findTransactionsByCriteria(whereFilter, orderBy, controlledLimit, offset)

    transactionsByCriteriaSql
      .as(transactionsByCriteriaSql.defaultParser.*)
      .map(convertRowToTransactionNew)

  }, s"Error while retrieving transactions by criteria:$criteria")

  def getOnFlyAggregation(criteria: TransactionCriteria, isLiability: Boolean): DaoResponse[Option[(BigDecimal, BigDecimal, BigDecimal)]] = withConnection({ implicit connection ⇒
    val filter = where(criteria.toOption)
    val sqlQuery = if (isLiability) onFlyAggregation(filter, "credit", "debit")
    else onFlyAggregation(filter, "debit", "credit")

    sqlQuery
      .as(sqlQuery.defaultParser.singleOpt)
      .flatMap(row ⇒ Try {
        (row[BigDecimal](cInflow), row[BigDecimal](cOutFlow), row[BigDecimal](cNet))
      }.toOption)

  }, s"Error while performing getOnFlyAggregation by criteria:$criteria")

  def getTransactionsByTxnId(txnId: String): DaoResponse[Seq[Transaction]] = {
    withConnection({ implicit connection ⇒
      val query = SQL(
        s"""
           |$qCommonSelectJoin WHERE $TableAlias.$cId = {$cId};
       """.stripMargin)
        .on(cId → txnId)

      query.as(query.defaultParser.*).map(convertRowToTransactionNew)

    }, s"Error while retrieving transactions by id:$txnId")

  }

  def getTransactionsByUniqueId(uniqueId: String): DaoResponse[Option[Transaction]] = {
    withConnection({ implicit connection ⇒
      val query = SQL(
        s"""
           |$qCommonSelectJoin WHERE $TableAlias.$cUniqueId = {$cUniqueId};
       """.stripMargin)
        .on(cId → uniqueId)

      query.as(query.defaultParser.*).map(convertRowToTransactionNew).headOption

    }, s"Error while retrieving transactions by id:$uniqueId")

  }

  override def aggregateTransactionByCriteriaAndPivots(
    criteria: TransactionCriteria,
    grouping: Seq[GroupingField]): DaoResponse[Seq[TransactionAggregation]] = {
    val whereFilter = where(criteria.toOption)
    val minimalWhereFilter = where(TransactionCriteria(
      createdAt = criteria.createdAt).toOption)
    val groupingString = groupBy(TableAlias, grouping)
    val selectWithGroupingString = selectWithGroups(TableAlias, grouping)
    withConnection({ implicit connection ⇒
      val query = SQL(
        s"""SELECT $selectWithGroupingString,
           |COUNT($TableAlias.$cId) as $cCount,
           |SUM($TableAlias.$cAmount) as $cSum
           |FROM $TableName $TableAlias
           |LEFT OUTER JOIN (
           |	SELECT $TableAlias.$cId, ${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cName} as $caExCurrency
           |  FROM $TableName $TableAlias JOIN ${CurrencySqlDao.TableName} ${CurrencySqlDao.TableAlias}
           |  on ${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cId} = $TableAlias.$cCurrencyId
           |  $minimalWhereFilter AND $cSequence = 1
           |) ${TableAlias}1 on $TableAlias.$cId = ${TableAlias}1.$cId
           |JOIN $CurrencyTblName $CurrencyTblAlias ON $TableAlias.$cCurrencyId = $CurrencyTblAlias.id
           |$whereFilter
           |$groupingString
          """.stripMargin)
      query.as(query.defaultParser.*).map(convertRowToTransactionAggr)
    }, s"Error while retrieving transactions aggregations")
  }
}

object TransactionSqlDao {

  import AccountSqlDao._
  import SqlDao._

  final val TableName = "transactions"
  final val TableAlias = "tx"

  final val cId = "id"
  final val cUniqueId = "unique_id"
  final val cSequence = "sequence"
  final val cPrimaryAccountId = "primary_account_id"
  final val cPrimaryAccountUuid = "primary_account_uuid"
  final val cPrimaryAccountName = "primary_account_name"
  final val cPrimaryAccountNumber = "primary_account_number"
  final val cPrimaryAccountUserId = "primary_account_user_id"
  final val cPrimaryUserUuid = "primary_user_uuid"
  final val cPrimaryAccountMainType = "primary_account_main_type"
  final lazy val cPrimaryUserUsername = UserSqlDao.username
  final lazy val cBusinessName = BusinessUserSqlDao.cBusinessName
  final lazy val cBrandName = BusinessUserSqlDao.cBrandName
  final lazy val cPrimaryIndividualUserName = IndividualUserSqlDao.name
  final lazy val cPrimaryIndividualUserFullname = IndividualUserSqlDao.fullName

  final val cSecondaryAccountId = "secondary_account_id"
  final val cSecondaryAccountUuid = "secondary_account_uuid"
  final val cSecondaryAccountName = "secondary_account_name"
  final val cSecondaryAccountNumber = "secondary_account_number"
  final val cSecondaryUserUuid = "secondary_user_uuid"
  final val cPrimaryAccountType = "primary_account_type"

  final val cPrimaryAccountPrevBal = "primary_account_previous_balance"
  final val cSecondaryAccountPrevBal = "secondary_account_previous_balance"

  final val cDirection = "direction"
  final val cType = "type"
  final val cAmount = "amount"
  final val cCurrencyId = "currency_id"
  final val cChannel = "channel"
  final val cExplanation = "explanation"
  final val cEffectiveRate = "effective_rate"
  final val cCostRate = "cost_rate"
  final val cStatus = "status"
  final val cCreatedAt = "created_at"

  final val cUpdatedAt = "updated_at"

  final val cInstrument = "instrument"

  final val caCurrency = "currency"
  final val caExCurrency = "exchanged_currency"

  final val cOtherParty = "other_party"
  final val cProviderId = "provider_id"
  final val cProviderName = "provider_name"

  final val cDashboardRevenue = "dashboard_revenue"

  //onfly agg columns
  private final val cInflow = "inflow"
  private final val cOutFlow = "outflow"
  private final val cNet = "net"

  private def countTransactionsByCriteriaNew(filters: String): SqlQuery = {
    SQL(
      s"""SELECT COUNT(*) as n
         |$qCommonJoin
         |$filters;""".stripMargin)
  }

  private def sumTransactionsByCriteria(filters: String): SqlQuery = {
    SQL(
      s"""SELECT SUM($cAmount) as n
         |$qCommonJoin
         |$filters;""".stripMargin)
  }

  private def convertRowToCount(row: Row): Int = row[Int]("n")

  private def convertRowToSum(row: Row): BigDecimal = row[BigDecimal]("n")

  private val qCommonJoin =
    s"""
       |FROM $TableName $TableAlias
       |
       |JOIN ${AccountSqlDao.TableName} ${AccountSqlDao.TableAlias}1 ON
       |${AccountSqlDao.TableAlias}1.${AccountSqlDao.cId} = $TableAlias.$cPrimaryAccountId
       |JOIN ${AccountTypesSqlDao.TableName} ${AccountTypesSqlDao.TableAlias} ON
       |${AccountTypesSqlDao.TableAlias}.${AccountTypesSqlDao.cId} = ${AccountSqlDao.TableAlias}1.${AccountSqlDao.cAccountTypeId}
       |JOIN ${UserSqlDao.TableName}  ${UserSqlDao.TableAlias}1 ON
       |${UserSqlDao.TableAlias}1.${UserSqlDao.id} = ${AccountSqlDao.TableAlias}1.${AccountSqlDao.cUserId}
       |
       |LEFT JOIN ${IndividualUserSqlDao.TableName} ${IndividualUserSqlDao.TableAlias} ON
       |${UserSqlDao.TableAlias}1.${UserSqlDao.id} = ${IndividualUserSqlDao.TableAlias}.${IndividualUserSqlDao.userId}
       |
       |LEFT JOIN ${BusinessUserSqlDao.TableName} ${BusinessUserSqlDao.TableAlias} ON
       |${UserSqlDao.TableAlias}1.${UserSqlDao.id} = ${BusinessUserSqlDao.TableAlias}.${BusinessUserSqlDao.cUserId}
       |
       |JOIN ${AccountSqlDao.TableName} ${AccountSqlDao.TableAlias}2 ON
       |${AccountSqlDao.TableAlias}2.${AccountSqlDao.cId} = $TableAlias.$cSecondaryAccountId
       |JOIN ${UserSqlDao.TableName} ${UserSqlDao.TableAlias}2 ON
       |${UserSqlDao.TableAlias}2.${UserSqlDao.id} = ${AccountSqlDao.TableAlias}2.${AccountSqlDao.cUserId}
       |
       |JOIN ${CurrencySqlDao.TableName} ${CurrencySqlDao.TableAlias} ON
       |$TableAlias.$cCurrencyId = ${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cId}
       |
       |LEFT JOIN ${ProviderSqlDao.TableName} ${ProviderSqlDao.TableAlias}
       |ON $TableAlias.$cProviderId = ${ProviderSqlDao.TableAlias}.${Provider.cId}
       |
     """.stripMargin

  private val qCommonSelectJoin =
    s"""
       |SELECT $TableAlias.*,
       |${UserSqlDao.TableAlias}1.${UserSqlDao.uuid} as $cPrimaryUserUuid,
       |${UserSqlDao.TableAlias}2.${UserSqlDao.uuid} as $cSecondaryUserUuid,
       |
       |${UserSqlDao.TableAlias}1.${UserSqlDao.username},
       |${UserSqlDao.TableAlias}1.${UserSqlDao.typeName},
       |${IndividualUserSqlDao.TableAlias}.${IndividualUserSqlDao.name},
       |${IndividualUserSqlDao.TableAlias}.${IndividualUserSqlDao.fullName},
       |${BusinessUserSqlDao.TableAlias}.${BusinessUserSqlDao.cBusinessName},
       |${BusinessUserSqlDao.TableAlias}.${BusinessUserSqlDao.cBrandName},
       |
       |${AccountSqlDao.TableAlias}1.${AccountSqlDao.cUuid} as $cPrimaryAccountUuid,
       |${AccountSqlDao.TableAlias}2.${AccountSqlDao.cUuid} as $cSecondaryAccountUuid,
       |${AccountSqlDao.TableAlias}1.${AccountSqlDao.cName} as $cPrimaryAccountName,
       |${AccountSqlDao.TableAlias}2.${AccountSqlDao.cName} as $cSecondaryAccountName,
       |${AccountSqlDao.TableAlias}1.${AccountSqlDao.cNumber} as $cPrimaryAccountNumber,
       |${AccountSqlDao.TableAlias}2.${AccountSqlDao.cNumber} as $cSecondaryAccountNumber,
       |${AccountTypesSqlDao.TableAlias}.${AccountTypesSqlDao.cAccountType} as $cPrimaryAccountType,
       |$CurrencyTblAlias.${CurrencySqlDao.cName} as $caCurrency,
       |${ProviderSqlDao.TableAlias}.${Provider.cName} as $cProviderName
       |
       |$qCommonJoin
     """.stripMargin

  private def findTransactionsByCriteria(
    filters: String,
    maybeOrderBy: Seq[Ordering],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): SqlQuery = {

    val ordering = maybeOrderBy.map({
      case ord if ord.field === "other_party" ⇒
        ord.copy(field = s"${ProviderSqlDao.TableAlias}.${Provider.cName}").toString
      case ord ⇒ ord.toString
    }).mkStringOrEmpty(" ORDER BY ", ", ", "")

    val pagination = SqlDao.getPagination(maybeLimit, maybeOffset)

    SQL(
      s"""
         |$qCommonSelectJoin
         |$filters $ordering $pagination""".stripMargin)
  }

  def where(maybeCriteria: Option[TransactionCriteria]): String = {
    val str = mapCriteriaFieldsToQuery(maybeCriteria)
    if (str.hasSomething) { //TODO refactor to something like  maybeCriteria.fold( ... )
      " WHERE " + str
    } else str
  }

  def mapCriteriaFieldsToQuery(maybeCriteria: Option[TransactionCriteria]): String = {
    maybeCriteria.map { criteria ⇒
      val strForAND = Seq[Option[String]](
        criteria.id.map(_.toSql(Some(cId), Some(TableAlias))),

        criteria.customerId.map(_.toSql(Some(UserSqlDao.uuid), Some(s"${UserSqlDao.TableAlias}1"))),
        criteria.accountId
          .map(_.toSql(Some(AccountSqlDao.cUuid), Some(s"${AccountSqlDao.TableAlias}1"))),
        criteria.currencyCode
          .map(_.toSql(Some(CurrencySqlDao.cName), Some(CurrencySqlDao.TableAlias))),
        criteria.accountNumbers
          .map(_.toSql(Some(AccountSqlDao.cNumber), Some(s"${AccountSqlDao.TableAlias}1"))),
        criteria.createdAt.map { c ⇒
          val formattedTime = c.value match {
            case (from: LocalDateTime, to: LocalDateTime) ⇒
              (from.format(DateTimeFormatter.ISO_DATE_TIME), to.format(DateTimeFormatter.ISO_DATE_TIME))
            case (created_at: LocalDateTime) ⇒
              created_at.format(DateTimeFormatter.ISO_DATE_TIME)
            case any ⇒ any
          }
          c.copy(value = formattedTime).toSql(Some(cCreatedAt), Some(TableAlias))
        },

        criteria.provider.map(_.toSql(Some(Provider.cName), Some(ProviderSqlDao.TableAlias))),

        criteria.transactionType
          .map(_.toSql(Some(cType), Some(TableAlias))),
        criteria.channel
          .map(_.toSql(Some(cChannel), Some(TableAlias))),
        criteria.status
          .map(_.toSql(Some(cStatus), Some(TableAlias))),
        criteria.direction
          .map(_.toSql(Some(cDirection), Some(TableAlias))),
        criteria.accountType
          .map(_.toSql(Some(AccountTypesSqlDao.cAccountType), Some(AccountTypesSqlDao.TableAlias)))).flatten.mkString(" AND ")

      val strForOR = Seq[Option[String]](
        criteria.primaryAccountUsersUsername.
          map(_.toSql(
            column = Some(UserSqlDao.username),
            tableAlias = Some(s"${UserSqlDao.TableAlias}1"))),

        criteria.primaryAccountIndividualUsersName.
          map(_.toSql(
            column = Some(IndividualUserSqlDao.name),
            tableAlias = Some(IndividualUserSqlDao.TableAlias))),

        criteria.primaryAccountIndividualUsersFullname
          .map(_.toSql(
            column = Some(IndividualUserSqlDao.fullName),
            tableAlias = Some(IndividualUserSqlDao.TableAlias))),

        criteria.primaryAccountBusinessUsersBusinessName
          .map(_.toSql(
            column = Some(BusinessUserSqlDao.cBusinessName),
            tableAlias = Some(BusinessUserSqlDao.TableAlias))),

        criteria.primaryAccountBusinessUsersBrandName
          .map(_.toSql(
            column = Some(BusinessUserSqlDao.cBrandName),
            tableAlias = Some(BusinessUserSqlDao.TableAlias)))).flatten.mkString(" OR ")

      //TODO better to take the actual boolean logic from CriteriaField
      //we should have an implicit for Iterable of CriteriaField .toSql

      strForAND + " " + strForOR
    }
  }.getOrElse("")

  private def onFlyAggregation(filter: String, inflowDirection: String, outFlowDirection: String) = {
    val query =
      s"""
         |SELECT SUM(CASE WHEN $TableAlias.$cDirection= '$inflowDirection' THEN $TableAlias.$cAmount ELSE 0 END) as $cInflow,
         |			 SUM(CASE WHEN $TableAlias.$cDirection='$outFlowDirection' THEN $TableAlias.$cAmount ELSE 0 END) as $cOutFlow,
         |			 (SUM(CASE WHEN $TableAlias.$cDirection ='$inflowDirection'
         |        THEN $TableAlias.$cAmount ELSE 0 END) - SUM(CASE WHEN $TableAlias.$cDirection = '$outFlowDirection'
         |        THEN $TableAlias.$cAmount ELSE 0 END)) as $cNet
         |$qCommonJoin $filter;
       """.stripMargin

    SQL(query)
  }

  private def convertRowToTransactionNew(row: Row) = {
    entity.Transaction(
      id = row[String](cId),
      uniqueId = row[Int](cUniqueId).toString(),
      sequence = row[Long](cSequence),
      primaryAccountInternalId = row[Int](cPrimaryAccountId),
      primaryAccountUuid = row[UUID](cPrimaryAccountUuid),
      primaryAccountName = row[String](cPrimaryAccountName),
      primaryAccountNumber = row[String](cPrimaryAccountNumber),
      primaryUserUuid = row[UUID](cPrimaryUserUuid),
      primaryUserType = row[Option[String]](s"${UserSqlDao.TableName}.${UserSqlDao.typeName}"),
      primaryUsername = row[Option[String]](s"${UserSqlDao.TableName}.${UserSqlDao.username}"),
      primaryIndividualUsersName = row[Option[String]](s"${IndividualUserSqlDao.TableName}.${IndividualUserSqlDao.name}"),
      primaryIndividualUsersFullname = row[Option[String]](s"${IndividualUserSqlDao.TableName}.${IndividualUserSqlDao.fullName}"),
      primaryBusinessUsersBusinessName = row[Option[String]](s"${BusinessUserSqlDao.TableName}.${BusinessUserSqlDao.cBusinessName}"),
      primaryBusinessUsersBrandName = row[Option[String]](s"${BusinessUserSqlDao.TableName}.${BusinessUserSqlDao.cBrandName}"),
      primaryAccountType = row[String](cPrimaryAccountType),
      secondaryAccountInternalId = row[Int](cSecondaryAccountId),
      secondaryAccountUuid = row[UUID](cSecondaryAccountUuid),
      secondaryAccountName = row[String](cSecondaryAccountName),
      secondaryAccountNumber = row[String](cSecondaryAccountNumber),
      secondaryUserUuid = row[UUID](cSecondaryUserUuid),
      direction = row[Option[String]](cDirection),
      `type` = row[Option[String]](s"${TableName}.${cType}"),
      amount = row[Option[BigDecimal]](cAmount),
      currency = row[Option[String]](caCurrency),
      exchangedCurrency = Try(row[Option[String]](caExCurrency).get).toOption,
      channel = row[Option[String]](cChannel),
      explanation = row[Option[String]](cExplanation),
      effectiveRate = row[Option[BigDecimal]](cEffectiveRate),
      costRate = row[Option[BigDecimal]](cCostRate),
      status = row[Option[String]](cStatus),
      instrument = row[Option[String]](cInstrument),
      createdAt = row[Option[LocalDateTime]](cCreatedAt),
      updatedAt = row[Option[LocalDateTime]](cUpdatedAt),
      primaryAccountPreviousBalance = row[Option[BigDecimal]](cPrimaryAccountPrevBal),
      secondaryAccountPreviousBalance = row[Option[BigDecimal]](cSecondaryAccountPrevBal),
      provider = row[Option[String]](cProviderName))
  }

  private def convertRowToTransactionAggr(row: Row) = {
    TransactionAggregation(
      direction = Try(row[Option[String]](cDirection).get).toOption,
      `type` = Try(row[Option[String]](cType).get).toOption,
      amount = Try(row[Option[BigDecimal]](cAmount).get).toOption,
      currency = Try(row[Option[String]](caCurrency).get).toOption,
      exchangedCurrency = Try(row[Option[String]](caExCurrency).get).toOption,
      channel = Try(row[Option[String]](cChannel).get).toOption,
      effectiveRate = Try(row[Option[BigDecimal]](cEffectiveRate).get).toOption,
      costRate = Try(row[Option[BigDecimal]](cCostRate).get).toOption,
      status = Try(row[Option[String]](cStatus).get).toOption,
      instrument = Try(row[Option[String]](cInstrument).get).toOption,
      createdAt = Try(row[Option[LocalDateTime]](cCreatedAt).get).toOption,
      date = Try(row[Option[LocalDate]](cDate).get).toOption,
      day = Try(row[Option[Int]](cDay).get).toOption,
      month = Try(row[Option[Int]](cMonth).get).toOption,
      year = Try(row[Option[Int]](cYear).get).toOption,
      hour = Try(row[Option[Int]](cHour).get).toOption,
      minute = Try(row[Option[Int]](cMinute).get).toOption,
      sum = Try(row[BigDecimal](cSum)).toOption,
      count = Try(row[Long](cCount)).toOption)
  }
}
