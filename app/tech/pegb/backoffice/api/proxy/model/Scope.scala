package tech.pegb.backoffice.api.proxy.model

case class Scope(parent: String) {
  val create: String = s"${parent}_create"

  val detail: String = s"${parent}_detail"

  val update: String = s"${parent}_edit"

  val delete: String = s"${parent}_delete"

  def byType: Scope.Type ⇒ String = {
    case Scope.Parent ⇒ parent
    case Scope.Create ⇒ create
    case Scope.Detail ⇒ detail
    case Scope.Update ⇒ update
    case Scope.Delete ⇒ delete
  }
}

object Scope {
  sealed trait Type

  case object Parent extends Type

  case object Create extends Type

  case object Detail extends Type

  case object Update extends Type

  case object Delete extends Type
}
