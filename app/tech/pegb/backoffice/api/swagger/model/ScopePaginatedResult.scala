package tech.pegb.backoffice.api.swagger.model

case class ScopePaginatedResult(
    total: Int,
    limit: Int,
    offset: Int,
    result: Array[tech.pegb.backoffice.api.auth.dto.ScopeToRead])
