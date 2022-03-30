package tech.pegb.backoffice.mapping.dao.domain.setting

import tech.pegb.backoffice.dao.settings.entity.{SystemSetting ⇒ SystemSettingDao}
import tech.pegb.backoffice.domain.settings.model.SystemSetting

object Implicits {

  implicit class SystemSettingDaoAdapter(arg: SystemSettingDao) {

    def asDomain: SystemSetting = {
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
