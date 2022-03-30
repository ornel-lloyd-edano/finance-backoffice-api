package tech.pegb.backoffice.dao.businessuserapplication.abstraction

import java.sql.Connection

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.businessuserapplication.dto.BUApplicPrimaryAddressToInsert
import tech.pegb.backoffice.dao.businessuserapplication.entity.BUApplicPrimaryAddress
import tech.pegb.backoffice.dao.businessuserapplication.sql.BUApplicPrimaryAddressesSqlDao

@ImplementedBy(classOf[BUApplicPrimaryAddressesSqlDao])
trait BUAppPrimaryAddressesDao extends Dao {

  def getByApplicationId(applicationId: Int): DaoResponse[Seq[BUApplicPrimaryAddress]]

  def insert(dto: Seq[BUApplicPrimaryAddressToInsert])(implicit txnConn: Option[Connection]): DaoResponse[Seq[BUApplicPrimaryAddress]]

  def deleteByApplicationId(applicationId: Int)(implicit txnConn: Option[Connection]): DaoResponse[Unit]

}
