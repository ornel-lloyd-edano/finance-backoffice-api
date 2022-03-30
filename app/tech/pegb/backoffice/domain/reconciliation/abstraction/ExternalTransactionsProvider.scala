package tech.pegb.backoffice.domain.reconciliation.abstraction

import java.time.LocalDateTime

import com.google.inject.ImplementedBy
import tech.pegb.backoffice.domain.reconciliation.model.{ParsingError, ThirdPartyTransaction}
import tech.pegb.backoffice.domain.reconciliation.implementation

@ImplementedBy(classOf[implementation.ExternalTransactionsProvider])
trait ExternalTransactionsProvider {

  def getExternalTransactions(
    thirdParty: String,
    source: Option[String],
    startDate: LocalDateTime,
    endDate: LocalDateTime): Seq[ThirdPartyTransaction]

  def getParser(key: String): Option[String ⇒ Either[ParsingError, ThirdPartyTransaction]]

  def addParser(key: String, parser: String ⇒ Either[ParsingError, ThirdPartyTransaction]): Boolean

  def addExternalSource(key: String, source: Seq[(String, String)] ⇒ Seq[String])

  def getExternalSource(key: String): Option[Seq[(String, String)] ⇒ Seq[String]]
}
