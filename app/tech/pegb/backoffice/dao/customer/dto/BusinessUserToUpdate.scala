package tech.pegb.backoffice.dao.customer.dto

import java.time.LocalDateTime

case class BusinessUserToUpdate(
    name: Option[String] = None,
    updatedAt: LocalDateTime,
    updatedBy: String)
