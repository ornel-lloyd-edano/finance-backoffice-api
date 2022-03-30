package tech.pegb.backoffice.dao.savings.entity

import java.time.LocalDateTime

trait SavingOption {
  val id: Int
  val uuid: String
  val userId: Int
  val userUuid: String
  val accountId: Int
  val accountUuid: String
  val currentAmount: BigDecimal
  val createdAt: LocalDateTime
  val currency: String
  val updatedAt: LocalDateTime
}
