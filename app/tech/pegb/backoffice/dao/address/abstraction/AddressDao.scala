package tech.pegb.backoffice.dao.address.abstraction

import java.sql.Connection

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.address.dto.{AddressCriteria, AddressToInsert, AddressToUpdate}
import tech.pegb.backoffice.dao.address.entity.Address
import tech.pegb.backoffice.dao.address.sql.AddressSqlDao
import tech.pegb.backoffice.dao.model.OrderingSet

@ImplementedBy(classOf[AddressSqlDao])
trait AddressDao extends Dao {

  def get(uuid: String)(implicit txnConn: Option[Connection]): DaoResponse[Option[Address]]

  def getByCriteria(
    criteria: AddressCriteria,
    orderBy: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int])(implicit txnConn: Option[Connection]): DaoResponse[Seq[Address]]

  def insert(dto: AddressToInsert)(implicit txnConn: Option[Connection]): DaoResponse[Address]

  def update(uuid: String, dto: AddressToUpdate)(implicit txnConn: Option[Connection]): DaoResponse[Option[Address]]
}
