package tech.pegb.backoffice.domain.currencyexchange.abstraction

import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.currencyexchange.dto.{SpreadCriteria, SpreadToCreate}
import tech.pegb.backoffice.domain.currencyexchange.implementation.SpreadsMgmtService
import tech.pegb.backoffice.domain.currencyexchange.model.Spread
import tech.pegb.backoffice.domain.model.Ordering

import scala.concurrent.Future

@ImplementedBy(classOf[SpreadsMgmtService])
trait SpreadsManagement {
  def getSpread(id: UUID): Future[ServiceResponse[Spread]]

  def getSpreadByCriteria(criteria: SpreadCriteria, orderBy: Seq[Ordering], limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[Spread]]]

  def countSpreadByCriteria(criteria: SpreadCriteria): Future[ServiceResponse[Int]]

  def createSpread(spreadToCreate: SpreadToCreate)(implicit requestId: UUID): Future[ServiceResponse[Spread]]
}
