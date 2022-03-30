package tech.pegb.backoffice.api.swagger.model

case class ParameterPaginatedResults(
    total: Int,
    limit: Int,
    offset: Int,
    results: Array[tech.pegb.backoffice.api.swagger.model.ParameterToRead])
