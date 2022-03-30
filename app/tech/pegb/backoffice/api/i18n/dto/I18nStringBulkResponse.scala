package tech.pegb.backoffice.api.i18n.dto

import io.swagger.annotations.ApiModelProperty

case class I18nStringBulkResponse(
    @ApiModelProperty(name = "inserted_count", example = "100", required = true) insertedCount: Int,
    @ApiModelProperty(name = "updated_count", example = "100", required = true) updatedCount: Int)
