package tech.pegb.backoffice.application

import java.util.UUID

import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.bind
import tech.pegb.backoffice.application.datatransfer.CouchbaseToHdfsDocMover
import tech.pegb.backoffice.dao.document.abstraction.{DocumentImmutableFileDao, DocumentTransientFileDao}
import tech.pegb.backoffice.util.WithExecutionContexts
import tech.pegb.core.{PegBNoDbTestApp, TestExecutionContext}

import scala.concurrent.Future

class CouchbaseToHdfsDocMoverSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  val documentTransientFileDao = stub[DocumentTransientFileDao]
  val documentImmutableFileDao = stub[DocumentImmutableFileDao]

  override def additionalBindings = super.additionalBindings ++
    Seq(
      bind[DocumentImmutableFileDao].to(documentImmutableFileDao),
      bind[DocumentTransientFileDao].to(documentTransientFileDao),
      bind[WithExecutionContexts].to(TestExecutionContext))

  "CouchbaseToHdfsDocMoverSpec" should {
    val couchbaseToHdfsDocMover = inject[CouchbaseToHdfsDocMover]
    "store a Couchbase document to HDFS" in {
      val documentUUID1 = UUID.randomUUID()
      val documentUUID2 = UUID.randomUUID()
      val documentUUID3 = UUID.randomUUID()
      val documentUUID4 = UUID.randomUUID()

      //read couchbase
      val mockCouchbaseDocByte = Array(1.toByte, 2.toByte)
      val couchbaseDocExpiration = couchbaseToHdfsDocMover.defaultTransientDocExpiration

      (documentTransientFileDao.readDocumentFile _).when(documentUUID1, Some(couchbaseDocExpiration)).returns(Future.successful(Right(Some(mockCouchbaseDocByte))))
      (documentTransientFileDao.readDocumentFile _).when(documentUUID2, Some(couchbaseDocExpiration)).returns(Future.successful(Right(Some(mockCouchbaseDocByte))))
      (documentTransientFileDao.readDocumentFile _).when(documentUUID3, Some(couchbaseDocExpiration)).returns(Future.successful(Right(Some(mockCouchbaseDocByte))))
      (documentTransientFileDao.readDocumentFile _).when(documentUUID4, Some(couchbaseDocExpiration)).returns(Future.successful(Right(Some(mockCouchbaseDocByte))))

      //write hdfs
      (documentImmutableFileDao.writeDocumentFile _).when(documentUUID1, mockCouchbaseDocByte, None).returns(Right(documentUUID1))
      (documentImmutableFileDao.writeDocumentFile _).when(documentUUID2, mockCouchbaseDocByte, None).returns(Right(documentUUID2))
      (documentImmutableFileDao.writeDocumentFile _).when(documentUUID3, mockCouchbaseDocByte, None).returns(Right(documentUUID3))
      (documentImmutableFileDao.writeDocumentFile _).when(documentUUID4, mockCouchbaseDocByte, None).returns(Right(documentUUID4))

      val expected = Seq(documentUUID1, documentUUID2, documentUUID3, documentUUID4)
      val result = couchbaseToHdfsDocMover.persistTransientDocument(documentUUID1, documentUUID2, documentUUID3, documentUUID4)

      whenReady(result) { updatedWalletApplication ⇒
        updatedWalletApplication.isRight mustBe true
        updatedWalletApplication.right.get mustBe expected
      }
    }
  }

}
