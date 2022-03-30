package tech.pegb.backoffice.dao.customer.abstraction

import java.util.UUID

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.dao.Dao
import tech.pegb.backoffice.dao.customer.dto.PaymentOptionDto
import tech.pegb.backoffice.dao.customer.sql.PaymentOptionSqlDao

@ImplementedBy(classOf[PaymentOptionSqlDao])
trait PaymentOptionDao extends Dao {

  def fetchPaymentOptions(customerId: UUID): Dao.DaoResponse[Seq[PaymentOptionDto]]

}
