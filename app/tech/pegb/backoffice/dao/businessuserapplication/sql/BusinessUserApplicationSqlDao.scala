package tech.pegb.backoffice.dao.businessuserapplication.sql

import java.sql.{Connection, SQLException}
import java.time.{LocalDate, LocalDateTime}

import anorm.SqlParser.scalar
import anorm.{NamedParameter, Row, RowParser, SQL, SimpleSql, SqlRequestError}
import cats.implicits._
import com.google.inject.Inject
import play.api.db.DBApi
import tech.pegb.backoffice.dao.Dao.{EntityId, IntEntityId, UUIDEntityId}
import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.businessuserapplication.abstraction.BusinessUserApplicationDao
import tech.pegb.backoffice.dao.businessuserapplication.dto.{BusinessUserApplicationCriteria, BusinessUserApplicationToInsert, BusinessUserApplicationToUpdate}
import tech.pegb.backoffice.dao.businessuserapplication.entity.BusinessUserApplication
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.sql.MostRecentUpdatedAtGetter
import tech.pegb.backoffice.dao.util.Implicits._
import tech.pegb.backoffice.domain.businessuserapplication.abstraction.Status
import tech.pegb.backoffice.mapping.domain.dao.Implicits._

import scala.util.Try

class BusinessUserApplicationSqlDao @Inject() (
    val dbApi: DBApi)
  extends BusinessUserApplicationDao with MostRecentUpdatedAtGetter[BusinessUserApplication, BusinessUserApplicationCriteria] with SqlDao {

  import BusinessUserApplicationSqlDao._

  protected def getUpdatedAtColumn: String = ???

  protected def getMainSelectQuery: String = ???

  protected def getRowToEntityParser: Row ⇒ BusinessUserApplication = ???

  protected def getWhereFilterFromCriteria(criteriaDto: Option[BusinessUserApplicationCriteria]): String = ???

  def insertBusinessUserApplication(dto: BusinessUserApplicationToInsert): DaoResponse[BusinessUserApplication] = {
    withTransaction({ implicit cxn: Connection ⇒
      val generatedId = insertSql
        .on(buildParametersForCreate(dto.uuid, dto): _*) //not nullable in db and same as created at on insertion
        .executeInsert(scalar[Int].single)

      internalGetById(generatedId.asEntityId) match {
        case Some(businessUserApplication) ⇒
          businessUserApplication
        case None ⇒ throw new Throwable("[insertBusinessUserApplication] Failed to fetch created businessUserApplication")
      }
    }, s"[insertBusinessUserApplication] Failed to create businessUserApplication: $dto",
      handlerPF = {
        case e: SQLException if isUniqueConstraintViolation(e) ⇒
          val errorMessage = s"[insertBusinessUserApplication] BusinessUserApplicationToCreate already exists $dto"
          entityAlreadyExistsError(errorMessage)
        case e: SQLException ⇒
          val errorMessage = s"[insertBusinessUserApplication] Could not create businessUserApplication $dto"
          logger.error(errorMessage, e)
          constraintViolationError(errorMessage)
        case generic ⇒
          logger.error("error encountered in [insertBusinessUserApplication]", generic)
          genericDbError(s"error encountered while inserting business user application $dto")
      })
  }

  def getBusinessUserApplicationByCriteria(criteria: BusinessUserApplicationCriteria,
                                           ordering: Option[OrderingSet],
                                           limit: Option[Int],
                                           offset: Option[Int]): DaoResponse[Seq[BusinessUserApplication]] = withConnection({ implicit connection ⇒

    val whereFilter = generateBusinessUserApplicationWhereFilter(criteria.some)

    val order = ordering.fold(s"ORDER BY ${BusinessUserApplicationSqlDao.TableAlias}.${BusinessUserApplicationSqlDao.cId}")(_.toString)
    val pagination = SqlDao.getPagination(limit, offset)

    val columns = s"$TableAlias.*"

    val businessUserApplicationByCriteriaSql = SQL(s"""${baseFindByCriteria(columns, whereFilter)} $order $pagination""".stripMargin)

    businessUserApplicationByCriteriaSql.as(businessUserApplicationParser.*).distinct

  }, s"Error while retrieving business user application by criteria: $criteria")

  def countBusinessUserApplicationByCriteria(criteria: BusinessUserApplicationCriteria): DaoResponse[Int] = withConnection({ implicit connection ⇒
    val whereFilter = generateBusinessUserApplicationWhereFilter(criteria.some)
    val column = s"COUNT(DISTINCT ${BusinessUserApplicationSqlDao.TableAlias}.${BusinessUserApplicationSqlDao.cId}) as n"

    val countByCriteriaSql = SQL(s"""${baseFindByCriteria(column, whereFilter)}""")

    countByCriteriaSql
      .as(countByCriteriaSql.defaultParser.singleOpt)
      .map(row ⇒ convertRowToCount(row)).getOrElse(0)

  }, s"Error while retrieving count by criteria:$criteria")

  def updateBusinessUserApplication(id: EntityId, dto: BusinessUserApplicationToUpdate)(implicit txnConn: Option[Connection]):
  DaoResponse[Option[BusinessUserApplication]] = {
    withTransaction({ conn: Connection ⇒
      for {
        _ ← internalGetById(id)(txnConn.getOrElse(conn))
        updateResult = updateQuery(id, dto).executeUpdate()(txnConn.getOrElse(conn))
        updated ← if (updateResult > 0) {
          internalGetById(id)(txnConn.getOrElse(conn))
        } else {
          throw new IllegalStateException(s"Update failed. BusinessUserApplication $id has been modified by another process.")
        }
      } yield {
        updated
      }
    }, s"Failed to update BusinessUserApplication $id",
      handlerPF = {
        case e: SQLException ⇒
          val errorMessage = s"Could not update BusinessUserApplication $id"
          logger.error(errorMessage, e)
          constraintViolationError(errorMessage)
        case ie: IllegalStateException ⇒
          preconditionFailed(ie.getMessage)
      })
  }

  private[sql] def internalGetById(entityId: EntityId)(implicit cxn: Connection): Option[BusinessUserApplication] = {
    val columns = s"$TableAlias.*"

    entityId match {
      case UUIDEntityId(uuid) ⇒
        val filters = s"""WHERE $TableAlias.$cUuid = {$cUuid}"""
        SQL(s"""${baseFindByCriteria(columns, filters)}""".stripMargin)
          .on(cUuid → uuid)
          .executeQuery().as(businessUserApplicationParser.singleOpt)
      case IntEntityId(id) ⇒
        val filters = s"""WHERE $TableAlias.$cId = {$cId}"""
        SQL(s"""${baseFindByCriteria(columns, filters)}""".stripMargin)
          .on(cId → id)
          .executeQuery().as(businessUserApplicationParser.singleOpt)
    }
  }
}

