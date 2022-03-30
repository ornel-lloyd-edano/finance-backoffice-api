package tech.pegb.backoffice.dao.customer.sql

import java.sql.Connection
import java.time.LocalDateTime
import java.util.UUID

import anorm._
import com.google.inject.Inject
import play.api.db.DBApi

import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.customer.abstraction.PaymentOptionDao
import tech.pegb.backoffice.dao.customer.dto.PaymentOptionDto

import scala.util.Try

class PaymentOptionSqlDao @Inject() (override val dbApi: DBApi) extends PaymentOptionDao with SqlDao {
  import PaymentOptionSqlDao._

  override def fetchPaymentOptions(customerId: UUID): DaoResponse[List[PaymentOptionDto]] = {
    withConnection(
      { implicit connection: Connection ⇒
        selectQuery.on(cCustomerId → customerId)
          .as(rowParser.*)
      }, s"Error while retrieving payment options for customer $customerId")
  }

}

object PaymentOptionSqlDao {
  private[dao] final val Table = "payment_options"
  private[dao] final val Alias = "po"

  private[dao] final val cId = "id"
  private[dao] final val cCustomerId = "customer_id"
  private[dao] final val cType = "type"
  private[dao] final val cProvider = "provider"
  private[dao] final val cMaskedNumber = "masked_number"
  private[dao] final val cAdditionalData = "additional_data"
  private[dao] final val cAddedAt = "added_at"

  private val selectQuery = SQL(
    s"""SELECT $Alias.$cId, ${UserSqlDao.TableAlias}.${UserSqlDao.uuid} as $cCustomerId,
       |$Alias.$cType, $Alias.$cProvider, $Alias.$cMaskedNumber, $Alias.$cAdditionalData, $Alias.$cAddedAt
       |FROM $Table $Alias
       |INNER JOIN ${UserSqlDao.TableName} ${UserSqlDao.TableAlias}
       |ON $Alias.$cCustomerId = ${UserSqlDao.TableAlias}.${UserSqlDao.id}
       |WHERE ${UserSqlDao.TableAlias}.${UserSqlDao.uuid} = {$cCustomerId}""".stripMargin)

  private val rowParser: RowParser[PaymentOptionDto] = {
    val parseRow: Row ⇒ PaymentOptionDto = row ⇒ {
      PaymentOptionDto(
        id = row[Int](cId),
        customerId = row[UUID](cCustomerId),
        `type` = row[Int](cType),
        provider = row[Int](cProvider),
        maskedNumber = row[String](cMaskedNumber),
        additionalData = row[Option[String]](cAdditionalData),
        addedAt = row[LocalDateTime](cAddedAt))
    }
    RowParser(row ⇒ {
      Try(parseRow(row)).fold(exc ⇒ Error(SqlRequestError(exc)), Success.apply)
    })
  }

}
