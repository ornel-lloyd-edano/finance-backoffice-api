package tech.pegb.backoffice.dao.currency.sql

import java.sql.Connection
import java.time.LocalDateTime

import anorm.SqlParser._
import anorm.{Row, SQL, SqlQuery}
import com.google.inject.Inject
import play.api.db.DBApi
import tech.pegb.backoffice.application.KafkaDBSyncService

import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.SqlDao.queryConditionClause
import tech.pegb.backoffice.dao.currency.abstraction.CurrencyDao
import tech.pegb.backoffice.dao.currency.dto.{CurrencyToUpdate, CurrencyToUpsert}
import tech.pegb.backoffice.dao.currency.entity.Currency
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.Implicits._

class CurrencySqlDao @Inject() (
    override protected val dbApi: DBApi,
    kafkaDBSyncService: KafkaDBSyncService)
  extends CurrencyDao with SqlDao {

  import CurrencySqlDao._

  def bulkUpsert(dto: Seq[CurrencyToUpsert], createdAt: LocalDateTime, createdBy: String): DaoResponse[Seq[Currency]] = {
    withTransaction({ implicit connection ⇒
      assert(dto.nonEmpty, "cannot upsert empty list of currencies")
      val result = dto.map { currencyToUpsert ⇒
        currencyToUpsert.id.fold {

          SQL(
            s"""
               |INSERT INTO $TableName
               |($cName, $cDescription, $cCreatedAt, $cCreatedBy, $cUpdatedAt, $cUpdatedBy, $cIsActive, $cIcon)
               |VALUES
               |${
              s"('${currencyToUpsert.name}', ${currencyToUpsert.description.map(d ⇒ s"'$d'").getOrElse("null")}, " +
                s"{$cCreatedAt}, {$cCreatedBy}, {$cUpdatedAt}, {$cUpdatedBy}," +
                s"'${currencyToUpsert.isActive.toInt}', ${currencyToUpsert.icon.map(i ⇒ s"'$i'").getOrElse("null")} )"
            }
           ;""".stripMargin).on(
              cCreatedAt → createdAt,
              cCreatedBy → createdBy,
              cUpdatedAt → createdAt, //not nullable in db and same as created at on insertion
              cUpdatedBy → createdBy).execute() //not nullable in db and same as created by on insertion

        } { existingId ⇒

          SQL(
            s"""
               |INSERT INTO $TableName
               |($cId, $cName, $cDescription, $cCreatedAt, $cCreatedBy, $cUpdatedAt, $cUpdatedBy, $cIsActive, $cIcon)
               |VALUES
               |${
              s"('$existingId', '${currencyToUpsert.name}', ${currencyToUpsert.description.map(d ⇒ s"'$d'").getOrElse("null")}, " +
                s"{$cCreatedAt}, {$cCreatedBy}, {$cUpdatedAt}, {$cUpdatedBy}, " +
                s"'${currencyToUpsert.isActive.toInt}', ${currencyToUpsert.icon.map(i ⇒ s"'$i'").getOrElse("null")} )"
            }
               |ON DUPLICATE KEY UPDATE
               |$cName = VALUES($cName),
               |$cDescription = VALUES($cDescription),
               |$cUpdatedAt   = VALUES($cCreatedAt),
               |$cUpdatedBy   = VALUES($cCreatedBy),
               |$cIsActive    = VALUES($cIsActive),
               |$cIcon        = VALUES($cIcon)
           ;""".stripMargin).on(
              cCreatedAt → createdAt,
              cCreatedBy → createdBy,
              cUpdatedAt → createdAt, //not nullable in db and same as created at on insertion
              cUpdatedBy → createdBy).execute() //not nullable in db and same as created by on insertion

        }

      }

      if (result.nonEmpty) {
        val currencyList = findAllInternal
        currencyList.foreach { currency ⇒
          kafkaDBSyncService.sendUpsert(TableName, currency)
        }
        currencyList
      } else {
        throw new Throwable("could not perform upsert for currencies")
      }

    }, s"could not bulk upsert to currencies")
  }

  def update(id: Int, currencyToUpdate: CurrencyToUpdate): DaoResponse[Option[Currency]] = {
    withConnection({ implicit connection ⇒
      val setClause = updateClause(currencyToUpdate)

      val result = updateSql(id, setClause).executeUpdate()
      if (result > 0) {
        findByIdInternal(id).map { currency ⇒
          kafkaDBSyncService.sendUpdate(TableName, currency)
          currency
        }
      } else None
    }, s"could not update currency $currencyToUpdate")
  }

  def getAll: DaoResponse[Set[Currency]] = {
    withConnection({ implicit connection ⇒

      selectAllQuery.as(selectAllQuery.defaultParser.*).map(convertRowToCurrency(_)).toSet
    }, s"could not fetch currencies from $TableName")
  }

  override def getAllNames: DaoResponse[Set[String]] = {
    withConnection({ implicit connection ⇒
      namesQuery
        .as(str(cName).*).toSet
    }, s"Couldn't fetch values from $TableName")
  }

  override def getCurrenciesWithId(hasIsActiveFilter: Option[Boolean] = None): DaoResponse[List[(Int, String)]] = {
    withConnection({ implicit connection ⇒
      hasIsActiveFilter match {
        case Some(value) ⇒
          namesWithIdQueryWithIsActiveFilter.on(cIsActive → value.toInt)
            .as((int(cId) ~ str(cName)).map(flatten).*)
        case None ⇒
          namesWithIdQuery
            .as((int(cId) ~ str(cName)).map(flatten).*)
      }
    }, s"Couldn't fetch values from $TableName")
  }

  override def getCurrenciesWithIdExtended: DaoResponse[List[(Int, String, String)]] = {
    withConnection({ implicit connection ⇒
      namesWithIdExtendedQuery
        .as((int(cId) ~ str(cName) ~ str(cDescription)).map(flatten).*)
    }, s"Couldn't fetch values from $TableName")
  }

  override def isCurrencyActive(currencyName: String): DaoResponse[Boolean] = {
    withConnectionAndFlatten({ implicit connection ⇒
      isActiveQuery
        .on(cName → currencyName)
        .as(int(cIsActive).singleOpt)
        .map(_ != 0)
        .toRight(entityNotFoundError(s"Currency $currencyName was not found"))
    }, s"Couldn't check if currency $currencyName exists")
  }

  private def findByIdInternal(id: Int)(implicit connection: Connection) =
    findByIdSql.on('id → id).as(findByIdSql.defaultParser.singleOpt)
      .map(convertRowToCurrency(_))

  private def findAllInternal(implicit connection: Connection) =
    selectAllQuery.as(selectAllQuery.defaultParser.*)
      .map(convertRowToCurrency(_))

}

object CurrencySqlDao {
  val TableName = "currencies"
  val TableAlias = "c"

  val cId = "id"
  val cName = "currency_name"
  val cDescription = "description"
  val cIsActive = "is_active"
  val cCreatedAt = "created_at"
  val cCreatedBy = "created_by"
  val cUpdatedAt = "updated_at"
  val cUpdatedBy = "updated_by"
  val cIcon = "icon"

  private[dao] val TableStr: String = Seq(cName, cDescription, cIsActive, cIcon, cCreatedAt, cCreatedBy, cUpdatedAt, cUpdatedBy).mkString(", ")

  private[dao] val insertQuery: SqlQuery = SQL(s"INSERT INTO $TableName ($TableStr) VALUES ({$cName}, {$cDescription}," +
    s" {$cIsActive}, {$cIcon}, {$cCreatedAt}, {$cCreatedBy}, {$cUpdatedAt}, {$cUpdatedBy})")

  private[dao] val findByIdSql = SQL(s"SELECT * FROM $TableName WHERE $cId={id}")

  private[dao] val selectAllQuery = SQL(s"SELECT * FROM $TableName")

  private[dao] val namesQuery: SqlQuery = SQL(s"SELECT $cName FROM $TableName")

  private[dao] val namesWithIdQuery: SqlQuery = SQL(s"SELECT $cId, $cName FROM $TableName")

  private[dao] val namesWithIdQueryWithIsActiveFilter: SqlQuery = SQL(s"SELECT $cId, $cName FROM $TableName WHERE $cIsActive = {$cIsActive}")

  private[dao] val namesWithIdExtendedQuery: SqlQuery = SQL(s"SELECT $cId, $cName, $cDescription FROM $TableName")

  private[dao] val isActiveQuery: SqlQuery = SQL(s"SELECT $cIsActive FROM $TableName WHERE $cName = {$cName} LIMIT 1;")

  private def updateSql(id: Int, setClause: String) = SQL(
    s"UPDATE $TableName $TableAlias SET $setClause WHERE $cId=$id")

  private[dao] def convertRowToCurrency(row: Row) = {
    Currency(
      id = row[Int](cId),
      name = row[String](cName),
      description = row[Option[String]](cDescription),
      isActive = row[Int](cIsActive).toBoolean,
      icon = row[Option[String]](cIcon),
      createdAt = row[LocalDateTime](cCreatedAt),
      createdBy = row[String](cCreatedBy),
      updatedAt = row[Option[LocalDateTime]](cUpdatedAt),
      updatedBy = row[Option[String]](cUpdatedBy))
  }

  private def updateClause(currencyToUpdate: CurrencyToUpdate): String =
    Seq(
      currencyToUpdate.currencyName.map(queryConditionClause(_, cName, Some(TableAlias))),
      currencyToUpdate.description.map(queryConditionClause(_, cDescription, Some(TableAlias))),
      currencyToUpdate.icon.map(queryConditionClause(_, cIcon, Some(TableAlias))),
      currencyToUpdate.isActive.map(queryConditionClause(_, cIsActive, Some(TableAlias))),
      Some(queryConditionClause(currencyToUpdate.updatedAt, cUpdatedAt, Some(TableAlias))),
      Some(queryConditionClause(currencyToUpdate.updatedBy, cUpdatedBy, Some(TableAlias)))).flatten.mkString(",")

}
