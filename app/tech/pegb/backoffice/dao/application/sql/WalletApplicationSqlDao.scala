package tech.pegb.backoffice.dao.application.sql

import java.sql.{Connection, SQLException}
import java.time.LocalDateTime
import java.util.UUID

import anorm._
import cats.syntax.either._
import com.google.inject.Inject
import play.api.db.DBApi
import tech.pegb.backoffice.application.KafkaDBSyncService
import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.application.abstraction.WalletApplicationDao
import tech.pegb.backoffice.dao.application.dto.{WalletApplicationCriteria, WalletApplicationToCreate, WalletApplicationToUpdate}
import tech.pegb.backoffice.dao.application.entity.WalletApplication
import tech.pegb.backoffice.dao.customer.sql.{IndividualUserSqlDao, UserSqlDao}
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.sql.MostRecentUpdatedAtGetter
import tech.pegb.backoffice.util.Implicits._
import scala.util.Try

class WalletApplicationSqlDao @Inject() (
    val dbApi: DBApi,
    kafkaDBSyncService: KafkaDBSyncService)
  extends WalletApplicationDao with MostRecentUpdatedAtGetter[WalletApplication, WalletApplicationCriteria] with SqlDao {
  import WalletApplicationSqlDao._

  protected def getUpdatedAtColumn: String = s"${WalletApplicationSqlDao.TableAlias}.${WalletApplicationSqlDao.updatedAt}"

  protected def getMainSelectQuery: String = WalletApplicationSqlDao.qCommonSelectJoin

  protected def getRowToEntityParser: Row ⇒ WalletApplication = (arg: Row) ⇒ WalletApplicationSqlDao.convertRowToWalletApplication(arg)

  protected def getWhereFilterFromCriteria(criteriaDto: Option[WalletApplicationCriteria]): String = criteriaDto.map(WalletApplicationSqlDao.generateWhereFilter(_, None)).getOrElse("")

  def getWalletApplicationByUUID(uuid: UUID): DaoResponse[Option[WalletApplication]] =
    withConnection({ implicit connection: Connection ⇒
      findWalletApplicationByUUIDInternal(uuid.toString)
        .map(convertRowToWalletApplication)
    }, s"Error while retrieving wallet application by uuid:$uuid", {
      case e: SQLException ⇒
        val errorMessage = s"Could not get application from db $uuid"
        logger.error(errorMessage, e)
        constraintViolationError(errorMessage)
    })

  def getWalletApplicationByUserUuid(userUuid: UUID): DaoResponse[Set[WalletApplication]] =
    withConnection({ implicit connection: Connection ⇒
      findWalletApplicationsByUserUuid(userUuid.toString)
        .map(convertRowToWalletApplication)
        .toSet
    }, s"Error while retrieving wallet application by user uuid:$userUuid")

  def getWalletApplicationByInternalId(id: Int): DaoResponse[Option[WalletApplication]] =
    withConnection({ implicit connection: Connection ⇒

      findWalletApplicationByIdInternal(id)
        .map(convertRowToWalletApplication)
    }, s"Error while retrieving wallet application by internal:$id")

  def getWalletApplicationsByCriteria(
    criteria: WalletApplicationCriteria,
    ordering: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int]): DaoResponse[Seq[WalletApplication]] =
    withConnection(
      { implicit connection: Connection ⇒

        val whereClause = generateWhereFilter(criteria, ordering)
        val pagination = SqlDao.getPagination(limit, offset)
        val criteriaSql = SQL(s"$qCommonSelectJoin $whereClause $pagination")

        criteriaSql
          .as(criteriaSql.defaultParser.*)
          .map(convertRowToWalletApplication)
      },
      s"""error while getting wallet application by search criteria $criteria with limit
         | $limit and offset $offset .getWalletApplicationsByCriteria"""".stripMargin)

  def countWalletApplicationsByCriteria(criteria: WalletApplicationCriteria): DaoResponse[Int] =
    withConnection(
      { implicit connection: Connection ⇒

        val whereClause = generateWhereFilter(criteria)
        val query = SQL(
          s"""SELECT count(*)
             |$qCommonJoin
             |$whereClause""".stripMargin)

        query.as(query.defaultParser.singleOpt).map(convertRowToCount).getOrElse(0)
      },
      s"""error while getting wallet application count by search criteria $criteria
         |.countWalletApplicationsByCriteria""".stripMargin)

  def updateWalletApplication(
    id: UUID,
    walletApplicationToUpdate: WalletApplicationToUpdate)(implicit maybeTransaction: Option[Connection]): DaoResponse[Option[WalletApplication]] = {
    withTransaction(
      { connection: Connection ⇒

        val allSetEntries = generateUpdateClause(walletApplicationToUpdate)
        val updatedRowsN = prepareWalletApplicationSql(id.toString, allSetEntries)
          .executeUpdate()(maybeTransaction.getOrElse(connection))

        if (updatedRowsN > 0) {
          findWalletApplicationByUUIDInternal(id.toString)(maybeTransaction.getOrElse(connection))
            .map(convertRowToWalletApplication)
            .map { walletApplication ⇒
              kafkaDBSyncService.sendUpdate(TableName, walletApplication)
              walletApplication
            }
        } else {
          None
        }
      }, "Unexpected exception in updateWalletApplication", {
        case e: SQLException ⇒
          val errorMessage = s"Could not update walletApplication ${id}"
          logger.error(errorMessage, e)
          constraintViolationError(errorMessage)
      })
  }

  def insertWalletApplication(walletApplicationToCreate: WalletApplicationToCreate): DaoResponse[WalletApplication] = {
    withTransaction({ implicit connection ⇒
      val columnsToInsert = buildColumnStringForInsert(walletApplicationToCreate)
      val placeHolders = buildInsertPlaceHolders(walletApplicationToCreate)
      val insertParam = buildParametersForInsert(walletApplicationToCreate)

      for {
        _ ← Try(insertSql(columnsToInsert, placeHolders).on(insertParam: _*).execute())
          .toEither
          .leftMap {
            case e: SQLException ⇒
              logger.error(s"[insertWalletApplication]", e)
              genericDbError(s"error encountered in insert wallet application [${walletApplicationToCreate.toSmartString}]")
          }
        walletApplication ← findWalletApplicationByUUIDInternal(walletApplicationToCreate.uuid.toString).map(convertRowToWalletApplication)
          .toRight(entityNotFoundError(s"Created walletApplication ${walletApplicationToCreate.uuid} couldn't be found"))
      } yield {
        kafkaDBSyncService.sendInsert(TableName, walletApplication)
        walletApplication
      }
    })
  }

  override def getInternalIdByUUID(id: UUID)(implicit cxn: Connection): DaoResponse[Int] = {
    getInternalIdRawSql
      .on("uuid" → id)
      .executeQuery()
      .as(SqlParser.scalar[Int].singleOpt)
      .toRight(entityNotFoundError(s"Application with uuid $id was not found"))
  }
}

