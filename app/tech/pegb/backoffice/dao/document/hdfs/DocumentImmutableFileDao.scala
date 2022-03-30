package tech.pegb.backoffice.dao.document.hdfs

import java.util.UUID

import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import tech.pegb.backoffice.dao.Dao.DaoResponse
import tech.pegb.backoffice.dao.DaoError
import tech.pegb.backoffice.dao.document.abstraction
import tech.pegb.backoffice.dao.hdfs.HdfsApi

import scala.collection.mutable.ArrayBuffer
import scala.util.Try

@Singleton
class DocumentImmutableFileDao @Inject() (
    hdfsProvider: HdfsApi, config: Configuration) extends abstraction.DocumentImmutableFileDao {

  val basePathForDocuments = config.get[String]("hdfs.document-path")

  private def getCompletePath(id: UUID, path: Option[String]): String = path.map(p ⇒ s"$basePathForDocuments/$p/$id").getOrElse(s"$basePathForDocuments/$id")

  def readDocumentFile(id: UUID, subpath: Option[String]): DaoResponse[Option[Array[Byte]]] = {
    hdfsProvider.getInputStream(getCompletePath(id, subpath))
      .right.map(inputStream ⇒ {
        val buffer = new Array[Byte](1024)
        val resultBuffer = new ArrayBuffer[Byte]()
        Stream.continually(inputStream.read(buffer))
          .takeWhile(_ != -1).foreach(_ ⇒ resultBuffer.append(buffer: _*))

        inputStream.close()

        Option(resultBuffer.toArray)
      }).left.map(e ⇒ {
        logger.error("error encountered in [readDocumentFile]", e)
        DaoError.EntityNotFoundError(s"Document $id not found. Subpath = $subpath")
      })
  }

  def writeDocumentFile(id: UUID, contents: Array[Byte], subpath: Option[String]): DaoResponse[UUID] = {
    hdfsProvider.getOutputStream(getCompletePath(id, subpath))
      .fold(
        error ⇒ {
          logger.error("error encountered in [writeDocumentFile]", error)
          Left(DaoError.GenericDbError(s"Unable to get hdfs output stream"))
        },
        outputStream ⇒ {
          Try(outputStream.write(contents))
            .fold(
              error ⇒ {
                logger.error("error encountered in [writeDocumentFile]", error)
                Left(DaoError.GenericDbError(s"Unable to write to hdfs"))
              },
              _ ⇒ {
                outputStream.close()
                Right(id)
              })
        })
  }

  def deleteDocumentFile(id: UUID): DaoResponse[Unit] = {
    Try(hdfsProvider.delete(getCompletePath(id, None))).fold(
      error ⇒ {
        logger.error("error encountered in [deleteDocumentFile]", error)
        Left(DaoError.GenericDbError(s"Error encountere while deleting document $id"))
      },
      _ ⇒ Right(()))
  }

}
