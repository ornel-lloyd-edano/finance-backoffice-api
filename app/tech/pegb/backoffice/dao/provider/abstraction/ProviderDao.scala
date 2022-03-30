package tech.pegb.backoffice.dao.provider.abstraction

import java.sql.Connection

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao.DaoResponse
import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.provider.dto.ProviderCriteria
import tech.pegb.backoffice.dao.provider.entity.Provider
import tech.pegb.backoffice.dao.provider.sql.ProviderSqlDao

@ImplementedBy(classOf[ProviderSqlDao])
trait ProviderDao extends {

  def get(id: Int)(implicit txnConn: Option[Connection]): DaoResponse[Option[Provider]]

  def getByCriteria(
    criteria: ProviderCriteria,
    ordering: Option[OrderingSet],
    limit: Option[Int], offset: Option[Int])(implicit txnConn: Option[Connection]): DaoResponse[Seq[Provider]]

}
