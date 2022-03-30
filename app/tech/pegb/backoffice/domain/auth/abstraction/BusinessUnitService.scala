package tech.pegb.backoffice.domain.auth.abstraction

import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService
import tech.pegb.backoffice.domain.auth._
import tech.pegb.backoffice.domain.auth.dto.{BusinessUnitCriteria, BusinessUnitToCreate, BusinessUnitToRemove, BusinessUnitToUpdate}
import tech.pegb.backoffice.domain.auth.model.BusinessUnit
import tech.pegb.backoffice.domain.model._

import scala.concurrent.Future

@ImplementedBy(classOf[implementation.BusinessUnitService])
trait BusinessUnitService extends BaseService {
  def create(dto: BusinessUnitToCreate, reactivateIfExisting: Boolean): Future[ServiceResponse[BusinessUnit]]

  def getAllActiveBusinessUnits(criteria: BusinessUnitCriteria, orderBy: Seq[Ordering], maybeLimit: Option[Int], maybeOffset: Option[Int]): Future[ServiceResponse[Seq[BusinessUnit]]]

  def countAllActiveBusinessUnits(criteria: BusinessUnitCriteria): Future[ServiceResponse[Int]]

  def update(id: UUID, dto: BusinessUnitToUpdate): Future[ServiceResponse[BusinessUnit]]

  def remove(id: UUID, dto: BusinessUnitToRemove): Future[ServiceResponse[Unit]]
}
