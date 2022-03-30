package tech.pegb.backoffice.domain.parameter.model

import play.api.libs.json.JsObject

case class MetadataSchema(
    metadataId: String,
    schema: JsObject,
    readOnlyFields: Seq[String],
    isCreationAllowed: Boolean,
    isDeletionAllowed: Boolean,
    isArray: Boolean)
