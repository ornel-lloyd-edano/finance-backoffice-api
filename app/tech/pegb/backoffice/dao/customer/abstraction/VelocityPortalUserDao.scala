package tech.pegb.backoffice.dao.customer.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.customer.dto.VelocityPortalUsersCriteria
import tech.pegb.backoffice.dao.customer.entity.VelocityPortalUser
import tech.pegb.backoffice.dao.customer.sql.VelocityPortalUserSqlDao
import tech.pegb.backoffice.dao.model.OrderingSet

@ImplementedBy(classOf[VelocityPortalUserSqlDao])
trait VelocityPortalUserDao extends Dao {

  def getVelocityPortalUsersByCriteria(
    criteria: VelocityPortalUsersCriteria,
    ordering: Option[OrderingSet],
    limit: Option[Int],
    offset: Option[Int]): DaoResponse[Seq[VelocityPortalUser]]

  def countVelocityPortalUserByCriteria(criteria: VelocityPortalUsersCriteria): DaoResponse[Int]

}
