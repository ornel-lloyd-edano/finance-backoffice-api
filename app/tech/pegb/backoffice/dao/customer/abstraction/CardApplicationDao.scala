package tech.pegb.backoffice.dao.customer.abstraction

import java.sql.Connection

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao

import tech.pegb.backoffice.dao.customer.entity._
import tech.pegb.backoffice.dao.customer.dto._
import tech.pegb.backoffice.dao.customer.sql.CardApplicationSqlDao

@ImplementedBy(classOf[CardApplicationSqlDao])
trait CardApplicationDao extends Dao {
  def getCardApplication(id: String): DaoResponse[Option[CardApplication]]

  def getCardTypes: DaoResponse[Set[CardType]]

  def getCardApplicationOperations: DaoResponse[Set[CardApplicationOperationType]]

  def getCardApplicationStatuses: DaoResponse[Set[CardApplicationStatus]]

  def getCardApplicationByCriteria(criteria: CardApplicationGetCriteria, limit: Option[Int], offset: Option[Int]): DaoResponse[Seq[CardApplication]]

  def countTotalCardApplicationByCriteria(criteria: CardApplicationGetCriteria): DaoResponse[Int]

  def insertCardApplication(cardApplication: CardApplicationToInsert)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[CardApplication]

  def updateCardApplication(id: String, cardApplication: CardApplicationToUpdate)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Option[CardApplication]]

  def updateCardApplicationByCriteria(criteria: CardApplicationGetCriteria, cardApplication: CardApplicationToUpdate)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Option[CardApplication]]

  def deleteCardApplication(id: String)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Unit]

  def deleteCardApplicationByCriteria(criteria: CardApplicationGetCriteria)(implicit maybeTransaction: Option[Connection] = None): DaoResponse[Int]
}

