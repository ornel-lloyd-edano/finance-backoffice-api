package tech.pegb.backoffice.dao.contacts.abstraction

import java.sql.Connection

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao.DaoResponse
import tech.pegb.backoffice.dao.contacts.dto.{ContactToInsert, ContactToUpdate, ContactsCriteria}
import tech.pegb.backoffice.dao.contacts.entity.Contact
import tech.pegb.backoffice.dao.contacts.sql.ContactsSqlDao
import tech.pegb.backoffice.dao.model.OrderingSet

@ImplementedBy(classOf[ContactsSqlDao])
trait ContactsDao extends {

  def get(uuid: String)(implicit txnConn: Option[Connection]): DaoResponse[Option[Contact]]

  def getByCriteria(
    criteria: ContactsCriteria,
    orderBy: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int])(implicit txnConn: Option[Connection]): DaoResponse[Seq[Contact]]

  def insert(dto: ContactToInsert)(implicit txnConn: Option[Connection]): DaoResponse[Contact]

  def update(uuid: String, dto: ContactToUpdate)(implicit txnConn: Option[Connection]): DaoResponse[Option[Contact]]
}
