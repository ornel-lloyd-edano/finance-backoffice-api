package tech.pegb.backoffice.api.auth.controllers

import com.google.inject.ImplementedBy
import io.swagger.annotations._
import play.api.libs.json.JsValue
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.auth.dto.LoginResponse
import tech.pegb.backoffice.domain.auth.model.LoginStatusResponse

@ImplementedBy(classOf[impl.AuthenticationController])
@Api(value = "Authentication", produces = "application/json", consumes = "application/json")
trait AuthenticationController {
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[LoginResponse], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.auth.dto.CredentialsToRead",
      example = "",
      paramType = "body",
      name = "CredentialsToRead")))
  def login: Action[JsValue]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[LoginResponse], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.auth.dto.CredentialsToUpdate",
      example = "",
      paramType = "body",
      name = "CredentialsToUpdate")))
  def updatePassword: Action[JsValue]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.auth.dto.ResetPasswordLinkRequest",
      example = "",
      paramType = "body",
      name = "CredentialsToRead")))
  def resetPassword: Action[JsValue]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, message = "")))
  def validateResetPasswordByToken(token: String): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[LoginResponse], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.auth.dto.PasswordReset",
      example = "",
      paramType = "body",
      name = "CredentialsToRead")))
  def updateResetPassword: Action[JsValue]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[LoginStatusResponse], message = "")))
  def getStatus: Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[LoginResponse], message = "")))
  def validateToken: Action[AnyContent]
}
