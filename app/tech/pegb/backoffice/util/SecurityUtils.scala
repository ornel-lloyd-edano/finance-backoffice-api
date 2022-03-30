package tech.pegb.backoffice.util

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {

  val SHA256 = "HmacSHA256"

  // copied from JwtUtils, so might make sense in future if we ever move from it
  /*if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
    Security.addProvider(new BouncyCastleProvider())
  }*/

  def hmacSha256(sharedSecret: String, preHashString: String): Array[Byte] = {
    val secret = new SecretKeySpec(sharedSecret.getBytes, SHA256)
    val mac = Mac.getInstance(SHA256)
    mac.init(secret)
    mac.doFinal(preHashString.getBytes)
  }

}
