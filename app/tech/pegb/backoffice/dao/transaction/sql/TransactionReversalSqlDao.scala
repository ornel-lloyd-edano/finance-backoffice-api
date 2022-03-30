package tech.pegb.backoffice.dao.transaction.sql

import java.sql.Connection
import java.time.LocalDateTime

import anorm.{RowParser, SQL, SqlQuery, SqlRequestError}
import com.google.inject.Inject
import play.api.db.DBApi

import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.transaction.abstraction.TransactionReversalDao
import tech.pegb.backoffice.dao.transaction.entity.TransactionReversal
import tech.pegb.backoffice.util.AppConfig

import scala.util.Try

class TransactionReversalSqlDao @Inject() (
    val dbApi: DBApi,
    config: AppConfig) extends TransactionReversalDao with SqlDao {

  import TransactionReversalSqlDao._

  def getTransactionReversalsByCriteriaById(reversedTransactionId: String): DaoResponse[Option[TransactionReversal]] = {
    withConnection({ implicit cxn: Connection ⇒
      internalGetTransactionReversal(reversedTransactionId)
    }, s"Failed to fetch transaction_reversal with reversedTransactionId: $reversedTransactionId")
  }

  private[sql] def internalGetTransactionReversal(
    id: String)(
    implicit
    cxn: Connection): Option[TransactionReversal] = {

    fetchByIdQuery.on(cReversedTransactionId → id)
      .executeQuery().as(transactionReversalRowParser.singleOpt)

  }

}

object TransactionReversalSqlDao {
  val TableName = "transaction_reversals"
  val TableAlias = "tr"

  //columns
  val cId = "id"
  val cReversedTransactionId = "reversed_transaction_id"
  val cReversalTransactionId = "reversal_transaction_id"
  val cReason = "reason"
  val cStatus = "status"
  val cCreatedBy = "created_by"
  val cUpdatedBy = "updated_by"
  val cCreatedAt = "created_at"
  val cUpdatedAt = "updated_at"

  private def fetchByIdQuery: SqlQuery = {
    val columns = s"$TableAlias.*"
    val filters = s"""WHERE $TableAlias.$cReversedTransactionId = {$cReversedTransactionId}"""

    SQL(s"""${baseFindTransactionReversalByCriteria(columns, filters)}""".stripMargin)
  }

  private def baseFindTransactionReversalByCriteria(selectColumns: String, filters: String): String = {
    s"""SELECT $selectColumns
       |FROM $TableName $TableAlias
       |$filters""".stripMargin
  }

  private val transactionReversalRowParser: RowParser[TransactionReversal] = row ⇒ {
    Try {
      TransactionReversal(
        id = row[Long](cId),
        reversedTransactionId = row[String](cReversedTransactionId),
        reversalTransactionId = row[String](cReversalTransactionId),
        reason = row[String](cReason),
        status = row[String](cStatus),
        createdBy = row[String](cCreatedBy),
        updatedBy = row[String](cUpdatedBy),
        createdAt = row[LocalDateTime](cCreatedAt),
        updatedAt = row[LocalDateTime](cUpdatedAt))

    }.fold(
      exc ⇒ anorm.Error(SqlRequestError(exc)),
      anorm.Success(_))
  }

}
