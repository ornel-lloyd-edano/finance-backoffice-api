package tech.pegb.backoffice.api.swagger.model

import tech.pegb.backoffice.api.makerchecker.dto.TaskToRead

case class TaskToReadPaginatedResults(
    total: Int,
    limit: Option[Int],
    offset: Option[Int],
    results: Array[TaskToRead]) {

}
