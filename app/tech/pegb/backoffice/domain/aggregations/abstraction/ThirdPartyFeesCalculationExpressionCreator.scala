package tech.pegb.backoffice.domain.aggregations.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.aggregations.dto.Entity
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.aggregations.implementation.ThirdPartyFeesExpressionCreator

import scala.concurrent.Future

@ImplementedBy(classOf[ThirdPartyFeesExpressionCreator])
trait ThirdPartyFeesCalculationExpressionCreator {
  //TODO break dependency from dao.aggregations.dto.Entity
  def getThirdPartyFeesCalculationExpressionById(
    entity: Entity,
    amountColumn: String,
    alias: Option[String],
    feeProfileId: String): Future[ServiceResponse[String]]

  def getThirdPartyFeesCalculationExpressionByCriteria(
    entity: Entity,
    amountColumn: String,
    alias: Option[String],
    providerName: String,
    currencyCode: Option[String],
    transactionType: Option[String]): Future[ServiceResponse[String]]

  def getCompleteThirdPartyFeesCalculationNestedExpression(
    entity: Entity,
    amountColumn: String,
    alias: Option[String]): Future[ServiceResponse[String]]
}

