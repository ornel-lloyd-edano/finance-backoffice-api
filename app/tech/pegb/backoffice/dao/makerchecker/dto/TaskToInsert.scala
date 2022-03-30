package tech.pegb.backoffice.dao.makerchecker.dto

import java.time.LocalDateTime

import tech.pegb.backoffice.dao.makerchecker.entity.MakerCheckerTask

case class TaskToInsert(
    uuid: String,
    module: String,
    action: String,
    verb: String,
    url: String,
    headers: String,
    body: Option[String],
    valueToUpdate: Option[String] = None,
    status: String,
    createdBy: String,
    createdAt: LocalDateTime,
    makerLevel: Int,
    makerBusinessUnit: String,
    checkedBy: Option[String],
    checkedAt: Option[LocalDateTime]) {

  def asEntity(id: Int) = {
    new MakerCheckerTask(
      id = id,
      uuid = uuid,
      module = module,
      action = action,
      verb = verb,
      url = url,
      headers = headers,
      body = body,
      valueToUpdate = valueToUpdate,
      status = status,
      createdBy = createdBy,
      createdAt = createdAt,
      makerLevel = makerLevel,
      makerBusinessUnit = makerBusinessUnit,
      checkedBy = checkedBy,
      checkedAt = checkedAt,
      reason = None,
      updatedAt = None //replace with true value
    )
  }
}
