package tech.pegb.backoffice.domain.i18n.implementation

import java.sql.Connection
import java.time.LocalDateTime
import java.util.UUID

import cats.data.{EitherT, NonEmptyList}
import cats.implicits._
import com.google.inject.Inject
import tech.pegb.backoffice.dao.i18n.abstraction.I18nStringDao
import tech.pegb.backoffice.dao.i18n.entity
import tech.pegb.backoffice.dao.types.abstraction.TypesDao
import tech.pegb.backoffice.domain.i18n.abstraction.I18nStringManagement
import tech.pegb.backoffice.domain.i18n.dto.{I18nStringCriteria, I18nStringToCreate, I18nStringToUpdate, I18nStringToUpdateWithId}
import tech.pegb.backoffice.domain.i18n.model.I18nAttributes.I18nLocale
import tech.pegb.backoffice.domain.i18n.model.{I18nBulkInsertResult, I18nPair, I18nString}
import tech.pegb.backoffice.domain.model._
import tech.pegb.backoffice.domain.{BaseService, FieldValueValidation, ServiceError}
import tech.pegb.backoffice.mapping.dao.domain.Implicits._
import tech.pegb.backoffice.mapping.dao.domain.i18n.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.Implicits._
import tech.pegb.backoffice.mapping.domain.dao.i18n.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.util.WithExecutionContexts

import scala.concurrent.Future

