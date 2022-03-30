package tech.pegb.backoffice.api.model

import play.api.libs.json.{Json, Writes}

case class PaginatedResult[T](total: Long, results: Iterable[T], limit: Option[Int], offset: Option[Int])

object PaginatedResult {
  implicit def paginatedResultWrites[T](implicit fmt: Writes[T]): Writes[PaginatedResult[T]] = new Writes[PaginatedResult[T]] {
    def writes(dto: PaginatedResult[T]) = Json.obj(
      "total" → dto.total,
      "results" → dto.results,
      "limit" → dto.limit,
      "offset" → dto.offset)
  }
}