object BusinessUserApplicationSqlDao {

  val TableName = "business_user_applications"
  val TableAlias = "bua"

  val cId = "id"
  val cUuid = "uuid"
  val cBusinessName = "business_name"
  val cBrandName = "brand_name"
  val cBusinessCategory = "business_category"
  val cStage = "stage"
  val cStatus = "status"
  val cUserTier = "user_tier"
  val cBusinessType = "business_type"
  val cRegistrationNumber = "registration_number"
  val cTaxNumber = "tax_number"
  val cRegistrationDate = "registration_date"
  val cExplanation = "explanation"
  val cSubmittedBy = "submitted_by"
  val cSubmittedAt = "submitted_at"
  val cCheckedBy = "checked_by"
  val cCheckedAt = "checked_at"
  val cCreatedBy = "created_by"
  val cCreatedAt = "created_at"
  val cUpdatedBy = "updated_by"
  val cUpdatedAt = "updated_at"
  val cUserId = "user_id"

  val TableFields = Seq(cUuid, cBusinessName, cBrandName, cBusinessCategory, cStage, cStatus,
    cUserTier, cBusinessType, cRegistrationNumber, cTaxNumber, cRegistrationDate,
    cCreatedBy, cUpdatedBy, cCreatedAt, cUpdatedAt)

