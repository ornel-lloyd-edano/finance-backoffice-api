package tech.pegb.backoffice.dao.report.entity

import play.api.libs.json.JsValue

case class Report(
    count: Long,
    result: Seq[JsValue])
