package tech.pegb.backoffice.dao.document.sql

import java.sql.{Connection, SQLException}
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.{Inject, Singleton}

import anorm._
import play.api.db.DBApi
import tech.pegb.backoffice.application.KafkaDBSyncService
import tech.pegb.backoffice.dao.{SqlDao}
import tech.pegb.backoffice.dao.application.abstraction.WalletApplicationDao
import tech.pegb.backoffice.dao.application.sql.WalletApplicationSqlDao
import tech.pegb.backoffice.dao.businessuserapplication.abstraction.BusinessUserApplicationDao
import tech.pegb.backoffice.dao.businessuserapplication.dto.BusinessUserApplicationCriteria
import tech.pegb.backoffice.dao.businessuserapplication.sql.BusinessUserApplicationSqlDao
import tech.pegb.backoffice.dao.currencyexchange.sql.SpreadsSqlDao.cUuid
import tech.pegb.backoffice.dao.customer.sql.{BusinessUserSqlDao, IndividualUserSqlDao, UserSqlDao}
import tech.pegb.backoffice.dao.document.abstraction.DocumentDao
import tech.pegb.backoffice.dao.document.dto.{DocumentCriteria, DocumentToCreate, DocumentToUpdate}
import tech.pegb.backoffice.dao.document.entity.Document
import tech.pegb.backoffice.dao.document.sql.DocumentSqlDao._
import tech.pegb.backoffice.dao.model.{MatchTypes, OrderingSet}
import tech.pegb.backoffice.dao.sql.MostRecentUpdatedAtGetter
import tech.pegb.backoffice.dao.transaction.sql.TransactionSqlDao
import tech.pegb.backoffice.util.AppConfig
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

