package tech.pegb.backoffice.domain.businessuserapplication.dto

import java.time.{LocalDate, LocalDateTime}
import java.util.UUID

import tech.pegb.backoffice.domain.BaseService.ServiceResponse
import tech.pegb.backoffice.domain.Validatable
import tech.pegb.backoffice.domain.businessuserapplication.model.BusinessUserApplicationAttributes._
import tech.pegb.backoffice.domain.businessuserapplication.model.{BusinessType, BusinessUserTier}
import tech.pegb.backoffice.domain.customer.model.CustomerAttributes.NameAttribute

case class BusinessUserApplicationToCreate(
    uuid: UUID,
    businessName: NameAttribute,
    brandName: NameAttribute,
    businessCategory: BusinessCategory,
    userTier: BusinessUserTier,
    businessType: BusinessType,
    registrationNumber: RegistrationNumber,
    taxNumber: Option[TaxNumber],
    registrationDate: Option[LocalDate],
    createdBy: String,
    createdAt: LocalDateTime) extends Validatable[Unit] {

  override def validate: ServiceResponse[Unit] = {
    for {
      _ ← userTier.validate
      _ ← businessType.validate
    } yield {
      ()
    }
  }
}
