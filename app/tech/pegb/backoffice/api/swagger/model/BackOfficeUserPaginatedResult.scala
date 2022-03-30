package tech.pegb.backoffice.api.swagger.model

case class BackOfficeUserPaginatedResult(
    total: Int,
    limit: Int,
    offset: Int,
    results: Array[tech.pegb.backoffice.api.auth.dto.BackOfficeUserToRead])
