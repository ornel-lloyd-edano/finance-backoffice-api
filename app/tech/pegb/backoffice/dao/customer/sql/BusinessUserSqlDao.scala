package tech.pegb.backoffice.dao.customer.sql

import java.sql.Connection
import java.time.{LocalDate, LocalDateTime}

import anorm._
import cats.implicits._
import com.google.inject.Inject
import play.api.db.DBApi
import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.account.sql.AccountSqlDao
import tech.pegb.backoffice.dao.customer.abstraction.{BusinessUserDao, UserDao}
import tech.pegb.backoffice.dao.customer.dto._
import tech.pegb.backoffice.dao.customer.entity._
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.util.Implicits._
import tech.pegb.backoffice.util.Logging

import scala.util.Try

class BusinessUserSqlDao @Inject() (
    val dbApi: DBApi,
    userDao: UserDao)
  extends BusinessUserDao with SqlDao {
  import BusinessUserSqlDao._

  def getBusinessUser(uuid: String) = ???

  def getBusinessUserByCriteria(
                                 criteria: BusinessUserCriteria,
                                 ordering: Option[OrderingSet],
                                 limit: Option[Int],
                                 offset: Option[Int]): DaoResponse[Seq[BusinessUser]] = withConnection({ implicit connection ⇒

    val whereFilter = generateBusinessUserWhereFilter(criteria.some)

    val order = ordering.fold(s"ORDER BY $TableAlias.$cId")(_.toString)
    val pagination = SqlDao.getPagination(limit, offset)

    val columns =
      s"""$TableAlias.*,
         |${UserSqlDao.TableAlias}.${UserSqlDao.uuid},
         |ca.uuid as $cCollectionAccountUUID,
         |da.uuid as $cDistributionAccountUUID""".stripMargin

    val businessUserByCriteriaSql = SQL(s"""${baseFindByCriteria(columns, whereFilter)} $order $pagination""".stripMargin)

    businessUserByCriteriaSql.as(businessUserParser.*)

  }, s"Error while retrieving business user application by criteria: $criteria")


  def getUserAndBusinessUserJoinByCriteria(criteria: UserAndBusinessUserJoinGetCriteria, limit: Option[Int], offset: Option[Int]) = ???

  def countTotalUserAndBusinessUserJoinByCriteria(criteria: UserAndBusinessUserJoinGetCriteria) = ???

  def updateBusinessUser(uuid: String, businessUser: BusinessUserToUpdate)(implicit maybeTransaction: Option[Connection]) = ???

  def insertBusinessUser(user: UserToInsert, businessUser: BusinessUserToInsert) = ???

  def deleteBusinessUser(uuid: String)(implicit maybeTransaction: Option[Connection]) = ???
}

object BusinessUserSqlDao extends Logging {

  final val TableName = "business_users"
  final val TableAlias = "b"

  final val cId = "id"
  final val cUuid = "uuid"
  final val cUserId = "user_id"
  final val cBusinessName = "business_name"
  final val cBrandName = "brand_name"
  final val cBusinessCategory = "business_category"
  final val cBusinessType = "business_type"
  final val cRegistrationNumber = "registration_number"
  final val cTaxNumber = "tax_number"
  final val cRegistrationDate = "registration_date"
  final val cCurrencyId = "currency_id"
  final val cCollectionAccountId = "collection_account_id"
  final val cDistributionAccountId = "distribution_account_id"
  final val cDefaultContactId = "default_contact_id"
  final val cTotalTransactionsAmount = "total_transactions_amount"
  final val cTransactionCount = "transaction_count"
  final val cCreatedAt = "created_at"
  final val cCreatedBy = "created_by"
  final val cUpdatedAt = "updated_at"
  final val cUpdatedBy = "updated_by"

  final val cCollectionAccountUUID = "collection_account_uuid"
  final val cDistributionAccountUUID = "distribution_account_uuid"


