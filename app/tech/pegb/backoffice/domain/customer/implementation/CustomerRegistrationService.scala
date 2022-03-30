package tech.pegb.backoffice.domain.customer.implementation

import java.time.LocalDateTime

import com.google.inject.Inject
import tech.pegb.backoffice.dao.customer.abstraction.{BusinessUserDao, CustomerExtraAttributeDao}
import tech.pegb.backoffice.domain.account.model.Account
import tech.pegb.backoffice.domain.account.model.AccountAttributes.{AccountMainType}
import tech.pegb.backoffice.domain.{BaseService}
import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.domain.customer.abstraction.{CustomerAccount, CustomerRegistration}
import tech.pegb.backoffice.domain.customer.model.CardApplication
import tech.pegb.backoffice.domain.customer.model.BusinessUsers.RegisteredButNotActivatedBusinessUser
import tech.pegb.backoffice.domain.customer.model.CardApplicationAttributes.{CardPin, NewCardApplicationDetails}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes._
import tech.pegb.backoffice.util.{AppConfig, WithExecutionContexts}

import scala.concurrent.Future

class CustomerRegistrationService @Inject() (
    conf: AppConfig,
    executionContexts: WithExecutionContexts,
    businessUserDao: BusinessUserDao,
    accountService: CustomerAccount,
    customerExtraAttributeDao: CustomerExtraAttributeDao)
  extends CustomerRegistration with BaseService {

  implicit val executionContext = executionContexts.blockingIoOperations

  def registerBusinessUser(
    username: LoginUsername, msisdn: Option[Msisdn], email: Email, name: NameAttribute,
    tier: Option[CustomerTier], subscription: Option[CustomerSubscription],
    newCardApplicationDetails: Option[NewCardApplicationDetails], accountMainType: AccountMainType,
    doneByBackOfficeUserName: String, doneAt: LocalDateTime): Future[ServiceResponse[RegisteredButNotActivatedBusinessUser]] = ???

  def createDefaultBusinessUserAccount(user: RegisteredButNotActivatedBusinessUser, doneByBackOfficeUserName: String): Future[ServiceResponse[Account]] = ???

  def createCardApplication(
    user: RegisteredButNotActivatedBusinessUser,
    cardPin: Option[CardPin], nameOnCard: NameAttribute,
    cardDeliveryAddress: Address, doneByBackOfficeUserName: String): Future[ServiceResponse[CardApplication]] = ???

}
