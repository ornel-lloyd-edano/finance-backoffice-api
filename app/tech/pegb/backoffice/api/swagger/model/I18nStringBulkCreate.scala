package tech.pegb.backoffice.api.swagger.model

import io.swagger.annotations.ApiModelProperty

case class I18nStringBulkCreate(
    @ApiModelProperty(name = "locale", required = true, example = "en") locale: String,
    strings: Array[I18nStringToCreate])

