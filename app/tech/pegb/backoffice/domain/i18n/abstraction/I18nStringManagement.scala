package tech.pegb.backoffice.domain.i18n.abstraction

import java.time.LocalDateTime
import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.i18n.dto.{I18nStringCriteria, I18nStringToCreate, I18nStringToUpdate}
import tech.pegb.backoffice.domain.i18n.implementation.I18nMgmtService
import tech.pegb.backoffice.domain.i18n.model.I18nAttributes.I18nLocale
import tech.pegb.backoffice.domain.i18n.model.{I18nBulkInsertResult, I18nPair, I18nString}
import tech.pegb.backoffice.domain.model

import scala.concurrent.Future

@ImplementedBy(classOf[I18nMgmtService])
trait I18nStringManagement {

  def createI18nString(createDto: I18nStringToCreate)(implicit requestId: UUID): Future[ServiceResponse[I18nString]]

  def bulkCreateI18nString(locale: I18nLocale, dtos: Seq[I18nStringToCreate]): Future[ServiceResponse[I18nBulkInsertResult]]

  def getI18nStringById(id: Int)(implicit requestId: UUID): Future[ServiceResponse[I18nString]]

  def getI18nStringByCriteria(criteriaDto: I18nStringCriteria, ordering: Seq[model.Ordering],
    limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[I18nString]]]

  def countI18nStringByCriteria(criteriaDto: I18nStringCriteria): Future[ServiceResponse[Int]]

  def updateI18nString(id: Int, updateDto: I18nStringToUpdate)(implicit requestId: UUID): Future[ServiceResponse[I18nString]]

  def deleteI18nString(id: Int, lastUpdatedAt: Option[LocalDateTime])(implicit requestId: UUID): Future[ServiceResponse[Int]]

  def getI18nDictionary(criteriaDto: I18nStringCriteria): Future[ServiceResponse[Seq[I18nPair]]]
}
