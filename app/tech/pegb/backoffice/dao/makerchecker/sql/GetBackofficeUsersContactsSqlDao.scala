package tech.pegb.backoffice.dao.makerchecker.sql

import anorm._
import com.google.inject.Inject
import play.api.db.DBApi
import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.makerchecker.abstraction.GetBackofficeUsersContactsDao
import tech.pegb.backoffice.dao.makerchecker.dto.BackofficeUserContact

import scala.util.Try

class GetBackofficeUsersContactsSqlDao @Inject() (val dbApi: DBApi) extends GetBackofficeUsersContactsDao with SqlDao {

  val TableName = "back_office_users"
  val TableAlias = "bou"
  val cId = "id"
  val cEmail = "email"
  val cPhoneNum = "phoneNumber"
  val cRoleId = "roleId"
  val cBuId = "businessUnitId"

  object Role {
    val TableName = "roles"
    val TableAlias = "r"
    val cId = "id"
    val cName = "name"
    val cLvl = "level"
  }

  object BusinessUnit {
    val TableName = "business_units"
    val TableAlias = "bu"
    val cId = "id"
    val cName = "name"
  }

  val query =
    s"""
       |SELECT $TableAlias.$cId, $TableAlias.$cEmail, $TableAlias.$cPhoneNum
       |FROM $TableName as $TableAlias
       |
       |INNER JOIN ${Role.TableName} as ${Role.TableAlias}
       |ON ${Role.TableAlias}.${Role.cId} = ${TableAlias}.${cRoleId}
       |
       |INNER JOIN ${BusinessUnit.TableName} as ${BusinessUnit.TableAlias}
       |ON ${BusinessUnit.TableAlias}.${BusinessUnit.cId} = ${TableAlias}.${cBuId}
       |
       |WHERE ${Role.TableAlias}.${Role.cLvl} <= {${Role.cLvl}}
       |AND ${BusinessUnit.TableAlias}.${BusinessUnit.cName} = {${BusinessUnit.cName}};
     """.stripMargin

  def getBackofficeUsersContactsByRoleLvlAndBusinessUnit(lessThanOrEqualThisRoleLevel: Int, businessUnit: String) = {
    withConnection({ implicit conn ⇒

      logger.info(s"query = $query")

      SQL(query).on(Role.cLvl → lessThanOrEqualThisRoleLevel, BusinessUnit.cName → businessUnit)
        .as(backofficeContactParser.*)

    }, s"Error while retrieving back_office_users less than equal level $lessThanOrEqualThisRoleLevel" +
      s" from business unit $businessUnit")
  }

  private val backofficeContactParser: RowParser[BackofficeUserContact] = row ⇒ {
    Try {
      BackofficeUserContact(
        backofficeUserId = row[String](cId),
        email = row[String](cEmail),
        phoneNumber = row[String](cPhoneNum))
    }.fold(
      exc ⇒ anorm.Error(SqlRequestError(exc)),
      anorm.Success(_))
  }
}
