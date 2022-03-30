package tech.pegb.backoffice.domain.parameter.implementation

import java.time.LocalDateTime
import java.util.UUID

import cats.implicits._
import com.google.inject.Inject
import org.coursera.autoschema.AutoSchema
import tech.pegb.backoffice.dao.account.abstraction.AccountTypesDao
import tech.pegb.backoffice.dao.account.dto.AccountTypeToUpsert
import tech.pegb.backoffice.dao.account.entity.AccountAttributes.AccountType
import tech.pegb.backoffice.dao.businessuserapplication.abstraction.CountryDao
import tech.pegb.backoffice.dao.businessuserapplication.dto.CountryToUpsert
import tech.pegb.backoffice.dao.businessuserapplication.entity.Country
import tech.pegb.backoffice.dao.currency.abstraction.CurrencyDao
import tech.pegb.backoffice.dao.currency.dto.CurrencyToUpsert
import tech.pegb.backoffice.dao.currency.entity.Currency
import tech.pegb.backoffice.dao.settings.abstraction.SystemSettingsDao
import tech.pegb.backoffice.dao.settings.dto.SystemSettingParameter
import tech.pegb.backoffice.dao.settings.entity.SystemSetting
import tech.pegb.backoffice.dao.types.abstraction.TypesDao
import tech.pegb.backoffice.dao.types.entity.{Description, DescriptionToUpsert}
import tech.pegb.backoffice.domain.parameter.abstraction.ParameterManagement
import tech.pegb.backoffice.domain.parameter.dto.{ParameterCriteria, ParameterToCreate, ParameterToUpdate}
import tech.pegb.backoffice.domain.parameter.implementation.ParameterMgmtService.Countries
import tech.pegb.backoffice.domain.parameter.model.{MetadataSchema, Parameter}
import tech.pegb.backoffice.domain.{BaseService, ServiceError, model}
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.parameter.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.parameter.Implicits._
import tech.pegb.backoffice.util.UUIDRepresentableId._
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.backoffice.util.Implicits._

import scala.concurrent.{ExecutionContext, Future}

