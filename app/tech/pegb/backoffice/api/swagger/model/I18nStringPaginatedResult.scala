package tech.pegb.backoffice.api.swagger.model

case class I18nStringPaginatedResult(
    total: Int,
    limit: Int,
    offset: Int,
    result: Array[tech.pegb.backoffice.api.i18n.dto.I18nStringToRead])
