package tech.pegb.backoffice.api.businessuserapplication.dto

import java.time.LocalDate

case class BusinessUserApplicationToCreate(
    businessName: String,
    brandName: String,
    businessCategory: String,
    userTier: String,
    businessType: String,
    registrationNumber: String,
    taxNumber: Option[String],
    registrationDate: Option[LocalDate]) extends BusinessUserApplicationToCreateT

trait BusinessUserApplicationToCreateT {
  def businessName: String
  def brandName: String
  def businessCategory: String
  def userTier: String
  def businessType: String
  def registrationNumber: String
  def taxNumber: Option[String]
  def registrationDate: Option[LocalDate]
}
