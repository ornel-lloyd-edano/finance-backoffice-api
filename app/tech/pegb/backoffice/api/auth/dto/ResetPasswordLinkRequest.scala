package tech.pegb.backoffice.api.auth.dto

import io.swagger.annotations.ApiModelProperty

//todo move Email to common source
case class ResetPasswordLinkRequest(
    @ApiModelProperty(name = "user_name", required = true, example = "david.salgado") userName: String,
    @ApiModelProperty(name = "email", required = true, example = "d.salgado@pegb.tech") email: String,
    captcha: Option[String])
