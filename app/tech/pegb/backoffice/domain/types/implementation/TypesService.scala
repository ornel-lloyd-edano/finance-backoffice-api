package tech.pegb.backoffice.domain.types.implementation

import java.time.LocalDateTime
import java.util.UUID

import com.google.inject.{Inject, Singleton}
import tech.pegb.backoffice.dao.account.abstraction.AccountTypesDao
import tech.pegb.backoffice.dao.types.abstraction.TypesDao
import tech.pegb.backoffice.domain.types.abstraction.TypesServiceLike
import tech.pegb.backoffice.domain.types.enum.CommissionCalculationMethods
import tech.pegb.backoffice.domain.types.model.TypeDescription
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.types.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.types.Implicits._

@Singleton
class TypesService @Inject() (dao: TypesDao, accountsTypeDao: AccountTypesDao)
  extends TypesServiceLike {

  //Define all bulkUpsert of TypeEnums in a single place
  dao.bulkUpsert(
    existingKind = CommissionCalculationMethods.toString,
    updatedAt = LocalDateTime.now(),
    updatedBy = "backoffice_api",
    lastUpdatedAt = None,
    newValues = CommissionCalculationMethods.toSeq.map(_.asDao),
    disableOptimisticLockCheck = true)

  override def fetchAllTypes(implicit requestId: UUID): ServiceResponse[Map[String, Seq[TypeDescription]]] =
    dao.fetchAllTypes.map(_.asDomain).asServiceResponse

  override def fetchCustomType(kind: String)(implicit requestId: UUID): ServiceResponse[Seq[TypeDescription]] = {
    /*for {
      rawValues ← dao.fetchCustomType(kind).asServiceResponse
      results ← Either.cond(rawValues.isEmpty, rawValues, notFoundEntityError(s"No values for $kind were found"))
    } yield results*/
    dao.fetchCustomType(kind).map(_.map(_.asDomain)).asServiceResponse
  }

  override def fetchAccountTypes(implicit requestId: UUID): ServiceResponse[Set[TypeDescription]] = {
    /*for {
      rawValues ← dao.fetchCustomType(kind).asServiceResponse
      results ← Either.cond(rawValues.isEmpty, rawValues, notFoundEntityError(s"No values for $kind were found"))
    } yield results*/
    accountsTypeDao.getAll.map(_.map(x ⇒ (x.id, x.accountTypeName, x.description).asDomain)).asServiceResponse
  }

  /*override def getCustomerTypes: ServiceResponse[Seq[String]] = customerTypes

  override def getCustomerStatuses: ServiceResponse[Seq[String]] = customerStatuses

  override def getIndividualUserTypes: ServiceResponse[Seq[String]] = individualUserTypes

  override def getCustomerTiers: ServiceResponse[Seq[String]] = customerTiers

  override def getCustomerSegments: ServiceResponse[Seq[String]] = customerSegments

  override def getCustomerSubscriptions: ServiceResponse[Seq[String]] = customerSubscriptions

  override def getCurrencies: ServiceResponse[Seq[String]] = currencies

  override def getCurrenciesWithId: ServiceResponse[Seq[(Int, String)]] = currenciesWithId

  override def getInstruments: ServiceResponse[Seq[String]] = instruments

  override def getChannels: ServiceResponse[Seq[String]] = channels

  override def getTransactionStatuses: ServiceResponse[Seq[String]] = transactionStatuses

  override def getTransactionTypes: ServiceResponse[Seq[String]] = transactionTypes

  override def getAccountTypes: ServiceResponse[Seq[String]] = accountTypes

  override def getAccountStatuses: ServiceResponse[Seq[String]] = accountStatuses

  override def getApplicationStatuses: ServiceResponse[Seq[String]] = applicationStatuses

  override def getApplicationStages: ServiceResponse[Seq[String]] = applicationStages

  override def getDocumentTypes: ServiceResponse[Seq[String]] = documentTypes

  override def getDocumentStatuses: ServiceResponse[Seq[String]] = documentStatuses

  override def getImageTypes: ServiceResponse[Seq[String]] = imageTypes

  override def getOccupations: ServiceResponse[Seq[String]] = occupations

  override def getNationalities: ServiceResponse[Seq[String]] = nationalities

  override def getEmployers: ServiceResponse[Seq[String]] = employers

  override def getCompanies: ServiceResponse[Seq[String]] = companies

  override def getLimitTypes: ServiceResponse[Seq[String]] = limitTypes

  protected lazy val accountStatuses: ServiceResponse[Seq[String]] =
    Right(Seq("active", "deactivated", "closed"))

  protected def accountTypes: ServiceResponse[Seq[String]] =
    dao.getAccountTypes.asServiceResponse

  protected def applicationStages: ServiceResponse[Seq[String]] =
    dao.getApplicationStages.asServiceResponse

  protected lazy val applicationStatuses: ServiceResponse[Seq[String]] =
    Right(Seq("pending", "in_process", "approved", "rejected"))

  protected def channels: ServiceResponse[Seq[String]] =
    dao.getChannels.asServiceResponse

  protected def companies: ServiceResponse[Seq[String]] =
    dao.getCompanies.asServiceResponse

  protected def currencies: ServiceResponse[Seq[String]] =
    dao.getCurrencies.asServiceResponse

  protected def currenciesWithId: ServiceResponse[Seq[(Int, String)]] =
    dao.getCurrenciesWithId.asServiceResponse

  protected def customerTypes: ServiceResponse[Seq[String]] =
    dao.getCustomerTypes.asServiceResponse

  protected lazy val customerStatuses: ServiceResponse[Seq[String]] =
    Right(Seq("waiting_for_activation", "active", "passive"))

  protected def customerTiers: ServiceResponse[Seq[String]] =
    dao.getCustomerTiers.asServiceResponse

  protected def customerSegments: ServiceResponse[Seq[String]] =
    dao.getCustomerSegments.asServiceResponse

  protected def customerSubscriptions: ServiceResponse[Seq[String]] =
    dao.getCustomerSubscriptions.asServiceResponse

  protected lazy val documentStatuses: ServiceResponse[Seq[String]] =
    Right(Seq("pending", "approved", "rejected"))

  protected def documentTypes: ServiceResponse[Seq[String]] =
    dao.getDocumentTypes.asServiceResponse

  protected def employers: ServiceResponse[Seq[String]] =
    dao.getEmployers.asServiceResponse

  protected def imageTypes: ServiceResponse[Seq[String]] =
    dao.getImageTypes.asServiceResponse

  protected def individualUserTypes: ServiceResponse[Seq[String]] =
    dao.getIndividualUserTypes.asServiceResponse

  protected def instruments: ServiceResponse[Seq[String]] =
    dao.getInstruments.asServiceResponse

  protected def nationalities: ServiceResponse[Seq[String]] =
    dao.getNationalities.asServiceResponse

  protected def occupations: ServiceResponse[Seq[String]] =
    dao.getOccupations.asServiceResponse

  protected lazy val transactionStatuses: ServiceResponse[Seq[String]] =
    Right(Seq("active", "reverse", "cancelled", "pending"))

  protected def transactionTypes: ServiceResponse[Seq[String]] = dao.getTransactionTypes.asServiceResponse

  protected def limitTypes: ServiceResponse[Seq[String]] = dao.getLimitTypes.asServiceResponse*/

}
