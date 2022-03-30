package tech.pegb.backoffice.dao.hdfs

import com.google.inject.Inject
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.{FSDataInputStream, FSDataOutputStream, FileSystem, Path}
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future

class HdfsProvider @Inject() (config: play.api.Configuration, lifecycle: ApplicationLifecycle) extends HdfsApi {

  private val hadoopConfig: Configuration = {
    val hdfsConfiguration = new Configuration()
    hdfsConfiguration.set("fs.defaultFS", config.get[String]("hdfs.uri"))
    hdfsConfiguration.set("fs.hdfs.impl", classOf[org.apache.hadoop.hdfs.DistributedFileSystem].getName)
    hdfsConfiguration.set("fs.file.impl", classOf[org.apache.hadoop.fs.LocalFileSystem].getName)
    hdfsConfiguration.set("dfs.replication", config.get[String]("hdfs.replication"))
    hdfsConfiguration
  }

  private val fs = {
    val fs = FileSystem.get(hadoopConfig)
    lifecycle.addStopHook(() ⇒ Future.successful(fs.close()))
    fs
  }

  private def getPath(arg: String): Either[Throwable, Path] = {
    val path = new Path(arg)
    if (fs.exists(path)) {
      Right(path)
    } else {
      Left(new Exception(s"Path [$arg] does not exist"))
    }
  }

  def getOutputStream(destinationPath: String): Either[Throwable, FSDataOutputStream] = {
    getPath(destinationPath).fold(
      doesNotExist ⇒ {
        Right(fs.create(new Path(destinationPath)))
      },
      createdPath ⇒ Left(new Exception(s"Unable to create [$destinationPath] because one already exists.")))
  }

  def getInputStream(sourcePath: String): Either[Throwable, FSDataInputStream] = {
    getPath(sourcePath).right.map(fs.open(_))
  }

  def delete(path: String): Unit = {
    getPath(path) match {
      case Left(_) ⇒ ()
      case Right(p) ⇒
        fs.delete(p, true)
        ()
    }
  }

}
