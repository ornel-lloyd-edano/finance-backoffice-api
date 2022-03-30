package tech.pegb.backoffice.api.swagger.model

import io.swagger.annotations.ApiModelProperty
import play.api.libs.json.JsObject
import tech.pegb.backoffice.api.makerchecker.dto.TaskToCreateI

case class TaskToCreate(
    @ApiModelProperty(name = "verb", example = "POST", required = true) verb: String,
    @ApiModelProperty(name = "url", example = "/api/manual_transactions", required = true) url: String,
    @ApiModelProperty(name = "body", example = "{}", required = false) body: Option[JsObject],
    @ApiModelProperty(name = "headers", example = "{}", required = true) headers: JsObject,
    @ApiModelProperty(name = "module", example = "transaction", required = true) module: String,
    @ApiModelProperty(name = "action", example = "Create manual transaction", required = true) action: String) extends TaskToCreateI {

}