  val TableFieldStr = TableFields.mkString(",")
  val ValuesPlaceHolders = TableFields.map(c ⇒ s"{$c}").mkString(",")

  final val insertSql = SQL(s"INSERT INTO $TableName ($TableFieldStr) VALUES ($ValuesPlaceHolders)")

  private def updateQuery(id: EntityId, dto: BusinessUserApplicationToUpdate): SimpleSql[Row] = {
    val paramsBuffer = dto.paramsBuilder
    val filterParam = convertEntityIdToParam(id)
    paramsBuffer += filterParam

    val preQuery = dto.createSqlString(TableName, Some(s"WHERE ${filterParam.name} = {${filterParam.name}}"))
    val params = paramsBuffer.result()
    SQL(preQuery).on(params: _*)
  }

  private def convertEntityIdToParam(id: EntityId): NamedParameter = id match {
    case UUIDEntityId(uuid) ⇒ (cUuid, uuid)
    case IntEntityId(intId) ⇒ (cId, intId)
  }

  private def baseFindByCriteria(selectColumns: String, filters: String): String = {
    //NOTE: distinct was added because of when LEFT JOIN was added the rows in the left table might repeat
    s"""SELECT DISTINCT $selectColumns
       |FROM $TableName $TableAlias
       |
       |LEFT JOIN ${BUApplicPrimaryContactsSqlDao.TableName} ${BUApplicPrimaryContactsSqlDao.TableAlias}
       |ON ${BusinessUserApplicationSqlDao.TableAlias}.${BUApplicPrimaryContactsSqlDao.cId} = ${BUApplicPrimaryContactsSqlDao.TableAlias}.${BUApplicPrimaryContactsSqlDao.cApplicId}
       |
       |$filters""".stripMargin
  }

  private def generateBusinessUserApplicationWhereFilter(mayBeCriteria: Option[BusinessUserApplicationCriteria]): String = {
    import SqlDao._
    mayBeCriteria.map{ criteria ⇒

      Seq(
        criteria.uuid.map(_.toSql(cUuid.some, TableAlias.some)),
        criteria.businessName.map(_.toSql(cBusinessName.some, TableAlias.some)),
        criteria.brandName.map(_.toSql(cBrandName.some, TableAlias.some)),
        criteria.businessCategory.map(_.toSql(cBusinessCategory.some, TableAlias.some)),
        criteria.stage.map(_.toSql(cStage.some, TableAlias.some)),
        criteria.status.map(_.toSql(cStatus.some, TableAlias.some)),
        criteria.userTier.map(_.toSql(cUserTier.some, TableAlias.some)),
        criteria.businessType.map(_.toSql(cBusinessType.some, TableAlias.some)),
        criteria.registrationNumber.map(_.toSql(cRegistrationNumber.some, TableAlias.some)),
        criteria.taxNumber.map(_.toSql(cTaxNumber.some, TableAlias.some)),
        criteria.registrationDate.map(_.toSql(cRegistrationDate.some, TableAlias.some)),
        criteria.submittedBy.map(_.toSql(cSubmittedBy.some, TableAlias.some)),
        criteria.submittedAt.map(_.toFormattedDateTime.toSql(cSubmittedAt.some, TableAlias.some)),
        criteria.checkedBy.map(_.toSql(cCheckedBy.some, TableAlias.some)),
        criteria.checkedAt.map(_.toFormattedDateTime.toSql(cCheckedAt.some, TableAlias.some)),
        criteria.createdBy.map(_.toSql(cCreatedBy.some, TableAlias.some)),
        criteria.createdAt.map(_.toFormattedDateTime.toSql(cCreatedAt.some, TableAlias.some)),
        criteria.updatedBy.map(_.toSql(cUpdatedBy.some, TableAlias.some)),
        criteria.updatedAt.map(_.toFormattedDateTime.toSql(cUpdatedAt.some, TableAlias.some)),
        criteria.contactsPhoneNumber.map(_.toSql( tableAlias = Some(BUApplicPrimaryContactsSqlDao.TableAlias))),
        criteria.contactsEmail.map(_.toSql( tableAlias = Some(BUApplicPrimaryContactsSqlDao.TableAlias))),
        //NOTE: why do we have isActive if there is no 'is_active' column ?
        criteria.isActive.map { cf ⇒
          if(cf.value)
            s"( $cStatus = '${Status.Ongoing}' OR $cStatus = '${Status.Pending}' OR $cStatus = '${Status.Approved}')"
          else
            s"( $cStatus = '${Status.Rejected}' OR $cStatus = '${Status.Cancelled}')"
        },
      ).flatten.toSql
    }.getOrElse("")
  }

