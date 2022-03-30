package tech.pegb.backoffice.dao

import java.lang.{Boolean ⇒ JBoolean}
import java.util.UUID

import com.couchbase.client.java.AsyncBucket
import com.couchbase.client.java.document.ByteArrayDocument
import com.couchbase.client.java.error.DocumentDoesNotExistException
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import play.api.inject.{Binding, bind}
import rx.Observable
import tech.pegb.backoffice.dao.couchbase.CouchbaseApi
import tech.pegb.backoffice.dao.document.couchbase.DocumentTransientFileDao
import tech.pegb.core.PegBNoDbTestApp

class DocumentCouchbaseDaoSpec extends PegBNoDbTestApp with MockFactory with ScalaFutures {

  private val validDocId = UUID.randomUUID()
  private val validDocStrId = validDocId.toString
  private val invalidDocId = UUID.randomUUID()
  private val invalidDocStrId = invalidDocId.toString
  private val validDocData: Array[Byte] = Seq[Byte](1, 10, 100, 127).toArray
  private val couchbaseDoc = ByteArrayDocument.create(validDocStrId, validDocData)

  override def additionalBindings: Seq[Binding[_]] = {
    val bucket = stub[AsyncBucket]
    (bucket.exists(_: String)).when(validDocStrId).returns(Observable.just[JBoolean](true))
    (bucket.exists(_: String)).when(invalidDocStrId).returns(Observable.just[JBoolean](false))
    (bucket.get[ByteArrayDocument](_: String, _: Class[ByteArrayDocument]))
      .when(validDocStrId, classOf[ByteArrayDocument])
      .returns(Observable.just[ByteArrayDocument](couchbaseDoc))
    (bucket.insert(_: ByteArrayDocument)).when(couchbaseDoc).returns(Observable.just(couchbaseDoc))
    (bucket.remove(_: String, _: Class[ByteArrayDocument]))
      .when(validDocStrId, classOf[ByteArrayDocument])
      .returns(Observable.just(couchbaseDoc))
    (bucket.remove(_: String, _: Class[ByteArrayDocument]))
      .when(invalidDocStrId, classOf[ByteArrayDocument])
      .returns(Observable.error(new DocumentDoesNotExistException()))
    val couchbaseApi = new CouchbaseApi {
      override def openBucket(bucketName: String): AsyncBucket = bucket
      override def openBucket(bucketName: String, bucketPassword: String): AsyncBucket = bucket
    }
    super.additionalBindings :+ bind[CouchbaseApi].toInstance(couchbaseApi)
  }

  "Document dao" should {

    "find existing document, insert file, remove file" in {
      val dao = inject[DocumentTransientFileDao]
      whenReady(dao.readDocumentFile(validDocId, None)) { resp ⇒
        resp mustBe Right(Option(validDocData))
      }
      whenReady(dao.readDocumentFile(invalidDocId, None)) { resp ⇒
        resp mustBe Right(None)
      }
      whenReady(dao.writeDocumentFile(validDocId, validDocData, None)) { resp ⇒
        resp mustBe Right(validDocId)
      }
      whenReady(dao.removeDocumentFile(validDocId)) { resp ⇒
        resp mustBe Right(true)
      }
      whenReady(dao.removeDocumentFile(invalidDocId)) { resp ⇒
        resp mustBe Right(false)
      }
    }
  }

}