  private def generateBusinessUserWhereFilter(mayBeCriteria: Option[BusinessUserCriteria]): String = {
    import SqlDao._
    mayBeCriteria.map{ criteria ⇒

      Seq(
        criteria.uuid.map(_.toSql(cUuid.some, TableAlias.some)),
        criteria.businessName.map(_.toSql(cBusinessName.some, TableAlias.some)),
        criteria.brandName.map(_.toSql(cBrandName.some, TableAlias.some)),
        criteria.businessCategory.map(_.toSql(cBusinessCategory.some, TableAlias.some)),
        criteria.businessType.map(_.toSql(cBusinessType.some, TableAlias.some)),
        criteria.registrationNumber.map(_.toSql(cRegistrationNumber.some, TableAlias.some)),
        criteria.taxNumber.map(_.toSql(cTaxNumber.some, TableAlias.some)),
        criteria.registrationDate.map(_.toSql(cRegistrationDate.some, TableAlias.some)),
        criteria.createdBy.map(_.toSql(cCreatedBy.some, TableAlias.some)),
        criteria.createdAt.map(_.toFormattedDateTime.toSql(cCreatedAt.some, TableAlias.some)),
        criteria.updatedBy.map(_.toSql(cUpdatedBy.some, TableAlias.some)),
        criteria.updatedAt.map(_.toFormattedDateTime.toSql(cUpdatedAt.some, TableAlias.some)),
      ).flatten.toSql
    }.getOrElse("")
  }

  private def baseFindByCriteria(selectColumns: String, filters: String): String = {
    s"""SELECT $selectColumns
       |FROM $TableName $TableAlias
       |JOIN ${UserSqlDao.TableName} ${UserSqlDao.TableAlias}
       |ON $TableAlias.$cUserId = ${UserSqlDao.TableAlias}.${UserSqlDao.id}
       |JOIN ${AccountSqlDao.TableName} ca
       |ON $TableAlias.$cCollectionAccountId = ca.${AccountSqlDao.cId}
       |JOIN ${AccountSqlDao.TableName} da
       |ON $TableAlias.$cDistributionAccountId = da.${AccountSqlDao.cId}
       |$filters""".stripMargin
  }

  private val businessUserParser: RowParser[BusinessUser] = row ⇒ {
    convertRowToBusinessUser(row).fold(
      exc ⇒ anorm.Error(SqlRequestError(exc)),
      anorm.Success(_))
  }

  def convertRowToBusinessUser(row: Row): Try[BusinessUser] = Try {
    BusinessUser(
      id = row[Int](s"$TableName.$cId"),
      uuid = row[String](s"$TableName.$cUuid"),
      userId = row[Int](s"$TableName.$cUserId"),
      userUUID = row[String](s"${UserSqlDao.TableName}.$cUuid"),

      businessName = row[String](s"$TableName.$cBusinessName"),
      brandName = row[String](s"$TableName.$cBrandName"),
      businessCategory = row[String](s"$TableName.$cBusinessCategory"),
      businessType = row[String](s"$TableName.$cBusinessType"),

      registrationNumber = row[String](s"$TableName.$cRegistrationNumber"),
      taxNumber = row[Option[String]](s"$TableName.$cTaxNumber"),

      registrationDate = row[Option[LocalDate]](s"$TableName.$cRegistrationDate"),
      currencyId = row[Int](s"$TableName.$cCurrencyId"),

      collectionAccountId = row[Option[Int]](s"$TableName.$cCollectionAccountId"),
      collectionAccountUUID = row[Option[String]](cCollectionAccountUUID),
      distributionAccountId = row[Option[Int]](s"$TableName.$cDistributionAccountId"),
      distributionAccountUUID = row[Option[String]](cDistributionAccountUUID),

      createdAt = row[LocalDateTime](s"$TableName.$cCreatedAt"),
      createdBy = row[String](s"$TableName.$cCreatedBy"),

      updatedAt = row[Option[LocalDateTime]](s"$TableName.$cUpdatedAt"),
      updatedBy = row[Option[String]](s"$TableName.$cUpdatedBy"))
  }

  def buildParametersForBusinessUserInsert(
    businessUserId: Int,
    businessUser: BusinessUserToInsert): Seq[NamedParameter] =
    Seq[NamedParameter](
      cUserId → businessUserId,
      cBusinessName → businessUser.businessName,
      cBrandName → businessUser.brandName,
      cBusinessCategory → businessUser.businessCategory,
      cBusinessType → businessUser.businessType,
      cRegistrationNumber → businessUser.registrationNumber,
      cTaxNumber → businessUser.taxNumber,
      cRegistrationDate → businessUser.registrationNumber,
      cCurrencyId → businessUser.currencyId,
      cCollectionAccountId → businessUser.collectionAccountId,
      cDistributionAccountId → businessUser.distributionAccountId,
      cDefaultContactId → businessUser.defaultContactId,

      cCreatedAt → businessUser.createdAt,
      cCreatedBy → businessUser.createdBy,
      cUpdatedAt → businessUser.createdAt,
      cUpdatedBy → businessUser.createdBy)

}

