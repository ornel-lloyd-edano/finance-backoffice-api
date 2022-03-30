package tech.pegb.backoffice.mapping.dao.domain.makerchecker

import java.util.UUID

import play.api.libs.json.{JsObject, Json}
import tech.pegb.backoffice.dao.makerchecker.entity.MakerCheckerTask
import tech.pegb.backoffice.domain.makerchecker.model._
import tech.pegb.backoffice.domain.makerchecker.{model ⇒ domain}
import tech.pegb.backoffice.api.json.Implicits._

import scala.util.Try
object Implicits {
  import domain.Statuses._

  implicit class MakerCheckerTaskDaoAdapter(val arg: MakerCheckerTask) extends AnyVal {

    def asDomain(currentValue: Option[String] = None) = Try {
      domain.MakerCheckerTask(
        id = UUID.fromString(arg.uuid),
        module = arg.module,
        actionRequired = arg.action,
        maker = MakerDetails(
          createdBy = arg.createdBy,
          createdAt = arg.createdAt,
          level = RoleLevels(arg.makerLevel),
          businessUnit = arg.makerBusinessUnit),
        makerRequest = MakerRequest(
          verb = arg.verb,
          url = arg.url,
          queryParams = None,
          body = arg.body.map(Json.parse(_).as[JsObject]),
          headers = Json.parse(arg.headers).as[JsObject]),
        status = arg.status.asDomain,
        reason = arg.reason,
        checker = arg.checkedBy.flatMap(cb ⇒ arg.checkedAt.map(ca ⇒ (cb, ca))).map(cbca ⇒
          CheckerDetails(
            checkedBy = cbca._1,
            checkedAt = cbca._2)),
        updatedAt = arg.updatedAt,

        change = arg.body.map(_.asJsNode), //TODO delta of body and current value
        original = arg.valueToUpdate.map(_.asJsNode),
        current = currentValue.map(_.asJsNode))
    }
  }

}
