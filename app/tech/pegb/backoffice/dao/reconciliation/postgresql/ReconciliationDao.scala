package tech.pegb.backoffice.dao.reconciliation.postgresql

import java.sql.{Connection, SQLException}
import java.time.{LocalDate, LocalDateTime}

import anorm._
import com.google.inject.Inject
import play.api.db.DBApi
import tech.pegb.backoffice.dao.PostgresDao
import tech.pegb.backoffice.dao.account.sql.AccountSqlDao
import tech.pegb.backoffice.dao.customer.sql.{BusinessUserSqlDao, IndividualUserSqlDao, UserSqlDao}
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.reconciliation.abstraction
import tech.pegb.backoffice.dao.reconciliation.dto.{InternalReconDetailsCriteria, InternalReconSummaryCriteria}
import tech.pegb.backoffice.dao.reconciliation.model.{InternReconDailySummaryResult, InternReconDailySummaryResultToUpdate, InternReconResult}
import tech.pegb.backoffice.dao.sql.MostRecentUpdatedAtGetter
import tech.pegb.backoffice.dao.transaction.sql.TransactionSqlDao
import tech.pegb.backoffice.dao.util.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, TimeoutException}
import scala.util.Try

class ReconciliationDao @Inject() (
    config: AppConfig,
    executionContexts: WithExecutionContexts,
    val dbApi: DBApi) extends abstraction.ReconciliationDao with PostgresDao
  with MostRecentUpdatedAtGetter[InternReconDailySummaryResult, InternalReconSummaryCriteria] {

  import ReconciliationDao._

  protected def getUpdatedAtColumn: String = s"${ReconSummaryTable.TableAlias}.${ReconSummaryTable.cUpdatedAt}"

  protected def getMainSelectQuery: String = reconSelectBaseQuery

  protected def getRowToEntityParser: Row ⇒ InternReconDailySummaryResult = (row: Row) ⇒
    ReconSummaryTable.internReconSummaryResultParser(row)

  protected def getWhereFilterFromCriteria(criteriaDto: Option[InternalReconSummaryCriteria]): String =
    whereClauseForSummary(criteriaDto)

  implicit val futureTimeout: FiniteDuration = config.FutureTimeout

  implicit val ec: ExecutionContext = executionContexts.blockingIoOperations

  val schemaName: String = config.SchemaName

  val (fullReconSummaryTableName, fullReconDetailsTableName,
    fullTransactionsTableName, fullAccountsTableName,
    fullUserTableName, fullIndividualUserTableName, fullBusinessUserTableName) = if (schemaName.isEmpty) {
    (ReconSummaryTable.TableName, ReconDetailsTable.TableName,
      TransactionSqlDao.TableName, AccountSqlDao.TableName, UserSqlDao.TableName,
      IndividualUserSqlDao.TableName, BusinessUserSqlDao.TableName)
  } else {
    (s"$schemaName.${ReconSummaryTable.TableName}", s"$schemaName.${ReconDetailsTable.TableName}",
      s"$schemaName.${TransactionSqlDao.TableName}", s"$schemaName.${AccountSqlDao.TableName}",
      s"$schemaName.${UserSqlDao.TableName}", s"$schemaName.${IndividualUserSqlDao.TableName}",
      s"$schemaName.${BusinessUserSqlDao.TableName}")
  }

  val reconCountBaseQuery = s"SELECT COUNT(*) as count"
  val reconSelectBaseQuery: String =
    s"""SELECT ${ReconSummaryTable.TableAlias}.*,
       |${UserSqlDao.TableAlias}.${UserSqlDao.username},
       |${IndividualUserSqlDao.TableAlias}.${IndividualUserSqlDao.fullName},
       |${IndividualUserSqlDao.TableAlias}.${IndividualUserSqlDao.name} as ${ReconSummaryTable.individualUserNameAlias},
       |${BusinessUserSqlDao.TableAlias}.${BusinessUserSqlDao.cBusinessName} as ${ReconSummaryTable.businessUserNameAlias},
       |${BusinessUserSqlDao.TableAlias}.${BusinessUserSqlDao.cBrandName} as ${ReconSummaryTable.businessUserBrandNameAlias}""".stripMargin
  val summaryAndUserJoinBaseQuery: String =
    s"""FROM $fullReconSummaryTableName as ${ReconSummaryTable.TableAlias}
       |LEFT JOIN $fullUserTableName as ${UserSqlDao.TableAlias}
       |ON ${ReconSummaryTable.TableAlias}.${ReconSummaryTable.cUserId} = ${UserSqlDao.TableAlias}.${UserSqlDao.id}
       |LEFT JOIN $fullBusinessUserTableName as ${BusinessUserSqlDao.TableAlias}
       |ON ${ReconSummaryTable.TableAlias}.${ReconSummaryTable.cUserId} = ${BusinessUserSqlDao.TableAlias}.${BusinessUserSqlDao.cUserId}
       |LEFT JOIN $fullIndividualUserTableName as ${IndividualUserSqlDao.TableAlias}
       |ON ${ReconSummaryTable.TableAlias}.${ReconSummaryTable.cUserId} = ${IndividualUserSqlDao.TableAlias}.${IndividualUserSqlDao.userId}""".stripMargin

  def getInternReconDetailsByCriteria(
    criteria: Option[InternalReconDetailsCriteria],
    ordering: Option[OrderingSet],
    limit: Option[Int], offset: Option[Int]): Future[DaoResponse[Seq[InternReconResult]]] = Future {
    withConnection({ implicit connection ⇒
      val predicate = whereClauseForDetails(criteria)
      val ord = ordering.map(_.toString).getOrElse("")
      val pagination = getPagination(limit, offset)
      val query = SQL(
        s"""
           |SELECT * FROM $fullReconDetailsTableName as ${ReconDetailsTable.TableAlias}
           |$predicate
           |$ord $pagination
         """.stripMargin)

      query.as(ReconDetailsTable.internReconResultDetailParser.*)

    }, s"Error while retrieving internal recon details with filter $criteria")
  }.futureWithTimeout.recover(timeoutErrorHandler("getInternReconResults"))

  def countInternReconDetailsByCriteria(
    mayBeCriteria: Option[InternalReconDetailsCriteria]): Future[DaoResponse[Int]] = {
    Future {

      withConnection({ implicit connection ⇒
        val predicate = whereClauseForDetails(mayBeCriteria)

        val query = SQL(
          s"""
             |SELECT COUNT(*) as count FROM $fullReconDetailsTableName as ${ReconDetailsTable.TableAlias}
             |$predicate
         """.stripMargin)

        query.as(parseRowToCount.single)

      }, s"Error while retrieving count of internal recon details")
    }.futureWithTimeout.recover(timeoutErrorHandler("countInternReconResults"))
  }

  def getInternReconResultsBySummaryId(summaryId: String): Future[DaoResponse[Seq[InternReconResult]]] = {
    Future {
      withConnection({ implicit connection: Connection ⇒

        val query = SQL(
          s"""
             |SELECT * FROM $fullReconDetailsTableName
             |WHERE ${ReconDetailsTable.cSummaryId} = {${ReconDetailsTable.cSummaryId}}
         """.stripMargin).on(ReconDetailsTable.cSummaryId → summaryId)

        query.as(ReconDetailsTable.internReconResultDetailParser.*)

      }, s"Error while retrieving internal recon details with summaryId: $summaryId")
    }.futureWithTimeout.recover(timeoutErrorHandler("getInternReconResultsBySummaryId"))
  }

  def getInternReconDailySummaryResult(id: String): Future[DaoResponse[Option[InternReconDailySummaryResult]]] = {
    Future {
      withConnection({ implicit connection: Connection ⇒
        internalGetInternReconDailySummaryResult(id)
      }, s"Error while retrieving internal recon summary with id: $id")
    }.futureWithTimeout.recover(timeoutErrorHandler("getInternReconDailySummaryResult"))
  }

  def getInternReconDailySummaryResults(
    maybeCriteria: Option[InternalReconSummaryCriteria],
    ordering: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int]): Future[DaoResponse[Seq[InternReconDailySummaryResult]]] = {
    Future {
      withConnection({ implicit connection ⇒

        val predicate = whereClauseForSummary(maybeCriteria)
        val ord = ordering.map(_.toString).getOrElse("")
        val pagination = getPagination(limit, offset)

        val query = SQL(s"$reconSelectBaseQuery $summaryAndUserJoinBaseQuery $predicate $ord $pagination".stripMargin)

        query.as(query.defaultParser.*).map(r ⇒ ReconSummaryTable.internReconSummaryResultParser(r))

      }, s"Error while retrieving internal recon summary results by criteria: ${maybeCriteria.map(_.toSmartString)}")
    }.futureWithTimeout.recover(timeoutErrorHandler("getInternReconDailySummaryResults"))
  }

  def countInternReconDailySummaryResults(filters: Option[InternalReconSummaryCriteria]): Future[DaoResponse[Int]] = {
    Future {

      withConnection({ implicit connection ⇒
        val predicate = whereClauseForSummary(filters)

        val query = SQL(
          s"$reconCountBaseQuery $summaryAndUserJoinBaseQuery $predicate".stripMargin)

        query.as(parseRowToCount.single)

      }, s"Error while retrieving count of internal recon summary")
    }.futureWithTimeout.recover(timeoutErrorHandler("countInternReconDailySummaryResults"))
  }

  def updateInternReconDailySummaryResult(
    id: String,
    reconResultToUpdate: InternReconDailySummaryResultToUpdate): Future[DaoResponse[Option[InternReconDailySummaryResult]]] = {
    Future {
      withTransaction(
        block = { implicit cxn ⇒
          for {
            existing ← internalGetInternReconDailySummaryResult(id)
            updateResult = updateQuery(id, reconResultToUpdate).executeUpdate()
            updated ← if (updateResult > 0) {
              internalGetInternReconDailySummaryResult(id)
            } else {
              throw new IllegalStateException(s"Update failed. Internal Recon daily Summary $id has been modified by another process.")
            }
          } yield updated
        },
        errorMsg = s"Failed to Recon summary $id",
        handlerPF = {
          case e: SQLException ⇒
            val errorMessage = s"Could not update internal recon daily summary result $id"
            logger.error(errorMessage, e)
            constraintViolationError(errorMessage)
          case ie: IllegalStateException ⇒
            preconditionFailed(ie.getMessage)
        })
    }.futureWithTimeout.recover(timeoutErrorHandler("updateInternReconDailySummaryResult"))
  }

  def internalGetInternReconDailySummaryResult(id: String)(implicit cnx: Connection): Option[InternReconDailySummaryResult] = {
    val query = SQL(
      s"$reconSelectBaseQuery $summaryAndUserJoinBaseQuery WHERE" +
        s" ${ReconSummaryTable.TableAlias}.${ReconSummaryTable.cId} = {${ReconSummaryTable.cId}}".stripMargin).on(ReconSummaryTable.cId → id)

    query.as(query.defaultParser.*).map(ReconSummaryTable.internReconSummaryResultParser(_)).headOption
  }

  private def updateQuery(id: String, dto: InternReconDailySummaryResultToUpdate): SimpleSql[Row] = {
    val paramsBuffer = dto.paramsBuilder
    val filterParam = NamedParameter(ReconSummaryTable.cId, id)
    paramsBuffer += filterParam

    val preQuery = dto.createSqlString(fullReconSummaryTableName, Some(s"WHERE ${filterParam.name} = {${filterParam.name}}"))
    val params = paramsBuffer.result()
    SQL(preQuery).on(params: _*)
  }

  def timeoutErrorHandler[T](methodName: String): PartialFunction[Throwable, DaoResponse[T]] = {
    case e: TimeoutException ⇒
      logger.error(s"query timeout for ReconciliationDao.$methodName, reason ${e.getMessage}", e)
      val errorMessage = s"query timeout for ReconciliationDao.$methodName"
      Left(timeoutError(errorMessage))

    case e: Throwable ⇒
      logger.error(s"error in ReconciliationDao.$methodName, reason ${e.getMessage}", e)
      val errorMessage = s"error in ReconciliationDao.$methodName"
      Left(genericDbError(errorMessage))
  }
}

