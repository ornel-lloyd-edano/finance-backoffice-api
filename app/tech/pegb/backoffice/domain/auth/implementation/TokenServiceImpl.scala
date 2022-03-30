package tech.pegb.backoffice.domain.auth.implementation

import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, PrivateKey, PublicKey, Security}
import java.time.Instant

import org.bouncycastle.jce.provider.BouncyCastleProvider
import pdi.jwt._
import play.api.libs.json.{Json, Writes}
import tech.pegb.backoffice.domain.auth.abstraction.TokenService
import tech.pegb.backoffice.domain.auth.dto.TokenExpiration

import scala.concurrent.duration._

class TokenServiceImpl extends TokenService {
  import TokenServiceImpl.{Issuer, epochSecondsNow}

  def generateToken[T](user: String, payload: T)(implicit writes: Writes[T], tokenExpiration: TokenExpiration): String = {
    generateTokenInternal(JwtJson)(user, Json.toJson(payload).toString())
  }

  private def generateTokenInternal[T](
    jwtCore: JwtCore[JwtHeader, JwtClaim])(
    user: String,
    payload: String)(implicit tokenExpiration: TokenExpiration): String = {
    val header = JwtHeader(TokenServiceImpl.Algo, typ = "JWT")
    val now = epochSecondsNow
    val expiration = now + tokenExpiration.expirationInMinutes.minutes.toSeconds
    val claim = JwtClaim(
      content = payload,
      issuer = Some(Issuer),
      audience = Some(Set(user)),
      issuedAt = Some(now),
      expiration = Some(expiration))
    jwtCore.encode(header, claim, TokenServiceImpl.AuthPrivateKey)
  }
}

// uses implementation details from JwtUtils
object TokenServiceImpl {
  // disable coverage to work around https://github.com/scoverage/scalac-scoverage-plugin/issues/125
  // $COVERAGE-OFF$
  private final val Provider = "BC"
  private final val AlgoString = "ECDSA"
  private final val Issuer = "PEGB"
  // $COVERAGE-ON$
  private val Algo = JwtAlgorithm.ES256

  if (Security.getProvider(Provider) == null) {
    Security.addProvider(new BouncyCastleProvider())
  }

  lazy final val AuthPublicKey: PublicKey = {
    val key = scala.io.Source.fromResource("auth.key.pub").mkString
    val spec = new X509EncodedKeySpec(parseKey(key))
    KeyFactory.getInstance(AlgoString, Provider).generatePublic(spec)
  }

  def epochSecondsNow: Long = Instant.now().getEpochSecond

  private lazy final val AuthPrivateKey: PrivateKey = {
    val key = scala.io.Source.fromResource("auth.key").mkString
    val spec = new PKCS8EncodedKeySpec(parseKey(key))
    KeyFactory.getInstance(AlgoString, Provider).generatePrivate(spec)
  }

  private def parseKey(key: String): Array[Byte] = JwtBase64.decodeNonSafe(
    key.replaceAll("-----BEGIN (.*)-----", "")
      .replaceAll("-----END (.*)-----", "")
      .replaceAll("\r\n", "")
      .replaceAll("\n", "")
      .trim)
}
