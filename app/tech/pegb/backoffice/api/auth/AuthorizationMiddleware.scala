package tech.pegb.backoffice.api.auth

import java.util.UUID

import com.google.inject.Singleton
import play.api.http.HttpVerbs
import tech.pegb.backoffice.api.ApiError
import tech.pegb.backoffice.api.proxy.model.Scope
import tech.pegb.backoffice.domain.auth.model.BackOfficeUser

@Singleton
class AuthorizationMiddleware {

  import tech.pegb.backoffice.api.ApiController._

  private def checkPermissions(scopeTypes: Scope.Type*)(user: BackOfficeUser)(implicit scopes: Scope, id: UUID): Either[ApiError, Unit] = {
    val required = scopeTypes.map(scopes.byType).toSet + scopes.parent
    if (user.permissions.exists(p ⇒ required.contains(p.scope.name))) {
      AuthorizationMiddleware.success
    } else {
      Left(("Required any of " + required.mkString("{", ", ", "}")).asNotAuthorizedApiError)
    }
  }

  def checkPermissions(bu: BackOfficeUser, method: String)(implicit scope: Scope, id: UUID): Either[ApiError, Unit] = {
    method match {
      case HttpVerbs.POST ⇒ checkPermissions(Scope.Create)(bu)
      case HttpVerbs.GET | HttpVerbs.HEAD ⇒ checkPermissions(Scope.Detail)(bu)
      case HttpVerbs.PUT | HttpVerbs.PATCH ⇒ checkPermissions(Scope.Update)(bu)
      case HttpVerbs.DELETE ⇒ checkPermissions(Scope.Delete)(bu)
      case HttpVerbs.OPTIONS ⇒ AuthorizationMiddleware.success
      case _ ⇒ Left("HTTP Method not supported".asNotAuthorizedApiError)
    }
  }

}

object AuthorizationMiddleware {

  private val success = Right(())

}
