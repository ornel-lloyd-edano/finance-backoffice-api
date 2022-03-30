package tech.pegb.backoffice.domain.businessuserapplication.abstraction

object Stages {

  lazy val toSeq = Seq(Identity, Config, Contact, Docs)

  val Identity = "identity_info"
  val Config = "config"
  val Contact = "contact_info"
  val Docs = "documents"

}
