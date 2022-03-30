package tech.pegb.backoffice.dao.auth.sql

import java.sql.{Connection, SQLException}
import java.time.LocalDateTime
import java.util.UUID

import anorm.{AnormException, NamedParameter, Row, RowParser, SQL, SqlRequestError}
import com.google.inject.{Inject, Singleton}
import play.api.db.DBApi
import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.auth.abstraction.BackOfficeUserDao
import tech.pegb.backoffice.dao.auth.dto.{BackOfficeUserCriteria, BackOfficeUserToInsert, BackOfficeUserToUpdate}
import tech.pegb.backoffice.dao.auth.entity.BackOfficeUser
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.sql.MostRecentUpdatedAtGetter
import tech.pegb.backoffice.dao.util.Implicits._
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

@Singleton
class BackOfficeUserSqlDao @Inject() (val dbApi: DBApi) extends BackOfficeUserDao
  with MostRecentUpdatedAtGetter[BackOfficeUser, BackOfficeUserCriteria] with SqlDao {
  import BackOfficeUserSqlDao._
  import tech.pegb.backoffice.dao.SqlDao._

  def createBackOfficeUser(dto: BackOfficeUserToInsert): DaoResponse[BackOfficeUser] = withConnection(
    { implicit conn ⇒

      val insertSql =
        s"""
           |INSERT INTO ${TableName}
           |($cId,   $cUsername,   $cPassword,   $cRoleId,   $cBuId,   $cEmail,   $cPhoneNumber,
           |$cFName, $cMidName, $cLName, $cDesc, $cHomePage, $cIsActive, $cActiveLang,
           |$cCustomData, $cLastLogin, $cCreatedBy, $cCreatedAt, $cUpdatedAt, $cUpdatedBy)
           |VALUES
           |({$cId}, {$cUsername}, {$cPassword}, {$cRoleId}, {$cBuId}, {$cEmail}, {$cPhoneNumber},
           |{$cFName}, {$cMidName}, {$cLName}, {$cDesc}, {$cHomePage}, {$cIsActive}, {$cActiveLang},
           |{$cCustomData}, {$cLastLogin}, {$cCreatedBy}, {$cCreatedAt}, {$cUpdatedAt}, {$cUpdatedBy});
       """.stripMargin

      val generatedUUID = UUID.randomUUID()
      SQL(insertSql)
        .on(cId → generatedUUID, cUsername → dto.userName, cPassword → dto.password, cRoleId → dto.roleId,
          cBuId → dto.businessUnitId, cEmail → dto.email, cPhoneNumber → dto.phoneNumber, cFName → dto.firstName,
          cMidName → dto.middleName, cLName → dto.lastName, cDesc → dto.description, cHomePage → dto.homePage,
          cIsActive → dto.isActive, cActiveLang → dto.activeLanguage, cCustomData → dto.customData, cLastLogin → dto.lastLoginTimestamp,
          cCreatedBy → dto.createdBy, cCreatedAt → dto.createdAt,
          cUpdatedBy → dto.updatedBy, cUpdatedAt → dto.updatedAt)
        .execute()

      internalGet(generatedUUID.toString) match {
        case Some(result) ⇒ result
        case None ⇒ throw new Exception(s"Failed to execute insert")
      }
    },
    s"Failed to insert back_office_user: ${dto.toSmartString}",
    {
      case error: java.sql.SQLTimeoutException ⇒
        timeoutError(s"Inserting the back_office_user [${dto.toSmartString}] took too long to complete. Confirm later if insert was successful or not.")
      case error: SQLException if isUniqueConstraintViolation(error) ⇒
        entityAlreadyExistsError(s"Failed to insert back_office_user [${dto.toSmartString}]. Id or name may already be existing.")
      case error: SQLException ⇒
        constraintViolationError(s"Failed to insert back_office_user [${dto.toSmartString}]. A value assigned to a column is not allowed.")
      case error: java.net.ConnectException ⇒
        connectionFailed(s"Failed to insert back_office_user [${dto.toSmartString}]. Connection to database was lost.")
    })

  def updateBackOfficeUser(id: String, dto: BackOfficeUserToUpdate): DaoResponse[Option[BackOfficeUser]] = withConnection(
    { implicit conn ⇒

      internalGet(id).flatMap { _ ⇒

        val paramsBuffer = dto.paramsBuilder
        val filterParam = NamedParameter(cId, id)
        paramsBuffer += filterParam

        val preQuery = dto.createSqlString(TableName, Some(s"WHERE ${filterParam.name} = {${filterParam.name}}"))

        val params = paramsBuffer.result()
        val updateResult = SQL(preQuery).on(params: _*).executeUpdate()

        if (updateResult.isUpdated) {
          internalGet(id)
        } else {
          throw new IllegalStateException(s"Update failed. Back_office_user ${id} has been modified by another process.")
        }
      }
    },
    s"Failed to update back_office_user [${id}]",
    {
      case error: java.sql.SQLTimeoutException ⇒
        timeoutError(s"Updating the back_office_user [${dto.toSmartString}] took too long to complete. Confirm later if update was successful or not.")
      case error: SQLException ⇒
        entityAlreadyExistsError(s"Failed to update back_office_user [${dto.toSmartString}]. Id or name may already be existing.")
      case error: java.net.ConnectException ⇒
        connectionFailed(s"Failed to update back_office_user [${dto.toSmartString}]. Connection to database was lost.")
      case error: IllegalStateException ⇒
        preconditionFailed(error.getMessage)
    })

  def countBackOfficeUsersByCriteria(criteria: Option[BackOfficeUserCriteria]): DaoResponse[Int] = withConnection(
    { implicit conn ⇒
      val whereClause = generateWhereFilter(criteria)

      val query = SQL(
        s"""
           |SELECT COUNT($TableAlias.$cId) as n FROM $TableName as $TableAlias
           |$commonJoin
           |$whereClause
         """.stripMargin)

      query.as(query.defaultParser.singleOpt).map(row ⇒ row[Int]("n")).get

    }, s"Unexpected error on count back_office_users by criteria [${criteria.toSmartString}]",
    {
      case error: java.sql.SQLTimeoutException ⇒
        timeoutError(s"Counting back_office_users with criteria [${criteria.toSmartString}] took too long to complete. Confirm later if insert was successful or not.")
      case error: java.net.ConnectException ⇒
        connectionFailed(s"Failed to count back_office_users with criteria [${criteria.toSmartString}]. Connection to database was lost.")
    })

  def getBackOfficeUsersByCriteria(
    criteria: Option[BackOfficeUserCriteria],
    orderBy: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int]): DaoResponse[Seq[BackOfficeUser]] = withConnection(
    { implicit conn ⇒

      val whereClause = generateWhereFilter(criteria)
      val orderByClause = orderBy.fold(s"ORDER BY ${TableAlias}.${cId} ASC") { orderingSet ⇒
        OrderingSet(orderingSet.underlying.map(o ⇒ o.copy(field = s"$TableAlias.${o.field}"))).toString
      }

      val paginationClause = getPagination(limit, offset)

      val query = SQL(
        s"""
           |$commonSelectQuery
           |$whereClause
           |$orderByClause
           |$paginationClause
         """.stripMargin)

      query.executeQuery().as(rowParser.*)
    },
    s"Unexpected error on get back_office_users by criteria [${criteria.toSmartString}]",
    {
      case error: AnormException ⇒
        rowParsingError(s"Data cannot be parsed to back_office_user correctly. Maybe missing/unknown column or type mismatch.")
      case error: java.sql.SQLTimeoutException ⇒
        timeoutError(s"Fetching back_office_users with criteria [${criteria.toSmartString}] took too long to complete.")
      case error: java.net.ConnectException ⇒
        connectionFailed(s"Failed to fetch back_office_users with criteria [${criteria.toSmartString}]. Connection to database was lost.")
    })

  private def internalGet(id: String)(implicit conn: Connection): Option[BackOfficeUser] = {
    val query = SQL(
      s"""
         |$commonSelectQuery WHERE $TableAlias.$cId = {$cId};
         """.stripMargin)

    query.on(cId → id).executeQuery().as(rowParser.singleOpt)
  }

  protected def generateWhereFilter(maybeCriteria: Option[BackOfficeUserCriteria]): String = {
    maybeCriteria.map { criteria ⇒
      Seq(
        criteria.id.map(_.toSql(Some(cId), Some(TableAlias))),
        criteria.userName.map(_.toSql(Some(cUsername), Some(TableAlias))),
        criteria.password.map(_.toSql(Some(cPassword), Some(TableAlias))),

        criteria.roleId.map(_.toSql(Some(cRoleId), Some(TableAlias))),
        criteria.roleName.map(_.toSql(Some(RoleSqlDao.cName), Some(RoleSqlDao.TableAlias))),
        criteria.roleLevel.map(_.toSql(Some(RoleSqlDao.cLevel), Some(RoleSqlDao.TableAlias))),

        criteria.businessUnitId.map(_.toSql(Some(cBuId), Some(TableAlias))),
        criteria.businessUnitName.map(_.toSql(Some(BusinessUnitSqlDao.cName), Some(BusinessUnitSqlDao.TableAlias))),

        criteria.scopeId.map(_.toSql(Some(ScopeSqlDao.cId), Some(ScopeSqlDao.TableAlias))),
        criteria.scopeId.map(_.toSql(Some(ScopeSqlDao.cName), Some(ScopeSqlDao.TableAlias))),

        criteria.email.map(_.toSql(Some(cEmail), Some(TableAlias))),
        criteria.phoneNumber.map(_.toSql(Some(cPhoneNumber), Some(TableAlias))),
        criteria.firstName.map(_.toSql(Some(cFName), Some(TableAlias))),
        criteria.middleName.map(_.toSql(Some(cMidName), Some(TableAlias))),
        criteria.lastName.map(_.toSql(Some(cLName), Some(TableAlias))),

        criteria.isActive.map(_.toSql(Some(cIsActive), Some(TableAlias))),
        criteria.createdAt.map(_.toSql(Some(cCreatedAt), Some(TableAlias))),
        criteria.createdBy.map(_.toSql(Some(cCreatedBy), Some(TableAlias))),
        criteria.updatedAt.map(_.toSql(Some(cUpdatedAt), Some(TableAlias))),
        criteria.updatedBy.map(_.toSql(Some(cUpdatedBy), Some(TableAlias))))
        .flatten.toSql
    }
  }.getOrElse("")

  private val rowParser: RowParser[BackOfficeUser] = row ⇒ Try {
    parseRowToEntity(row)
  }.fold(
    exc ⇒ anorm.Error(SqlRequestError(exc)),
    anorm.Success(_))

  private def parseRowToEntity(row: Row) = {
    BackOfficeUser(
      id = row[String](s"$TableName.$cId"),
      userName = row[String](s"$TableName.$cUsername"),
      password = row[Option[String]](s"$TableName.$cPassword"),
      email = row[String](s"$TableName.$cEmail"),
      phoneNumber = row[Option[String]](s"$TableName.$cPhoneNumber"),
      firstName = row[String](s"$TableName.$cFName"),
      middleName = row[Option[String]](s"$TableName.$cMidName"),
      lastName = row[String](s"$TableName.$cLName"),
      description = row[Option[String]](s"$TableName.$cDesc"),
      homePage = row[Option[String]](s"$TableName.$cHomePage"),
      activeLanguage = row[Option[String]](s"$TableName.$cActiveLang"),
      customData = row[Option[String]](s"$TableName.$cCustomData"),
      lastLoginTimestamp = row[Option[Long]](s"$TableName.$cLastLogin"),
      roleId = row[String](s"$TableName.$cRoleId"),
      roleName = row[String](s"${RoleSqlDao.TableName}.${RoleSqlDao.cName}"),
      roleLevel = row[Int](s"${RoleSqlDao.TableName}.${RoleSqlDao.cLevel}"),
      roleCreatedBy = row[Option[String]](s"${RoleSqlDao.TableName}.${RoleSqlDao.cCreatedBy}"),
      roleUpdatedBy = row[Option[String]](s"${RoleSqlDao.TableName}.${RoleSqlDao.cUpdatedBy}"),
      roleCreatedAt = row[Option[LocalDateTime]](s"${RoleSqlDao.TableName}.${RoleSqlDao.cCreatedAt}"),
      roleUpdatedAt = row[Option[LocalDateTime]](s"${RoleSqlDao.TableName}.${RoleSqlDao.cUpdatedAt}"),
      businessUnitId = row[String](s"$TableName.$cBuId"),
      businessUnitName = row[String](s"${BusinessUnitSqlDao.TableName}.${BusinessUnitSqlDao.cName}"),
      businessUnitCreatedBy = row[Option[String]](s"${BusinessUnitSqlDao.TableName}.${BusinessUnitSqlDao.cCreatedBy}"),
      businessUnitUpdatedBy = row[Option[String]](s"${BusinessUnitSqlDao.TableName}.${BusinessUnitSqlDao.cUpdatedBy}"),
      businessUnitCreatedAt = row[Option[LocalDateTime]](s"${BusinessUnitSqlDao.TableName}.${BusinessUnitSqlDao.cCreatedAt}"),
      businessUnitUpdatedAt = row[Option[LocalDateTime]](s"${BusinessUnitSqlDao.TableName}.$cUpdatedAt"),
      isActive = row[Int](s"$TableName.$cIsActive"),
      createdBy = row[Option[String]](s"$TableName.$cCreatedBy"),
      updatedBy = row[Option[String]](s"$TableName.$cUpdatedBy"),
      createdAt = row[Option[LocalDateTime]](s"$TableName.$cCreatedAt"),
      updatedAt = row[Option[LocalDateTime]](s"$TableName.$cUpdatedAt"))
  }

  def updateLastLoginTimestamp(id: String): DaoResponse[Option[BackOfficeUser]] = withTransaction({ implicit connection ⇒
    val updateResult = SQL(s"UPDATE $TableName SET $cLastLogin = UNIX_TIMESTAMP();").executeUpdate()

    if (updateResult > 0) {
      internalGet(id)
    } else {
      throw new Exception("Error encountered in updateLastLoginTimestamp")
    }

  }, s"Error updating last logintimestamp",
    handlerPF = {
      case error: java.sql.SQLTimeoutException ⇒
        timeoutError(s"Updating the lastLoginTimestamp of user $id took too long to complete. Confirm later if update was successful or not.")
      case error: java.net.ConnectException ⇒
        connectionFailed(s"Failed to update lastLoginTimestamp of user $id. Connection to database was lost.")
      case error ⇒
        logger.error("error encountered in [updateLastLoginTimestamp]", error)
        genericDbError(s"error encountered while updating last login timestamp: id = $id")
    })

  protected def getUpdatedAtColumn = s"$TableAlias.$cUpdatedAt"

  protected def getMainSelectQuery = commonSelectQuery

  protected def getRowToEntityParser = parseRowToEntity

  protected def getWhereFilterFromCriteria(criteriaDto: Option[BackOfficeUserCriteria]) = generateWhereFilter(criteriaDto)
}

