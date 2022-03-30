package tech.pegb.backoffice.dao.auth.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao

import tech.pegb.backoffice.dao.auth.dto.{ScopeCriteria, ScopeToInsert, ScopeToUpdate}
import tech.pegb.backoffice.dao.auth.entity.Scope
import tech.pegb.backoffice.dao.auth.sql.ScopeSqlDao
import tech.pegb.backoffice.dao.model.OrderingSet

@ImplementedBy(classOf[ScopeSqlDao])
trait ScopeDao extends Dao {

  def insertScope(dto: ScopeToInsert): DaoResponse[Scope]

  def getScopeByCriteria(criteria: ScopeCriteria, ordering: Option[OrderingSet], limit: Option[Int], offset: Option[Int]): DaoResponse[Seq[Scope]]

  def countScopeByCriteria(criteria: ScopeCriteria): DaoResponse[Int]

  def updateScope(id: String, dto: ScopeToUpdate): DaoResponse[Option[Scope]]

  def getScopeIdByName(name: String): DaoResponse[Option[String]]

}
