package tech.pegb.backoffice.domain.auth.abstraction

import com.google.inject.ImplementedBy
import play.api.libs.json.Writes
import tech.pegb.backoffice.domain.auth.dto.TokenExpiration
import tech.pegb.backoffice.domain.auth.implementation.TokenServiceImpl

@ImplementedBy(classOf[TokenServiceImpl])
trait TokenService {
  def generateToken[T](user: String, payload: T)(implicit writes: Writes[T], tokenExpiration: TokenExpiration): String
}

