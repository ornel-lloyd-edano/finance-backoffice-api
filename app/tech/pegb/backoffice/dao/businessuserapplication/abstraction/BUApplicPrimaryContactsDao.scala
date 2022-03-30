package tech.pegb.backoffice.dao.businessuserapplication.abstraction

import java.sql.Connection

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.businessuserapplication.dto.BUApplicPrimaryContactToInsert
import tech.pegb.backoffice.dao.businessuserapplication.entity.BUApplicPrimaryContact
import tech.pegb.backoffice.dao.businessuserapplication.sql.BUApplicPrimaryContactsSqlDao

@ImplementedBy(classOf[BUApplicPrimaryContactsSqlDao])
trait BUApplicPrimaryContactsDao extends Dao {

  def getByApplicationId(applicationId: Int): DaoResponse[Seq[BUApplicPrimaryContact]]

  def insert(dto: Seq[BUApplicPrimaryContactToInsert])(implicit txnConn: Option[Connection]): DaoResponse[Seq[BUApplicPrimaryContact]]

  def deleteByApplicationId(applicationId: Int)(implicit txnConn: Option[Connection]): DaoResponse[Unit]

}
