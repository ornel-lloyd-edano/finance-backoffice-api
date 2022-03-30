package tech.pegb.backoffice.domain.businessuserapplication.abstraction

object Status {

  lazy val toSeq = Seq(Ongoing, Pending, Cancelled, Rejected, Approved)

  val Ongoing = "ongoing"
  val Pending = "pending" //aka Submitted
  val Cancelled = "cancelled"
  val Rejected = "rejected"
  val Approved = "approved"

}
