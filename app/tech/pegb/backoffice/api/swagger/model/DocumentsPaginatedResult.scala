package tech.pegb.backoffice.api.swagger.model

case class DocumentsPaginatedResult(
    total: Int,
    limit: Int,
    offset: Int,
    results: Array[tech.pegb.backoffice.api.document.dto.DocumentToRead])
