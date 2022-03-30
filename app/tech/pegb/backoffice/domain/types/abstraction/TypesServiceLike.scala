package tech.pegb.backoffice.domain.types.abstraction

import java.util.UUID

import tech.pegb.backoffice.domain.BaseService
import tech.pegb.backoffice.domain.types.model.TypeDescription

trait TypesServiceLike extends BaseService {

  def fetchAllTypes(implicit requestId: UUID): ServiceResponse[Map[String, Seq[TypeDescription]]]

  def fetchCustomType(kind: String)(implicit requestId: UUID): ServiceResponse[Seq[TypeDescription]]

  def fetchAccountTypes(implicit requestId: UUID): ServiceResponse[Set[TypeDescription]]

}
