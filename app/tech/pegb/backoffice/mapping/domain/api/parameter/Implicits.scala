package tech.pegb.backoffice.mapping.domain.api.parameter

import tech.pegb.backoffice.api.json.Implicits._
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.api.parameter.dto.{MetadataToRead, ParameterToRead}
import tech.pegb.backoffice.domain.parameter.model.{MetadataSchema, Parameter}

object Implicits {

  implicit class ParameterDomainToApiAdapter(val arg: Parameter) extends AnyVal {
    def asApi: ParameterToRead = {
      ParameterToRead(
        id = arg.id,
        key = arg.key,
        value = arg.value.toString.asJsNode,
        platforms = arg.platforms.map(_.toString),
        metadataId = arg.metadataId,
        explanation = arg.explanation,
        createdAt = arg.createdAt.map(_.toZonedDateTimeUTC),
        createdBy = arg.createdBy,
        updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC),
        updatedBy = arg.updatedBy)
    }
  }

  implicit class MetadataSchemaToApiAdapter(val arg: (String, MetadataSchema)) extends AnyVal {
    def asApi = {
      MetadataToRead(
        metadataId = arg._1,
        schema = arg._2.schema.toString().asJsNode,
        readOnlyFields = arg._2.readOnlyFields,
        isCreationAllowed = arg._2.isCreationAllowed,
        isDeletionAllowed = arg._2.isDeletionAllowed,
        isArray = arg._2.isArray)
    }
  }

  implicit class MetadataSchemaByIdToApiAdapter(val arg: MetadataSchema) extends AnyVal {
    def asApi = {
      MetadataToRead(
        metadataId = arg.metadataId,
        schema = arg.schema.toString().asJsNode,
        readOnlyFields = arg.readOnlyFields,
        isCreationAllowed = arg.isCreationAllowed,
        isDeletionAllowed = arg.isDeletionAllowed,
        isArray = arg.isArray)
    }
  }
}
