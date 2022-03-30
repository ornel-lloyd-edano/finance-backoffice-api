package tech.pegb.backoffice.dao.types.entity

import java.time.LocalDateTime

case class DescriptionType(
    id: Int,
    `type`: String,
    createdAt: LocalDateTime,
    createdBy: String,
    updatedAt: Option[LocalDateTime],
    updatedBy: Option[String]) {
  override def equals(obj: scala.Any): Boolean = {
    val other = obj.asInstanceOf[DescriptionType]
    `type` == other.`type`
  }
}

case class Description(
    id: Int,
    name: String,
    description: Option[String]) {
  override def equals(obj: scala.Any): Boolean = {
    val other = obj.asInstanceOf[Description]
    name == other.name
  }
}

case class DescriptionTypeToCreate(
    `type`: String,
    createdAt: LocalDateTime,
    createdBy: String)

case class DescriptionToUpsert(
    id: Option[Int],
    name: String,
    description: Option[String])

