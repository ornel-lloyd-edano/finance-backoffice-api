package tech.pegb.backoffice.util

import java.util.UUID

import Implicits._

import scala.util.Try

case class UUIDLike(underlying: String) {
  assert(underlying.hasSomething, "UUID is empty")
  assert(
    underlying.matches(
      """[A-Za-z0-9]{4,8}[-]?[A-Za-z0-9]{0,4}[-]?[A-Za-z0-9]{0,4}[-]?[A-Za-z0-9]{0,4}[-]?[A-Za-z0-9]{0,12}"""),
    "invalid partial UUID")

  override def toString: String = underlying

  def toUUID: Try[UUID] = {
    val srcArray = underlying.toCharArray
    val destArray = UUIDLike.empty.toString.toCharArray

    val completeUUID = destArray.zipAll(srcArray, ' ', ' ')
      .foldLeft(Seq.empty[Char]) {
        (accumulated, current) ⇒
          {
            accumulated :+ (if (current._2 != ' ') current._2 else current._1)
          }
      }.mkString

    Try(UUID.fromString(completeUUID))
  }

  override def equals(obj: Any): Boolean = {
    obj match {
      case obj: UUIDLike ⇒ obj.underlying == this.underlying
      case _ ⇒ false
    }
  }
}

object UUIDLike {
  val empty = UUID.fromString("00000000-0000-0000-0000-000000000000")
}
