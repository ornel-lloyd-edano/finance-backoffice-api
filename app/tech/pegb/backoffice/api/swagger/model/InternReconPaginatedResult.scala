package tech.pegb.backoffice.api.swagger.model

case class InternReconPaginatedResult(
    total: Int,
    limit: Int,
    offset: Int,
    results: Array[tech.pegb.backoffice.api.recon.dto.InternReconResultToRead])
