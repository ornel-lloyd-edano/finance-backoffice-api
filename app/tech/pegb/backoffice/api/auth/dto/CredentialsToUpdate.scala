package tech.pegb.backoffice.api.auth.dto

import io.swagger.annotations.ApiModelProperty

case class CredentialsToUpdate(
    user: String,
    @ApiModelProperty(name = "old_password", required = true, example = "P@ssw0rd123!") oldPassword: String,
    @ApiModelProperty(name = "new_password", required = true, example = "NewP@ssw0rd123!") newPassword: String)
