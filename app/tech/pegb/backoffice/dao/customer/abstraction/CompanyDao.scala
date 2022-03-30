package tech.pegb.backoffice.dao.customer.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.customer.dto.CompanyToInsert
import tech.pegb.backoffice.dao.customer.entity.Company
import tech.pegb.backoffice.dao.customer.sql.CompanySqlDao

@ImplementedBy(classOf[CompanySqlDao])
trait CompanyDao extends Dao {
  def create(company: CompanyToInsert): DaoResponse[Company]

  def getAll: DaoResponse[Set[Company]]
}

