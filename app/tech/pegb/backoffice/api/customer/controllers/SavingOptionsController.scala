package tech.pegb.backoffice.api.customer.controllers

import java.util.UUID

import com.google.inject.ImplementedBy
import io.swagger.annotations._
import play.api.mvc.{Action, AnyContent}
import tech.pegb.backoffice.api.Routable
import tech.pegb.backoffice.api.swagger.model.SavingOptionsToRead

@ImplementedBy(classOf[impl.SavingOptionsController])
@Api(value = "Saving Options", produces = "application/json", consumes = "application/json")
trait SavingOptionsController extends Routable {
  def getRoute: String = "saving_options"
  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[Array[SavingOptionsToRead]], message = "")))
  def getSavingOptionsByCustomerId(id: UUID, status: Option[String]): Action[AnyContent]

  @ApiResponses(
    Array(
      new ApiResponse(code = 200, response = classOf[SavingOptionsToRead], message = "")))
  @ApiImplicitParams(Array(
    new ApiImplicitParam(
      value = "",
      required = true,
      dataType = "tech.pegb.backoffice.api.swagger.model.GenericRequestWithUpdatedAt",
      example = "",
      paramType = "body",
      name = "GenericRequestWithUpdatedAt")))
  def deactivateSavingOption(customerId: UUID, id: UUID): Action[String]
}