object WalletApplicationSqlDao {

  final val TableName = "user_applications"
  final val TableAlias = "ua"

  //columns
  private[dao] final val id = "id"
  private[dao] final val uuid = "uuid"
  private[dao] final val userId = "user_id"
  private[dao] final val status = "status"
  private[dao] final val stage = "stage"
  private[dao] final val rejectedReason = "rejection_reason"
  private[dao] final val checkedBy = "checked_by"
  private[dao] final val checkedAt = "checked_at"
  private[dao] final val totalScore = "total_score"

  private[dao] final val fullnameScore = "fullname_score"
  private[dao] final val fullnameOriginal = "fullname_original"
  private[dao] final val fullnameUpdated = "fullname_updated"

  private[dao] final val birthdateScore = "birthdate_score"
  private[dao] final val birthdateOriginal = "birthdate_original"
  private[dao] final val birthdateUpdated = "birthdate_updated"

  private[dao] final val birthplaceScore = "birthplace_score"
  private[dao] final val birthplaceOriginal = "birthplace_original"
  private[dao] final val birthplaceUpdated = "birthplace_updated"

  private[dao] final val genderScore = "gender_score"
  private[dao] final val genderOriginal = "gender_original"
  private[dao] final val genderUpdated = "gender_updated"

  private[dao] final val nationalityScore = "nationality_score"
  private[dao] final val nationalityOriginal = "nationality_original"
  private[dao] final val nationalityUpdated = "nationality_updated"

