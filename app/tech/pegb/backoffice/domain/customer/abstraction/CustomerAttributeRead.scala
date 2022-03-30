package tech.pegb.backoffice.domain.customer.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes._
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.customer.implementation.CustomerAttributeReadService

@ImplementedBy(classOf[CustomerAttributeReadService])
trait CustomerAttributeRead {

  def getCustomerSegments: ServiceResponse[Set[CustomerSegment]]

  def getCustomerSubscriptions: ServiceResponse[Set[CustomerSubscription]]

  def getCustomerTiers: ServiceResponse[Set[CustomerTier]]

  def getCustomerStatuses: ServiceResponse[Set[CustomerStatus]]

  def getBusinessUserTypes: ServiceResponse[Set[BusinessUserType]]

  def getEmployers: ServiceResponse[Set[Employer]]

  def getNationalities: ServiceResponse[Set[Nationality]]

  def getOccupations: ServiceResponse[Set[Occupation]]

  def getCompanies: ServiceResponse[Set[Company]]

}
