package tech.pegb.backoffice.domain.mock

import javax.inject.Inject
import net.sf.ehcache.Element
import play.api.cache.SyncCacheApi

import scala.collection.mutable
import scala.concurrent.duration.Duration
import scala.reflect.ClassTag

class InMemorySyncCacheApi @Inject() extends SyncCacheApi {

  private val cache: mutable.Map[String, Element] = mutable.Map[String, Element]()
  override def set(key: String, value: Any, expiration: Duration): Unit = {
    val element = new Element(key, value)
    if (expiration.isFinite()) {
      element.setTimeToLive(expiration.toSeconds.toInt)
    } else {
      element.setEternal(true)
    }
    cache.put(key, element)
  }

  override def getOrElseUpdate[A](key: String, expiration: Duration)(orElse: ⇒ A)(implicit evidence$1: ClassTag[A]): A = {
    get[A](key).fold {
      val value = orElse
      set(key, value, expiration)
      value
    }(identity)
  }

  override def get[T](key: String)(implicit evidence$2: ClassTag[T]): Option[T] = {
    cache.get(key).flatMap(entry ⇒ Option(entry.getObjectValue.asInstanceOf[T]))
  }

  override def remove(key: String): Unit = cache -= key
}
