package tech.pegb.backoffice.api.swagger.model

case class PermissionPaginatedResult(
    total: Int,
    limit: Int,
    offset: Int,
    result: Array[tech.pegb.backoffice.api.auth.dto.PermissionToRead])