@Singleton
class DocumentSqlDao @Inject() (
    override protected val dbApi: DBApi,
    config: AppConfig,
    userSqlDao: UserSqlDao,
    applicationSqlDao: WalletApplicationDao,
    buApplicationSqlDao: BusinessUserApplicationDao,
    kafkaDBSyncService: KafkaDBSyncService)
  extends DocumentDao with MostRecentUpdatedAtGetter[Document, DocumentCriteria] with SqlDao {

  protected def getUpdatedAtColumn: String = s"${DocumentSqlDao.TableAlias}.${TransactionSqlDao.cUpdatedAt}"

  protected def getMainSelectQuery: String = DocumentSqlDao.qSelect

  protected def getRowToEntityParser: Row ⇒ Document = (row: Row) ⇒ DocumentSqlDao.convertRowToDocument(row)

  protected def getWhereFilterFromCriteria(criteriaDto: Option[DocumentCriteria]): String = generateWhereFilter(criteriaDto)

  override def getDocument(id: UUID): DaoResponse[Option[Document]] = {
    withConnection({ implicit cxn ⇒
      internalGetDoc(id)
    }, s"Unexpected error when fetching doc `$id`")
  }

  override def getDocumentByInternalId(id: Int): DaoResponse[Option[Document]] = {
    withConnection({ implicit cxn ⇒
      internalGetDocByAutoId(id)
    }, s"Unexpected error when fetching doc `$id`")
  }

  override def getDocumentsByCriteria(
    criteria: DocumentCriteria,
    maybeOrderBy: Option[OrderingSet],
    maybeLimit: Option[Int],
    maybeOffset: Option[Int]): DaoResponse[Seq[Document]] = {
    withConnectionAndFlatten({ implicit cxn ⇒
      val whereClause = generateWhereFilter(criteria.toOption)

      val pagination = SqlDao.getPagination(maybeLimit, maybeOffset)
      val ordering = maybeOrderBy.fold("")(_.toString)

      val tail = s"$ordering $pagination;"
      SQL(qSelect + whereClause + tail).as[Seq[Document]](docRowParser.*).toRight

    }, s"Couldn't fetch documents by filters $criteria")
  }

  override def countDocumentsByCriteria(criteria: DocumentCriteria): DaoResponse[Int] = {
    withConnectionAndFlatten({ implicit cxn ⇒
      // all other filters are not used now
      val tail = generateWhereFilter(criteria.toOption)
      criteria.applicationId.fold {
        Right(SQL(qCount + tail + ";").as(SqlParser.scalar[Int].single)): DaoResponse[Int]
      } { aid ⇒
        for {
          internalApplicationId ← applicationSqlDao.getInternalIdByUUID(aid)
        } yield {
          SQL(qCount + s" WHERE $TableAlias.$cIndivUserApplicationId = {$cIndivUserApplicationId};")
            .on(cIndivUserApplicationId → internalApplicationId)
            .as(SqlParser.scalar[Int].single)
        }
      }
    }, s"Couldn't count documents by filters $criteria")
  }

  override def createDocument(document: DocumentToCreate)(implicit txnConn: Option[Connection]): DaoResponse[Document] = {
    withTransactionAndFlatten({ cxn ⇒
      implicit val txnConnection = txnConn.getOrElse(cxn)

      for {
        userId ← document.customerId.map(custId ⇒ userSqlDao.getInternalUserId(custId.toString)) match {
          case Some(Right(result)) ⇒ Right(result)
          case Some(Left(error)) ⇒ Left(error)
          case None ⇒ Right(None)
        }
        walletApplicationId ← document.walletApplicationId.map(id ⇒ {
          applicationSqlDao.getInternalIdByUUID(id)
        }) match {
          case Some(Right(result)) ⇒ Right(Some(result))
          case Some(Left(error)) ⇒ Left(error)
          case None ⇒ Right(None)
        }
        buApplicationId ← document.businessApplicationId.map(id ⇒ {
          buApplicationSqlDao.getBusinessUserApplicationByCriteria(BusinessUserApplicationCriteria(id), None, None, None)
            .map(_.headOption.map(_.id))
        }) match {
          case Some(Right(result)) ⇒ Right(result)
          case Some(Left(error)) ⇒ Left(error)
          case None ⇒ Right(None)
        }
        id = CreateDocQuery
          .on(
            cUuid → UUID.randomUUID(),
            cCustomerId → userId,
            cIndivUserApplicationId → walletApplicationId,
            cBusiUserApplicationId → buApplicationId,
            cStatus → DocumentToCreate.InitialStatus,
            cDocumentNumber → document.documentIdentifier,
            cDocumentType → document.documentType,
            cPurpose → document.purpose,
            cRejectionReason → Option.empty[String],
            cImageType → Option.empty[String],
            cProperties → Option.empty[String],
            cCreatedAt → document.createdAt,
            cCreatedBy → document.createdBy,
            cCheckedAt → Option.empty[LocalDateTime],
            cCheckedBy → Option.empty[String],
            cFileName → document.fileName,
            cFileUploadedAt → Option.empty[LocalDateTime],
            cFileUploadedBy → Option.empty[String],
            cUpdatedAt → document.createdAt, //not nullable in db and same as created at on insertion
            cUpdatedBy → document.createdBy) //not nullable in db and same as created by on insertion
          .executeInsert(SqlParser.scalar[Int].single)

        doc ← internalGetDocByAutoId(id)
          .toRight(entityNotFoundError(s"Couldn't find just created doc $id"))
      } yield {
        kafkaDBSyncService.sendInsert(TableName, doc)
        doc
      }
    }, "Couldn't create document")
  }

  override def updateDocument(id: UUID, documentToUpdate: DocumentToUpdate)(implicit txnConn: Option[Connection]): DaoResponse[Option[Document]] = {
    withTransaction({ cxn ⇒
      implicit val txnConnection = txnConn.getOrElse(cxn)
      for {
        existing ← internalGetDoc(id)
        updateResult = internalUpdateDoc(id, documentToUpdate)
        updated ← if (updateResult > 0) {
          internalGetDoc(id)
        } else {
          throw new IllegalStateException(s"Update failed. Document $id has been modified by another process.")
        }
      } yield {
        kafkaDBSyncService.sendUpdate(TableName, updated)
        updated
      }
    }, s"Couldn't update document `$id`",
      handlerPF = {
        case e: SQLException ⇒
          val errorMessage = s"Could not update document $id"
          logger.error(errorMessage, e)
          constraintViolationError(errorMessage)
        case ie: IllegalStateException ⇒
          preconditionFailed(ie.getMessage)
      })
  }

  override def delsert(dto: Seq[DocumentToCreate], criteriaToDelete: DocumentCriteria)(implicit txnConn: Option[Connection]): DaoResponse[Seq[Document]] = {
    withTransactionAndFlatten({ cxn ⇒
      implicit val actualTxn = txnConn.getOrElse(cxn)

      for {
        docs ← getDocumentsByCriteria(criteriaToDelete, None, None, None)

        _ ← if (docs.nonEmpty) {
          val rawDeleteSql =
            s"""
               |DELETE FROM $TableName
               |WHERE $cId IN (${docs.map(doc ⇒ s"'${doc.id}'").mkString(", ")})
            """.stripMargin

          Right(SQL(rawDeleteSql).executeUpdate())

        } else {
          Right(0)
        }

      } yield {
        val (success, failures) = dto.map(createDocument(_)(Some(actualTxn))).partition(_.isRight)
        if (failures.nonEmpty) throw new IllegalStateException("failure to insert 1 or more documents")
        success.map(_.right.get)
      }

    }, s"Couldn't insert the documents ${dto.defaultMkString} and delete documents based on criteria [${criteriaToDelete.toSmartString}]")
  }

  protected def internalGetDoc(id: UUID)(implicit cxn: Connection): Option[Document] = {
    SelectDocByUuidQuery.on(cUuid → id)
      .as[Option[Document]](docRowParser.singleOpt)
  }

  protected def internalGetDocByAutoId(id: Int)(implicit cxn: Connection): Option[Document] = {
    SelectDocByIdQuery
      .on(cId → id)
      .as(docRowParser.singleOpt)
  }
}

