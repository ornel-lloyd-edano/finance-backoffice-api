package tech.pegb.backoffice.mapping.domain.dao.notification

import java.time.LocalDateTime
import java.util.UUID

import cats.implicits._
import tech.pegb.backoffice.dao.model.{CriteriaField, MatchTypes}
import tech.pegb.backoffice.dao.notification.{dto ⇒ dao}
import tech.pegb.backoffice.domain.notification.dto._
import tech.pegb.backoffice.util.Implicits._

object Implicits {

  implicit class NotificationTemplateToCreateDaoAdapter(val arg: NotificationTemplateToCreate) extends AnyVal {
    def asDao = dao.NotificationTemplateToInsert(
      createdAt = arg.createdAt,
      createdBy = arg.createdBy,
      name = arg.name,
      titleResource = arg.titleResource,
      defaultTitle = arg.defaultTitle,
      contentResource = arg.contentResource,
      defaultContent = arg.defaultContent,
      channels = s"[${arg.channels.mkString(",")}]",
      description = arg.description,
      isActive = true)
  }

  implicit class NotificationTemplateUpdateDaoAdapter(val arg: NotificationTemplateToUpdate) extends AnyVal {
    def asDao = dao.NotificationTemplateToUpdate(
      updatedAt = arg.updatedAt,
      updatedBy = arg.updatedBy,
      isActive = arg.isActive,

      name = arg.name,
      titleResource = arg.titleResource,
      defaultTitle = arg.defaultTitle,
      contentResource = arg.contentResource,
      defaultContent = arg.defaultContent,
      description = arg.description,
      channels = arg.channels.map(_.defaultMkString),

      lastUpdatedAt = arg.lastUpdatedAt)
  }

  //TODO replace the magic strings with NotificationTemplateSqlDao properties for column names
  implicit class NotificationTemplateCriteriaDaoAdapter(val arg: NotificationTemplateCriteria) extends AnyVal {
    def asDao = dao.NotificationTemplateCriteria(
      uuid = arg.id.map(id ⇒
        CriteriaField[String]("uuid", id.underlying,
          if (arg.partialMatchFields.contains("id")) MatchTypes.Partial else MatchTypes.Exact)),
      name = arg.name.map(name ⇒
        CriteriaField[String]("name", name,
          if (arg.partialMatchFields.contains("name")) MatchTypes.Partial else MatchTypes.Exact)),

      channels = arg.channel.map(channel ⇒
        CriteriaField[String]("channels", channel,
          if (arg.partialMatchFields.contains("channel")) MatchTypes.Partial else MatchTypes.Exact)),

      createdAt = (arg.createdAtFrom, arg.createdAtTo) match {
        case (Some(createdAtFrom), Some(createdAtTo)) ⇒
          CriteriaField[(LocalDateTime, LocalDateTime)]("created_at", (createdAtFrom, createdAtTo), MatchTypes.InclusiveBetween).toOption
        case (Some(createdAtFrom), None) ⇒
          CriteriaField[LocalDateTime]("created_at", createdAtFrom, MatchTypes.GreaterOrEqual).toOption
        case (None, Some(createdAtTo)) ⇒
          CriteriaField[LocalDateTime]("created_at", createdAtTo, MatchTypes.LesserOrEqual).toOption
        case _ ⇒ None
      },

      isActive = arg.isActive.map(isActive ⇒
        CriteriaField[Boolean]("is_active", isActive)))
  }

  //TODO replace the magic strings with NotificationTemplateSqlDao properties for column names
  implicit class NotificationTemplateCriteriaFromUUIDDaoAdapter(val arg: UUID) extends AnyVal {
    def asDao = dao.NotificationTemplateCriteria(uuid = CriteriaField("uuid", arg.toString).some)
  }

  //TODO replace the magic strings with NotificationTemplateSqlDao properties for column names
  implicit class NotificationCriteriaDaoAdapter(val arg: NotificationCriteria) extends AnyVal {
    def asDao() = dao.NotificationCriteria(
      uuid = arg.id.map(id ⇒ CriteriaField("uuid", id.underlying,
        if (arg.partialMatchFields.contains("id")) MatchTypes.Partial else MatchTypes.Exact)),

      templateUuid = arg.templateId.map(templateId ⇒
        CriteriaField[String]("template_id", templateId.underlying,
          if (arg.partialMatchFields.contains("template_id")) MatchTypes.Partial else MatchTypes.Exact)),

      operationId = arg.operationId.map(operationId ⇒
        CriteriaField[String]("operation_id", operationId,
          if (arg.partialMatchFields.contains("channel")) MatchTypes.Partial else MatchTypes.Exact)),

      channel = arg.channel.map(channel ⇒
        CriteriaField[String]("channel", channel,
          if (arg.partialMatchFields.contains("channel")) MatchTypes.Partial else MatchTypes.Exact)),

      title = arg.title.map(title ⇒
        CriteriaField[String]("title", title,
          if (arg.partialMatchFields.contains("title")) MatchTypes.Partial else MatchTypes.Exact)),

      content = arg.content.map(content ⇒
        CriteriaField[String]("content", content,
          if (arg.partialMatchFields.contains("content")) MatchTypes.Partial else MatchTypes.Exact)),

      address = arg.address.map(address ⇒
        CriteriaField[String]("address", address,
          if (arg.partialMatchFields.contains("address")) MatchTypes.Partial else MatchTypes.Exact)),

      userUuid = arg.userId.map(userId ⇒
        CriteriaField[String]("user_uuid", userId.underlying,
          if (arg.partialMatchFields.contains("user_id")) MatchTypes.Partial else MatchTypes.Exact)),

      status = arg.status.map(status ⇒ CriteriaField[String]("status", status)),

      createdAt = (arg.createdAtFrom, arg.createdAtTo) match {
        case (Some(createdAtFrom), Some(createdAtTo)) ⇒
          CriteriaField[(LocalDateTime, LocalDateTime)]("created_at", (createdAtFrom, createdAtTo), MatchTypes.InclusiveBetween).toOption
        case (Some(createdAtFrom), None) ⇒
          CriteriaField[LocalDateTime]("created_at", createdAtFrom, MatchTypes.GreaterOrEqual).toOption
        case (None, Some(createdAtTo)) ⇒
          CriteriaField[LocalDateTime]("created_at", createdAtTo, MatchTypes.LesserOrEqual).toOption
        case _ ⇒ None
      })

  }

}
