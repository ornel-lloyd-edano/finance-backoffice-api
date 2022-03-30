package tech.pegb.backoffice.api.auth.dto

import java.util.UUID

case class ScopeToCreate(
    name: String,
    parentId: Option[UUID],
    description: Option[String] = None) extends ScopeToCreateT

trait ScopeToCreateT {
  def name: String
  def parentId: Option[UUID]
  def description: Option[String]
}
