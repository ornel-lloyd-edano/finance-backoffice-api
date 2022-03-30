package tech.pegb.backoffice.api.document.dto

import java.util.UUID

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

case class DocumentFileToRead(
    @JsonProperty(required = true)@ApiModelProperty(name = "id", required = true) id: UUID,
    @JsonProperty(required = true)@ApiModelProperty(name = "file_contents", required = true) fileContents: String)
