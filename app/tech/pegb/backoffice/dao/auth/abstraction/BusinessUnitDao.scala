package tech.pegb.backoffice.dao.auth.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.auth.dto.{BusinessUnitCriteria, BusinessUnitToInsert}
import tech.pegb.backoffice.dao.auth.sql.BusinessUnitSqlDao
import tech.pegb.backoffice.dao.auth.dto.BusinessUnitToUpdate
import tech.pegb.backoffice.dao.auth.entity.BusinessUnit
import tech.pegb.backoffice.dao.model.OrderingSet

@ImplementedBy(classOf[BusinessUnitSqlDao])
trait BusinessUnitDao extends Dao {

  def create(dto: BusinessUnitToInsert): DaoResponse[BusinessUnit]

  def getBusinessUnitsByCriteria(dto: BusinessUnitCriteria, ordering: Option[OrderingSet], maybeLimit: Option[Int], maybeOffset: Option[Int]): DaoResponse[Seq[BusinessUnit]]

  def countBusinessUnitsByCriteria(dto: BusinessUnitCriteria): DaoResponse[Int]

  def update(id: String, dto: BusinessUnitToUpdate): DaoResponse[Option[BusinessUnit]]

}
