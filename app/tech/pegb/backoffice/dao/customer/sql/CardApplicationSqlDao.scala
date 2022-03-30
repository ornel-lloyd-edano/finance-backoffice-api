package tech.pegb.backoffice.dao.customer.sql

import java.sql.Connection

import anorm.{NamedParameter, SQL}
import com.google.inject.Inject
import play.api.db.DBApi
import tech.pegb.backoffice.application.KafkaDBSyncService

import tech.pegb.backoffice.dao.SqlDao
import tech.pegb.backoffice.dao.customer.abstraction.CardApplicationDao
import tech.pegb.backoffice.dao.customer.entity._
import tech.pegb.backoffice.dao.customer.dto._

class CardApplicationSqlDao @Inject() (val dbApi: DBApi, kafkaDBSyncService: KafkaDBSyncService) extends CardApplicationDao with SqlDao {

  import CardApplicationSqlDao._

  def getCardApplication(id: String): DaoResponse[Option[CardApplication]] = ???

  def getCardTypes: DaoResponse[Set[CardType]] = ???

  def getCardApplicationOperations: DaoResponse[Set[CardApplicationOperationType]] = ???

  def getCardApplicationStatuses: DaoResponse[Set[CardApplicationStatus]] = ???

  def getCardApplicationByCriteria(criteria: CardApplicationGetCriteria, limit: Option[Int], offset: Option[Int]): DaoResponse[Seq[CardApplication]] = ???

  def countTotalCardApplicationByCriteria(criteria: CardApplicationGetCriteria): DaoResponse[Int] = ???

  def insertCardApplication(cardApplicationToInsert: CardApplicationToInsert)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[CardApplication] =
    withConnection(
      { implicit connection: Connection ⇒

        val id = SqlDao.genId().toString

        val parameters = buildParametersForCardApplication(id, cardApplicationToInsert)
        insertSql.on(parameters: _*).executeInsert()

        val application = CardApplication(
          id = id,
          userId = cardApplicationToInsert.userId,
          operationType = cardApplicationToInsert.operationType,
          cardType = cardApplicationToInsert.cardType,
          nameOnCard = cardApplicationToInsert.nameOnCard,
          cardPin = cardApplicationToInsert.cardPin,
          deliveryAddress = cardApplicationToInsert.deliveryAddress,
          status = cardApplicationToInsert.status,
          createdAt = cardApplicationToInsert.createdAt,
          createdBy = cardApplicationToInsert.createdBy,
          updatedAt = None,
          updatedBy = None)

        kafkaDBSyncService.sendInsert(CardTable, application)
        application
      }, s"Error while inserting application card $cardApplicationToInsert")

  def updateCardApplication(id: String, cardApplication: CardApplicationToUpdate)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Option[CardApplication]] = ???

  def updateCardApplicationByCriteria(criteria: CardApplicationGetCriteria, cardApplication: CardApplicationToUpdate)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Option[CardApplication]] = ???

  def deleteCardApplication(id: String)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Unit] = ???

  def deleteCardApplicationByCriteria(criteria: CardApplicationGetCriteria)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Int] = ???
}

object CardApplicationSqlDao {

  private[dao] final val CardTable = "card_applications"

  private[dao] final val Fields: Seq[String] =
    Seq(
      "id", "user_id", "operation_type", "card_type", "name_on_card", "card_pin",
      "delivery_address", "status", "created_at", "created_by", "updated_at", "updated_by")

  private[dao] final val insertSql = SQL(
    s"""INSERT INTO $CardTable $Fields VALUES ({"id"}, {"user_id"}, {"operation_type"}, {"card_type"},
       |{"name_on_card"}, {"card_pin"}, {"delivery_address"}, {"status"}, {"created_at"}, {"created_by"},
       | {"updated_at"}, {"updated_by}");""".stripMargin)

  def buildParametersForCardApplication(
    id: String,
    cardApplication: CardApplicationToInsert): Seq[NamedParameter] =
    Seq[NamedParameter](
      "id" → id,
      "user_id" → cardApplication.userId,
      "operation_type" → cardApplication.operationType,
      "card_type" → cardApplication.cardType,
      "name_on_card" → cardApplication.nameOnCard,
      "card_pin" → cardApplication.cardPin,
      "delivery_address" → cardApplication.deliveryAddress,
      "status" → cardApplication.status,
      "created_at" → cardApplication.createdAt,
      "created_by" → cardApplication.createdBy,
      "updated_at" → cardApplication.createdAt, //not nullable in db and same as created at on insertion
      "updated_by" → cardApplication.createdBy) //not nullable in db and same as created by on insertion

}

