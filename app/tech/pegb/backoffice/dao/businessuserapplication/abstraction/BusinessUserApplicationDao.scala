package tech.pegb.backoffice.dao.businessuserapplication.abstraction

import java.sql.Connection

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.Dao.EntityId
import tech.pegb.backoffice.dao.businessuserapplication.dto.{BusinessUserApplicationCriteria, BusinessUserApplicationToInsert, BusinessUserApplicationToUpdate}
import tech.pegb.backoffice.dao.businessuserapplication.entity.BusinessUserApplication
import tech.pegb.backoffice.dao.businessuserapplication.sql.BusinessUserApplicationSqlDao
import tech.pegb.backoffice.dao.model.OrderingSet

@ImplementedBy(classOf[BusinessUserApplicationSqlDao])
trait BusinessUserApplicationDao extends Dao {

  def insertBusinessUserApplication(dto: BusinessUserApplicationToInsert): DaoResponse[BusinessUserApplication]

  def getBusinessUserApplicationByCriteria(
    criteria: BusinessUserApplicationCriteria,
    ordering: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int]): DaoResponse[Seq[BusinessUserApplication]]

  def countBusinessUserApplicationByCriteria(criteria: BusinessUserApplicationCriteria): DaoResponse[Int]

  def updateBusinessUserApplication(id: EntityId, dto: BusinessUserApplicationToUpdate)(implicit txnConn: Option[Connection] = None): DaoResponse[Option[BusinessUserApplication]]
}
