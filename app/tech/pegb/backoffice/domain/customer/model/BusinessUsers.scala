package tech.pegb.backoffice.domain.customer.model

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.domain.account.model.Account
import tech.pegb.backoffice.domain.auth.model.Email
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes._

object BusinessUsers {

  case class ActivatedBusinessUser(id: UUID, username: LoginUsername, password: String, tier: CustomerTier, segment: Option[CustomerSegment], subscription: CustomerSubscription, emails: Set[Email], status: CustomerStatus, name: NameAttribute, addresses: Set[Address], phoneNumbers: Set[PhoneNumber], activationRequirements: Set[ActivationRequirement], accounts: Set[Account], activatedAt: Option[LocalDateTime], passwordUpdatedAt: Option[LocalDateTime], createdAt: LocalDateTime, createdBy: Option[String], updatedAt: Option[LocalDateTime], updatedBy: Option[String]) {

    assert(addresses.nonEmpty, "empty address")
    assert(emails.nonEmpty, "empty emails")
    assert(phoneNumbers.nonEmpty, "empty phone numbers")
    assert(activationRequirements.nonEmpty, "empty activation requirements")
  }

  object ActivatedBusinessUser {
    def getEmpty = ActivatedBusinessUser(id = UUID.randomUUID(), username = LoginUsername("username"), password = "", tier = CustomerTier("empty"), segment = None, subscription = CustomerSubscription("empty"), emails = Set(Email("email@domain.com")), status = CustomerStatus("empty"), name = NameAttribute("empty"), addresses = Set(Address("address")), phoneNumbers = Set(PhoneNumber("0000000000")), activationRequirements = Set(ActivationRequirement("some document identifier", ActivationDocumentType("some document type"), "", LocalDateTime.now)), accounts = Set.empty, activatedAt = None, passwordUpdatedAt = None, createdAt = LocalDateTime.now, createdBy = Some(""), updatedAt = None, updatedBy = None)
  }

  case class RegisteredButNotActivatedBusinessUser(id: UUID, username: Option[LoginUsername], email: Email, name: NameAttribute, accounts: Set[Account], createdAt: LocalDateTime, createdBy: Option[String])

  object RegisteredButNotActivatedBusinessUser {
    def getEmpty = RegisteredButNotActivatedBusinessUser(id = UUID.randomUUID(), username = None, email = Email("email@domain.com"), name = NameAttribute("company"), accounts = Set.empty, createdAt = LocalDateTime.now, createdBy = Some(""))
  }
}

