package tech.pegb.backoffice.domain.customer.model

import java.time.LocalDateTime

import tech.pegb.backoffice.util.Implicits._

//TODO deprecate this
object CustomerAttributes {

  case class CustomerType(underlying: String) {
    assert(underlying.hasSomething, "empty customer type")
  }

  case class LoginUsername(underlying: String) {
    assert(underlying.hasSomething, "empty LoginUsername")
    /*assert(underlying.matches("""[A-Za-z]+[A-Za-z0-9\@\.\_]*"""), "invalid LoginUsername")*/
  }

  case class NameAttribute(underlying: String) {
    //assert(underlying.hasSomething, "empty name attribute")
    /* assert(underlying.matches("""[A-Za-z]+[A-Za-z\.\' ]*"""), "invalid name attribute")*/
  }

  case class CustomerTier(underlying: String) {
    assert(underlying.hasSomething, "empty CustomerTier")
    assert(underlying.matches("""[A-Za-z]+[A-Za-z0-9\-\_ ]*"""), s"invalid CustomerTier: ${underlying}")
  }

  case class CustomerSegment(underlying: String) {
    assert(underlying.hasSomething, "empty CustomerSegment")
    assert(underlying.matches("""[A-Za-z]+[A-Za-z0-9\-\_ ]*"""), s"invalid CustomerSegment: ${underlying}")
  }

  case class CustomerSubscription(underlying: String) {
    assert(underlying.hasSomething, "empty CustomerSubscription")
    assert(underlying.matches("""[A-Za-z]+[A-Za-z0-9\-\_ ]*"""), s"invalid CustomerSubscription: ${underlying}")
  }

  case class CustomerStatus(underlying: String) {
    assert(underlying.hasSomething, "empty CustomerStatus")
    assert(underlying.matches("""[A-Za-z]+[A-Za-z0-9\-\_ ]*"""), s"invalid CustomerStatus: ${underlying}")
  }

  case class Msisdn(underlying: String) {
    assert(underlying.hasSomething, "empty Msisdn")
    assert(underlying.matches("""[\+]?[1-9][0-9]{4,14}"""), s"invalid Msisdn: ${underlying}")
  }

  case class MsisdnLike(underlying: String) {
    assert(underlying.hasSomething, "empty Msisdn")
    assert(underlying.matches("""\+?\d{3,15}"""), s"invalid Msisdn: $underlying")
  }

  case class BusinessUserType(underlying: String) {
    assert(underlying.hasSomething, "empty BusinessUserType")
    assert(underlying.matches("""[A-Za-z]+[A-Za-z0-9\-\_ ]*"""), s"invalid BusinessUserType: ${underlying}")
  }

  case class Address(
      underlying: String,
      country: Option[String] = None, postalCode: Option[String] = None, city: Option[String] = None,
      municipality: Option[String] = None, district: Option[String] = None, province: Option[String] = None,
      state: Option[String] = None, building: Option[String] = None, street: Option[String] = None,
      village: Option[String] = None, room: Option[String] = None, lot: Option[String] = None,
      block: Option[String] = None, houseNum: Option[String] = None) {
    assert(underlying.hasSomething, "empty Address")
    assert(underlying.matches("""[A-Za-z0-9]+[A-Za-z0-9\,\.\' ]*"""), s"invalid Address: ${underlying}")
  }

  case class PhoneNumber(underlying: String) {
    assert(underlying.hasSomething, "empty PhoneNumber")
    assert(underlying.matches("""[\+]?[0-9]+[0-9\- ]*"""), s"invalid PhoneNumber: ${underlying}")
  }

  case class ActivationDocumentType(underlying: String) {
    assert(underlying.hasSomething, "empty ActivationDocumentType")
    assert(underlying.matches("""[A-Za-z]+[A-Za-z\-\_ ]*"""), s"invalid ActivationDocumentType: ${underlying}")

    override def equals(obj: scala.Any): Boolean = {
      obj.asInstanceOf[ActivationDocumentType].underlying.equalsIgnoreCase(this.underlying)
    }
  }

  case class ActivationRequirement(identifier: String, documentType: ActivationDocumentType, verifiedBy: String, verifiedAt: LocalDateTime) {
    assert(identifier.hasSomething, "empty ActivationRequirement")
    assert(identifier.matches("""[A-Za-z0-9]+[A-Za-z0-9\-\_ ]*"""), s"invalid ActivationRequirement: ${identifier}")
  }

  case class Employer(underlying: String) {
    assert(underlying.hasSomething, "empty Employer")
    assert(underlying.matches("""[A-Za-z]+[A-Za-z\.\' ]*"""), s"invalid Employer: ${underlying}")
  }

  case class Nationality(underlying: String) {
    assert(underlying.hasSomething, "empty Nationality")
    assert(underlying.matches("""[A-Za-z]+[A-Za-z\.\' ]*"""), s"invalid Nationality: ${underlying}")
  }

  case class Occupation(underlying: String) {
    assert(underlying.hasSomething, "empty Occupation")
    assert(underlying.matches("""[A-Za-z]+[A-Za-z\.\' ]*"""), s"invalid Occupation: ${underlying}")
  }

  case class Company(underlying: String) {
    assert(underlying.hasSomething, "empty Company")
    assert(underlying.matches("""[A-Za-z]+[A-Za-z\.\' ]*"""), s"invalid Company: ${underlying}")
  }

}

