package tech.pegb.backoffice.dao.settings.abstraction

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao

import tech.pegb.backoffice.dao.model.OrderingSet
import tech.pegb.backoffice.dao.settings.dto.{SystemSettingsCriteria, SystemSettingToInsert, SystemSettingToUpdate}
import tech.pegb.backoffice.dao.settings.entity.SystemSetting
import tech.pegb.backoffice.dao.settings.sql.SystemSettingsSqlDao

@ImplementedBy(classOf[SystemSettingsSqlDao])
trait SystemSettingsDao extends Dao {

  def insertSystemSetting(dto: SystemSettingToInsert): DaoResponse[SystemSetting]

  def getSystemSettingById(id: Long): DaoResponse[Option[SystemSetting]]

  def getSystemSettingByKey(key: String): DaoResponse[Option[SystemSetting]]

  def getSystemSettingsByCriteria(
    criteria: Option[SystemSettingsCriteria],
    ordering: Option[OrderingSet] = None,
    limit: Option[Int] = None,
    offset: Option[Int] = None): DaoResponse[Seq[SystemSetting]]

  def getSystemSettingsTypes(): DaoResponse[Seq[String]]

  def countSystemSettingsByCriteria(criteria: SystemSettingsCriteria): DaoResponse[Int]

  def updateSystemSettings(id: Long, dto: SystemSettingToUpdate): DaoResponse[SystemSetting]

}
