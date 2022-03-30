package tech.pegb.backoffice.api.reportsv2.dto

case class ReportRoutes(
    name: String = "reporting",
    title: String = "reporting",
    key: String = "reporting",
    routes: Seq[ReportResource])
