package tech.pegb.backoffice.domain.report.abstraction

import java.util.UUID

trait CustomReportDefinition {
  val id: UUID
  val name: String
  val title: String
  val description: String

  def isUniqueReport: Boolean

}