  private[dao] final val personIdScore = "person_id_score"
  private[dao] final val personIdOriginal = "person_id_original"
  private[dao] final val personIdUpdated = "person_id_updated"

  private[dao] final val documentNumberScore = "document_number_score"
  private[dao] final val documentNumberOriginal = "document_number_original"
  private[dao] final val documentNumberUpdated = "document_number_updated"
  private[dao] final val documentType = "document_type"
  private[dao] final val documentModel = "document_model"

  private[dao] final val createdBy = "created_by"
  private[dao] final val createdAt = "created_at"
  private[dao] final val updatedBy = "updated_by"
  private[dao] final val updatedAt = "updated_at"

  private val userUuid = "user_uuid"
  private val userStatus = "user_status"

  private final def insertSql(fieldsTupleAsString: String, values: String) = {
    SQL(s"INSERT INTO $TableName $fieldsTupleAsString VALUES $values;")
  }

  private def generateUpdateClause(
    walletApplicationToUpdate: WalletApplicationToUpdate,
    tableName: Option[String] = None): String = {
    import SqlDao._

    val updateWalletApplicationValues =
      Seq(
        walletApplicationToUpdate.status.map(queryConditionClause(_, status, tableName)),
        walletApplicationToUpdate.applicationStage.map(queryConditionClause(_, stage, tableName)),
        walletApplicationToUpdate.checkedBy.map(queryConditionClause(_, checkedBy, tableName)),
        walletApplicationToUpdate.checkedAt.map(queryConditionClause(_, checkedAt, tableName)),
        walletApplicationToUpdate.rejectionReason.map(queryConditionClause(_, rejectedReason, tableName)),
        walletApplicationToUpdate.createdAt.map(queryConditionClause(_, createdAt, tableName)),
        walletApplicationToUpdate.createdBy.map(queryConditionClause(_, createdBy, tableName)),
        Option(queryConditionClause(walletApplicationToUpdate.updatedAt, updatedAt, tableName)),
        Option(queryConditionClause(walletApplicationToUpdate.updatedBy, updatedBy, tableName))).flatten.mkString(", ")

    if (updateWalletApplicationValues.nonEmpty) s"$updateWalletApplicationValues" else ""
  }

  def prepareWalletApplicationSql(walletId: String, setValues: String): SimpleSql[Row] =
    SQL(
      s"""UPDATE $TableName $TableAlias SET $setValues WHERE
         | $TableAlias.$uuid = '$walletId'""".stripMargin)

  def findWalletApplicationByUUIDInternal(uuid: String)(implicit connection: Connection): Option[Row] = {
    val queryFindWalletApplicationByApplicationUuid =
      SQL(s"$qCommonSelectJoin WHERE $TableAlias.${WalletApplicationSqlDao.uuid} = {${WalletApplicationSqlDao.uuid}}")
        .on(WalletApplicationSqlDao.uuid → uuid)

    queryFindWalletApplicationByApplicationUuid /*.on(WalletApplicationSqlDao.uuid → uuid)*/
      .as(queryFindWalletApplicationByApplicationUuid.defaultParser.singleOpt)
      .headOption
  }

  def findWalletApplicationsByUserUuid(userUuid: String)(implicit connection: Connection): Seq[Row] = {
    val query = SQL(
      s"""$qCommonSelectJoin
         |WHERE ${UserSqlDao.TableAlias}.${UserSqlDao.uuid} = {${WalletApplicationSqlDao.userUuid}}"""
        .stripMargin)
      .on(WalletApplicationSqlDao.userUuid → userUuid)

    query.as(query.defaultParser.*)
  }

  def findWalletApplicationByIdInternal(id: Int)(implicit connection: Connection): Option[Row] = {
    val queryFindWalletApplicationById = SQL(s"$qCommonSelectJoin WHERE $TableAlias.${WalletApplicationSqlDao.id} = {${WalletApplicationSqlDao.id}}")

    queryFindWalletApplicationById
      .on(WalletApplicationSqlDao.id → id)
      .as(queryFindWalletApplicationById.defaultParser.*)
      .headOption
  }