class I18nMgmtService @Inject() (
    executionContexts: WithExecutionContexts,
    val typesDao: TypesDao,
    dao: I18nStringDao) extends I18nStringManagement with BaseService with FieldValueValidation {

  implicit val ec = executionContexts.blockingIoOperations

  def createI18nString(createDto: I18nStringToCreate)(implicit requestId: UUID): Future[ServiceResponse[I18nString]] = {
    (for {
      _ ← EitherT.fromEither[Future](
        validateFieldsFromKnownTypes(createDto.locale.underlying, "locale"))

      _ ← EitherT.fromEither[Future](
        validateFieldsFromKnownTypes(createDto.platform.underlying, "platform"))

      i18nString ← EitherT.fromEither[Future](dao.insertString(createDto.asDao).asServiceResponse)
    } yield {
      i18nString.asDomain
    }).value
  }

  def bulkCreateI18nString(locale: I18nLocale, dtos: Seq[I18nStringToCreate]): Future[ServiceResponse[I18nBulkInsertResult]] = {
    (for {
      _ ← EitherT.fromEither[Future](validateBulkList(locale, dtos))

      partitionResponse ← {
        logger.info("[bulkCreateI18nString] partitioning list to insert or update")
        EitherT.fromEither[Future](partitionExistingKeys(locale, dtos)(None))
      }

      (dtosToUpdate, dtosToInsert) = partitionResponse

      txnConn ← EitherT.fromEither[Future](dao.startTransaction.asServiceResponse)

      bulkInsertResult ← {
        logger.info(s"[bulkCreateI18nString] conenction to use $txnConn")
        dtosToInsert match {
          case head :: tail ⇒
            logger.info(s"[bulkCreateI18nString] performing bulk insert on ${dtosToInsert.size} items")
            EitherT.fromEither[Future](dao.bulkInsertString(NonEmptyList(head.asDao, tail.map(_.asDao)))(txnConn.some).asServiceResponse)
          case Nil ⇒
            logger.info(s"[bulkCreateI18nString] no items to insert")
            EitherT.fromEither[Future](0.asRight[ServiceError])
        }
      }
      bulkUpdateResult ← {
        logger.info(s"[bulkCreateI18nString] performing update on ${dtosToUpdate.size} item(s)")
        EitherT(bulkUpdate(dtosToUpdate, txnConn))
      }

      _ ← {
        logger.info(s"[bulkCreateI18nString] closing txn")
        EitherT.fromEither[Future](dao.endTransaction(txnConn).asServiceResponse)
      }
    } yield {
      I18nBulkInsertResult(
        insertedRowCount = bulkInsertResult,
        updatedRowCount = bulkUpdateResult)
    }).value

  }

  def getI18nStringById(id: Int)(implicit requestId: UUID): Future[ServiceResponse[I18nString]] = {
    (for {
      i18nStringOption ← EitherT.fromEither[Future](dao.getStringById(id).asServiceResponse)

      i18nString ← EitherT.fromOption[Future](
        i18nStringOption,
        ServiceError.notFoundError(s"I18n String id [$id] not found", requestId.toOption))
    } yield i18nString.asDomain).value
  }

  def getI18nStringByCriteria(
    criteriaDto: I18nStringCriteria,
    ordering: Seq[Ordering],
    limit: Option[Int],
    offset: Option[Int]): Future[ServiceResponse[Seq[I18nString]]] = Future {
    dao.getStringByCriteria(criteriaDto.asDao, ordering.asDao, limit, offset).map(_.map(_.asDomain)).asServiceResponse
  }

  def countI18nStringByCriteria(criteriaDto: I18nStringCriteria): Future[ServiceResponse[Int]] = Future {
    dao.countStringByCriteria(criteriaDto.asDao).asServiceResponse
  }

  def updateI18nString(id: Int, updateDto: I18nStringToUpdate)(implicit requestId: UUID): Future[ServiceResponse[I18nString]] = {
    (for {
      i18nStringOption ← EitherT.fromEither[Future](dao.updateString(id, updateDto.asDao).asServiceResponse)
      updateResult ← EitherT.fromOption[Future](i18nStringOption, notFoundError(s"I18n string $id was not found"))
    } yield updateResult.asDomain).value
  }

  def deleteI18nString(id: Int, lastUpdatedAt: Option[LocalDateTime])(implicit requestId: UUID): Future[ServiceResponse[Int]] = {
    (for {
      deleteResultOption ← EitherT.fromEither[Future](dao.deleteString(id, lastUpdatedAt).asServiceResponse)
      deletedId ← EitherT.fromOption[Future](deleteResultOption, notFoundError(s"I18n string $id was not found"))
    } yield deletedId).value
  }

  def validateFieldsFromKnownTypes(fieldvalue: String, fieldName: String): ServiceResponse[String] = {
    val result = fieldName match {
      case "locale" ⇒ typesDao.getLocales.map(_.find(_._2 == fieldvalue).map(_._2))
      case "platform" ⇒ typesDao.getPlatformTypes.map(_.find(_._2 == fieldvalue).map(_._2))
      case _ ⇒ Right(Option.empty[String])
    }

    result.asServiceResponse
      .flatMap(_.toRight(ServiceError.validationError(s"provided element `$fieldvalue` is invalid for '$fieldName'")))

  }

  def getI18nDictionary(criteriaDto: I18nStringCriteria): Future[ServiceResponse[Seq[I18nPair]]] = Future {
    dao.getI18nPairsByCriteria(criteriaDto.asDao).map(_.map(_.asDomain)).asServiceResponse
  }

  private def validateBulkList(locale: I18nLocale, dtos: Seq[I18nStringToCreate]): ServiceResponse[Unit] = {
    val itemsWithDifferentLocale = dtos.filter(d ⇒ d.locale != locale)
    val compoundKeyWithDuplicates = dtos.groupBy(p ⇒ p.getCompoundKey).filter { case (key, value) ⇒ value.size > 1 }

    for {
      _ ← Either.cond(itemsWithDifferentLocale.isEmpty, (), ServiceError.validationError(s"Items with different locale from ${locale.underlying} are found: ${itemsWithDifferentLocale.map(_.getCompoundKey)}"))
      _ ← Either.cond(compoundKeyWithDuplicates.isEmpty, (), ServiceError.validationError(s"Duplicates found in items to create: ${compoundKeyWithDuplicates.keySet}"))
    } yield {
      ()
    }
  }

  private def partitionExistingKeys(locale: I18nLocale, dtoToCreate: Seq[I18nStringToCreate])(connection: Option[Connection] = None): ServiceResponse[(Seq[I18nStringToUpdateWithId], Seq[I18nStringToCreate])] = {
    (for {
      existingStrings ← dao.getStringByCriteria(I18nStringCriteria(locale = locale.some).asDao, None, None, None)(connection).asServiceResponse
      existingKeyMap = existingStrings.map(string ⇒ string.asCompoundKey → string.id).toMap
    } yield {
      val partionedDto = dtoToCreate.partition {
        dto ⇒ existingKeyMap.keySet.contains(dto.getCompoundKey)
      }

      partionedDto.copy(_1 = partionedDto._1.map(dto ⇒ dto.toUpdateWithIdDto(existingKeyMap(dto.getCompoundKey))))
    })
  }

  private def bulkUpdate(dtos: Seq[I18nStringToUpdateWithId], connection: Connection): Future[ServiceResponse[Int]] = {

    //parallel execution of update using the same connection
    val updateResultSeqFuture = Future.sequence(
      dtos.map(d ⇒ Future(dao.updateString(d.id, d.dto.asDao, disableOptimisticLock = true)(connection.some).asServiceResponse)))

    //reduce sequence of either to either of sequence (inside future)
    val futureOfServiceResponse = updateResultSeqFuture.map(seq ⇒ seq.toList.sequence[ServiceResponse, Option[entity.I18nString]])

    //return number of updated values
    EitherT(futureOfServiceResponse).map(_.size).value
  }
}
