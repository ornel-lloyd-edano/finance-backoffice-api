package tech.pegb.backoffice.domain.customer.abstraction

import java.time.LocalDateTime

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.account.model.Account
import tech.pegb.backoffice.domain.account.model.AccountAttributes.AccountMainType
import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.domain.customer.implementation.CustomerRegistrationService
import tech.pegb.backoffice.domain.customer.model.BusinessUsers.RegisteredButNotActivatedBusinessUser
import tech.pegb.backoffice.domain.customer.model.CardApplicationAttributes.{CardPin, NewCardApplicationDetails}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes._
import tech.pegb.backoffice.domain.customer.model._

import scala.concurrent.Future

@ImplementedBy(classOf[CustomerRegistrationService])
trait CustomerRegistration {

  def registerBusinessUser(
    username: LoginUsername,
    msisdn: Option[Msisdn],
    email: Email,
    company: NameAttribute,
    tier: Option[CustomerTier],
    subscription: Option[CustomerSubscription],
    newCardApplicationDetails: Option[NewCardApplicationDetails],
    accountMainType: AccountMainType,
    doneByBackOfficeUserName: String,
    doneAt: LocalDateTime): Future[ServiceResponse[RegisteredButNotActivatedBusinessUser]]

  def createDefaultBusinessUserAccount(user: RegisteredButNotActivatedBusinessUser, doneByBackOfficeUserName: String): Future[ServiceResponse[Account]]

  def createCardApplication(
    user: RegisteredButNotActivatedBusinessUser,
    cardPin: Option[CardPin], nameOnCard: NameAttribute,
    cardDeliveryAddress: Address, doneByBackOfficeUserName: String): Future[ServiceResponse[CardApplication]]

}
