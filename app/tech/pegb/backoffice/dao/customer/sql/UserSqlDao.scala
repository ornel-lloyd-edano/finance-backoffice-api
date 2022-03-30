package tech.pegb.backoffice.dao.customer.sql

import java.sql.Connection
import java.time.LocalDateTime

import anorm._
import cats.implicits._
import com.google.inject.Inject
import play.api.db.DBApi
import tech.pegb.backoffice.application.KafkaDBSyncService
import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.account.sql.AccountSqlDao
import tech.pegb.backoffice.dao.customer.abstraction.UserDao
import tech.pegb.backoffice.dao.customer.dto.{GenericUserCriteria, UserToInsert, UserToUpdate}
import tech.pegb.backoffice.dao.customer.entity.{GenericUser, User}
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.sql.MostRecentUpdatedAtGetter
import tech.pegb.backoffice.dao.util.Implicits._

import scala.util.Try

class UserSqlDao @Inject()(val dbApi: DBApi, kafkaDBSyncService: KafkaDBSyncService) extends UserDao
  with MostRecentUpdatedAtGetter[GenericUser, GenericUserCriteria]
  with SqlDao {

  import SqlDao._
  import UserSqlDao._

  protected def getUpdatedAtColumn: String = s"${TableAlias}.${updatedAt}"

  protected def getMainSelectQuery: String = qCommonSelect

  protected def getRowToEntityParser: Row ⇒ GenericUser = (arg: Row) ⇒ rowToGenericUser(arg)

  protected def getWhereFilterFromCriteria(criteriaDto: Option[GenericUserCriteria]): String = generateWhereFilter(criteriaDto)

  def insertUser(user: UserToInsert): DaoResponse[(Int, String)] = withConnectionAndFlatten({ implicit connection ⇒
    val gid = SqlDao.genId().toString
    val parameters = Seq[NamedParameter](
      uuid → gid,
      username → user.userName.trim,
      password → user.password,
      email → user.email,
      activatedAt → Option.empty[LocalDateTime],
      passwordUpdatedAt → Option.empty[LocalDateTime],
      createdAt → user.createdAt,
      createdBy → user.createdBy,
      updatedAt → user.createdAt, //not nullable in db and same as created at on insertion
      updatedBy → user.createdBy, //not nullable in db and same as created by on insertion
      subscription → user.subscription,
      typeName → user.`type`,
      status → user.status,
      tier → user.tier,
      segment → user.segment)

    (for {
      id ← insertUserSql.on(parameters: _*) executeInsert SqlParser.scalar[Int].singleOpt
    } yield {
      val created = findByPrimaryId(id).map(row ⇒ convertRowToUser(row).get)
      created.foreach(u ⇒ kafkaDBSyncService.sendInsert(TableName, u))

      (id, uuid)
    })
      .toRight(genericDbError(s"Could not insert user $user"))

  }, s"Error while trying to insert user $user")

  def getUser(uuid: String): DaoResponse[Option[User]] =
    withConnection(
      { implicit connection: Connection ⇒

        findByIdInternal(uuid).map(row ⇒ convertRowToUser(row).get)
      }, s"Error while retrieving user by id:$uuid")

  def updateUser(uuid: String, user: UserToUpdate)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Option[User]] =
    withTransaction({ connection: Connection ⇒

      val allSetEntries = Seq(
        user.userName.map(queryConditionClause(_, username)),
        user.password.map(queryConditionClause(_, password)),
        user.`type`.map(queryConditionClause(_, typeName)),
        user.tier.map(queryConditionClause(_, tier)),
        user.segment.map(queryConditionClause(_, segment)),
        user.subscription.map(queryConditionClause(_, subscription)),
        user.email.map(queryConditionClause(_, email)),
        user.status.map(queryConditionClause(_, status)),
        user.activatedAt.map(queryConditionClause(_, activatedAt)),
        user.passwordUpdatedAt.map(queryConditionClause(_, passwordUpdatedAt)),
        Some(queryConditionClause(user.updatedAt, updatedAt, Some(TableName))),
        Some(queryConditionClause(user.updatedBy, updatedBy, Some(TableName)))).flatten.mkString(", ")

      val updatedRowsN = prepareUpdateSql(uuid, allSetEntries).executeUpdate()(maybeTransaction.getOrElse(connection))

      if (updatedRowsN.isUpdated) {
        val userOpt = findByIdInternal(uuid)(maybeTransaction.getOrElse(connection)).map(row ⇒ convertRowToUser(row).get)
        userOpt.foreach(u ⇒ kafkaDBSyncService.sendInsert(TableName, u))
        userOpt
      } else {
        None
      }
    }, s"Error while updating user with id $uuid and values $user")

  def getInternalUserId(uuid: String): DaoResponse[Option[Int]] = withConnection(
    { implicit connection: Connection ⇒
      findIdByUUIDInternal(uuid)
        .map(row ⇒ row[Int]("id"))
    }, s"Error while retrieving internal user id from `users` with uuid $uuid")

  def getUUIDByInternalUserId(id: Int): DaoResponse[Option[String]] = withConnection(
    { implicit connection: Connection ⇒
      findUUIDByIdIn(id)
        .map(row ⇒ row[String]("uuid"))
    }, s"Error while retrieving internal user uuid from `users` with id $id")

  def getUserByUserId(userId: Int): DaoResponse[Option[User]] =
    withConnection(
      { implicit connection: Connection ⇒

        findByPrimaryId(userId).map(row ⇒ convertRowToUser(row).get)

      }, s"Error while retrieving user by primary id:$userId")

  def getUserByCriteria(
                         criteria: GenericUserCriteria,
                         orderBy: Option[OrderingSet],
                         limit: Option[Int],
                         offset: Option[Int]): DaoResponse[Seq[GenericUser]] = withConnection({ implicit connection: Connection ⇒
    val whereFilter = generateWhereFilter(criteria.some)

    val aliasedOrderBy = orderBy.map(
      _.underlying.map(o ⇒ o.copy(maybeTableNameOrAlias = columnTableLookUp.get(o.field))))
      .map(OrderingSet(_))

    val genericUserByCriteriaSql = findGenericUserByCriteriaQuery(whereFilter, aliasedOrderBy, limit, offset)

    genericUserByCriteriaSql
      .as(genericUserRowParser.*)

  }, s"Can't retrieve user:$criteria with offset:$offset and limit:$limit")


  def countUserByCriteria(criteria: GenericUserCriteria): DaoResponse[Int] = withConnection({ implicit connection ⇒
    val whereFilter = generateWhereFilter(criteria.some)
    val column = "COUNT(*) as n"
    val countByCriteriaSql = SQL(s"""${baseFindGenericUserByCriteria(column, whereFilter)}""")

    countByCriteriaSql
      .as(countByCriteriaSql.defaultParser.singleOpt)
      .map(row ⇒ rowToCountParser(row)).getOrElse(0)

  }, s"Error while retrieving count by criteria:$criteria")


  def convertRowToUser(row: Row): Try[User] = Try {
    User(
      id = row[Int](id),
      uuid = row[String](uuid),
      userName = row[String](username),
      password = row[Option[String]](password),
      `type` = row[Option[String]](typeName),
      tier = row[Option[String]](tier),
      segment = row[Option[String]](segment),
      subscription = row[Option[String]](subscription),
      email = row[Option[String]](email),
      status = row[Option[String]](status),
      activatedAt = row[Option[LocalDateTime]](activatedAt),
      passwordUpdatedAt = row[Option[LocalDateTime]](passwordUpdatedAt),
      createdAt = row[LocalDateTime](createdAt),
      createdBy = row[String](createdBy),
      updatedAt = row[Option[LocalDateTime]](updatedAt),
      updatedBy = row[Option[String]](updatedBy))
  }

  private def prepareUpdateSql(id: String, setValues: String): SimpleSql[Row] = {
    val sql = s"UPDATE $TableName SET $setValues WHERE $uuid = {uuid}"
    logger.debug(s"prepareUpdateSql:${System.lineSeparator()}$sql")
    SQL(sql).on('uuid → id)
  }

}

