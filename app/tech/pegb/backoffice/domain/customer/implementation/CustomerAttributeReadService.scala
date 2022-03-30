package tech.pegb.backoffice.domain.customer.implementation

import com.google.inject.Inject
import tech.pegb.backoffice.dao.customer.abstraction._
import tech.pegb.backoffice.domain.customer.abstraction.CustomerAttributeRead
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}
import tech.pegb.backoffice.domain.BaseService
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.customer.Implicits._

class CustomerAttributeReadService @Inject() (
    conf: AppConfig,
    executionContexts: WithExecutionContexts,
    employerDao: EmployerDao,
    nationalityDao: NationalityDao,
    occupationDao: OccupationDao,
    companyDao: CompanyDao) extends CustomerAttributeRead with BaseService {

  def getCustomerSegments: ServiceResponse[Set[CustomerSegment]] = ???

  def getCustomerSubscriptions: ServiceResponse[Set[CustomerSubscription]] = ???

  def getCustomerTiers: ServiceResponse[Set[CustomerTier]] = ???

  def getCustomerStatuses: ServiceResponse[Set[CustomerStatus]] = ???

  def getBusinessUserTypes: ServiceResponse[Set[BusinessUserType]] = ???

  def getEmployers: ServiceResponse[Set[Employer]] = {
    employerDao.getAll.asServiceResponse.map(_.map(_.asDomain()))
  }

  def getNationalities: ServiceResponse[Set[Nationality]] = {
    nationalityDao.getAll.asServiceResponse.map(_.map(_.asDomain()))
  }

  def getOccupations: ServiceResponse[Set[Occupation]] = {
    occupationDao.getAll.asServiceResponse.map(_.map(_.asDomain()))
  }

  def getCompanies: ServiceResponse[Set[Company]] = {
    companyDao.getAll.asServiceResponse.map(_.map(_.asDomain()))
  }

}