object DocumentSqlDao {
  final val TableName = "application_documents"
  final val TableAlias = "ad"
  final val cId = "id"
  final val cUuid = "uuid"
  final val cCustomerId = "user_id"
  final val cCustomerUuid = "user_uuid"
  final val cIndivUserApplicationId = "application_id"
  final val cIndivUserApplicationUuid = "application_uuid"
  final val cBusiUserApplicationId = "bu_application_id"
  final val cBusiUserApplicationUuid = "bu_application_uuid"
  final val cStatus = "status"
  final val cDocumentNumber = "document_number"
  final val cDocumentType = "document_type"
  final val cPurpose = "purpose"
  final val cRejectionReason = "rejection_reason"
  final val cImageType = "image_type"
  final val cProperties = "properties"
  final val cCreatedAt = "created_at"
  final val cCreatedBy = "created_by"
  final val cCheckedAt = "checked_at"
  final val cCheckedBy = "checked_by"
  final val cFileName = "file_name"
  final val cFileUploadedAt = "file_uploaded_at"
  final val cFileUploadedBy = "file_uploaded_by"
  final val cFilePersistedAt = "file_persisted_at"
  final val cUpdatedAt = "updated_at"
  final val cUpdatedBy = "updated_by"

  private final val CreateDocQuery: SqlQuery = SQL(s"INSERT INTO $TableName " +
    s"($cUuid, $cCustomerId, $cIndivUserApplicationId, $cBusiUserApplicationId, $cStatus, $cDocumentNumber, " +
    s"$cDocumentType, $cPurpose, $cRejectionReason, $cImageType, $cProperties, " +
    s"$cCreatedBy, $cCreatedAt,  $cFileName,  $cFileUploadedBy, $cFileUploadedAt, " +
    s"$cCheckedBy, $cCheckedAt, $cUpdatedBy, $cUpdatedAt) " +
    s"VALUES ({$cUuid}, {$cCustomerId}, {$cIndivUserApplicationId}, {$cBusiUserApplicationId}, {$cStatus}, {$cDocumentNumber}, {$cDocumentType}, " +
    s"{$cPurpose}, {$cRejectionReason}, {$cImageType}, {$cProperties}, {$cCreatedBy}, {$cCreatedAt}, " +
    s"{$cFileName}, {$cFileUploadedBy}, {$cFileUploadedAt}, {$cCheckedBy}, {$cCheckedAt}, {$cUpdatedBy}, {$cUpdatedAt});")