  private val qCommonJoin =
    s"""
       |FROM $TableName $TableAlias
       |INNER JOIN ${IndividualUserSqlDao.TableName} ${IndividualUserSqlDao.TableAlias}
       |ON $TableAlias.$userId = ${IndividualUserSqlDao.TableAlias}.${IndividualUserSqlDao.userId}
       |INNER JOIN ${UserSqlDao.TableName} ${UserSqlDao.TableAlias}
       |ON ${IndividualUserSqlDao.TableAlias}.${IndividualUserSqlDao.userId} = ${UserSqlDao.TableAlias}.${UserSqlDao.id}
     """.stripMargin

  private val qCommonSelectJoin =
    s"""
       |SELECT $TableAlias.*,
       |${UserSqlDao.TableAlias}.${UserSqlDao.uuid} AS $userUuid,
       |${UserSqlDao.TableAlias}.${UserSqlDao.status} AS $userStatus,
       |${IndividualUserSqlDao.TableAlias}.${IndividualUserSqlDao.name},
       |${IndividualUserSqlDao.TableAlias}.${IndividualUserSqlDao.fullName},
       |${IndividualUserSqlDao.TableAlias}.${IndividualUserSqlDao.msisdn}
       |$qCommonJoin
     """.stripMargin

  private final val getInternalIdRawSql = SQL(s"SELECT id FROM $TableName WHERE uuid = {uuid};")

  def convertRowToWalletApplication(row: Row): WalletApplication = {
    WalletApplication(
      id = row[Int](s"$TableName.$id"),
      uuid = row[UUID](s"$TableName.$uuid"),
      userUuid = row[UUID](s"$userUuid"),
      msisdn = row[Option[String]](IndividualUserSqlDao.msisdn),
      status = row[String](s"$TableName.$status"),
      applicationStage = row[String](s"$TableName.$stage"),
      checkedBy = row[Option[String]](s"$TableName.$checkedBy"),
      checkedAt = row[Option[LocalDateTime]](s"$TableName.$checkedAt"),
      rejectionReason = row[Option[String]](s"$TableName.$rejectedReason"),
      totalScore = row[Option[BigDecimal]](s"$TableName.$totalScore"),

      fullNameScore = row[Option[BigDecimal]](s"$TableName.$fullnameScore"),
      fullNameOriginal = row[Option[String]](s"$TableName.$fullnameOriginal"),
      fullNameUpdated = row[Option[String]](s"$TableName.$fullnameUpdated"),

      birthdateScore = row[Option[BigDecimal]](s"$TableName.$birthdateScore"),
      birthdateOriginal = row[Option[String]](s"$TableName.$birthdateOriginal"),
      birthdateUpdated = row[Option[String]](s"$TableName.$birthdateUpdated"),

      birthplaceScore = row[Option[BigDecimal]](s"$TableName.$birthplaceScore"),
      birthplaceOriginal = row[Option[String]](s"$TableName.$birthplaceOriginal"),
      birthplaceUpdated = row[Option[String]](s"$TableName.$birthplaceUpdated"),

      genderScore = row[Option[BigDecimal]](s"$TableName.$genderScore"),
      genderOriginal = row[Option[String]](s"$TableName.$genderOriginal"),
      genderUpdated = row[Option[String]](s"$TableName.$genderUpdated"),

      nationalityScore = row[Option[BigDecimal]](s"$TableName.$nationalityScore"),
      nationalityOriginal = row[Option[String]](s"$TableName.$nationalityOriginal"),
      nationalityUpdated = row[Option[String]](s"$TableName.$nationalityUpdated"),

      personIdScore = row[Option[BigDecimal]](s"$TableName.$personIdScore"),
      personIdOriginal = row[Option[String]](s"$TableName.$personIdOriginal"),
      personIdUpdated = row[Option[String]](s"$TableName.$personIdUpdated"),

      documentNumberScore = row[Option[BigDecimal]](s"$TableName.$documentNumberScore"),
      documentNumberOriginal = row[Option[String]](s"$TableName.$documentNumberOriginal"),
      documentNumberUpdated = row[Option[String]](s"$TableName.$documentNumberUpdated"),
      documentType = row[Option[String]](s"$TableName.$documentType"),
      documentModel = row[Option[String]](s"$TableName.$documentModel"),

      createdAt = row[LocalDateTime](s"$TableName.$createdAt"),
      createdBy = row[String](s"$TableName.$createdBy"),
      updatedAt = row[Option[LocalDateTime]](s"$TableName.$updatedAt"),
      updatedBy = row[Option[String]](s"$TableName.$updatedBy"))
  }

