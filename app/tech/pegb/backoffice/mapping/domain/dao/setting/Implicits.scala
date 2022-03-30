package tech.pegb.backoffice.mapping.domain.dao.setting

import tech.pegb.backoffice.dao.model.CriteriaField
import tech.pegb.backoffice.dao.model.MatchTypes.{Exact, Partial}
import tech.pegb.backoffice.dao.settings.dto.SystemSettingsCriteria
import tech.pegb.backoffice.domain.settings.dto.{SystemSettingsCriteria ⇒ domainSystemSettingCriteria}
import tech.pegb.backoffice.domain.settings.model.{SystemSetting ⇒ domainSystemSettings}
import tech.pegb.backoffice.dao.settings.entity.SystemSetting

object Implicits {

  implicit class SettingCriteriaAdapter(arg: domainSystemSettingCriteria) {
    def asDao: SystemSettingsCriteria = {

      SystemSettingsCriteria(
        id = arg.id.map(i ⇒ CriteriaField("", i, Exact)),
        key = arg.key.map(k ⇒ CriteriaField("key", k, Exact)),
        explanation = arg.explanation.map(e ⇒ CriteriaField("", e, Partial)),
        forAndroid = arg.forAndroid.map(e ⇒ CriteriaField("", e, Exact)),
        forIOS = arg.forIOS.map(e ⇒ CriteriaField("", e, Exact)),
        forBackoffice = arg.forBackoffice.map(e ⇒ CriteriaField("", e, Exact)))

    }
  }

  implicit class SystemSettingAdapter(arg: domainSystemSettings) {
    def asDao: SystemSetting = {

      SystemSetting(
        id = arg.id,
        key = arg.key,
        value = arg.value,
        `type` = arg.`type`,
        explanation = arg.explanation,
        forAndroid = arg.forAndroid,
        forIOS = arg.forIOS,
        forBackoffice = arg.forBackoffice,
        createdAt = arg.createdAt,
        createdBy = arg.createdBy,
        updatedAt = arg.updatedAt,
        updatedBy = arg.updatedBy)

    }
  }

}