  private val qCommonJoin =
    s"""
       |FROM $TableName $TableAlias
       |
       |LEFT JOIN ${UserSqlDao.TableName} ${UserSqlDao.TableAlias}
       |ON ${UserSqlDao.TableAlias}.${UserSqlDao.id} = $TableAlias.$cCustomerId
       |
       |LEFT JOIN ${WalletApplicationSqlDao.TableName} ${WalletApplicationSqlDao.TableAlias}
       |ON ${WalletApplicationSqlDao.TableAlias}.${WalletApplicationSqlDao.id} = $TableAlias.$cIndivUserApplicationId
       |
       |LEFT JOIN ${BusinessUserApplicationSqlDao.TableName} ${BusinessUserApplicationSqlDao.TableAlias}
       |ON ${BusinessUserApplicationSqlDao.TableAlias}.${BusinessUserApplicationSqlDao.cId} = $TableAlias.$cBusiUserApplicationId
       |
       |LEFT JOIN ${BusinessUserSqlDao.TableName} ${BusinessUserSqlDao.TableAlias}
       |ON ${UserSqlDao.TableAlias}.${UserSqlDao.id} = ${BusinessUserSqlDao.TableAlias}.${BusinessUserSqlDao.cUserId}
       |
       |LEFT JOIN ${IndividualUserSqlDao.TableName} ${IndividualUserSqlDao.TableAlias}
       |ON ${UserSqlDao.TableAlias}.${UserSqlDao.id} = ${IndividualUserSqlDao.TableAlias}.${IndividualUserSqlDao.userId}
     """.stripMargin

  private final val qSelect: String =
    s"""
       |SELECT $TableAlias.*,
       |${UserSqlDao.TableAlias}.${UserSqlDao.uuid}  as $cCustomerUuid,
       |${IndividualUserSqlDao.TableAlias}.${IndividualUserSqlDao.fullName},
       |${IndividualUserSqlDao.TableAlias}.${IndividualUserSqlDao.msisdn},
       |
       |${BusinessUserSqlDao.TableAlias}.${BusinessUserSqlDao.cBusinessName},
       |${BusinessUserSqlDao.TableAlias}.${BusinessUserSqlDao.cBrandName},
       |
       |${WalletApplicationSqlDao.TableAlias}.${WalletApplicationSqlDao.uuid} as $cIndivUserApplicationUuid,
       |${BusinessUserApplicationSqlDao.TableAlias}.${BusinessUserApplicationSqlDao.cUuid} as $cBusiUserApplicationUuid
       |
       |$qCommonJoin
     """.stripMargin

  private final val qCount: String = s"SELECT COUNT($TableAlias.$cId) $qCommonJoin"

  private final val SelectDocByUuidQuery: SqlQuery = SQL(qSelect + s" WHERE $TableAlias.$cUuid = {$cUuid};")

  private final val SelectDocByIdQuery: SqlQuery = SQL(qSelect + s" WHERE $TableAlias.$cId = {$cId};")

  private final def internalUpdateDoc(id: UUID, documentToUpdate: DocumentToUpdate)(implicit cxn: Connection): Int = {
    val paramsBuilder = documentToUpdate.paramsBuilder
    paramsBuilder += cUuid → id
    val preQuery = documentToUpdate.createSqlString(TableName, Some(s" WHERE $TableName.$cUuid = {$cUuid}"))
    val params = paramsBuilder.result()
    SQL(preQuery).on(params: _*).executeUpdate()
  }

