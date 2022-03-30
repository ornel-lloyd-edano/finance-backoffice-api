package tech.pegb.backoffice.dao.businessuserapplication.sql

import java.sql.Connection
import java.time.LocalDateTime

import anorm._
import com.google.inject.{Inject, Singleton}
import play.api.db.DBApi
import tech.pegb.backoffice.dao.{RowParsingException, SqlDao}
import tech.pegb.backoffice.dao.businessuserapplication.abstraction.CountryDao
import tech.pegb.backoffice.dao.businessuserapplication.dto.{CountryToUpsert}
import tech.pegb.backoffice.dao.businessuserapplication.entity.Country
import tech.pegb.backoffice.dao.currency.sql.CurrencySqlDao
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

@Singleton
class CountrySqlDao @Inject() (val dbApi: DBApi) extends CountryDao with SqlDao {
  import CountrySqlDao._

  def getCountries: DaoResponse[Seq[Country]] = withConnection({ implicit conn ⇒
    val rawSql =
      s"""
         |SELECT $TableAlias.*, ${CurrencySqlDao.cName}
         |FROM $TableName $TableAlias
         |LEFT JOIN ${CurrencySqlDao.TableName} ${CurrencySqlDao.TableAlias}
         |ON $TableAlias.$cCurrencyId = ${CurrencySqlDao.TableAlias}.${CurrencySqlDao.cId}
       """.stripMargin

    val sql = SQL(rawSql)
    val results = sql.as(sql.defaultParser.*).map(parseRow)

    val isThereParsingError = results.find(_.isFailure).isDefined

    if (isThereParsingError) throw new RowParsingException(s"Failed to parse result of this query: $rawSql")

    results.map(_.get)
  }, s"Unexpected error in getCountries")

  def upsertCountry(dto: Seq[CountryToUpsert]): DaoResponse[Unit] =
    withTransaction({ implicit conn ⇒
      val (countriesToUpdate, countriesToInsert) = dto.partition(isNameExisting)

      if (countriesToInsert.nonEmpty) {
        val insertSql =
          s"""
             |INSERT INTO ${TableName}
             |($cName,   $cLabel,   $cIcon,   $cIsActive,   $cCurrencyId,   $cCreatedAt,   $cUpdatedAt)
             |VALUES
             |({$cName}, {$cLabel}, {$cIcon}, {$cIsActive}, {$cCurrencyId}, {$cCreatedAt}, {$cUpdatedAt})
         """.stripMargin

        BatchSql(
          insertSql,
          getNamedParams(countriesToInsert.head),
          countriesToInsert.tail.map(getNamedParams): _*).execute()
      }

      if (countriesToUpdate.nonEmpty) {
        val updateSql =
          s"""
             |UPDATE ${TableName}
             |SET
             |$cLabel = {$cLabel}, $cIcon = {$cIcon}, $cIsActive = {$cIsActive},
             |$cCurrencyId = {$cCurrencyId}, $cUpdatedAt = {$cUpdatedAt}
             |WHERE $cName = {$cName}
           """.stripMargin

        countriesToUpdate.foreach(e ⇒ {
          SQL(updateSql).on(getNamedParams(e): _*).executeUpdate()
        })
      }

      ()

    }, s"Unexpected error in upsertCountry(${dto.map(_.toSmartString).defaultMkString})")

  private def getNamedParams(dto: CountryToUpsert) = {
    Seq[NamedParameter](
      cName → dto.name,
      cIcon → dto.icon,
      cLabel → dto.label,
      cIsActive → dto.isActive.map(_.toInt),
      cCurrencyId → dto.currencyId,
      cCreatedAt → dto.createdAt,
      cUpdatedAt → dto.updatedAt)
  }

  private def isNameExisting(dto: CountryToUpsert)(implicit conn: Connection): Boolean = {
    val rawSql =
      s"""
         |SELECT * FROM $TableName WHERE $cName = {$cName}
       """.stripMargin

    val sql = SQL(rawSql).on(cName → dto.name)
    sql.as(sql.defaultParser.singleOpt).isDefined
  }

  private def parseRow(row: Row) = Try {
    Country(
      id = row[Int](cId),
      name = row[String](cName),
      label = row[Option[String]](cLabel),
      icon = row[Option[String]](cIcon),
      isActive = row[Option[Int]](cIsActive).map(_.toBoolean),
      currencyId = row[Option[Int]](cCurrencyId),
      currencyCode = row[Option[String]](CurrencySqlDao.cName),
      createdBy = "Not Available",
      createdAt = row[LocalDateTime](cCreatedAt),
      updatedAt = row[Option[LocalDateTime]](cUpdatedAt))
  }
}

object CountrySqlDao {
  val TableName = "countries"
  val TableAlias = "co"

  val cId = "id"
  val cName = "name"
  val cLabel = "label"
  val cIcon = "icon"
  val cIsActive = "is_active"
  val cCurrencyId = "currency_id"
  val cCreatedAt = "created_at"
  val cUpdatedAt = "updated_at"
}