  def buildParametersForCreate(uuid: String, dto: BusinessUserApplicationToInsert): Seq[NamedParameter] =
    Seq[NamedParameter](
      cUuid → uuid,
      cBusinessName → dto.businessName,
      cBrandName → dto.brandName,
      cBusinessCategory → dto.businessCategory,
      cStage → dto.stage,
      cStatus → dto.status,
      cUserTier → dto.userTier,
      cBusinessType → dto.businessType,
      cRegistrationNumber → dto.registrationNumber,
      cTaxNumber → dto.taxNumber,
      cRegistrationDate → dto.registrationDate,
      cCreatedBy → dto.createdBy,
      cUpdatedBy → dto.createdBy,
      cCreatedAt → dto.createdAt,
      cUpdatedAt → dto.createdAt)

  def rowToBusinessUserApplication(row: Row): BusinessUserApplication = {
    BusinessUserApplication(
      id = row[Int](s"$TableName.$cId"),
      uuid = row[String](s"$TableName.$cUuid"),
      businessName = row[String](s"$TableName.$cBusinessName"),
      brandName = row[String](s"$TableName.$cBrandName"),
      businessCategory = row[String](s"$TableName.$cBusinessCategory"),
      stage = row[String](s"$TableName.$cStage"),
      status = row[String](s"$TableName.$cStatus"),
      userTier = row[String](s"$TableName.$cUserTier"),
      businessType = row[String](s"$TableName.$cBusinessType"),
      registrationNumber = row[String](s"$TableName.$cRegistrationNumber"),
      taxNumber = row[Option[String]](s"$TableName.$cTaxNumber"),
      registrationDate = row[Option[LocalDate]](s"$TableName.$cRegistrationDate"),
      explanation = row[Option[String]](s"$TableName.$cExplanation"),
      userId = row[Option[Int]](s"$TableName.$cUserId"),
      submittedBy = row[Option[String]](s"$TableName.$cSubmittedBy"),
      submittedAt = row[Option[LocalDateTime]](s"$TableName.$cSubmittedAt"),
      checkedBy = row[Option[String]](s"$TableName.$cCheckedBy"),
      checkedAt = row[Option[LocalDateTime]](s"$TableName.$cCheckedAt"),
      createdBy = row[String](s"$TableName.$cCreatedBy"),
      createdAt = row[LocalDateTime](s"$TableName.$cCreatedAt"),
      updatedBy = row[Option[String]](s"$TableName.$cUpdatedBy"),
      updatedAt = row[Option[LocalDateTime]](s"$TableName.$cUpdatedAt"))
  }

  private val businessUserApplicationParser: RowParser[BusinessUserApplication] = row ⇒ {
    Try {
      rowToBusinessUserApplication(row)
    }.fold(
      exc ⇒ anorm.Error(SqlRequestError(exc)),
      anorm.Success(_))
  }

  private def convertRowToCount(row: Row): Int = row[Int]("n")

}
