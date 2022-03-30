package tech.pegb.backoffice.dao.document.couchbase

import java.lang.{Boolean ⇒ JBoolean}
import java.time.Instant
import java.util.UUID

import com.couchbase.client.java.document.ByteArrayDocument
import com.couchbase.client.java.error.DocumentDoesNotExistException

import com.google.inject._
import play.api.Configuration
import rx.Observable
import tech.pegb.backoffice.dao.Dao.DaoResponse
import tech.pegb.backoffice.dao.couchbase.CouchbaseApi
import tech.pegb.backoffice.dao.document.abstraction

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration.Duration

@Singleton
class DocumentTransientFileDao @Inject() (
    configuration: Configuration,
    cbApi: CouchbaseApi)
  extends abstraction.DocumentTransientFileDao {

  private val docsConfig = configuration.get[Configuration]("couchbase.docs")
  private val filesBucketName = docsConfig.get[String]("buckets.files.name")
  private lazy val bucket = cbApi.openBucket(filesBucketName, docsConfig.get[String]("buckets.files.password"))

  def readDocumentFile(id: UUID, extendExpiration: Option[Duration]): Future[DaoResponse[Option[Array[Byte]]]] = {
    val strId = id.toString
    val observable =
      bucket.exists(strId)
        .flatMap((flag: JBoolean) ⇒ {
          if (flag) {
            extendExpiration.fold { //TODO: Pls modify behavior... should only extend if date now is 3 days (or less) away from expiration date
              bucket.get(strId, classOf[ByteArrayDocument])
            } { expiryOffset ⇒
              val expiry = durationToExpiry(expiryOffset)
              bucket.getAndTouch(strId, expiry.toInt, classOf[ByteArrayDocument])
            }.map[DaoResponse[Option[Array[Byte]]]]((doc: ByteArrayDocument) ⇒ Right(Option(doc.content())))
          } else {
            Observable.just[DaoResponse[Option[Array[Byte]]]](Right(None))
          }
        })
    val promise = Promise[DaoResponse[Option[Array[Byte]]]]
    observable.subscribe(
      (bytesResponse: DaoResponse[Option[Array[Byte]]]) ⇒ {
        promise.success(bytesResponse)
        ()
      },
      (exc: Throwable) ⇒ {
        promise.success(Left(genericDbError(s"Couchbase read failed. id = $id")))
        logger.warn("Couchbase failed", exc)
      })
    promise.future
  }

  def writeDocumentFile(id: UUID, contents: Array[Byte], autoExpiration: Option[Duration]): Future[DaoResponse[UUID]] = {
    val idStr = id.toString
    val doc = autoExpiration.fold {
      ByteArrayDocument.create(idStr, contents)
    } { expiryOffset ⇒
      val expiry = durationToExpiry(expiryOffset)
      ByteArrayDocument.create(idStr, expiry.toInt, contents)
    }
    val promise = Promise[DaoResponse[UUID]]
    bucket.insert(doc)
      .subscribe(
        (saved: ByteArrayDocument) ⇒ {
          promise.success(Right(UUID.fromString(saved.id())))
          ()
        },
        (exc: Throwable) ⇒ {
          promise.success(Left(genericDbError(s"Couchbase save failed. id = $id")))
          logger.warn("Failed to save doc in couchbase", exc)
        })
    promise.future
  }

  def removeDocumentFile(id: UUID): Future[DaoResponse[Boolean]] = {
    val idStr = id.toString
    val promise = Promise[DaoResponse[Boolean]]
    bucket.remove(idStr, classOf[ByteArrayDocument]).subscribe(
      (_: ByteArrayDocument) ⇒ {
        promise.success(Right(true))
        ()
      },
      (exc: Throwable) ⇒ {
        exc match {
          case _: DocumentDoesNotExistException ⇒
            promise.success(Right(false))
          case _ ⇒
            promise.success(Left(genericDbError(s"Couchbase delete failed. id = $id")))
        }
        logger.warn("Couldn't remove doc from couchbase", exc)
      })
    promise.future
  }

  /*
   * Need to be casted to Int because at the moment it accepts integers, not longs
   */
  protected def durationToExpiry(duration: Duration): Long = {
    Instant.now().plusSeconds(duration.toSeconds).getEpochSecond
  }
}
