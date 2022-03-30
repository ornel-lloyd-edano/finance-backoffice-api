package tech.pegb.backoffice.domain

import tech.pegb.backoffice.dao.types.abstraction.TypesDao
import tech.pegb.backoffice.domain.BaseService.ServiceResponse

trait FieldValueValidation {

  val typesDao: TypesDao

  def validateFieldsFromKnownTypes(
    fieldvalue: String,
    fieldName: String): ServiceResponse[String]

}
