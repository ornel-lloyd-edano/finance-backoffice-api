package tech.pegb.backoffice.api.parameter.dto

import com.fasterxml.jackson.databind.JsonNode

case class MetadataToRead(metadataId: String, schema: JsonNode, readOnlyFields: Seq[String], isCreationAllowed: Boolean, isDeletionAllowed: Boolean, isArray: Boolean) {

}
