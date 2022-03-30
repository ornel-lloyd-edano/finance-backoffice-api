package tech.pegb.backoffice.api.makerchecker.dto

trait TaskCreatedI {
  val id: String
  val link: String
}

case class TaskCreated(id: String, link: String) extends TaskCreatedI {

}
