package tech.pegb.backoffice.mapping.dao.domain.parameter

import java.time.LocalDateTime

import cats.implicits._
import play.api.libs.json._
import tech.pegb.backoffice.dao.account.entity.AccountAttributes.AccountType
import tech.pegb.backoffice.dao.businessuserapplication.entity.Country
import tech.pegb.backoffice.dao.businessuserapplication.sql.CountrySqlDao
import tech.pegb.backoffice.dao.currency.entity.Currency
import tech.pegb.backoffice.dao.settings.entity.SystemSetting
import tech.pegb.backoffice.dao.types.entity.{Description, DescriptionType}
import tech.pegb.backoffice.domain.parameter.model.{Parameter, Platforms}
import tech.pegb.backoffice.domain.parameter.implementation.ParameterMgmtService._
import tech.pegb.backoffice.util.UUIDRepresentableId

object Implicits {

  implicit val localDateOrdering: Ordering[LocalDateTime] = _ compareTo _
  implicit val accountFormat: OWrites[AccountType] = Json.writes[AccountType]
  implicit val currencyFormat: OWrites[Currency] = Json.writes[Currency]
  implicit val countryFormat: OWrites[Country] = Json.writes[Country]
  implicit val descriptionFormat: OWrites[Description] = Json.writes[Description]
  implicit val systemSettingFormat: OWrites[SystemSetting] = Json.writes[SystemSetting]

  implicit class AccountTypesAdapter(val arg: Seq[AccountType]) extends AnyVal {

    def firstCreated = arg.sortBy(_.createdAt).headOption
    def lastUpdated = arg.sortWith((a, b) ⇒ Ordering[Option[LocalDateTime]].gt(a.updatedAt, b.updatedAt)).headOption

    def asDomain = Parameter(
      id = UUIDRepresentableId(MetadataMap(AccountTypes), 1).toUUID,
      key = AccountTypes,
      value = JsArray(arg.map(Json.toJson(_))),
      explanation = None,
      metadataId = AccountTypes,
      platforms = Platforms.AllValidPlatforms,
      createdAt = firstCreated.map(_.createdAt),
      createdBy = firstCreated.map(_.createdBy),
      updatedAt = lastUpdated.flatMap(_.updatedAt),
      updatedBy = lastUpdated.flatMap(_.updatedBy))
  }

  implicit class CurrencyAdapter(val arg: Seq[Currency]) extends AnyVal {

    def firstCreated = arg.sortBy(_.createdAt).headOption
    def lastUpdated = arg.sortWith((a, b) ⇒ Ordering[Option[LocalDateTime]].gt(a.updatedAt, b.updatedAt)).headOption

    def asDomain = Parameter(
      id = UUIDRepresentableId(MetadataMap(Currencies), 1).toUUID,
      key = Currencies,
      value = JsArray(arg.map(Json.toJson(_))),
      explanation = None,
      metadataId = Currencies,
      platforms = Platforms.AllValidPlatforms,
      createdAt = firstCreated.map(_.createdAt),
      createdBy = firstCreated.map(_.createdBy),
      updatedAt = lastUpdated.flatMap(_.updatedAt),
      updatedBy = lastUpdated.flatMap(_.updatedBy))
  }

  implicit class TypesAdapter(val arg: (DescriptionType, Seq[Description])) extends AnyVal {

    def asDomain = Parameter(
      id = UUIDRepresentableId(MetadataMap(Types), arg._1.id).toUUID,
      key = arg._1.`type`,
      value = JsArray(arg._2.map(Json.toJson(_))),
      explanation = None,
      metadataId = Types,
      platforms = Platforms.AllValidPlatforms,
      createdAt = arg._1.createdAt.some,
      createdBy = arg._1.createdBy.some,
      updatedAt = arg._1.updatedAt,
      updatedBy = arg._1.updatedBy)
  }

  implicit class SystemSettingsAdapter(val arg: SystemSetting) extends AnyVal {

    def web = if (arg.forBackoffice) Platforms.BackofficeWeb.some else none
    def android = if (arg.forAndroid) Platforms.MobileAndroid.some else none
    def ios = if (arg.forIOS) Platforms.MobileIOS.some else none

    def asDomain = {
      Parameter(
        id = UUIDRepresentableId(MetadataMap(SystemSettings), arg.id).toUUID,
        key = arg.key,
        value = Json.toJson(arg),
        explanation = arg.explanation,
        metadataId = SystemSettings,
        platforms = Seq(web, android, ios).flatten,
        createdAt = arg.createdAt.some,
        createdBy = arg.createdBy.some,
        updatedAt = arg.updatedAt,
        updatedBy = arg.updatedBy)
    }
  }

  implicit class CountryAdapter(val arg: Seq[Country]) extends AnyVal {
    def firstCreated = arg.sortBy(_.createdAt).headOption
    def lastUpdated = arg.sortWith((a, b) ⇒ Ordering[Option[LocalDateTime]].gt(a.updatedAt, b.updatedAt)).headOption

    def asDomain = Parameter(
      id = UUIDRepresentableId(MetadataMap(CountrySqlDao.TableName), 1).toUUID,
      key = CountrySqlDao.TableName,
      value = JsArray(arg.map(Json.toJson(_))),
      explanation = None,
      metadataId = CountrySqlDao.TableName,
      platforms = Platforms.AllValidPlatforms,
      createdAt = firstCreated.map(_.createdAt),
      createdBy = firstCreated.map(_.createdBy),
      updatedAt = lastUpdated.flatMap(_.updatedAt),
      updatedBy = lastUpdated.flatMap(_.updatedBy))
  }
}
