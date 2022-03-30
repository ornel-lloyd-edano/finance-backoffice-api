package tech.pegb.backoffice.dao.customer.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.customer.dto.EmployerToInsert
import tech.pegb.backoffice.dao.customer.entity.Employer
import tech.pegb.backoffice.dao.customer.sql.EmployerSqlDao

@ImplementedBy(classOf[EmployerSqlDao])
trait EmployerDao extends Dao {
  def create(employerToInsert: EmployerToInsert): DaoResponse[Employer]

  def getAll: DaoResponse[Set[Employer]]
}