object BackOfficeUserSqlDao {
  val TableName = "back_office_users"
  val TableAlias = "bou"

  val cId = "id"

  //TODO change to snake_case when standalone backoffice_auth is taken down
  val cUsername = "userName"
  val cPassword = "password"
  val cRoleId = "roleId"
  val cBuId = "businessUnitId"
  val cEmail = "email"
  val cPhoneNumber = "phoneNumber"
  val cFName = "firstName"
  val cMidName = "middleName"
  val cLName = "lastName"
  val cDesc = "description"
  val cHomePage = "homePage"
  val cIsActive = "is_active"
  val cActiveLang = "activeLanguage"
  val cCustomData = "customData"
  val cLastLogin = "lastLoginTimestamp"
  val cCreatedBy = "created_by"
  val cCreatedAt = "created_at"
  val cUpdatedAt = "updated_at"
  val cUpdatedBy = "updated_by"

  val commonJoin =
    s"""
       |JOIN ${RoleSqlDao.TableName} ${RoleSqlDao.TableAlias}
       |ON ${RoleSqlDao.TableAlias}.${RoleSqlDao.cId} = $TableAlias.$cRoleId
       |
       |JOIN ${BusinessUnitSqlDao.TableName} ${BusinessUnitSqlDao.TableAlias}
       |ON ${BusinessUnitSqlDao.TableAlias}.${BusinessUnitSqlDao.cId} = $TableAlias.$cBuId
     """.stripMargin

  val commonSelectQuery =
    s"""
       |SELECT $TableAlias.*, ${RoleSqlDao.TableAlias}.*, ${BusinessUnitSqlDao.TableAlias}.*
       |FROM $TableName as $TableAlias
       |
       |$commonJoin
     """.stripMargin
}
