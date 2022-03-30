package tech.pegb.backoffice.dao.types.abstraction

import java.time.LocalDateTime

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.types.entity.{Description, DescriptionToUpsert, DescriptionType}
import tech.pegb.backoffice.dao.types.sql.TypesSqlDao

//TODO it seems TypesDao is more like TypesService, fix this by swapping their responsibility
@ImplementedBy(classOf[TypesSqlDao])
trait TypesDao extends Dao {

  type R = Dao.DaoResponse[List[(Int, String, Option[String])]]

  def insertDescription(existingKind: String, newValue: String, explanation: Option[String]): Dao.DaoResponse[Int]

  def insertType(
    newKind: String,
    createdAt: LocalDateTime,
    createdBy: String,
    newValues: Seq[DescriptionToUpsert]): Dao.DaoResponse[(DescriptionType, Seq[Description])]

  def bulkUpsert(
    existingKind: String,
    updatedAt: LocalDateTime,
    updatedBy: String,
    lastUpdatedAt: Option[LocalDateTime],
    newValues: Seq[DescriptionToUpsert],
    disableOptimisticLockCheck: Boolean = false): Dao.DaoResponse[(DescriptionType, Seq[Description])]

  def getDescTypeAndDescriptionsById(id: Long): Dao.DaoResponse[Option[(DescriptionType, Seq[Description])]]

  def fetchAllTypes: Dao.DaoResponse[Map[DescriptionType, Seq[Description]]]

  def fetchCustomType(kind: String): Dao.DaoResponse[List[(Int, String, Option[String])]]

  def getCustomerTypes: R

  def getIndividualUserTypes: R

  def getCustomerTiers: R

  def getBusinessUserTiers: R

  def getCustomerSegments: R

  def getCustomerSubscriptions: R

  def getInstruments: R

  def getChannels: R

  def getTransactionTypes: R

  def getApplicationStages: R

  def getDocumentTypes: R

  def getImageTypes: R

  def getOccupations: R

  def getNationalities: R

  def getEmployers: R

  def getCompanies: R

  def getLimitTypes: R

  def getFeeTypes: R

  def getFeeCalculationMethod: R

  def getTimeIntervalTypes: R

  def getPlatformTypes: R

  def getLocales: R

  def getTaskStatuses: R

  def getCommunicationChannels: R

  def getFeeMethods: R

  def getBusinessTypes: R

  def getBusinessCategories: R
}
