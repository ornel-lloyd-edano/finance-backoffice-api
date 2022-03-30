package tech.pegb.backoffice.dao.hdfs

import com.google.inject.ImplementedBy
import org.apache.hadoop.fs.{FSDataInputStream, FSDataOutputStream}

@ImplementedBy(classOf[HdfsProvider])
trait HdfsApi {

  def getOutputStream(destinationPath: String): Either[Throwable, FSDataOutputStream]

  def getInputStream(sourcePath: String): Either[Throwable, FSDataInputStream]

  //used for unit test
  def delete(path: String): Unit
}