object ReconciliationDao {

  import PostgresDao._

  val accountTblAlias = "acc"

  object ReconSummaryTable {
    val TableName = "internal_recon_daily_summary"
    val TableAlias = "rs"
    val cId = "id"
    val cReconDate = "recon_date"
    val cAccountId = "account_id"
    val cAccountNumber = "account_number"
    val cAccountType = "account_type"
    val cAccountMainType = "main_account_type"
    val cUserId = "user_id"
    val cUserUuid = "user_uuid"
    val cCurrency = "currency"
    val cEndOfDayBal = "end_of_day_balance"
    val cValueChange = "value_change"
    val cTxnTotalAmt = "transaction_total_amount"
    val cTxnTotalCnt = "transaction_total_count"
    val cProblemCnt = "problematic_transaction_count"
    val cStatus = "status"
    val cComments = "comments"
    val cCreatedAt = "created_at"
    val cUpdatedAt = "updated_at"
    val cUpdatedBy = "updated_by"

    val individualUserNameAlias = "indvidual_user_name"
    val businessUserNameAlias = "business_name"
    val businessUserBrandNameAlias = "brand_name"

    def internReconSummaryResultParser(row: Row): InternReconDailySummaryResult = {
      val username = row[Option[String]](s"${UserSqlDao.username}")
      val individualUserFullNameOption = row[Option[String]](s"${IndividualUserSqlDao.fullName}")
      val individualUserNameOption = row[Option[String]](s"${ReconSummaryTable.individualUserNameAlias}")
      val businessUserCompanyNameOption = row[Option[String]](s"${ReconSummaryTable.businessUserNameAlias}")

      val customerName =
        (businessUserCompanyNameOption, individualUserFullNameOption.orElse(individualUserNameOption)) match {
          case (Some(_), None) ⇒ businessUserCompanyNameOption
          case (None, Some(_)) ⇒ individualUserFullNameOption.orElse(individualUserNameOption)
          case _ ⇒ username
        }

      InternReconDailySummaryResult(
        id = row[String](cId),
        reconDate = row[LocalDate](cReconDate),
        accountId = row[String](cAccountId),
        accountNumber = row[String](cAccountNumber),
        accountType = row[String](cAccountType),
        accountMainType = row[String](cAccountMainType),
        userId = row[Int](cUserId),
        userUuid = row[String](cUserUuid),
        userFullName = individualUserFullNameOption.orElse(individualUserNameOption),
        anyCustomerName = customerName,
        currency = row[String](cCurrency),
        endOfDayBalance = row[BigDecimal](cEndOfDayBal),
        valueChange = row[BigDecimal](cValueChange),
        transactionTotalAmount = row[BigDecimal](cTxnTotalAmt),
        transactionTotalCount = row[Int](cTxnTotalCnt),
        problematicTxnCount = row[Int](cProblemCnt),
        status = row[String](cStatus),
        comments = row[Option[String]](cComments),
        updatedAt = row[Option[LocalDateTime]](cUpdatedAt),
        updatedBy = row[Option[String]](cUpdatedBy))
    }

  }

