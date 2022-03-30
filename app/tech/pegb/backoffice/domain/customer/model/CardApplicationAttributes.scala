package tech.pegb.backoffice.domain.customer.model

import java.time.LocalDateTime
import java.util.UUID

import tech.pegb.backoffice.domain.customer.model.CardApplicationAttributes._
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.{Address, NameAttribute}
import tech.pegb.backoffice.util.Implicits._
import java.security.SecureRandom

case class CardApplication(
    applicationId: UUID,
    customerId: UUID,
    operationType: CardApplicationType,
    cardType: CardType,
    nameOnCard: NameAttribute,
    cardPin: CardPin,
    deliveryAddress: Address,
    status: CardApplicationStatus,
    createdAt: LocalDateTime,
    createdBy: Option[String])

object CardApplicationAttributes {
  private val random: SecureRandom = SecureRandom.getInstance("SHA1PRNG")

  case class CardApplicationRequirementType(underlying: String) {
    assert(underlying.hasSomething, "empty CardApplicationRequirementType")
    assert(underlying.matches("""[A-Za-z]+[A-Za-z0-9\-\_ ]*"""), "invalid CardApplicationRequirementType")
  }

  case class CardApplicationRequirement(identifier: String, requirementType: CardApplicationRequirementType, verifiedBy: String, verifiedAt: LocalDateTime) {
    assert(identifier.hasSomething, "empty CardApplicationRequirement")
    assert(identifier.matches("""[A-Za-z0-9]+[A-Za-z0-9\-\_ ]*"""), "invalid CardApplicationRequirement")
  }

  case class Card(
      cardType: String,
      cardNumber: String,
      cardPin: CardPin,
      cardName: NameAttribute,
      expiryDate: String,
      verificationCode: CardVerificationCode,
      status: String)

  case class CardPin(underlying: Int) {
    assert(underlying.toString.length == 4, "invalid CardPin")
  }

  object CardPin {
    def apply(arg: String): CardPin = new CardPin(arg.toInt)
    def generate = new CardPin(((random.nextDouble() * 9000) + 1000).toInt)
  }

  case class CardVerificationCode(underlying: Int) {
    assert(underlying.toString.length == 3, "invalid CardVerificationCode")
  }

  object CardVerificationCode {
    def generate = CardVerificationCode(((random.nextDouble() * 900) + 100).toInt)
  }

  case class CardApplicationType(underlying: String) {
    assert(underlying.hasSomething, "empty CardApplicationType")
    assert(underlying.matches("""[A-Za-z]+[A-Za-z\-\_ ]*"""), "invalid CardApplicationType")
  }

  case class CardType(underlying: String) {
    assert(underlying.hasSomething, "empty CardType")
    assert(underlying.matches("""[A-Za-z]+[A-Za-z\-\_ ]*"""), "invalid CardType")
  }

  case class NewCardApplicationDetails(
      nameOnCard: NameAttribute,
      cardPin: CardPin,
      deliveryAddress: Address)

  case class CardApplicationStatus(underlying: String) {
    assert(underlying.hasSomething, "empty CardApplicationStatus")
    assert(underlying.matches("""[A-Za-z]+[A-Za-z\-\_ ]*"""), "invalid CardApplicationStatus")
  }

}

