package tech.pegb.backoffice.util

import tech.pegb.backoffice.dao.model.MatchType

trait CriteriaWriter {

  def writeCriteria(aliasedField: String, realValue: String, operator: MatchType): String
}
