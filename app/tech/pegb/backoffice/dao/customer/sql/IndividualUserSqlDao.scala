package tech.pegb.backoffice.dao.customer.sql

import java.sql.{Connection, SQLException}
import java.time.{LocalDate, LocalDateTime}

import anorm._
import com.google.inject.Inject
import play.api.db.DBApi
import tech.pegb.backoffice.application.KafkaDBSyncService

import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.account.sql.AccountSqlDao
import tech.pegb.backoffice.dao.application.sql.WalletApplicationSqlDao
import tech.pegb.backoffice.dao.customer.abstraction.{IndividualUserDao, UserDao}
import tech.pegb.backoffice.dao.customer.dto.IndividualUserToUpdate
import tech.pegb.backoffice.dao.customer.entity.IndividualUser
import tech.pegb.backoffice.dao.customer.dto.IndividualUserCriteria
import tech.pegb.backoffice.dao.model.{GroupingField, MatchTypes, Ordering}
import tech.pegb.backoffice.dao.sql.MostRecentUpdatedAtGetter
import tech.pegb.backoffice.dao.customer.dto.CustomerAggregation
import tech.pegb.backoffice.dao.transaction.dto.TransactionCriteria
import tech.pegb.backoffice.dao.transaction.sql.TransactionSqlDao

import scala.util.Try

class IndividualUserSqlDao @Inject() (
    val dbApi: DBApi,
    userDao: UserDao,
    kafkaDBSyncService: KafkaDBSyncService) extends IndividualUserDao with MostRecentUpdatedAtGetter[IndividualUser, IndividualUserCriteria] with SqlDao {

  import IndividualUserSqlDao._

  protected def getUpdatedAtColumn: String = s"${IndividualUserSqlDao.TableAlias}.${IndividualUserSqlDao.updatedAt}"

  protected def getMainSelectQuery: String = IndividualUserSqlDao.qCommonSelectJoin

  protected def getRowToEntityParser: Row ⇒ IndividualUser = (arg: Row) ⇒ IndividualUserSqlDao.convertRowToIndividualUser(arg)

  protected def getWhereFilterFromCriteria(criteriaDto: Option[IndividualUserCriteria]): String = criteriaDto.map(generateWhereFilter(_)).getOrElse("")

  def getIndividualUser(uuid: String): DaoResponse[Option[IndividualUser]] = {
    withConnection({ implicit connection: Connection ⇒
      queryFindUserAndIndividualUserJoin.on("user_uuid" → uuid)
        .as(queryFindUserAndIndividualUserJoin.defaultParser.singleOpt)
        .map(convertRowToIndividualUser)
    }, s"Couldn't find individual user with uuid = $uuid")
  }

  override def getIndividualUsersByCriteria(
    criteria: IndividualUserCriteria,
    orderBy: Seq[Ordering],
    limit: Option[Int] = None,
    offset: Option[Int] = None): DaoResponse[Seq[IndividualUser]] =
    withConnection({ implicit connection: Connection ⇒
      val whereFilter = generateWhereFilter(criteria)
      findIndividualUserByCriteriaSql(whereFilter, orderBy, limit, offset)
        .as(findIndividualUserByCriteriaSql(whereFilter, orderBy, limit, offset).defaultParser.*)
        .map(convertRowToIndividualUser)
    }, s"Can't retrieve user and individual_user join by criteria:$criteria with offset:$offset and limit:$limit")

  def countIndividualUserByCriteria(criteria: IndividualUserCriteria): DaoResponse[Int] =
    withConnection(
      { implicit connection: Connection ⇒
        val whereFilter = generateWhereFilter(criteria)
        val countIndividualUserByCriteria = SQL(s"SELECT count(*) $qCommonJoin $whereFilter")

        countIndividualUserByCriteria.as(countIndividualUserByCriteria.defaultParser.singleOpt)
          .map(convertRowToCount).headOption.get

      }, s"Error while retrieving count for of user and individual_user join by criteria:$criteria")

  def updateStatusByMsisdn(msisdn: String, status: String): DaoResponse[Option[IndividualUser]] =
    withConnection({ implicit connection: Connection ⇒

      val updatedCount = queryUpdateStatusByMsisdn(msisdn, status).executeUpdate()

      if (updatedCount > 0) {
        val individualUser = queryFindUserByMsisdn
          .on("msisdn" -> msisdn)
          .as(queryFindUserByMsisdn.defaultParser.singleOpt)
          .map(convertRowToIndividualUser)

        individualUser.foreach(iu ⇒ kafkaDBSyncService.sendUpdate(TableName, iu))
        individualUser
      } else None

    }, s"Couldn't find update status by msisdn = $msisdn")

  def updateIndividualUser(
    uuid: String,
    individualUser: IndividualUserToUpdate)(implicit maybeTransaction: Option[Connection]): DaoResponse[Option[IndividualUser]] = {
    withTransaction(
      { connection: Connection ⇒

        val allSetEntries = generateUpdateClause(individualUser, Some(TableAlias))
        val updatedRowsN = prepareIndividualUserUpdateSql(uuid, allSetEntries)
          .executeUpdate()(maybeTransaction.getOrElse(connection))

        if (updatedRowsN > 0) {
          val individualUser = findIndividualUserByIdInternal(uuid)(maybeTransaction.getOrElse(connection))
            .map(convertRowToIndividualUser)

          individualUser.foreach(iu ⇒ kafkaDBSyncService.sendUpdate(TableName, iu))
          individualUser
        } else {
          None
        }
      }, "Unexpected exception in updateIndividualUser", {
        case e: SQLException ⇒
          val errorMessage = s"Could not update user $uuid"
          logger.error(errorMessage, e)
          constraintViolationError(errorMessage)
      })

  }

  override def aggregateCustomersByCriteriaAndPivots(
    criteria: IndividualUserCriteria,
    trxCriteria: TransactionCriteria,
    grouping: Seq[GroupingField]): DaoResponse[Seq[CustomerAggregation]] =
    withConnection({ implicit connection: Connection ⇒
      val aggrSql = aggregateIndividualUsersByCriteriaSql(criteria, trxCriteria, grouping)
      aggrSql.as(aggrSql.defaultParser.*).map(convertRowToCustomerAggregation)
    }, s"Can't retrieve user aggregations by criteria:$criteria, grouped by: $grouping")
}

