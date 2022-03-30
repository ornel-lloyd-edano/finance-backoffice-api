package tech.pegb.backoffice.dao.customer.abstraction

import java.sql.Connection

import tech.pegb.backoffice.dao.Dao.DaoResponse

trait Transactional {

  def startTransaction: DaoResponse[Connection]

  def endTransaction(implicit txnConn: Connection): DaoResponse[Unit]

}
