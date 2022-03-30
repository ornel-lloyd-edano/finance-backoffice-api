package tech.pegb.backoffice.dao.account.entity

import java.time.LocalDateTime

import org.coursera.autoschema.annotations._
import play.api.libs.json.Json

object AccountAttributes {

  case class AccountStatus(
      accountStatusName: String,
      description: String,
      isActive: Boolean,
      createdAt: LocalDateTime,
      createdBy: String,
      updatedAt: Option[LocalDateTime],
      updatedBy: Option[String])

  case class AccountType(
      id: Int,
      accountTypeName: String,
      description: Option[String],
      isActive: Boolean,
      @Term.Hide createdAt: LocalDateTime,
      @Term.Hide createdBy: String,
      @Term.Hide updatedAt: Option[LocalDateTime],
      @Term.Hide updatedBy: Option[String])

  object AccountType {
    implicit val f = Json.format[AccountType]
  }

}

