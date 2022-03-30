package tech.pegb.backoffice.domain.businessuserapplication.model

import java.time.{LocalDate, LocalDateTime}
import java.util.{Currency, UUID}

import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.businessuserapplication.abstraction.Status
import tech.pegb.backoffice.domain.businessuserapplication.model.BusinessTypes.{Agent, Merchant, SuperMerchant}
import tech.pegb.backoffice.domain.businessuserapplication.model.BusinessUserApplicationAttributes._
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.NameAttribute
import tech.pegb.backoffice.domain.document.model.Document
import tech.pegb.backoffice.domain.transaction.model.TransactionType
import tech.pegb.backoffice.domain.{ServiceError, Validatable}
import tech.pegb.backoffice.util.Implicits._

case class BusinessUserApplication(
    id: Int,
    uuid: UUID,
    businessName: NameAttribute,
    brandName: NameAttribute,
    businessCategory: BusinessCategory,
    stage: ApplicationStage, //objectify further
    status: ApplicationStatus, //objectify further
    userTier: BusinessUserTier,
    businessType: BusinessType,
    registrationNumber: RegistrationNumber,
    taxNumber: Option[TaxNumber],
    registrationDate: Option[LocalDate],
    explanation: Option[String],
    transactionConfig: Seq[TransactionConfig] = Nil,
    accountConfig: Seq[AccountConfig] = Nil,
    externalAccounts: Seq[ExternalAccount] = Nil,
    contactPersons: Seq[ContactPerson] = Nil,
    contactAddress: Seq[ContactAddress] = Nil,
    documents: Seq[Document] = Nil,
    defaultCurrency: Currency,
    submittedBy: Option[String],
    submittedAt: Option[LocalDateTime],
    checkedBy: Option[String],
    checkedAt: Option[LocalDateTime],
    createdBy: String,
    createdAt: LocalDateTime,
    updatedBy: Option[String],
    updatedAt: Option[LocalDateTime]) extends Validatable[BusinessUserApplication] {

  import BusinessUserApplication._

  def addConfigs(
    transactionConfig: Seq[TransactionConfig],
    accountConfig: Seq[AccountConfig],
    externalAccount: Seq[ExternalAccount]): BusinessUserApplication = {

    this.copy(transactionConfig = transactionConfig, accountConfig = accountConfig, externalAccounts = externalAccount)
  }

  def addContactInfo(
    contactPersons: Seq[ContactPerson],
    contactAddress: Seq[ContactAddress]): BusinessUserApplication = {
    this.copy(contactPersons = contactPersons, contactAddress = contactAddress)
  }

  def addDocuments(documents: Seq[Document]): BusinessUserApplication = {
    this.copy(documents = documents)
  }

  def getValidTransactionConfig: Seq[TransactionConfig] = {
    ValidTransactionMap(businessType.toString)
      .map(transactionType ⇒ TransactionConfig(TransactionType(transactionType), defaultCurrency))
  }

  def validate: ServiceResponse[BusinessUserApplication] = {
    (status.underlying === "approved", this.transactionConfig.nonEmpty, this.accountConfig.nonEmpty, contactPersons.nonEmpty, documents.nonEmpty) match {
      case (true, false, _, _, _) ⇒
        Left(ServiceError.validationError("At least 1 transaction config is required"))
      case (true, _, false, _, _) ⇒
        Left(ServiceError.validationError("At least 1 account config is required"))
      case (true, _, _, false, _) ⇒
        Left(ServiceError.validationError("At least 1 contact person is required"))
      case (true, _, _, _, false) ⇒
        Left(ServiceError.validationError("At least 1 document is required"))
      case _ ⇒
        Right(this)
    }
  }

  def validateNewStatus(newStatus: String): ServiceResponse[Unit] = {
    StatusTransitionMap.get(this.status.underlying) match {
      case None ⇒ Left(ServiceError.validationError(s"${this.status.underlying} is not valid"))
      case Some(allowedStatus) ⇒
        if (!allowedStatus.contains(newStatus)) {
          Left(ServiceError.validationError(s"Status transition from ${this.status.underlying} to $newStatus is not allowed."))
        } else {
          Right(())
        }
    }
  }

  def validateChecker(checker: String): ServiceResponse[Unit] = {
    Either.cond(!submittedBy.contains(checker), (),
      ServiceError.validationError(s"Checker cannot be the submitter of application, applicationId: $uuid"))
  }
}

object BusinessUserApplication {
  val StatusTransitionMap: Map[String, Set[String]] = Map(
    Status.Ongoing → Set(Status.Cancelled, Status.Pending),
    Status.Pending → Set(Status.Approved, Status.Rejected, Status.Ongoing),
    Status.Rejected → Set(),
    Status.Cancelled → Set(),
    Status.Approved → Set())

  val CashIn = "cashin"
  val CashOut = "cashout"
  val MerchantPayment = "merchant_payment"

  val ValidTransactionMap: Map[String, Seq[String]] = Map(
    Merchant.toString → Seq(MerchantPayment),
    SuperMerchant.toString → Seq(MerchantPayment, CashIn, CashOut),
    Agent.toString → Seq(CashIn, CashOut)).withDefaultValue(Seq.empty[String])

  def validateTransactionConfig(txConfig: Seq[TransactionConfig], expectedConfig: Seq[TransactionConfig]): ServiceResponse[Unit] = {
    Either.cond(txConfig.toSet == expectedConfig.toSet, (), ServiceError.validationError(s"Invalid transaction_config $txConfig for business user, expected config: $expectedConfig"))
  }
}