  object ReconDetailsTable {
    val TableName = "internal_recon_daily_details"
    val TableAlias = "rd"
    val cId = "id"
    val cSummaryId = "internal_reconciliation_summary_id"
    val cReconDate = "recon_date"
    val cAccountId = "account_id"
    val cAccountNumber = "account_number"
    val cCurrency = "currency"
    val cCurrentTxnId = "current_txn_id"
    val cCurrentTxnSequence = "current_txn_sequence"
    val cCurrentTxnDirection = "current_txn_direction"
    val cCurrentTxnTimestamp = "current_txn_timestamp"
    val cCurrentTxnAmount = "current_txn_amount"
    val cCurrentTxnPrevBal = "current_txn_previous_balance"

    val cPrevTxnId = "previous_txn_id"
    val cPrevTxnSequence = "previous_txn_sequence"
    val cPrevTxnDirection = "previous_txn_direction"
    val cPrevTxnTimestamp = "previous_txn_timestamp"
    val cPrevTxnAmount = "previous_txn_amount"
    val cPrevTxnPrevBal = "previous_txn_previous_balance"
    val cReconStatus = "recon_status"
    val cCreatedAt = "created_at"

    val internReconResultDetailParser: RowParser[InternReconResult] = row ⇒ Try {
      InternReconResult(
        id = row[String](cId),
        internReconSummaryResultId = row[String](cSummaryId),
        reconDate = row[LocalDate](cReconDate),
        accountId = row[String](cAccountId),
        accountNumber = row[String](cAccountNumber),
        currency = row[String](cCurrency),
        currentTransactionId = row[Long](cCurrentTxnId),
        currentTxnSequence = row[Int](cCurrentTxnSequence),
        currentTxnDirection = row[String](cCurrentTxnDirection),
        currentTxnTimestamp = row[LocalDateTime](cCurrentTxnTimestamp),
        currentTxnAmount = row[BigDecimal](cCurrentTxnAmount),
        currentTxnPreviousBalance = row[Option[BigDecimal]](cCurrentTxnPrevBal),

        previousTransactionId = row[Option[Long]](cPrevTxnId),
        previousTxnSequence = row[Option[Int]](cPrevTxnSequence),
        previousTxnDirection = row[Option[String]](cPrevTxnDirection),
        previousTxnTimestamp = row[Option[LocalDateTime]](cPrevTxnTimestamp),
        previousTxnAmount = row[Option[BigDecimal]](cPrevTxnAmount),
        previousTxnPreviousBalance = row[Option[BigDecimal]](cPrevTxnPrevBal),
        reconStatus = row[String](cReconStatus))
    }.fold(
      exc ⇒ anorm.Error(SqlRequestError(exc)),
      anorm.Success(_))
  }

