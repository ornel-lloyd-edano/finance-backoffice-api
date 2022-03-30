package tech.pegb.backoffice.domain.parameter.abstraction

import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.model
import tech.pegb.backoffice.domain.parameter.dto.{ParameterCriteria, ParameterToCreate, ParameterToUpdate}
import tech.pegb.backoffice.domain.parameter.implementation.ParameterMgmtService
import tech.pegb.backoffice.domain.parameter.model.{MetadataSchema, Parameter}

import scala.concurrent.Future

@ImplementedBy(classOf[ParameterMgmtService])
trait ParameterManagement {

  def getParameterUUIDPrefix(metadataId: String): Future[ServiceResponse[Int]]

  def getMetadataSchemaById(metadataId: String): Future[ServiceResponse[MetadataSchema]]

  def getMetadataSchema: Future[ServiceResponse[Seq[(String, MetadataSchema)]]]

  def convertIdToParameterUUID(metadataId: String, actualDbId: Int): Future[ServiceResponse[UUID]]

  def convertParameterUUIDToId(metadataId: String, uuid: UUID): Future[ServiceResponse[Int]]

  def createParameter(createDto: ParameterToCreate): Future[ServiceResponse[Parameter]]

  def getParameters: Future[ServiceResponse[Seq[Parameter]]]

  def filterParametersByCriteria(parameters: Seq[Parameter], criteriaDto: ParameterCriteria, ordering: Seq[model.Ordering],
    limit: Option[Int], offset: Option[Int]): Future[ServiceResponse[Seq[Parameter]]]

  def getLatestVersion(baseParameters: Seq[Parameter]): Future[ServiceResponse[Option[String]]]

  def countParametersByCriteria(baseParameters: Seq[Parameter], criteriaDto: ParameterCriteria): Future[ServiceResponse[Int]]

  def updateParameter(id: UUID, updateDto: ParameterToUpdate): Future[ServiceResponse[Parameter]]

}
