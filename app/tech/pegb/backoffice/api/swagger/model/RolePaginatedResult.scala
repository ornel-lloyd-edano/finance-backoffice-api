package tech.pegb.backoffice.api.swagger.model

class RolePaginatedResult(
    total: Int,
    limit: Int,
    offset: Int,
    result: Array[tech.pegb.backoffice.api.auth.dto.RoleToRead])
