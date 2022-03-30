package tech.pegb.backoffice.domain.model

import java.time.LocalDateTime
import tech.pegb.backoffice.util.Implicits._

trait CreateUpdateValidation {
  def isValidCreatedBy(createdBy: String): Boolean = {
    createdBy.hasSomething
  }

  def isValidUpdatedBy(updatedBy: Option[String]): Boolean = {
    if (updatedBy.isDefined) updatedBy.get.hasSomething else true
  }

  def isValidUpdatedAt(updatedAt: Option[LocalDateTime], createdAt: LocalDateTime): Boolean = {
    if (updatedAt.isDefined) {
      !updatedAt.get.isBefore(createdAt)
    } else {
      true
    }
  }

  def isValidUpdate(updatedBy: Option[String], updatedAt: Option[LocalDateTime]): Boolean = {
    (updatedBy, updatedAt) match {
      case (Some(updatedBy), Some(updatedAt)) ⇒ true
      case (None, None) ⇒ true
      case _ ⇒ false
    }
  }
}
