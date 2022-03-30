package tech.pegb.backoffice.mapping.api.domain.i18n

import java.time.{LocalDateTime, ZonedDateTime}

import cats.implicits._
import tech.pegb.backoffice.api.i18n.dto.{I18nStringBulkCreate, I18nStringToCreate, I18nStringToUpdate}
import tech.pegb.backoffice.domain.i18n.dto
import tech.pegb.backoffice.domain.i18n.dto.I18nStringCriteria
import tech.pegb.backoffice.domain.i18n.model.I18nAttributes.{I18nKey, I18nLocale, I18nPlatform, I18nText}
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

object Implicits {

  implicit class I18nStringToCreateAdapter(val arg: I18nStringToCreate) extends AnyVal {
    def asDomain(createdAt: LocalDateTime): Try[dto.I18nStringToCreate] = Try {
      dto.I18nStringToCreate(
        key = I18nKey(arg.key.sanitize),
        text = I18nText(arg.text.sanitize),
        locale = I18nLocale(arg.locale.sanitize),
        platform = I18nPlatform(arg.platform.sanitize),
        `type` = arg.`type`.map(_.sanitize),
        explanation = arg.explanation.map(_.sanitize),
        createdAt = createdAt)
    }
  }

  implicit class I18nStringBulkCreateAdapter(val arg: I18nStringBulkCreate) extends AnyVal {
    def getDomainCreateDtos(createdAt: LocalDateTime): Try[Seq[dto.I18nStringToCreate]] = {
      arg.strings.map { toCreate ⇒
        toCreate.asDomain(createdAt)
      }.toList.sequence[Try, dto.I18nStringToCreate]
    }

    def getDomainLocale(): Try[I18nLocale] = Try {
      I18nLocale(arg.locale.sanitize)
    }
  }

  implicit class I18nStringToCriteriaAdapter(val arg: (Option[Int], Option[String], Option[String], Option[String], Option[String], Option[String], Set[String])) extends AnyVal {

    def asDomain: Try[dto.I18nStringCriteria] =
      Try(I18nStringCriteria(
        id = arg._1,
        key = arg._2.map(I18nKey),
        locale = arg._3.map(I18nLocale),
        platform = arg._4.map(I18nPlatform),
        `type` = arg._5,
        explanation = arg._6,
        partialMatchFields = arg._7))
  }

  implicit class I18nStringToUpdateAdapter(val arg: I18nStringToUpdate) extends AnyVal {
    def asDomain(doneAt: ZonedDateTime): Try[dto.I18nStringToUpdate] = Try {
      dto.I18nStringToUpdate(
        key = arg.key.map(I18nKey),
        text = arg.text.map(I18nText),
        locale = arg.locale.map(I18nLocale),
        platform = arg.platform.map(I18nPlatform),
        `type` = arg.`type`,
        explanation = arg.explanation,
        updatedAt = doneAt.toLocalDateTimeUTC,
        lastUpdatedAt = arg.lastUpdatedAt.map(_.toLocalDateTimeUTC))
    }
  }
}
