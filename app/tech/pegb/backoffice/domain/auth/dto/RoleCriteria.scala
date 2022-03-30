package tech.pegb.backoffice.domain.auth.dto

import java.util.UUID

case class RoleCriteria(id: Option[UUID] = None, name: Option[String] = None, level: Option[Int] = None) {

}
