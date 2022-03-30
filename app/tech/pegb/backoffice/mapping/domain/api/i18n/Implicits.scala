package tech.pegb.backoffice.mapping.domain.api.i18n

import tech.pegb.backoffice.api.i18n.dto.{I18nPairToRead, I18nStringBulkResponse, I18nStringToRead}
import tech.pegb.backoffice.util.Implicits._
import tech.pegb.backoffice.domain.i18n.model.{I18nBulkInsertResult, I18nPair, I18nString}

object Implicits {

  implicit class I18nStringToReadAdapter(val arg: I18nString) extends AnyVal {
    def asApi: I18nStringToRead = {
      I18nStringToRead(
        id = arg.id,
        key = arg.key.underlying,
        text = arg.text.underlying,
        locale = arg.locale.underlying,
        platform = arg.platform.underlying,
        `type` = arg.`type`,
        explanation = arg.explanation,
        createdAt = arg.createdAt.toZonedDateTimeUTC,
        updatedAt = arg.updatedAt.map(_.toZonedDateTimeUTC))
    }
  }

  implicit class I18nBulkCreateResultAdapter(val arg: I18nBulkInsertResult) extends AnyVal {
    def asApi: I18nStringBulkResponse = {
      I18nStringBulkResponse(
        insertedCount = arg.insertedRowCount,
        updatedCount = arg.updatedRowCount)
    }
  }

  implicit class I18nPairToReadAdapter(val arg: I18nPair) extends AnyVal {
    def asApi = {
      I18nPairToRead(
        key = arg.key.underlying,
        text = arg.text.underlying)
    }
  }
}
