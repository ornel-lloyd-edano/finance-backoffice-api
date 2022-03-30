package tech.pegb.backoffice.domain.businessuserapplication.abstraction

object ContactTypes {
  lazy val toSeq = Seq(Owner, Assoc, Employee)
  val Owner = "business_owner"
  val Assoc = "associate"
  val Employee = "empoyee"
}
