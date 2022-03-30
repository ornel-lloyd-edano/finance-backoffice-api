package tech.pegb.backoffice.domain.report.model

import play.api.libs.json.JsValue

case class Report(
    count: Long,
    result: Seq[JsValue])