class ParameterMgmtService @Inject() (
    executionContexts: WithExecutionContexts,
    typesDao: TypesDao,
    systemSettingsDao: SystemSettingsDao,
    currencyDao: CurrencyDao,
    countryDao: CountryDao,
    accountTypeDao: AccountTypesDao) extends ParameterManagement with BaseService {

  import ParameterMgmtService._

  implicit val ec: ExecutionContext = executionContexts.blockingIoOperations
  implicit val localDateOrdering: Ordering[LocalDateTime] = _ compareTo _

  def getParameterUUIDPrefix(metadataId: String): Future[ServiceResponse[Int]] = {
    Future.successful(MetadataMap.get(metadataId)
      .fold[ServiceResponse[Int]](ServiceError.notFoundError(s"parameter uuid prefix not found, metadata id: $metadataId").toLeft)(_.toRight))
  }

  def getMetadataSchemaById(metadataId: String): Future[ServiceResponse[MetadataSchema]] = {
    Future.successful(MetadataToSchemaLookupMap.get(metadataId)
      .fold[ServiceResponse[MetadataSchema]](ServiceError.notFoundError(s"metadata schema not found with id: $metadataId").toLeft)(_.toRight))
  }

  def getMetadataSchema: Future[ServiceResponse[Seq[(String, MetadataSchema)]]] = {
    Future.successful(MetadataToSchemaSeq.toRight)
  }

  def convertIdToParameterUUID(metadataId: String, actualDbId: Int): Future[ServiceResponse[UUID]] = ???

  def convertParameterUUIDToId(metadataId: String, uuid: UUID): Future[ServiceResponse[Int]] = ???

  def createParameter(createDto: ParameterToCreate): Future[ServiceResponse[Parameter]] = Future {

    createDto.metadataId match {
      //Note: not allowed to create AccountType/Currency/Country because it means creating a new table
      //if you want to add a new AccountType/Currency/Country use update
      case AccountTypes ⇒ Left(ServiceError.validationError("operation not allowed by design"))
      case Currencies ⇒ Left(ServiceError.validationError("operation not allowed by design"))
      case Countries ⇒ Left(ServiceError.validationError("operation not allowed by design"))

      case SystemSettings ⇒
        val systemSettingsParameter = createDto.value.as[SystemSettingParameter]
        systemSettingsDao.insertSystemSetting(systemSettingsParameter
          .asDao(createDto.createdAt, createDto.createdBy)).map(_.asDomain).asServiceResponse
      case Types ⇒
        val typeDescriptionValues = createDto.value.as[Seq[DescriptionToUpsert]]
        typesDao.insertType(createDto.key, createDto.createdAt, createDto.createdBy, typeDescriptionValues)
          .map(_.asDomain).asServiceResponse

      case _ ⇒ validationError(s"Unknown metadata_id [${createDto.metadataId}]").toLeft
    }
  }

  def getParameters: Future[ServiceResponse[Seq[Parameter]]] = Future((for {
    accountTypeParameter ← accountTypeDao.getAll.asServiceResponse(_.toSeq.asDomain)

    currencyParameter ← currencyDao.getAll.map(_.toSeq.asDomain).leftMap(_.asDomainError)

    typesParameter ← typesDao.fetchAllTypes.asServiceResponse(_.toSeq.map(_.asDomain))

    systemSettingParameter ← systemSettingsDao.getSystemSettingsByCriteria(None, None, None, None)
      .asServiceResponse(_.map(_.asDomain))

    countryParameter ← countryDao.getCountries.asServiceResponse(_.asDomain)

  } yield accountTypeParameter +: currencyParameter +: countryParameter +:
    typesParameter ++: systemSettingParameter))

  def filterParametersByCriteria(
    baseParameters: Seq[Parameter],
    criteriaDto: ParameterCriteria,
    ordering: Seq[model.Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[Parameter]]] = Future.successful {

    val idFiltered = criteriaDto.id.fold(baseParameters)(uuid ⇒ baseParameters.filter(_.id.toString == uuid.toString))
    val keyFiltered = criteriaDto.key.fold(idFiltered)(key ⇒ idFiltered.filter(_.key == key))
    val metadataIdFiltered = criteriaDto.metadataId.fold(keyFiltered)(metadataId ⇒ keyFiltered.filter(_.metadataId.toLowerCase == metadataId.toLowerCase))
    val platformFiltered = criteriaDto.platforms.fold(metadataIdFiltered)(platforms ⇒ metadataIdFiltered.filter(p ⇒ platforms.forall(p.platforms.contains)))

    val orderedParameterList = ordering.headOption.fold(platformFiltered.sortBy(_.key))(ordering ⇒ {
      val sorted = ordering.field match {

        case "id" ⇒ platformFiltered.sortBy(_.id.toString)
        case "key" ⇒ platformFiltered.sortBy(_.key.toString)
        case "value" ⇒ platformFiltered.sortBy(_.value.toString())
        case "explanation" ⇒ platformFiltered.sortWith((a, b) ⇒ Ordering[Option[String]].lt(a.explanation, b.explanation))
        case "metadata_id" ⇒ platformFiltered.sortBy(_.metadataId)
        case "created_at" ⇒ platformFiltered.sortWith((a, b) ⇒ Ordering[Option[LocalDateTime]].lt(a.createdAt, b.createdAt))
        case "created_by" ⇒ platformFiltered.sortWith((a, b) ⇒ Ordering[Option[String]].lt(a.createdBy, b.createdBy))
        case "updated_at" ⇒ platformFiltered.sortWith((a, b) ⇒ Ordering[Option[LocalDateTime]].lt(a.updatedAt, b.updatedAt))
        case "updated_by" ⇒ platformFiltered.sortWith((a, b) ⇒ Ordering[Option[String]].lt(a.updatedBy, b.updatedBy))
      }

      if (ordering.order == model.Ordering.ASCENDING) {
        sorted
      } else {
        sorted.reverse
      }
    })

    ((limit, offset) match {
      case (Some(lim), Some(off)) ⇒
        orderedParameterList.drop(off).take(lim)
      case (Some(lim), None) ⇒
        orderedParameterList.take(lim)
      case (None, Some(off)) ⇒
        orderedParameterList.drop(off).take(Int.MaxValue)
      case _ ⇒
        orderedParameterList
    }).toRight

  }

  def getLatestVersion(baseParameters: Seq[Parameter]): Future[ServiceResponse[Option[String]]] = Future.successful {

    val sortForLastUpdatedAt = baseParameters.sortWith((a, b) ⇒ Ordering[Option[LocalDateTime]].lt(a.updatedAt, b.updatedAt))

    Right(sortForLastUpdatedAt.lastOption.map(_.updatedAt.toString))
  }

  def countParametersByCriteria(baseParameters: Seq[Parameter], criteriaDto: ParameterCriteria): Future[ServiceResponse[Int]] = {

    val idFiltered = criteriaDto.id.fold(baseParameters)(uuid ⇒ baseParameters.filter(_.id.toString == uuid))
    val keyFiltered = criteriaDto.key.fold(idFiltered)(key ⇒ idFiltered.filter(_.key == key))
    val metadataIdFiltered = criteriaDto.metadataId.fold(keyFiltered)(metadataId ⇒ keyFiltered.filter(_.metadataId.toLowerCase == metadataId.toLowerCase))
    val platformFiltered = criteriaDto.platforms.fold(metadataIdFiltered)(platforms ⇒ metadataIdFiltered.filter(p ⇒ platforms.forall(p.platforms.contains)))

    Future.successful(platformFiltered.size.toRight)
  }

  def updateParameter(uuid: UUID, updateDto: ParameterToUpdate): Future[ServiceResponse[Parameter]] = Future {
    implicit val requestId: UUID = UUID.randomUUID()

    uuid.toPrefix.fold(
      e ⇒ Left(ServiceError.validationError(s"invalid id provided to identify parameter, ${e.getMessage}")),
      {
        case AccountTypesPrefix ⇒
          val json = updateDto.value
          val accountTypesToSave = json.as[Seq[AccountTypeToUpsert]]

          accountTypeDao.bulkUpsert(accountTypesToSave, updateDto.updatedAt, updateDto.updatedBy).map(_.asDomain).asServiceResponse

        case CurrenciesPrefix ⇒
          val json = updateDto.value
          val currenciesToSave = json.as[Set[CurrencyToUpsert]]
          currencyDao.bulkUpsert(currenciesToSave.toSeq, updateDto.updatedAt, updateDto.updatedBy)
            .map(_.asDomain).asServiceResponse

        case CountriesPrefix ⇒
          val dto = updateDto.value.as[Seq[CountryToUpsert]]
          for {
            _ ← countryDao.upsertCountry(dto).asServiceResponse
            countriesParam ← countryDao.getCountries.asServiceResponse(_.asDomain)
          } yield {
            countriesParam
          }

        case SystemSettingsPrefix ⇒
          uuid.toSuffix.fold(
            e ⇒ Left(ServiceError.validationError(s"invalid id to update $SystemSettings parameter, ${e.getMessage}")),
            idToUpdate ⇒ {

              val systemSettingToUpdate = updateDto.value.as[SystemSettingParameter]
              val dto = systemSettingToUpdate.asDao(updateDto.updatedAt, updateDto.updatedBy, updateDto.lastUpdatedAt)

              systemSettingsDao.updateSystemSettings(idToUpdate, dto)
                .map(_.asDomain).asServiceResponse

            })

        case TypesPrefix ⇒
          uuid.toSuffix.fold(
            e ⇒ Left(ServiceError.validationError(s"invalid id to update $Types parameter, ${e.getMessage}")),
            idToUpdate ⇒ {
              typesDao.getDescTypeAndDescriptionsById(idToUpdate).fold[ServiceResponse[Parameter]](
                _.asDomainError.toLeft,
                maybeTypeAndDesc ⇒ maybeTypeAndDesc.fold[ServiceResponse[Parameter]](
                  Left(ServiceError.notFoundError(s"no description type found with id $idToUpdate", requestId.toOption))) { typeAndDesc ⇒
                    val typeDescriptionValues = updateDto.value.as[Seq[DescriptionToUpsert]]

                    typesDao.bulkUpsert(
                      typeAndDesc._1.`type`,
                      updateDto.updatedAt,
                      updateDto.updatedBy,
                      updateDto.lastUpdatedAt,
                      typeDescriptionValues)
                      .map(_.asDomain).asServiceResponse

                  })

            })

      })

  }

}

