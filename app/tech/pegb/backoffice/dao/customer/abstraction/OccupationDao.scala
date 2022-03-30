package tech.pegb.backoffice.dao.customer.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.customer.dto.OccupationToInsert
import tech.pegb.backoffice.dao.customer.entity.Occupation
import tech.pegb.backoffice.dao.customer.sql.OccupationSqlDao

@ImplementedBy(classOf[OccupationSqlDao])
trait OccupationDao extends Dao {
  def create(occupationToInsert: OccupationToInsert): DaoResponse[Occupation]

  def getAll: DaoResponse[Set[Occupation]]
}
