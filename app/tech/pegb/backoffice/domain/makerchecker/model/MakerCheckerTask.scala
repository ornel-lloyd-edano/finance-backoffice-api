package tech.pegb.backoffice.domain.makerchecker.model

import java.time.LocalDateTime
import java.util.UUID

import scala.collection.JavaConverters._
import com.fasterxml.jackson.databind.JsonNode
import tech.pegb.backoffice.util.Implicits._

import scala.util.Try

case class MakerCheckerTask(
    id: UUID,
    module: String,
    actionRequired: String,
    maker: MakerDetails,
    makerRequest: MakerRequest,
    status: Status,
    reason: Option[String],
    checker: Option[CheckerDetails] = None,
    updatedAt: Option[LocalDateTime] = None,

    change: Option[JsonNode] = None,
    current: Option[JsonNode] = None,
    original: Option[JsonNode] = None) {

  assert(module.hasSomething, "module cannot be empty")
  assert(actionRequired.hasSomething, "action cannot be empty")

  assert(if (status.isChecked) checker.nonEmpty else checker.isEmpty, s"Status cannot be ${status} because checker details are present")

  def isCheckerAllowed(checkerName: String, checkerLevel: RoleLevel, checkerBusinessUnit: String): Boolean = {
    if (checkerName === maker.createdBy) { //User cannot check his own tasks
      false
    } else if (checkerLevel.isHighestLevel) { // CA or Highest Level can check everything
      true
    } else if (checkerLevel.isDepartmentApproverLevel) { //DA can only approve tasks on same level(DA level 1) and below, and on the same business_unit
      checkerLevel.isSameOrHigherThan(maker.level) && checkerBusinessUnit === maker.businessUnit
    } else { // Normal level can only approve tasks below his level, and on the same business_unti
      checkerLevel.isHigherThan(maker.level) && checkerBusinessUnit === maker.businessUnit
    }
  }

  def isStale: Option[Boolean] = {
    //true whenever the updated_at in the task body is different than the current_value one if any else false

    val makerRequestBodyUpdatedAt = makerRequest.body.flatMap(_.fields.find(_._1 == "updated_at").flatMap(_._2.asOpt[String]))

    val currentValueUpdatedAt = current.flatMap(_.fields().asScala.map(e ⇒ e.getKey → e.getValue)
      .find(_._1 == "updated_at").flatMap { case (_, upd) ⇒ Try(upd.asText()).toOption })

    (makerRequestBodyUpdatedAt, currentValueUpdatedAt) match {
      case (Some(makerRequestBodyUpdatedAt), Some(currentValueUpdatedAt)) ⇒

        Some(makerRequestBodyUpdatedAt.trim.replace("\"", "") != currentValueUpdatedAt.trim.replace("\"", ""))
      case (Some(makerRequestBodyUpdatedAt), _) ⇒
        Some(false)
      case (_, Some(currentValueUpdatedAt)) ⇒
        Some(false)
      case _ ⇒ Some(false)
    }
  }

}

object MakerCheckerTask {
  val statusOnCreate = "pending"
}