object UserSqlDao {

  final val TableName = "users"
  final val TableAlias = "u"

  final val id = "id"
  final val uuid = "uuid"
  final val username = "username"
  final val password = "password"
  final val email = "email"

  final val activatedAt = "activated_at"
  final val passwordUpdatedAt = "password_updated_at"
  final val createdAt = "created_at"
  final val createdBy = "created_by"
  final val updatedAt = "updated_at"
  final val updatedBy = "updated_by"
  final val subscription = "subscription"
  final val typeName = "type"
  final val status = "status"
  final val tier = "tier"
  final val segment = "segment"

  val columnTableLookUp: Map[String, String] = Map(
    username → TableAlias,
    tier → TableAlias,
    segment → TableAlias,
    subscription → TableAlias,
    email → TableAlias,
    status → TableAlias,
    typeName → TableAlias,
    createdAt → TableAlias,
    createdBy → TableAlias,
    updatedAt → TableAlias,
    updatedBy → TableAlias,

    IndividualUserSqlDao.msisdn → IndividualUserSqlDao.TableAlias,
    IndividualUserSqlDao.name → IndividualUserSqlDao.TableAlias,
    IndividualUserSqlDao.fullName → IndividualUserSqlDao.TableAlias,
    IndividualUserSqlDao.gender → IndividualUserSqlDao.TableAlias,
    IndividualUserSqlDao.personId → IndividualUserSqlDao.TableAlias,
    IndividualUserSqlDao.documentNumber → IndividualUserSqlDao.TableAlias,
    IndividualUserSqlDao.documentType → IndividualUserSqlDao.TableAlias,
    IndividualUserSqlDao.nationality → IndividualUserSqlDao.TableAlias,
    IndividualUserSqlDao.occupation → IndividualUserSqlDao.TableAlias,
    IndividualUserSqlDao.company → IndividualUserSqlDao.TableAlias,
    IndividualUserSqlDao.employer → IndividualUserSqlDao.TableAlias
  )