  def convertRowToCount(row: Row): Int = row[Int]("count(*)")

  private def generateWhereFilter(
    criteria: WalletApplicationCriteria,
    optionalOrdering: Option[OrderingSet] = None) = {

    import SqlDao._

    val filterInvalidStatus = criteria.inactiveStatuses
      .map(v ⇒ queryConditionClauseForFilter(v.toUpperCase, UserSqlDao.status, Some(UserSqlDao.TableAlias)))
    val filters = (Seq(
      criteria.customerId.map(queryConditionClause(_, UserSqlDao.uuid, Some(UserSqlDao.TableAlias))),
      criteria.msisdn.map(queryConditionClause(_, IndividualUserSqlDao.msisdn, Some(IndividualUserSqlDao.TableAlias))),
      criteria.name.
        map(queryConditionClause(_, IndividualUserSqlDao.name, Some(IndividualUserSqlDao.TableAlias))),
      criteria.nationalId.
        map(queryWithIfColIsEmptyStatementsClause(_, Seq(personIdUpdated), personIdOriginal, Some(TableAlias))),
      criteria.fullName.
        map(queryWithIfColIsEmptyStatementsClause(_, Seq(fullnameUpdated), fullnameOriginal, Some(TableAlias))),
      criteria.status
        .map(queryConditionClause(_, status, Some(TableAlias))),
      criteria.applicationStage
        .map(queryConditionClause(_, stage, Some(TableAlias))),
      criteria.checkedBy
        .map(queryConditionClause(_, checkedBy, Some(TableAlias))),
      formDateRange(TableAlias, checkedAt, criteria.checkedAtStartingFrom, criteria.checkedAtUpTo),
      formDateRange(TableAlias, createdAt, criteria.createdAtStartingFrom, criteria.createdAtUpTo))
      .flatten ++ filterInvalidStatus).mkString(" AND ")

    (optionalOrdering.nonEmpty, filters.nonEmpty) match {
      case (true, true) ⇒ s"WHERE $filters ${
        optionalOrdering.get.toString
      }"
      case (true, false) ⇒ optionalOrdering.get.toString
      case (false, true) ⇒ s"WHERE $filters"
      case (_, _) ⇒ ""
    }
  }

  def buildColumnStringForInsert(walletApplicationToCreate: WalletApplicationToCreate): String = {
    Seq(
      walletApplicationToCreate.id.map(_ ⇒ id),
      Some(uuid),
      Some(userId),
      Some(status),
      Some(stage),
      walletApplicationToCreate.rejectedReason.map(_ ⇒ rejectedReason),
      walletApplicationToCreate.checkedBy.map(_ ⇒ checkedBy),
      walletApplicationToCreate.checkedAt.map(_ ⇒ checkedAt),
      walletApplicationToCreate.totalScore.map(_ ⇒ totalScore),

      walletApplicationToCreate.fullnameScore.map(_ ⇒ fullnameScore),
      walletApplicationToCreate.fullnameOriginal.map(_ ⇒ fullnameOriginal),
      walletApplicationToCreate.fullnameUpdated.map(_ ⇒ fullnameUpdated),

      walletApplicationToCreate.birthdateScore.map(_ ⇒ birthdateScore),
      walletApplicationToCreate.birthdateOriginal.map(_ ⇒ birthdateOriginal),
      walletApplicationToCreate.birthdateUpdated.map(_ ⇒ birthdateUpdated),

      walletApplicationToCreate.birthplaceScore.map(_ ⇒ birthplaceScore),
      walletApplicationToCreate.birthplaceOriginal.map(_ ⇒ birthplaceOriginal),
      walletApplicationToCreate.birthplaceUpdated.map(_ ⇒ birthplaceUpdated),

      walletApplicationToCreate.genderScore.map(_ ⇒ genderScore),
      walletApplicationToCreate.genderOriginal.map(_ ⇒ genderOriginal),
      walletApplicationToCreate.genderUpdated.map(_ ⇒ genderUpdated),

      walletApplicationToCreate.nationalityScore.map(_ ⇒ nationalityScore),
      walletApplicationToCreate.nationalityOriginal.map(_ ⇒ nationalityScore),
      walletApplicationToCreate.nationalityUpdated.map(_ ⇒ nationalityUpdated),

      walletApplicationToCreate.personIdScore.map(_ ⇒ personIdScore),
      walletApplicationToCreate.personIdOriginal.map(_ ⇒ personIdOriginal),
      walletApplicationToCreate.personIdUpdated.map(_ ⇒ personIdUpdated),

      walletApplicationToCreate.documentNumberScore.map(_ ⇒ documentNumberScore),
      walletApplicationToCreate.documentNumberOriginal.map(_ ⇒ documentNumberOriginal),
      walletApplicationToCreate.documentNumberUpdated.map(_ ⇒ documentNumberUpdated),
      walletApplicationToCreate.documentType.map(_ ⇒ documentType),

      Some(createdBy),
      Some(createdAt),
      Some(updatedBy),
      Some(updatedAt)).flatten.mkString("(", ", ", ")")
  }

  def buildInsertPlaceHolders(walletApplicationToCreate: WalletApplicationToCreate): String = {
    Seq(
      walletApplicationToCreate.id.map(_ ⇒ s"{$id}"),
      Some(s"{$uuid}"),
      Some(s"{$userId}"),
      Some(s"{$status}"),
      Some(s"{$stage}"),
      walletApplicationToCreate.rejectedReason.map(_ ⇒ s"{$rejectedReason}"),
      walletApplicationToCreate.checkedBy.map(_ ⇒ s"{$checkedBy}"),
      walletApplicationToCreate.checkedAt.map(_ ⇒ s"{$checkedAt}"),
      walletApplicationToCreate.totalScore.map(_ ⇒ s"{$totalScore}"),

      walletApplicationToCreate.fullnameScore.map(_ ⇒ s"{$fullnameScore}"),
      walletApplicationToCreate.fullnameOriginal.map(_ ⇒ s"{$fullnameOriginal}"),
      walletApplicationToCreate.fullnameUpdated.map(_ ⇒ s"{$fullnameUpdated}"),

      walletApplicationToCreate.birthdateScore.map(_ ⇒ s"{$birthdateScore}"),
      walletApplicationToCreate.birthdateOriginal.map(_ ⇒ s"{$birthdateOriginal}"),
      walletApplicationToCreate.birthdateUpdated.map(_ ⇒ s"{$birthdateUpdated}"),

      walletApplicationToCreate.birthplaceScore.map(_ ⇒ s"{$birthplaceScore}"),
      walletApplicationToCreate.birthplaceOriginal.map(_ ⇒ s"{$birthplaceOriginal}"),
      walletApplicationToCreate.birthplaceUpdated.map(_ ⇒ s"{$birthplaceUpdated}"),

      walletApplicationToCreate.genderScore.map(_ ⇒ s"{$genderScore}"),
      walletApplicationToCreate.genderOriginal.map(_ ⇒ s"{$genderOriginal}"),
      walletApplicationToCreate.genderUpdated.map(_ ⇒ s"{$genderUpdated}"),

      walletApplicationToCreate.genderScore.map(_ ⇒ s"{$genderScore}"),
      walletApplicationToCreate.genderOriginal.map(_ ⇒ s"{$genderOriginal}"),
      walletApplicationToCreate.genderUpdated.map(_ ⇒ s"{$genderUpdated}"),

      walletApplicationToCreate.nationalityScore.map(_ ⇒ s"{$nationalityScore}"),
      walletApplicationToCreate.nationalityOriginal.map(_ ⇒ s"{$nationalityScore}"),
      walletApplicationToCreate.nationalityUpdated.map(_ ⇒ s"{$nationalityUpdated}"),

      walletApplicationToCreate.personIdScore.map(_ ⇒ s"{$personIdScore}"),
      walletApplicationToCreate.personIdOriginal.map(_ ⇒ s"{$personIdOriginal}"),
      walletApplicationToCreate.personIdUpdated.map(_ ⇒ s"{$personIdUpdated}"),

      walletApplicationToCreate.documentNumberScore.map(_ ⇒ s"{$documentNumberScore}"),
      walletApplicationToCreate.documentNumberOriginal.map(_ ⇒ s"{$documentNumberOriginal}"),
      walletApplicationToCreate.documentNumberUpdated.map(_ ⇒ s"{$documentNumberUpdated}"),
      walletApplicationToCreate.documentType.map(_ ⇒ s"{$documentType}"),

      Some(s"{$createdBy}"),
      Some(s"{$createdAt}"),
      Some(s"{$updatedBy}"),
      Some(s"{$updatedAt}")).flatten.mkString("(", ", ", ")")
  }

  def buildParametersForInsert(walletApplicationToCreate: WalletApplicationToCreate): Seq[NamedParameter] = {
    Seq[Option[NamedParameter]](
      walletApplicationToCreate.id.map(v ⇒ 'id → v),
      Some('uuid → walletApplicationToCreate.uuid.toString),
      Some('user_id → walletApplicationToCreate.userId),
      Some('status → walletApplicationToCreate.status),
      Some('stage → walletApplicationToCreate.stage),
      walletApplicationToCreate.rejectedReason.map(v ⇒ 'rejected_reason → v),
      walletApplicationToCreate.checkedBy.map(v ⇒ 'checked_by → v),
      walletApplicationToCreate.checkedAt.map(v ⇒ 'checked_at → v),
      walletApplicationToCreate.totalScore.map(v ⇒ 'total_score → v),

      walletApplicationToCreate.fullnameScore.map(v ⇒ 'fullname_score → v),
      walletApplicationToCreate.fullnameOriginal.map(v ⇒ 'fullname_original → v),
      walletApplicationToCreate.fullnameUpdated.map(v ⇒ 'fullname_updated → v),

      walletApplicationToCreate.birthdateScore.map(v ⇒ 'birthdate_score → v),
      walletApplicationToCreate.birthdateOriginal.map(v ⇒ 'birthdate_original → v),
      walletApplicationToCreate.birthdateUpdated.map(v ⇒ 'birthdate_updated → v),

      walletApplicationToCreate.birthplaceScore.map(v ⇒ 'birthplace_score → v),
      walletApplicationToCreate.birthplaceOriginal.map(v ⇒ 'birthplace_original → v),
      walletApplicationToCreate.birthplaceUpdated.map(v ⇒ 'birthplace_updated → v),

      walletApplicationToCreate.genderScore.map(v ⇒ 'gender_score → v),
      walletApplicationToCreate.genderOriginal.map(v ⇒ 'gender_original → v),
      walletApplicationToCreate.genderUpdated.map(v ⇒ 'gender_updated → v),

      walletApplicationToCreate.nationalityScore.map(v ⇒ 'nationality_score → v),
      walletApplicationToCreate.nationalityOriginal.map(v ⇒ 'nationality_original → v),
      walletApplicationToCreate.nationalityUpdated.map(v ⇒ 'nationality_updated → v),

      walletApplicationToCreate.personIdScore.map(v ⇒ 'person_id_score → v),
      walletApplicationToCreate.personIdOriginal.map(v ⇒ 'person_id_original → v),
      walletApplicationToCreate.personIdUpdated.map(v ⇒ 'person_id_updated → v),

      walletApplicationToCreate.documentNumberScore.map(v ⇒ 'document_number_score → v),
      walletApplicationToCreate.documentNumberOriginal.map(v ⇒ 'document_number_original → v),
      walletApplicationToCreate.documentNumberUpdated.map(v ⇒ 'document_number_updated → v),
      walletApplicationToCreate.documentType.map(v ⇒ 'document_type → v),

      Some('created_by → walletApplicationToCreate.createdBy),
      Some('created_at → walletApplicationToCreate.createdAt),
      Some('updated_by → walletApplicationToCreate.updatedBy.getOrElse(walletApplicationToCreate.createdBy)),
      Some('updated_at → walletApplicationToCreate.updatedAt.getOrElse(walletApplicationToCreate.createdAt))).flatten
  }
}

