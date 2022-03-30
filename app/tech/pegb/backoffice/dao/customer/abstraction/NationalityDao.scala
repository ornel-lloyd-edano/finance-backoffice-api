package tech.pegb.backoffice.dao.customer.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.customer.dto.NationalityToInsert
import tech.pegb.backoffice.dao.customer.entity.Nationality
import tech.pegb.backoffice.dao.customer.sql.NationalitySqlDao

@ImplementedBy(classOf[NationalitySqlDao])
trait NationalityDao extends Dao {
  def create(nationalityToInsert: NationalityToInsert): DaoResponse[Nationality]

  def getAll: DaoResponse[Set[Nationality]]
}