  private[dao] final val userFields: Seq[String] =
    Seq(
      uuid, username, password, email, activatedAt, passwordUpdatedAt, createdAt, createdBy,
      updatedAt, updatedBy, subscription, typeName, status, tier, segment)
  private[dao] final val userFieldsAsString: String = userFields.mkString(", ")
  private[dao] final val userFieldsTupleAsString: String = s"($userFieldsAsString)"

  private[dao] final val insertUserSql = SQL(
    s"""INSERT INTO $TableName $userFieldsTupleAsString VALUES ({$uuid},{$username},{$password},
       |{$email},{$activatedAt},{$passwordUpdatedAt},{$createdAt},{$createdBy},{$updatedAt},{$updatedBy},
       |{$subscription},{$typeName},{$status},{$tier},{$segment});""".stripMargin)

  private[dao] final val findUserByIdSql = SQL(s"""SELECT * FROM $TableName WHERE $uuid = {uuid}""")

  private[dao] final val findUserByPrimaryIdSql = SQL(s"""SELECT * FROM $TableName WHERE $id = {id}""")

  def findByIdInternal(uuid: String)(implicit connection: Connection): Option[Row] = findUserByIdSql
    .on('uuid → uuid)
    .as(findUserByIdSql.defaultParser.*)
    .headOption

  def findByPrimaryId(id: Int)(implicit connection: Connection): Option[Row] = findUserByPrimaryIdSql
    .on('id → id)
    .as(findUserByPrimaryIdSql.defaultParser.*)
    .headOption

  private[dao] final val findIdByUUIDSql = SQL(s"""SELECT id FROM $TableName WHERE $uuid = {uuid}""")

  private[dao] final val findUUIDByIdSql = SQL(s"""SELECT uuid FROM $TableName WHERE $id = {id}""")

  private def findIdByUUIDInternal(userUUID: String)(implicit connection: Connection) = findIdByUUIDSql
    .on('uuid → userUUID)
    .as(findIdByUUIDSql.defaultParser.singleOpt)

  private def findUUIDByIdIn(userId: Int)(implicit connection: Connection) = findUUIDByIdSql
    .on('id → userId)
    .as(findUUIDByIdSql.defaultParser.singleOpt)

  def buildParametersForUserInsert(
                                    userUUID: String,
                                    user: UserToInsert): Seq[NamedParameter] =
    Seq[NamedParameter](
      uuid → userUUID,
      username → user.userName.trim,
      password → user.password,
      email → user.email,
      activatedAt → Option.empty[LocalDateTime],
      passwordUpdatedAt → Option.empty[LocalDateTime],
      createdAt → user.createdAt,
      createdBy → user.createdBy,
      updatedAt → user.createdAt, //not nullable in db and same as created at on insertion
      updatedBy → user.createdBy, //not nullable in db and same as created by on insertion
      subscription → user.subscription,
      typeName → user.`type`,
      status → user.status,
      tier → user.tier,
      segment → user.segment)

  private def baseFindGenericUserByCriteria(selectColumns: String, filters: String): String = {
    s"""SELECT $selectColumns
       |FROM $TableName $TableAlias
       |LEFT JOIN ${IndividualUserSqlDao.TableName} ${IndividualUserSqlDao.TableAlias}
       |ON $TableAlias.$id = ${IndividualUserSqlDao.TableAlias}.${IndividualUserSqlDao.userId}
       |LEFT JOIN ${BusinessUserSqlDao.TableName} ${BusinessUserSqlDao.TableAlias}
       |ON $TableAlias.$id = ${BusinessUserSqlDao.TableAlias}.${BusinessUserSqlDao.cUserId}
       |LEFT JOIN ${AccountSqlDao.TableName} ca
       |ON ${BusinessUserSqlDao.TableAlias}.${BusinessUserSqlDao.cCollectionAccountId} = ca.${AccountSqlDao.cId}
       |LEFT JOIN ${AccountSqlDao.TableName} da
       |ON ${BusinessUserSqlDao.TableAlias}.${BusinessUserSqlDao.cDistributionAccountId} = da.${AccountSqlDao.cId}
       |$filters""".stripMargin
  }

  private val qCommonSelect =
    s"""SELECT $TableAlias.*, ${IndividualUserSqlDao.TableAlias}.*, ${BusinessUserSqlDao.TableAlias}.*,
       |ca.uuid as ${BusinessUserSqlDao.cCollectionAccountUUID},
       |da.uuid as ${BusinessUserSqlDao.cDistributionAccountUUID}
       |FROM $TableName $TableAlias
       |LEFT JOIN ${IndividualUserSqlDao.TableName} ${IndividualUserSqlDao.TableAlias}
       |ON $TableAlias.$id = ${IndividualUserSqlDao.TableAlias}.${IndividualUserSqlDao.userId}
       |LEFT JOIN ${BusinessUserSqlDao.TableName} ${BusinessUserSqlDao.TableAlias}
       |ON $TableAlias.$id = ${BusinessUserSqlDao.TableAlias}.${BusinessUserSqlDao.cUserId}
       |LEFT JOIN ${AccountSqlDao.TableName} ca
       |ON ${BusinessUserSqlDao.TableAlias}.${BusinessUserSqlDao.cCollectionAccountId} = ca.${AccountSqlDao.cId}
       |LEFT JOIN ${AccountSqlDao.TableName} da
       |ON ${BusinessUserSqlDao.TableAlias}.${BusinessUserSqlDao.cDistributionAccountId} = da.${AccountSqlDao.cId}""".stripMargin

  private def findGenericUserByCriteriaQuery(
                                              filters: String,
                                              maybeOrderBy: Option[OrderingSet],
                                              maybeLimit: Option[Int],
                                              maybeOffset: Option[Int]): SqlQuery = {

    val ordering = maybeOrderBy.fold("")(_.toString)
    val pagination = SqlDao.getPagination(maybeLimit, maybeOffset)

    val columns =
      s"""$TableAlias.*,
         |${IndividualUserSqlDao.TableAlias}.*,
         |${BusinessUserSqlDao.TableAlias}.*,
         |ca.uuid as ${BusinessUserSqlDao.cCollectionAccountUUID},
         |da.uuid as ${BusinessUserSqlDao.cDistributionAccountUUID}""".stripMargin

    SQL(s"""${baseFindGenericUserByCriteria(columns, filters)} $ordering $pagination""".stripMargin)
  }

  private def generateWhereFilter(criteriaOption: Option[GenericUserCriteria]): String = {
    import SqlDao._
    val u = Some(TableAlias)
    val iu = Some(IndividualUserSqlDao.TableAlias)
    val bu = Some(BusinessUserSqlDao.TableAlias)

    criteriaOption.fold("") { criteria ⇒
      Seq(
        criteria.anyName.map { cf ⇒
          s"(${cf.toSql(username.some, u)} OR " +
            s"${cf.toSql(IndividualUserSqlDao.name.some, iu)} OR " +
            s"${cf.toSql(IndividualUserSqlDao.fullName.some, iu)} OR " +
            s"${cf.toSql(BusinessUserSqlDao.cBusinessName.some, bu)} OR " +
            s"${cf.toSql(BusinessUserSqlDao.cBrandName.some, bu)})"
        },

        criteria.userUuid.map(_.toSql(uuid.some, u)),
        criteria.msisdn.map(_.toSql(IndividualUserSqlDao.msisdn.some, iu)),
        criteria.tier.map(_.toSql(tier.some, u)),
        criteria.status.map(_.toSql(status.some, u)),
        criteria.segment.map(_.toSql(segment.some, u)),
        criteria.subscription.map(_.toSql(subscription.some, u)),
        criteria.customerType.map(_.toSql(typeName.some, u)),
        criteria.name.map { cf ⇒
          s"(${cf.toSql(IndividualUserSqlDao.name.some, iu)} OR " +
            s"${cf.toSql(IndividualUserSqlDao.fullName.some, iu)} OR " +
            s"${cf.toSql(BusinessUserSqlDao.cBusinessName.some, bu)} OR " +
            s"${cf.toSql(BusinessUserSqlDao.cBrandName.some, bu)})"
        },

        criteria.createdBy.map(_.toSql(createdBy.some, u)),
        criteria.updatedBy.map(_.toSql(updatedBy.some, u)),
        criteria.createdAt.map(_.toFormattedDateTime.toSql(createdAt.some, u)),
        criteria.updatedAt.map(_.toFormattedDateTime.toSql(updatedAt.some, u)),

        //individual user specific filters
        criteria.fullName.map { cf ⇒
          s"(${cf.toSql(IndividualUserSqlDao.fullName.some, iu)} OR ${cf.toSql(IndividualUserSqlDao.name.some, iu)})"
        },
        criteria.gender.map(_.toSql(IndividualUserSqlDao.gender.some, iu)),
        criteria.personId.map(_.toSql(IndividualUserSqlDao.personId.some, iu)),
        criteria.documentNumber.map(_.toSql(IndividualUserSqlDao.documentNumber.some, iu)),
        criteria.documentType.map(_.toSql(IndividualUserSqlDao.documentType.some, iu)),
        criteria.birthDate.map(_.toSql(IndividualUserSqlDao.birthdate.some, iu)),
        criteria.birthPlace.map(_.toSql(IndividualUserSqlDao.birthPlace.some, iu)),
        criteria.nationality.map(_.toSql(IndividualUserSqlDao.nationality.some, iu)),
        criteria.occupation.map(_.toSql(IndividualUserSqlDao.occupation.some, iu)),
        criteria.company.map(_.toSql(IndividualUserSqlDao.company.some, iu)),
        criteria.employer.map(_.toSql(IndividualUserSqlDao.employer.some, iu))).flatten.toSql
    }
  }

  private def rowToCountParser(row: Row): Int = row[Int]("n")

  def rowToGenericUser(row: Row): GenericUser = {
    val u = TableName
    val iu = IndividualUserSqlDao.TableName
    val bu = BusinessUserSqlDao.TableName

    val individualUserIdOption = row[Option[Int]](s"$iu.$id")
    val businessUserIdOption = row[Option[Int]](s"$bu.$id")

    GenericUser(
      id = row[Int](s"$u.$id"),
      uuid = row[String](s"$u.$uuid"),
      userName = row[String](s"$u.$username"),
      password = row[Option[String]](s"$u.$password"),
      customerType = row[Option[String]](s"$u.$typeName"),
      tier = row[Option[String]](s"$u.$tier"),
      segment = row[Option[String]](s"$u.$segment"),
      subscription = row[Option[String]](s"$u.$subscription"),
      email = row[Option[String]](s"$u.$email"),
      status = row[Option[String]](s"$u.$status"),
      activatedAt = row[Option[LocalDateTime]](s"$u.$activatedAt"),
      passwordUpdatedAt = row[Option[LocalDateTime]](s"$u.$passwordUpdatedAt"),
      createdAt = row[LocalDateTime](s"$u.$createdAt"),
      createdBy = row[String](s"$u.$createdBy"),
      updatedAt = row[Option[LocalDateTime]](s"$u.$updatedAt"),
      updatedBy = row[Option[String]](s"$u.$updatedBy"),

      customerName = (individualUserIdOption, businessUserIdOption) match {
        case (Some(_), None) ⇒
          row[Option[String]](s"$iu.${IndividualUserSqlDao.fullName}")
            .orElse(row[Option[String]](s"$iu.${IndividualUserSqlDao.name}"))
        case (None, Some(_)) ⇒
          row[Option[String]](s"$bu.${BusinessUserSqlDao.cBusinessName}")
        case _ ⇒
          row[Option[String]](s"$u.$username")
      },

      //TODO what is the need to get the whole businessUser and individualUser data?
      businessUserFields = None,
      individualUserFields = None,
    )
  }

  private val genericUserRowParser: RowParser[GenericUser] = row ⇒ {
    val u = TableName
    val iu = IndividualUserSqlDao.TableName
    val bu = BusinessUserSqlDao.TableName

    val individualUserIdOption = row[Option[Int]](s"$iu.$id")
    val businessUserIdOption = row[Option[Int]](s"$bu.$id")

    (for{
      individualUserIdOption ← Try(row[Option[Int]](s"$iu.$id"))
      businessUserIdOption ← Try(row[Option[Int]](s"$bu.$id"))
      genericUser ← Try(rowToGenericUser(row))
      individualUserFieldsOption ← if(individualUserIdOption.isDefined){
        Try(Some(IndividualUserSqlDao.convertRowToIndividualUser(row)))
      } else {
        scala.util.Success(None)
      }
      businessUserFieldsOption ← if(businessUserIdOption.isDefined){
        BusinessUserSqlDao.convertRowToBusinessUser(row).map(Option(_))
      } else {
        scala.util.Success(None)
      }
    } yield {
      genericUser.copy(
        businessUserFields = businessUserFieldsOption,
        individualUserFields = individualUserFieldsOption)
    }).fold(
      exc ⇒ anorm.Error(SqlRequestError(exc)),
      anorm.Success(_))
  }

}
