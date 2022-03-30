package tech.pegb.backoffice.core.integration.dto

import java.util.UUID

import play.api.libs.json.{Json, JsonConfiguration}
import play.api.libs.json.JsonNaming.SnakeCase

case class BusinessUserCreateResponse(
    id: Int,
    uuid: UUID,
    userId: Int,
    businessName: String,
    brandName: String,
    businessCategory: String,
    businessType: String,
    registrationNumber: String,
    taxNumber: Option[String],
    registrationDate: Option[String],
    createdBy: String,
    createdAt: String,
    updatedBy: Option[String],
    updatedAt: Option[String])

object BusinessUserCreateResponse {
  implicit val config = JsonConfiguration(SnakeCase)

  implicit val businessUserCreateResponseFormat = Json.format[BusinessUserCreateResponse]
}