object IndividualUserSqlDao {

  import UserSqlDao._
  import SqlDao._

  final val TableName = "individual_users"
  final val TableAlias = "iu"

  //columns
  final val msisdn = "msisdn"
  final val userId = "user_id"
  final val typeName = "type"
  final val name = "name"
  final val fullName = "fullname"
  final val gender = "gender"
  final val personId = "person_id"
  final val documentNumber = "document_number"
  final val documentType = "document_type"
  final val documentModel = "document_model"
  final val company = "company"
  final val birthdate = "birthdate"
  final val birthPlace = "birth_place"
  final val nationality = "nationality"
  final val occupation = "occupation"
  final val employer = "employer"
  final val createdAt = "created_at"
  final val updatedAt = "updated_at"
  final val createdBy = "created_by"
  final val updatedBy = "updated_by"

  private val qCommonJoin =
    s"""
       |FROM ${UserSqlDao.TableName} ${UserSqlDao.TableAlias}
       |INNER JOIN $TableName $TableAlias
       |ON ${UserSqlDao.TableAlias}.${UserSqlDao.id} = $TableAlias.$userId
     """.stripMargin

  private val qCommonSelectJoin =
    s"""
       |SELECT ${UserSqlDao.TableAlias}.*, $TableAlias.*
       |$qCommonJoin
     """.stripMargin

  private def queryUpdateStatusByMsisdn(msisdn: String, status: String) = {
    val subquery = SqlDao.subQueryConditionClause(
      fieldValue = msisdn,
      fieldName = id,
      tableName = UserSqlDao.TableAlias,
      refFieldName = userId,
      conditionalFieldName = IndividualUserSqlDao.msisdn,
      refTableName = TableName)

    SQL(
      s"""
         |UPDATE ${UserSqlDao.TableName} AS ${UserSqlDao.TableAlias} SET ${UserSqlDao.TableAlias}.${UserSqlDao.status} = '$status'
         |WHERE $subquery;
     """.stripMargin)
  }

  private val queryFindUserAndIndividualUserJoin = SQL(
    s"""
       |$qCommonSelectJoin
       |WHERE ${UserSqlDao.TableAlias}.${UserSqlDao.uuid} = {user_uuid}
     """.stripMargin)

