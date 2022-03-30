package tech.pegb.backoffice.util

import java.nio.ByteBuffer
import java.util.UUID

import tech.pegb.backoffice.util.UUIDRepresentableId._

import scala.util.Try

case class UUIDRepresentableId(prefix: Int, suffix: Long) {

  val maxPrefix = 9999
  val maxSuffix = 99999999999L

  assert(prefix >= 0 && prefix <= maxPrefix, s"prefix must be positive number and cannot be larger than $maxPrefix for UUIDRepresentableId")
  assert(suffix >= 0 && suffix <= maxSuffix, s"suffix must be positive number and cannot be larger than $maxSuffix for UUIDRepresentableId")

  override def toString = {
    //"0001:00000000001"
    "%04d".format(prefix) + separatorChar + "%011d".format(suffix)
  }

  def toUUID = {
    getUUIDFromBytes(this.toString.getBytes)
  }

}

object UUIDRepresentableId {
  private val separatorChar = ":"

  private def getUUIDFromBytes(bytes: Array[Byte]): UUID = {
    val byteBuffer = ByteBuffer.wrap(bytes).asLongBuffer()
    val high = byteBuffer.get()
    val low = byteBuffer.get()
    new UUID(high, low)
  }

  private def getBytesFromUUID(uuid: UUID): Array[Byte] = {
    val bb = ByteBuffer.wrap(new Array[Byte](16))
    bb.putLong(uuid.getMostSignificantBits)
    bb.putLong(uuid.getLeastSignificantBits)
    bb.array
  }

  implicit class UUIDToIdAdapter(val arg: UUID) extends AnyVal {

    //
    def toPrefix: Try[Int] = {
      Try(new String(getBytesFromUUID(arg)).split(separatorChar).head.toInt)
    }

    def toSuffix: Try[Long] = {
      Try(new String(getBytesFromUUID(arg)).split(separatorChar).tail.head.toLong)
    }

  }

}
