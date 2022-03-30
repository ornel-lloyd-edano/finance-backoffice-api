package tech.pegb.backoffice.dao.makerchecker.dto

import java.time.LocalDateTime

case class TaskToUpdate(
    module: Option[String] = None,
    action: Option[String] = None,
    verb: Option[String] = None,
    url: Option[String] = None,
    headers: Option[String] = None,
    body: Option[String] = None,
    status: Option[String] = None,
    createdBy: Option[String] = None,
    createdAt: Option[LocalDateTime] = None,
    makerLevel: Option[Int] = None,
    makerBusinessUnit: Option[String] = None,
    checkedBy: Option[String] = None,
    checkedAt: Option[LocalDateTime] = None,
    reason: Option[String] = None) {

}
