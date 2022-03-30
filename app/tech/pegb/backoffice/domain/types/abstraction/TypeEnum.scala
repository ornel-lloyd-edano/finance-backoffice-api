package tech.pegb.backoffice.domain.types.abstraction

trait TypeEnum {
  def kind: String
  def isUnknown: Boolean
}
