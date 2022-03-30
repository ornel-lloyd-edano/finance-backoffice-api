package tech.pegb.backoffice.dao.hdfs

import com.google.inject.Inject
import org.apache.hadoop.fs.{FSDataInputStream, FSDataOutputStream}
import play.api.inject.ApplicationLifecycle

//TODO just to unblock OCP preprod env
class MockHdfsProvider @Inject() (config: play.api.Configuration, lifecycle: ApplicationLifecycle) extends HdfsApi {

  def getOutputStream(destinationPath: String): Either[Throwable, FSDataOutputStream] = {
    Left(new Exception("mock implemetation only"))
  }

  def getInputStream(sourcePath: String): Either[Throwable, FSDataInputStream] = {
    Left(new Exception("mock implemetation only"))
  }

  def delete(path: String): Unit = {
    ()
  }

}