  private val queryFindUserByMsisdn =
    SQL(
      s"""
         |SELECT $TableAlias.*,${UserSqlDao.TableAlias}.*
         |FROM $TableName $TableAlias
         |INNER JOIN ${UserSqlDao.TableName} ${UserSqlDao.TableAlias}
         |ON ${UserSqlDao.TableAlias}.${UserSqlDao.id} = $TableAlias.$userId
         |WHERE $TableAlias.$msisdn = {msisdn}
     """.stripMargin)

  private[sql] final val findIndividualUserByIdSql =
    SQL(s"SELECT * FROM $TableName WHERE $userId = {user_id}")

  def findIndividualUserByIdInternal(uuid: String)(implicit connection: Connection): Option[Row] =
    queryFindUserAndIndividualUserJoin
      .on('user_uuid → uuid)
      .as(queryFindUserAndIndividualUserJoin.defaultParser.*)
      .headOption

  def prepareIndividualUserUpdateSql(individualUserId: String, setValues: String): SimpleSql[Row] =
    SQL(
      s"""UPDATE $TableName $TableAlias SET $setValues WHERE
         | $TableAlias.$userId = (SELECT $id FROM ${UserSqlDao.TableName} WHERE $uuid = '$individualUserId')""".stripMargin)

  private def createOrderBy(maybeOrderBy: Seq[Ordering]): String = {
    val lookUpMap = Map(
      "id" → s"${UserSqlDao.TableAlias}.${UserSqlDao.id}",
      "uuid" → s"${UserSqlDao.TableAlias}.${UserSqlDao.uuid}",
      "username" → s"${UserSqlDao.TableAlias}.${UserSqlDao.username}",
      "tier" → s"${UserSqlDao.TableAlias}.${UserSqlDao.tier}",
      "segment" → s"${UserSqlDao.TableAlias}.${UserSqlDao.segment}",
      "subscription" → s"${UserSqlDao.TableAlias}.${UserSqlDao.subscription}",
      "email" → s"${UserSqlDao.TableAlias}.${UserSqlDao.email}",
      "status" → s"${UserSqlDao.TableAlias}.${UserSqlDao.status}")

    if (maybeOrderBy.isEmpty) ""
    else maybeOrderBy.map(o ⇒ s"${lookUpMap.get(o.field).getOrElse(s"$TableAlias.${o.field}")} ${o.order}").mkString("ORDER BY ", ", ", " ")
  }

  def findIndividualUserByCriteriaSql(
    filters: String,
    maybeOrderBy: Seq[Ordering],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): SqlQuery = {
    val pagination = SqlDao.getPagination(maybeLimit, maybeOffset)

    val ordering = createOrderBy(maybeOrderBy)

    SQL(s"$qCommonSelectJoin $filters $ordering $pagination".stripMargin)
  }

  def aggregateIndividualUsersByCriteriaSql(criteria: IndividualUserCriteria, trxCriteria: TransactionCriteria,
    grouping: Seq[GroupingField]): SqlQuery = {
    val whereFilter = generateWhereFilter(criteria)
    val groupingString = groupBy(UserSqlDao.TableAlias, grouping)
    val selectWithGroupingString = selectWithGroups(UserSqlDao.TableAlias, grouping)
    val trxQueryFilters = { //TODO refactor into something like TransactionSqlDao.mapCriteriaFieldsToQuery(Option(trxCriteria)).fold(...)
      val str = TransactionSqlDao.mapCriteriaFieldsToQuery(Option(trxCriteria))
      if (str.trim.nonEmpty) " AND " + str else str
    }
    /* FIXME this should probably be done on a less coupled way, but this was the only way I found to join against ONLY
     * the last application for each user (NDB does not support windowing functions such as rank() and that will also
     * make it coupled to a specific version of the driver and db. */
    SQL(s"""SELECT $selectWithGroupingString,
           |COUNT(${UserSqlDao.TableAlias}.$id) as $cCount,
           |SUM(${TransactionSqlDao.cAmount}) as $cSum
           |FROM ${UserSqlDao.TableName} ${UserSqlDao.TableAlias}
           |LEFT OUTER JOIN ${AccountSqlDao.TableName} ${AccountSqlDao.TableAlias}
           |on ${AccountSqlDao.TableAlias}.${AccountSqlDao.cUserId} = ${UserSqlDao.TableAlias}.id
           |LEFT OUTER JOIN ${TransactionSqlDao.TableName} ${TransactionSqlDao.TableAlias}
           |on ${TransactionSqlDao.TableAlias}.${TransactionSqlDao.cPrimaryAccountId} = ${AccountSqlDao.TableAlias}.${AccountSqlDao.cId} $trxQueryFilters
           |LEFT OUTER JOIN (
           |  SELECT ${WalletApplicationSqlDao.status}, ${WalletApplicationSqlDao.id}, ${WalletApplicationSqlDao.userId}
           |  FROM (SELECT @prev := '') init
           |  JOIN (
           |  SELECT ${WalletApplicationSqlDao.userId} != @prev AS first, @prev := ${WalletApplicationSqlDao.userId}, ${WalletApplicationSqlDao.status}, ${WalletApplicationSqlDao.id}, ${WalletApplicationSqlDao.userId}
           |  FROM  ${WalletApplicationSqlDao.TableName}
           |  ORDER BY ${WalletApplicationSqlDao.userId}, ${WalletApplicationSqlDao.updatedAt} desc
           |  LIMIT 999999
           |  ) x where x.first
           |) ${WalletApplicationSqlDao.TableAlias}
           |on ${WalletApplicationSqlDao.TableAlias}.${WalletApplicationSqlDao.userId} = ${UserSqlDao.TableAlias}.$id
           |$whereFilter
           |$groupingString
          """.stripMargin)
  }

  def countIndividualUsersByCriteriaSql(filters: String): SqlQuery =
    SQL(s"SELECT count(*) $qCommonJoin $filters")

  def convertRowToCount(row: Row): Int = row[Int]("count(*)")

  def convertRowToIndividualUser(row: Row): IndividualUser = {
    IndividualUser(
      id = row[Int](s"${UserSqlDao.TableName}.${UserSqlDao.id}"),
      uuid = row[String](s"${UserSqlDao.TableName}.${UserSqlDao.uuid}").toString,
      username = row[Option[String]](s"${UserSqlDao.TableName}.${UserSqlDao.username}"),
      password = row[Option[String]](s"${UserSqlDao.TableName}.${UserSqlDao.password}"),
      tier = row[Option[String]](s"${UserSqlDao.TableName}.${UserSqlDao.tier}"),
      segment = row[Option[String]](s"${UserSqlDao.TableName}.${UserSqlDao.segment}"),
      subscription = row[Option[String]](s"${UserSqlDao.TableName}.${UserSqlDao.subscription}"),
      email = row[Option[String]](s"${UserSqlDao.TableName}.${UserSqlDao.email}"),
      status = row[String](s"${UserSqlDao.TableName}.${UserSqlDao.status}"),

      msisdn = row[String](s"${IndividualUserSqlDao.TableName}.${IndividualUserSqlDao.msisdn}"),
      `type` = row[Option[String]](s"${IndividualUserSqlDao.TableName}.${IndividualUserSqlDao.typeName}"),
      name = row[Option[String]](s"${IndividualUserSqlDao.TableName}.${IndividualUserSqlDao.name}"),
      fullName = row[Option[String]](s"${IndividualUserSqlDao.TableName}.${IndividualUserSqlDao.fullName}"),
      gender = row[Option[String]](s"${IndividualUserSqlDao.TableName}.${IndividualUserSqlDao.gender}"),
      personId = row[Option[String]](s"${IndividualUserSqlDao.TableName}.${IndividualUserSqlDao.personId}"),
      documentNumber = row[Option[String]](s"${IndividualUserSqlDao.TableName}.${IndividualUserSqlDao.documentNumber}"),
      documentType = row[Option[String]](s"${IndividualUserSqlDao.TableName}.${IndividualUserSqlDao.documentType}"),
      documentModel = row[Option[String]](s"${IndividualUserSqlDao.TableName}.${IndividualUserSqlDao.documentModel}"),
      company = row[Option[String]](s"${IndividualUserSqlDao.TableName}.${IndividualUserSqlDao.company}"),
      birthDate = row[Option[LocalDate]](s"${IndividualUserSqlDao.TableName}.${IndividualUserSqlDao.birthdate}"),
      birthPlace = row[Option[String]](s"${IndividualUserSqlDao.TableName}.${IndividualUserSqlDao.birthPlace}"),
      nationality = row[Option[String]](s"${IndividualUserSqlDao.TableName}.${IndividualUserSqlDao.nationality}"),
      occupation = row[Option[String]](s"${IndividualUserSqlDao.TableName}.${IndividualUserSqlDao.occupation}"),
      employer = row[Option[String]](s"${IndividualUserSqlDao.TableName}.${IndividualUserSqlDao.employer}"),
      createdAt = row[LocalDateTime](s"${IndividualUserSqlDao.TableName}.${IndividualUserSqlDao.createdAt}"),
      updatedAt = row[Option[LocalDateTime]](s"${IndividualUserSqlDao.TableName}.${IndividualUserSqlDao.updatedAt}"),
      createdBy = row[String](s"${IndividualUserSqlDao.TableName}.${IndividualUserSqlDao.createdBy}"),
      updatedBy = row[Option[String]](s"${IndividualUserSqlDao.TableName}.${IndividualUserSqlDao.updatedBy}"),
      activatedAt = row[Option[LocalDateTime]](s"${UserSqlDao.TableName}.${UserSqlDao.activatedAt}"))
  }

  def convertRowToCustomerAggregation(row: Row): CustomerAggregation = {
    CustomerAggregation(
      userName = Try(row[String](s"${UserSqlDao.TableName}.${UserSqlDao.username}")).toOption,
      tier = Try(row[String](s"${UserSqlDao.TableName}.${UserSqlDao.tier}")).toOption,
      segment = Try(row[String](s"${UserSqlDao.TableName}.${UserSqlDao.segment}")).toOption,
      subscription = Try(row[String](s"${UserSqlDao.TableName}.${UserSqlDao.subscription}")).toOption,
      email = Try(row[String](s"${UserSqlDao.TableName}.${UserSqlDao.email}")).toOption,
      status = Try(row[String](s"${UserSqlDao.TableName}.${UserSqlDao.status}")).toOption,
      msisdn = Try(row[String](s"$msisdn")).toOption,
      name = Try(row[String](s"$name")).toOption,
      fullName = Try(row[String](s"$fullName")).toOption,
      gender = Try(row[String](s"$gender")).toOption,
      personId = Try(row[String](s"$personId")).toOption,
      documentNumber = Try(row[String](s"$documentNumber")).toOption,
      documentType = Try(row[String](s"$documentType")).toOption,
      companyName = Try(row[String](s"$company")).toOption,
      birthDate = Try(row[LocalDate](s"$birthdate")).toOption,
      birthPlace = Try(row[String](s"$birthPlace")).toOption,
      nationality = Try(row[String](s"$nationality")).toOption,
      createdAt = Try(row[LocalDateTime](s"$createdAt")).toOption,
      activatedAt = Try(row[LocalDateTime](s"${UserSqlDao.TableName}.${UserSqlDao.activatedAt}")).toOption,
      isActivated = Try(row[Int]("is_activated") == 1).toOption,
      isActive = Try(row[Int]("is_active") == 1).toOption,
      applicationStatus = Try(row[String]("applicationStatus")).toOption,
      date = Try(row[Option[LocalDate]](cDate).get).toOption,
      day = Try(row[Option[Int]](cDay).get).toOption,
      month = Try(row[Option[Int]](cMonth).get).toOption,
      year = Try(row[Option[Int]](cYear).get).toOption,
      hour = Try(row[Option[Int]](cHour).get).toOption,
      minute = Try(row[Option[Int]](cMinute).get).toOption,
      sum = Try(row[BigDecimal](cSum)).toOption,
      count = Try(row[Long](cCount)).toOption)
  }

  def generateUpdateClause(
    individualUserToUpdate: IndividualUserToUpdate,
    tableName: Option[String] = None): String = {
    import SqlDao._

    val updateUserValues =
      Seq(
        individualUserToUpdate.`type`.map(queryConditionClause(_, typeName, tableName)),
        individualUserToUpdate.msisdn.map(queryConditionClause(_, msisdn, tableName)),
        individualUserToUpdate.name.map(queryConditionClause(_, name, tableName)),
        individualUserToUpdate.fullName.map(queryConditionClause(_, fullName, tableName)),
        individualUserToUpdate.gender.map(queryConditionClause(_, gender, tableName)),
        individualUserToUpdate.personId.map(queryConditionClause(_, personId, tableName)),
        individualUserToUpdate.documentNumber.map(queryConditionClause(_, documentNumber, tableName)),
        individualUserToUpdate.documentType.map(queryConditionClause(_, documentType, tableName)),
        individualUserToUpdate.company.map(queryConditionClause(_, company, tableName)),
        individualUserToUpdate.birthDate.map(queryConditionClause(_, birthdate, tableName)),
        individualUserToUpdate.birthPlace.map(queryConditionClause(_, birthPlace, tableName)),
        individualUserToUpdate.nationality.map(queryConditionClause(_, nationality, tableName)),
        individualUserToUpdate.occupation.map(queryConditionClause(_, occupation, tableName)),
        individualUserToUpdate.employer.map(queryConditionClause(_, employer, tableName)),
        individualUserToUpdate.updatedAt.map(queryConditionClause(_, updatedAt, tableName)),
        individualUserToUpdate.updatedBy.map(queryConditionClause(_, updatedBy, tableName)))
        .flatten.mkString(", ")

    if (updateUserValues.nonEmpty) s"$updateUserValues" else ""
  }

  private def generateWhereFilter(criteria: IndividualUserCriteria): String = {
    import SqlDao._
    val alias = Some(TableAlias)
    val uAlias = Some(UserSqlDao.TableAlias)

    val userUuid = criteria.userUuid.map { cf ⇒
      queryConditionClause(cf.value, UserSqlDao.uuid, uAlias, cf.operator == MatchTypes.Partial)
    }

    val userMsisdn = criteria.msisdn.map { cf ⇒
      queryConditionClause(cf.value, msisdn, alias, cf.operator == MatchTypes.Partial)
    }

    val userTier = criteria.tier
      .map(queryConditionClause(_, tier, alias))
    val userStatus = criteria.status.
      map(queryConditionClause(_, status, uAlias))
    val userSegment = criteria.segment
      .map(queryConditionClause(_, segment, alias))
    val userSubscription = criteria.subscription
      .map(queryConditionClause(_, subscription, uAlias))

    val individualUserType = criteria.individualUserType
      .map(queryConditionClause(_, typeName, alias))
    val nameQ = criteria.name
      .map(queryConditionClause(_, name, alias))

    val fullNameQ = criteria.fullName.map { cf ⇒
      s"(${queryConditionClause(cf.value, fullName, alias, cf.operator == MatchTypes.Partial)} OR ${queryConditionClause(cf.value, name, alias, cf.operator == MatchTypes.Partial)})"
    }

    val genderQ = criteria.gender
      .map(queryConditionClause(_, gender, alias))
    val personIdQ = criteria.personId
      .map(queryConditionClause(_, personId, alias))
    val documentNumberQ = criteria.documentNumber
      .map(queryConditionClause(_, documentNumber, alias))
    val documentTypeQ = criteria.documentType
      .map(queryConditionClause(_, documentType, alias))
    val userBirthDate = criteria.birthDate
      .map(queryConditionClause(_, birthdate, alias))
    val userBirthPlace = criteria.birthPlace
      .map(queryConditionClause(_, birthPlace, alias))
    val userNationality = criteria.nationality
      .map(queryConditionClause(_, nationality, alias))
    val userOccupation = criteria.occupation
      .map(queryConditionClause(_, occupation, alias))
    val userCompanyName = criteria.company
      .map(queryConditionClause(_, company, alias))
    val userEmployer = criteria.employer
      .map(queryConditionClause(_, employer, alias))
    val cBy = criteria.createdBy.map(queryConditionClause(_, createdBy, uAlias))
    val cRange = formDateRange(UserSqlDao.TableAlias, UserSqlDao.createdAt, criteria.createdDateFrom, criteria.createdDateTo)
    val uRange = formDateRange(TableAlias, updatedAt, criteria.updatedDateFrom, criteria.updatedDateTo) //using individual user table for update range
    val uBy = criteria.updatedBy.map(queryConditionClause(_, updatedBy, alias))

    val filters = Seq(userUuid, userMsisdn, userTier, userStatus, userSegment, userSubscription,
      individualUserType, nameQ, fullNameQ, genderQ, personIdQ, documentNumberQ, documentTypeQ,
      userBirthDate, userBirthPlace, userNationality, userOccupation,
      userCompanyName, userEmployer, cBy, cRange, uRange, uBy)
      .flatten.mkString(" AND ")

    if (filters.nonEmpty) s"WHERE $filters"
    else ""

  }
}