object ParameterMgmtService {
  val AccountTypes = "account_types"
  val Currencies = "currencies"
  val SystemSettings = "system_settings"
  val Types = "types"
  val Countries = "countries"

  val AccountTypesPrefix = 1
  val CurrenciesPrefix = 2
  val SystemSettingsPrefix = 3
  val TypesPrefix = 4
  val CountriesPrefix = 5

  val MetadataMap: Map[String, Int] = Map[String, Int](
    AccountTypes → AccountTypesPrefix,
    Currencies → CurrenciesPrefix,
    SystemSettings → SystemSettingsPrefix,
    Types → TypesPrefix,
    Countries → CountriesPrefix)

  val MetadataToSchemaSeq = Seq(
    (AccountTypes, MetadataSchema(
      metadataId = AccountTypes,
      schema = AutoSchema.createSchema[AccountType],
      readOnlyFields = Seq("id"),
      isCreationAllowed = false,
      isDeletionAllowed = false,
      isArray = true)),
    (Currencies, MetadataSchema(
      metadataId = Currencies,
      schema = AutoSchema.createSchema[Currency],
      readOnlyFields = Seq("id"),
      isCreationAllowed = false,
      isDeletionAllowed = false,
      isArray = true)),
    (SystemSettings, MetadataSchema(
      metadataId = SystemSettings,
      schema = AutoSchema.createSchema[SystemSetting],
      readOnlyFields = Seq("id"),
      isCreationAllowed = true,
      isDeletionAllowed = false,
      isArray = false)),
    (Types, MetadataSchema(
      metadataId = Types,
      schema = AutoSchema.createSchema[Description],
      readOnlyFields = Seq("id"),
      isCreationAllowed = true,
      isDeletionAllowed = false,
      isArray = true)),

    (Countries, MetadataSchema(
      metadataId = Countries,
      schema = AutoSchema.createSchema[Country],
      readOnlyFields = Seq("id"),
      isCreationAllowed = false,
      isDeletionAllowed = false,
      isArray = true)))

  val MetadataToSchemaLookupMap = MetadataToSchemaSeq.toMap
}
