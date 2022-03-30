package tech.pegb.backoffice.dao.couchbase

import com.couchbase.client.java.AsyncBucket
import com.google.inject.ImplementedBy

@ImplementedBy(classOf[CouchbaseClusterProvider])
trait CouchbaseApi {
  def openBucket(bucketName: String): AsyncBucket

  def openBucket(bucketName: String, bucketPassword: String): AsyncBucket
}