  private def convertRowToDocument(row: Row): Document = {
    Document(
      id = row[Int](s"${TableName}.$cId"),
      uuid = row[UUID](s"${TableName}.$cUuid"),
      customerId = row[Option[UUID]](cCustomerUuid),
      walletApplicationId = row[Option[UUID]](cIndivUserApplicationUuid),
      businessUserApplicationId = row[Option[UUID]](cBusiUserApplicationUuid),
      documentType = row[String](s"${TableName}.$cDocumentType"),
      documentIdentifier = row[Option[String]](s"${TableName}.$cDocumentNumber"),
      purpose = row[String](s"${TableName}.$cPurpose"),
      status = row[String](s"${TableName}.$cStatus"),
      rejectionReason = row[Option[String]](s"${TableName}.$cRejectionReason"),
      checkedBy = row[Option[String]](s"${TableName}.$cCheckedBy"),
      checkedAt = row[Option[LocalDateTime]](s"${TableName}.$cCheckedAt"),
      fileName = row[Option[String]](cFileName),
      fileUploadedBy = row[Option[String]](s"${TableName}.$cFileUploadedBy"),
      fileUploadedAt = row[Option[LocalDateTime]](s"${TableName}.$cFileUploadedAt"),
      createdBy = row[String](s"${TableName}.$cCreatedBy"),
      createdAt = row[LocalDateTime](s"${TableName}.$cCreatedAt"),
      updatedBy = row[Option[String]](s"${TableName}.$cUpdatedBy"),
      updatedAt = row[Option[LocalDateTime]](s"${TableName}.$cUpdatedAt"))
  }

  private val docRowParser: RowParser[Document] = row ⇒ {
    Try {
      convertRowToDocument(row)
    }.fold(
      exc ⇒ anorm.Error(SqlRequestError(exc)),
      anorm.Success(_))
  }

  private def generateWhereFilter(maybeCriteria: Option[DocumentCriteria]) = {
    import SqlDao._

    maybeCriteria match {
      case Some(criteria) ⇒
        val customerId = criteria.customerId.map { cf ⇒
          queryConditionClause(cf.value, UserSqlDao.uuid, Some(UserSqlDao.TableAlias), cf.operator == MatchTypes.Partial)
        }

        val individualUserMsisdn = criteria.individualUserMsisdn.map { cf ⇒
          queryConditionClause(cf.value, IndividualUserSqlDao.msisdn, Some(IndividualUserSqlDao.TableAlias), cf.operator == MatchTypes.Partial)
        }

        val individualUserFullName = criteria.individualUserFullName.map { cf ⇒
          queryConditionClause(cf.value, IndividualUserSqlDao.fullName, Some(IndividualUserSqlDao.TableAlias), cf.operator == MatchTypes.Partial)
        }

        val documentType = criteria.documentType.
          map(queryConditionClause(_, cDocumentType, Some(TableAlias)))
        val documentIdentifier = criteria.documentIdentifier.
          map(queryConditionClause(_, cDocumentNumber, Some(TableAlias)))
        val status = criteria.status.
          map(queryConditionClause(_, cStatus, Some(TableAlias)))
        val applicationId = criteria.applicationId.
          map(queryConditionClause(_, WalletApplicationSqlDao.uuid, Some(WalletApplicationSqlDao.TableAlias)))
        val businessUserApplicationId = criteria.businessUserApplicationId.
          map(queryConditionClause(_, BusinessUserApplicationSqlDao.cUuid, Some(BusinessUserApplicationSqlDao.TableAlias)))
        val checkedBy = criteria.checkedBy.
          map(queryConditionClause(_, cCheckedBy, Some(TableAlias)))
        val checkedRange = formDateRange(TableAlias, cCheckedAt, criteria.checkedAtStartingFrom, criteria.checkedAtUpTo) //using business user table for update range
        val createdBy = criteria.createdBy.map(queryConditionClause(_, cCreatedBy, Some(TableAlias)))
        val createdRange = formDateRange(TableAlias, cCreatedAt, criteria.createdAtStartingFrom, criteria.createdAtUpTo)

        val filters = Seq(
          customerId, individualUserMsisdn, individualUserFullName, documentType, documentIdentifier, status, applicationId,
          businessUserApplicationId, checkedBy, checkedRange, createdBy, createdRange).flatten.mkString(" AND ")

        if (filters.nonEmpty) s"WHERE $filters"
        else ""
      case None ⇒ ""
    }

  }
}
