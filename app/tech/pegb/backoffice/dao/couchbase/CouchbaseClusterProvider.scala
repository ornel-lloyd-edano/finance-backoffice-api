package tech.pegb.backoffice.dao.couchbase

import com.couchbase.client.java.env.DefaultCouchbaseEnvironment
import com.couchbase.client.java.{AsyncBucket, CouchbaseCluster}
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.inject.ApplicationLifecycle

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

@Singleton
class CouchbaseClusterProvider @Inject() (
    configuration: Configuration,
    lifecycle: ApplicationLifecycle)
  extends CouchbaseApi {

  private val cbUser = configuration.get[String]("couchbase.user")
  private val cbPassword = configuration.get[String]("couchbase.password")
  private lazy val url = configuration.get[String]("couchbase.url")
  private lazy val timeout = configuration.get[FiniteDuration]("couchbase.timeout").toMillis
  private lazy val env = DefaultCouchbaseEnvironment.builder()
    .maxRequestLifetime(timeout * 2)
    .connectTimeout(timeout)
    .queryTimeout(timeout)
    .searchTimeout(timeout)
    .viewTimeout(timeout)
    .bootstrapHttpDirectPort(url.substring(url.lastIndexOf(':') + 1).toInt)
    .build()
  private lazy val cluster = {
    val baseCluster = CouchbaseCluster.fromConnectionString(env, url)
    val finalCluster = if (cbUser.isEmpty && cbPassword.isEmpty) {
      baseCluster
    } else {
      baseCluster.authenticate(cbUser, cbPassword)
    }
    lifecycle.addStopHook(() ⇒ Future.successful(finalCluster.disconnect()))
    finalCluster
  }

  override def openBucket(bucketName: String): AsyncBucket = {
    cluster.openBucket(bucketName).async()
  }

  override def openBucket(bucketName: String, bucketPassword: String): AsyncBucket = {
    cluster.openBucket(bucketName, bucketPassword).async()
  }
}
