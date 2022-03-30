package tech.pegb.backoffice.dao

import org.scalamock.scalatest.MockFactory
import tech.pegb.core.PegBNoDbTestApp

class DocumentHdfsDaoSpec extends PegBNoDbTestApp with MockFactory {

  "hdfs.DocumentImmutableFileDao readDocumentFile" should {
    "return Right[Option[String]] content of hdfs file if file equal to given document id was found on given path" in {
      false
    }
    "return Right(None) if filename equal to given document id was not found on given path" in {
      false
    }
    "return Left[ConnectionError] if cannot connect to hdfs" in {
      false
    }
  }

  "hdfs.writeDocumentFile writeDocumentFile" should {
    "return Right[UUID] if successfully written to hdfs on the given path" in {
      false
    }
    "return Left[NotAllowed] if filename equal to given document id already exists in the given path" in {
      false
    }

    "return Left[ConnectionError] if cannot connect to hdfs" in {
      false
    }
  }

}