  val parseRowToCount: RowParser[Int] = row ⇒ Try {
    row[Int]("count")
  }.fold(
    exc ⇒ anorm.Error(SqlRequestError(exc)),
    anorm.Success(_))

  def getPagination(maybeLimit: Option[Int], maybeOffset: Option[Int]): String =
    (maybeLimit, maybeOffset) match {
      case (Some(limit), Some(offset)) ⇒
        s"LIMIT $limit OFFSET $offset"
      case (Some(limit), None) ⇒
        s"LIMIT $limit OFFSET 0"
      case (None, Some(offset)) ⇒
        s"LIMIT ${Int.MaxValue} OFFSET $offset"
      case _ ⇒
        ""
    }

  def whereClauseForSummary(maybeCriteria: Option[InternalReconSummaryCriteria]): String = {

    maybeCriteria.map(criteria ⇒ {
      val anyCustomerName = criteria.maybeAnyCustomerName.map { name ⇒
        s"(${name.toSql(Some(UserSqlDao.username), Some(UserSqlDao.TableAlias))} OR " +
          s"${name.toSql(Some(IndividualUserSqlDao.name), Some(IndividualUserSqlDao.TableAlias))} OR " +
          s"${name.toSql(Some(IndividualUserSqlDao.fullName), Some(IndividualUserSqlDao.TableAlias))} OR " +
          s"${name.toSql(Some(BusinessUserSqlDao.cBusinessName), Some(BusinessUserSqlDao.TableAlias))} OR" +
          s"${name.toSql(Some(BusinessUserSqlDao.cBrandName), Some(BusinessUserSqlDao.TableAlias))})"
      }
      Seq(
        anyCustomerName,
        criteria.maybeId.map(_.toSql(Some(ReconSummaryTable.cId), Some(ReconSummaryTable.TableAlias))),

        criteria.maybeDateRange.map(_.toSql(Some(ReconSummaryTable.cReconDate), Some(ReconSummaryTable.TableAlias))),

        criteria.maybeAccountNumber.map(_.toSql(Some(ReconSummaryTable.cAccountNumber), Some(ReconSummaryTable.TableAlias))),

        criteria.maybeAccountType.map(_.toSql(Some(ReconSummaryTable.cAccountType), Some(ReconSummaryTable.TableAlias))),

        criteria.maybeUserUuid.map(_.toSql(Some(ReconSummaryTable.cUserUuid), Some(ReconSummaryTable.TableAlias))),

        criteria.maybeStatus.map(_.toSql(Some(ReconSummaryTable.cStatus), Some(ReconSummaryTable.TableAlias))))
        .flatten.toSql
    }).getOrElse("")

  }

  private def whereClauseForDetails(maybeCriteria: Option[InternalReconDetailsCriteria]): String = {
    maybeCriteria.map(criteria ⇒ {
      Seq(
        criteria.maybeReconSummaryId.map(_.toSql(Some(ReconDetailsTable.cSummaryId), Some(ReconDetailsTable.TableAlias))),

        criteria.mayBeDateRange.map(_.toSql(Some(ReconDetailsTable.cReconDate), Some(ReconDetailsTable.TableAlias))),

        criteria.maybeAccountNumber.map(_.toSql(Some(ReconDetailsTable.cAccountNumber), Some(ReconDetailsTable.TableAlias))),

        criteria.maybeCurrency.map(_.toSql(Some(ReconDetailsTable.cCurrency), Some(ReconDetailsTable.TableAlias))))
        .flatten.toSql
    }).getOrElse("")

  }

}

