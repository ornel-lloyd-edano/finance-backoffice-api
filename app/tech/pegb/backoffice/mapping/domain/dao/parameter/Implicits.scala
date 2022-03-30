package tech.pegb.backoffice.mapping.domain.dao.parameter

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.account.dto.AccountTypeToUpsert
import tech.pegb.backoffice.dao.currency.dto.CurrencyToUpsert
import play.api.libs.json.JsonConfiguration.Aux
import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json._
import tech.pegb.backoffice.dao.businessuserapplication.dto.CountryToUpsert
import tech.pegb.backoffice.dao.types.entity._
import tech.pegb.backoffice.dao.settings.dto.{SystemSettingParameter, SystemSettingToInsert, SystemSettingToUpdate}

object Implicits {

  implicit val config: Aux[Json.MacroOptions] = JsonConfiguration(SnakeCase)

  implicit val typesAndDescription: OFormat[DescriptionToUpsert] = Json.format[DescriptionToUpsert]

  implicit val currencyInserts: OFormat[CurrencyToUpsert] = Json.format[CurrencyToUpsert]

  implicit val accountsTypeInserts: OFormat[AccountTypeToUpsert] = Json.format[AccountTypeToUpsert]

  implicit val systemSetting: OFormat[SystemSettingParameter] = Json.format[SystemSettingParameter]

  implicit val countryToInsert = Json.format[CountryToUpsert]

  implicit class SystemSettingDaoConverter(val systemSettingParameter: SystemSettingParameter) extends AnyVal {

    def asDao(
      updatedAt: LocalDateTime,
      updatedBy: String,
      lastUpdatedAt: Option[LocalDateTime]): SystemSettingToUpdate = {
      SystemSettingToUpdate(
        key = systemSettingParameter.key,
        value = systemSettingParameter.value,
        `type` = systemSettingParameter.`type`,
        explanation = systemSettingParameter.explanation,
        forAndroid = systemSettingParameter.forAndroid,
        forIOS = systemSettingParameter.forIOS,
        forBackoffice = systemSettingParameter.forBackoffice,
        updatedAt = updatedAt,
        updatedBy = updatedBy,
        lastUpdatedAt = lastUpdatedAt)
    }
  }

  implicit class SystemSettingCreator(val systemSettingParameter: SystemSettingParameter) extends AnyVal {

    def asDao(
      createdAt: LocalDateTime,
      createdBy: String): SystemSettingToInsert = {
      SystemSettingToInsert(
        key = systemSettingParameter.key.getOrElse(""),
        value = systemSettingParameter.value.getOrElse(""),
        `type` = systemSettingParameter.`type`.getOrElse(""),
        explanation = systemSettingParameter.explanation,
        forAndroid = systemSettingParameter.forAndroid.getOrElse(false),
        forIOS = systemSettingParameter.forIOS.getOrElse(false),
        forBackoffice = systemSettingParameter.forBackoffice.getOrElse(false),
        createdAt = createdAt,
        createdBy = createdBy)
    }
  }

}
